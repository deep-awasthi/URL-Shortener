package com.deepawasthi.URLShortener.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    @Value("${ratelimit.requests-per-second:10}")
    private int requestsPerSecond;

    @Value("${ratelimit.redirects-per-minute:60}")
    private int redirectsPerMinute;

    private final ConcurrentHashMap<String, Bucket> apiBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> redirectBuckets = new ConcurrentHashMap<>();

    public boolean allowApiRequest(String clientIp) {
        Bucket bucket = apiBuckets.computeIfAbsent(clientIp, k -> newBucket(requestsPerSecond, Duration.ofSeconds(1)));
        return bucket.tryConsumeAndReturnRemaining(1).isConsumed();
    }

    public boolean allowRedirect(String shortCode) {
        Bucket bucket = redirectBuckets.computeIfAbsent(shortCode, k -> newBucket(redirectsPerMinute, Duration.ofMinutes(1)));
        return bucket.tryConsumeAndReturnRemaining(1).isConsumed();
    }

    private Bucket newBucket(long capacity, Duration period) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, period)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
