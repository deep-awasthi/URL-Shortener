package com.deepawasthi.URLShortener.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates collision-resistant identifiers using a combination of
 * ULID-style time-prefix and cryptographic randomness.
 * Format: 48-bit ms timestamp (Base32) + 80-bit random (Base32) = 26 chars.
 */
@Service
public class IdGeneratorService {

    private static final char[] BASE32 = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final AtomicLong SUB_MS_COUNTER = new AtomicLong(0);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long MAX_SUB_MS = 0xFFFL; // 12 bits = 4096 values per ms

    public String generateULID() {
        long nowMs = Instant.now().toEpochMilli();
        long subMs = SUB_MS_COUNTER.getAndUpdate(v -> (v + 1) > MAX_SUB_MS ? 0 : v + 1);
        long timeBits = (nowMs << 12) | (subMs & MAX_SUB_MS);

        byte[] randomBytes = new byte[10];
        SECURE_RANDOM.nextBytes(randomBytes);

        return encodeTime(timeBits) + encodeRandom(randomBytes);
    }

    private String encodeTime(long timeBits) {
        char[] chars = new char[10];
        for (int i = 9; i >= 0; i--) {
            chars[i] = BASE32[(int)(timeBits & 0x1F)];
            timeBits >>>= 5;
        }
        return new String(chars);
    }

    private String encodeRandom(byte[] bytes) {
        long value = 0;
        for (byte b : bytes) {
            value = (value << 8) | (b & 0xFF);
        }
        char[] chars = new char[16];
        for (int i = 15; i >= 0; i--) {
            chars[i] = BASE32[(int)(value & 0x1F)];
            value >>>= 5;
        }
        return new String(chars);
    }
}
