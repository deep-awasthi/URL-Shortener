package com.deepawasthi.URLShortener.service;

import com.deepawasthi.URLShortener.model.AnalyticsEvent;
import com.deepawasthi.URLShortener.model.Url;
import com.deepawasthi.URLShortener.model.UrlDto;
import com.deepawasthi.URLShortener.model.UrlSearchDocument;
import com.deepawasthi.URLShortener.repository.UrlRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UrlServiceImpl implements UrlService {

    private static final Logger log = LoggerFactory.getLogger(UrlServiceImpl.class);
    private static final char[] BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private final UrlRepository urlRepository;
    private final IdGeneratorService idGenerator;
    private final BloomFilterService bloomFilter;
    private final AnalyticsService analyticsService;
    private final UrlSearchService searchService;
    private final RateLimitService rateLimitService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${urlshortener.short-code-length:10}")
    private int shortCodeLength;

    public UrlServiceImpl(UrlRepository urlRepository,
                          IdGeneratorService idGenerator,
                          BloomFilterService bloomFilter,
                          AnalyticsService analyticsService,
                          UrlSearchService searchService,
                          RateLimitService rateLimitService) {
        this.urlRepository = urlRepository;
        this.idGenerator = idGenerator;
        this.bloomFilter = bloomFilter;
        this.analyticsService = analyticsService;
        this.searchService = searchService;
        this.rateLimitService = rateLimitService;
    }

    @Override
    @Transactional
    public Url generateShortLink(UrlDto urlDto) {
        if (urlDto == null || !StringUtils.hasText(urlDto.getUrl())) {
            throw new IllegalArgumentException("URL is required.");
        }

        String originalUrl = normalizeUrl(urlDto.getUrl());
        String shortCode = StringUtils.hasText(urlDto.getCustomCode())
                ? urlDto.getCustomCode()
                : generateUniqueShortCode();

        if (urlRepository.existsByShortCode(shortCode)) {
            throw new IllegalArgumentException("Short code already in use.");
        }

        LocalDateTime now = LocalDateTime.now();
        Url url = new Url(
                idGenerator.generateULID(),
                originalUrl,
                shortCode,
                now,
                getExpirationDate(urlDto.getExpirationDate(), now));

        Url saved = urlRepository.save(url);
        bloomFilter.put(saved.getShortCode());
        searchService.index(saved);
        return saved;
    }

    @Override
    @Cacheable(value = "urls", key = "#shortCode")
    @CircuitBreaker(name = "urlLookup", fallbackMethod = "fallbackLookup")
    @Retry(name = "dbRetry")
    public Url getEncodedUrl(String shortCode) {
        if (!bloomFilter.mightContain(shortCode)) {
            log.debug("Bloom filter negative for {}", shortCode);
            return null;
        }
        return urlRepository.findByShortCode(shortCode).orElse(null);
    }

    public Url fallbackLookup(String shortCode, Throwable t) {
        log.warn("Circuit breaker fallback for {}: {}", shortCode, t.getMessage());
        return urlRepository.findByShortCode(shortCode).orElse(null);
    }

    @Override
    public List<Url> getAllUrls() {
        return urlRepository.findAll();
    }

    @Override
    @CacheEvict(value = "urls", key = "#shortCode")
    @Transactional
    public void deleteShortLink(String shortCode) {
        urlRepository.findByShortCode(shortCode).ifPresent(url -> {
            urlRepository.delete(url);
            searchService.removeIndex(url.getId());
        });
    }

    @Override
    @CircuitBreaker(name = "elasticsearch", fallbackMethod = "fallbackSearch")
    public List<Url> searchUrls(String query) {
        return searchService.search(query).stream()
                .map(doc -> new Url(doc.getId(), doc.getOriginalUrl(), doc.getShortCode(),
                        doc.getCreationDate(), doc.getExpirationDate()))
                .collect(Collectors.toList());
    }

    public List<Url> fallbackSearch(String query, Throwable t) {
        log.warn("Elasticsearch fallback for query '{}': {}", query, t.getMessage());
        return Collections.emptyList();
    }

    @Transactional
    public void recordClick(String shortCode, String originalUrl,
                            String ipAddress, String userAgent, String referer) {
        analyticsService.recordClick(new AnalyticsEvent(
                shortCode, originalUrl, LocalDateTime.now(),
                ipAddress, userAgent, referer));
        urlRepository.incrementClickCount(shortCode);
    }

    private String normalizeUrl(String url) {
        try {
            URI uri = new URI(url.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException("Only absolute http and https URLs are supported.");
            }
            return uri.normalize().toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL.");
        }
    }

    private String generateUniqueShortCode() {
        int length = Math.max(6, shortCodeLength);
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = randomBase62(length + (attempt / 5));
            if (!urlRepository.existsByShortCode(code) && bloomFilter.mightContain(code)) {
                // Bloom says it might exist — try again
            } else if (!urlRepository.existsByShortCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to generate a unique short code.");
    }

    private String randomBase62(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(BASE62[secureRandom.nextInt(BASE62.length)]);
        }
        return sb.toString();
    }

    private LocalDateTime getExpirationDate(String expirationDate, LocalDateTime creationDate) {
        if (!StringUtils.hasText(expirationDate)) {
            return creationDate.plusDays(30);
        }
        try {
            return LocalDateTime.parse(expirationDate);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(expirationDate).atStartOfDay();
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(
                        "expirationDate must be ISO-8601, e.g. 2026-12-31 or 2026-12-31T23:59:59.");
            }
        }
    }
}
