package com.aegis.meshpay.model;

import java.math.BigDecimal;

public record PaymentInstruction(
        String voucherId,
        long seqNo,
        String senderVpa,
        String receiverVpa,
        BigDecimal amount,
        String pin,
        String nonce,
        long timestamp,
        String devicePublicKey,
        String deviceSignature
) {
    public String getSigningPayload() {
        return (voucherId != null ? voucherId : "NONE") + ":" +
                seqNo + ":" +
                senderVpa + ":" +
                receiverVpa + ":" +
                amount.toPlainString() + ":" +
                nonce + ":" +
                timestamp;
    }
}
