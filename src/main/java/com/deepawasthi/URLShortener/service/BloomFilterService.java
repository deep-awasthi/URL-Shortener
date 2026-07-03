package com.deepawasthi.URLShortener.service;

import com.deepawasthi.URLShortener.model.Url;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * In-memory Bloom filter that short-circuits lookups for short codes
 * that were never created. Populated on startup from the database.
 * False positives are harmless — they just hit the DB.
 */
@Service
public class BloomFilterService {

    private static final Logger log = LoggerFactory.getLogger(BloomFilterService.class);
    private static final int EXPECTED_INSERTIONS = 5_000_000;
    private static final double FPP = 0.001;

    private BloomFilter<String> bloomFilter;

    private final JpaRepository<Url, String> urlRepository;

    public BloomFilterService(JpaRepository<Url, String> urlRepository) {
        this.urlRepository = urlRepository;
    }

    @PostConstruct
    public void warmUp() {
        bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                EXPECTED_INSERTIONS,
                FPP);
        List<Url> allUrls = urlRepository.findAll();
        for (Url url : allUrls) {
            bloomFilter.put(url.getShortCode());
        }
        log.info("Bloom filter warmed with {} entries", allUrls.size());
    }

    public boolean mightContain(String shortCode) {
        return bloomFilter.mightContain(shortCode);
    }

    public void put(String shortCode) {
        bloomFilter.put(shortCode);
    }
}
