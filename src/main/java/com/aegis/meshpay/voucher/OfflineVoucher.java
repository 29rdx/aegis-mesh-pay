package com.aegis.meshpay.voucher;

import java.math.BigDecimal;

public record OfflineVoucher(
        String voucherId,
        String accountVpa,
        BigDecimal maxAllowance,
        long issuedAt,
        long expiresAt,
        String bankSignature
) {
    public String getSigningPayload() {
        return voucherId + ":" + accountVpa + ":" + maxAllowance.toPlainString() + ":" + issuedAt + ":" + expiresAt;
    }
}
