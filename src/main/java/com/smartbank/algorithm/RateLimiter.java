package com.smartbank.algorithm;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimiter {

    private final int maxTokens;
    private final long refillRateMs;
    private final ConcurrentHashMap<String, TokenBucket> buckets;

    public RateLimiter(int maxTokens, long refillRateMs) {
        this.maxTokens = maxTokens;
        this.refillRateMs = refillRateMs;
        this.buckets = new ConcurrentHashMap<>();
    }

    public boolean allowRequest(String key) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(maxTokens, refillRateMs));
        return bucket.tryConsume();
    }

    static class TokenBucket {
        private final int capacity;
        private final long refillRateMs;
        private final AtomicLong tokens;
        private volatile long lastRefillTimestamp;

        TokenBucket(int capacity, long refillRateMs) {
            this.capacity = capacity;
            this.refillRateMs = refillRateMs;
            this.tokens = new AtomicLong(capacity);
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long timePassed = now - lastRefillTimestamp;
            long tokensToAdd = timePassed / refillRateMs;

            if (tokensToAdd > 0) {
                tokens.set(Math.min(capacity, tokens.get() + tokensToAdd));
                lastRefillTimestamp = now;
            }
        }
    }
}
