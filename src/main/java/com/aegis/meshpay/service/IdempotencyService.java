package com.aegis.meshpay.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotencyService {

    public enum Status {
        PROCESSING,
        SETTLED,
        FAILED
    }

    public record Entry(Status status, long timestamp, Long transactionId) {}

    private final Map<String, Entry> cache = new ConcurrentHashMap<>();
    private static final long TTL_MS = 24 * 3600 * 1000L; // 24-hour deduplication window

    public synchronized boolean tryAcquire(String packetHash) {
        if (cache.containsKey(packetHash)) {
            return false; // Duplicate
        }
        cache.put(packetHash, new Entry(Status.PROCESSING, System.currentTimeMillis(), null));
        return true;
    }

    public void markSettled(String packetHash, Long transactionId) {
        cache.put(packetHash, new Entry(Status.SETTLED, System.currentTimeMillis(), transactionId));
    }

    public void markFailed(String packetHash) {
        cache.put(packetHash, new Entry(Status.FAILED, System.currentTimeMillis(), null));
    }

    public void release(String packetHash) {
        cache.remove(packetHash);
    }

    public boolean isSeen(String packetHash) {
        return cache.containsKey(packetHash);
    }

    public void clear() {
        cache.clear();
    }

    @Scheduled(fixedRate = 3600000)
    public void evictExpired() {
        long cutoff = System.currentTimeMillis() - TTL_MS;
        cache.entrySet().removeIf(entry -> entry.getValue().timestamp() < cutoff);
    }
}
