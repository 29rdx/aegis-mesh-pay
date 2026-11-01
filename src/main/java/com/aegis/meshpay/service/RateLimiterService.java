package com.aegis.meshpay.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final int burstCapacity;
    private final int refillRatePerSecond;

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimiterService(
            @Value("${aegis.mesh.rate-limit.burst-capacity:50}") int burstCapacity,
            @Value("${aegis.mesh.rate-limit.refill-rate:10}") int refillRatePerSecond
    ) {
        this.burstCapacity = burstCapacity;
        this.refillRatePerSecond = refillRatePerSecond;
    }

    public synchronized boolean allowRequest(String key) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(burstCapacity, refillRatePerSecond));
        return bucket.tryConsume();
    }

    private static class TokenBucket {
        private final int capacity;
        private final int refillRate;
        private double tokens;
        private long lastRefillTimestamp;

        public TokenBucket(int capacity, int refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = capacity;
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            double secondsElapsed = (now - lastRefillTimestamp) / 1000.0;
            if (secondsElapsed > 0) {
                tokens = Math.min(capacity, tokens + secondsElapsed * refillRate);
                lastRefillTimestamp = now;
            }
        }
    }
}
