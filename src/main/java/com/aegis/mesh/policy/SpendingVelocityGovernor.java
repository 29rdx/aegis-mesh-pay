package com.aegis.mesh.policy;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class SpendingVelocityGovernor {
    private final BigDecimal maxSingleTransaction;
    private final BigDecimal cumulativeDailyLimit;
    private final ConcurrentHashMap<String, AtomicReference<BigDecimal>> userDailySpending = new ConcurrentHashMap<>();

    public SpendingVelocityGovernor(BigDecimal maxSingleTransaction, BigDecimal cumulativeDailyLimit) {
        this.maxSingleTransaction = maxSingleTransaction;
        this.cumulativeDailyLimit = cumulativeDailyLimit;
    }

    public boolean validateAndRecord(String userId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || amount.compareTo(maxSingleTransaction) > 0) {
            return false;
        }
        userDailySpending.putIfAbsent(userId, new AtomicReference<>(BigDecimal.ZERO));
        AtomicReference<BigDecimal> spentRef = userDailySpending.get(userId);
        while (true) {
            BigDecimal current = spentRef.get();
            BigDecimal next = current.add(amount);
            if (next.compareTo(cumulativeDailyLimit) > 0) {
                return false; // Limit exceeded
            }
            if (spentRef.compareAndSet(current, next)) {
                return true;
            }
        }
    }
}
