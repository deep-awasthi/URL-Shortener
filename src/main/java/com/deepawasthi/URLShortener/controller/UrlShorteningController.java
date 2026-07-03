package com.deepawasthi.URLShortener.controller;

import com.deepawasthi.URLShortener.model.Url;
import com.deepawasthi.URLShortener.model.UrlDto;
import com.deepawasthi.URLShortener.model.UrlErrorResponseDto;
import com.deepawasthi.URLShortener.model.UrlResponseDto;
import com.deepawasthi.URLShortener.service.RateLimitService;
import com.deepawasthi.URLShortener.service.UrlService;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
public class UrlShorteningController {

    private final UrlService urlService;
    private final RateLimitService rateLimitService;
    private final Counter redirectCounter;
    private final Counter createCounter;

    public UrlShorteningController(UrlService urlService,
                                   RateLimitService rateLimitService,
                                   MeterRegistry meterRegistry) {
        this.urlService = urlService;
        this.rateLimitService = rateLimitService;
        this.redirectCounter = Counter.builder("url.redirects")
                .description("Total redirect attempts").register(meterRegistry);
        this.createCounter = Counter.builder("url.created")
                .description("Total URLs created").register(meterRegistry);
    }

    @PostMapping({"/api/urls", "/generate"})
    @Timed(value = "url.create", description = "Time to create a short URL")
    public ResponseEntity<?> generateShortLink(@Valid @RequestBody UrlDto urlDto,
                                               HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        if (!rateLimitService.allowApiRequest(clientIp)) {
            return error(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Try again shortly.");
        }
        try {
            Url url = urlService.generateShortLink(urlDto);
            createCounter.increment();
            UrlResponseDto resp = new UrlResponseDto(
                    url.getId(),
                    url.getOriginalUrl(),
                    buildShortUrl(request, url.getShortCode()),
                    url.getCreationDate(),
                    url.getExpirationDate(),
                    url.getClickCount());
            return new ResponseEntity<>(resp, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/{shortCode:[A-Za-z0-9]{4,32}}")
    @Timed(value = "url.redirect", description = "Time to redirect")
    public ResponseEntity<?> redirectToOriginalUrl(@PathVariable String shortCode,
                                                   HttpServletRequest request) {
        if (!StringUtils.hasText(shortCode)) {
            return error(HttpStatus.BAD_REQUEST, "Invalid URL.");
        }

        if (!rateLimitService.allowRedirect(shortCode)) {
            return error(HttpStatus.TOO_MANY_REQUESTS, "Too many requests for this link.");
        }

        Url url = urlService.getEncodedUrl(shortCode);
        if (url == null) {
            return error(HttpStatus.NOT_FOUND, "URL does not exist or it might have expired.");
        }
        if (url.getExpirationDate().isBefore(LocalDateTime.now())) {
            urlService.deleteShortLink(shortCode);
            return error(HttpStatus.GONE, "URL expired. Please generate a fresh one.");
        }

        redirectCounter.increment();
        urlService.recordClick(
                shortCode,
                url.getOriginalUrl(),
                request.getRemoteAddr(),
                request.getHeader("User-Agent"),
                request.getHeader("Referer"));

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", url.getOriginalUrl())
                .build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }

    @GetMapping(value = "/api/urls", produces = "application/json")
    public ResponseEntity<List<Url>> listUrls() {
        return ResponseEntity.ok(urlService.getAllUrls());
    }

    @GetMapping("/api/urls/search")
    public ResponseEntity<List<Url>> searchUrls(@RequestParam String q) {
        return ResponseEntity.ok(urlService.searchUrls(q));
    }

    private ResponseEntity<UrlErrorResponseDto> error(HttpStatus status, String message) {
        UrlErrorResponseDto dto = new UrlErrorResponseDto();
        dto.setError(message);
        dto.setStatus(String.valueOf(status.value()));
        return new ResponseEntity<>(dto, status);
    }

    private String buildShortUrl(HttpServletRequest request, String shortCode) {
        return URI.create(request.getRequestURL().toString())
                .resolve("/" + shortCode)
                .toString();
    }
}
