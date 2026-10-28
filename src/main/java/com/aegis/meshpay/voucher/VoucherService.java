package com.aegis.meshpay.voucher;

import com.aegis.meshpay.crypto.ServerKeyHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VoucherService {

    private final ServerKeyHolder serverKeyHolder;
    private final Map<String, OfflineVoucher> activeVouchers = new ConcurrentHashMap<>();
    private final Map<String, Long> deviceSequenceCounters = new ConcurrentHashMap<>();

    public VoucherService(ServerKeyHolder serverKeyHolder) {
        this.serverKeyHolder = serverKeyHolder;
    }

    public OfflineVoucher issueVoucher(String vpa, BigDecimal allowanceAmount) {
        try {
            String voucherId = UUID.randomUUID().toString();
            long now = System.currentTimeMillis();
            long expiresAt = now + (7 * 24 * 3600 * 1000L); // 7 days validity

            String payload = voucherId + ":" + vpa + ":" + allowanceAmount.toPlainString() + ":" + now + ":" + expiresAt;
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(serverKeyHolder.getPrivateKey());
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            String bankSig = Base64.getEncoder().encodeToString(signature.sign());

            OfflineVoucher voucher = new OfflineVoucher(voucherId, vpa, allowanceAmount, now, expiresAt, bankSig);
            activeVouchers.put(voucherId, voucher);
            deviceSequenceCounters.put(vpa, 0L);
            return voucher;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign voucher", e);
        }
    }

    public boolean verifyVoucher(OfflineVoucher voucher) {
        if (voucher == null) return false;
        if (System.currentTimeMillis() > voucher.expiresAt()) return false;

        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(serverKeyHolder.getPublicKey());
            signature.update(voucher.getSigningPayload().getBytes(StandardCharsets.UTF_8));
            byte[] sigBytes = Base64.getDecoder().decode(voucher.bankSignature());
            return signature.verify(sigBytes);
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized boolean validateAndIncrementSequence(String vpa, long incomingSeqNo) {
        long currentSeq = deviceSequenceCounters.getOrDefault(vpa, 0L);
        if (incomingSeqNo <= currentSeq) {
            return false; // Replayed or out-of-order sequence!
        }
        deviceSequenceCounters.put(vpa, incomingSeqNo);
        return true;
    }

    public OfflineVoucher getVoucher(String voucherId) {
        return activeVouchers.get(voucherId);
    }
}
