package com.aegis.meshpay.service;

import com.aegis.meshpay.crypto.DeviceSignatureService;
import com.aegis.meshpay.crypto.HybridCryptoService;
import com.aegis.meshpay.crypto.ServerKeyHolder;
import com.aegis.meshpay.model.MeshPacket;
import com.aegis.meshpay.model.PaymentInstruction;
import com.aegis.meshpay.model.Transaction;
import com.aegis.meshpay.voucher.OfflineVoucher;
import com.aegis.meshpay.voucher.VoucherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BridgeIngestionService {

    public enum IngestionOutcome {
        SETTLED,
        DUPLICATE_DROPPED,
        INVALID,
        RATE_LIMITED,
        UNAUTHORIZED_BRIDGE
    }

    public record IngestionResult(IngestionOutcome outcome, String packetHash, String reason, Long transactionId, String receiptHash) {}

    private final HybridCryptoService cryptoService;
    private final ServerKeyHolder serverKeyHolder;
    private final DeviceSignatureService signatureService;
    private final VoucherService voucherService;
    private final IdempotencyService idempotencyService;
    private final SettlementService settlementService;
    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    @Value("${aegis.mesh.bridge.auth-token:aegis-sec-bridge-token-2026-xyz}")
    private String expectedBridgeToken;

    public BridgeIngestionService(
            HybridCryptoService cryptoService,
            ServerKeyHolder serverKeyHolder,
            DeviceSignatureService signatureService,
            VoucherService voucherService,
            IdempotencyService idempotencyService,
            SettlementService settlementService,
            RateLimiterService rateLimiterService,
            ObjectMapper objectMapper
    ) {
        this.cryptoService = cryptoService;
        this.serverKeyHolder = serverKeyHolder;
        this.signatureService = signatureService;
        this.voucherService = voucherService;
        this.idempotencyService = idempotencyService;
        this.settlementService = settlementService;
        this.rateLimiterService = rateLimiterService;
        this.objectMapper = objectMapper;
    }

    public IngestionResult ingest(MeshPacket packet, String bridgeNodeId, String bridgeToken) {
        // 1. Bridge Authentication Guard
        if (expectedBridgeToken != null && !expectedBridgeToken.isBlank()) {
            if (bridgeToken == null || !expectedBridgeToken.equals(bridgeToken)) {
                return new IngestionResult(IngestionOutcome.UNAUTHORIZED_BRIDGE, null, "INVALID_BRIDGE_AUTH_TOKEN", null, null);
            }
        }

        // 2. Rate Limiting Defense
        String rateLimitKey = (bridgeNodeId != null && !bridgeNodeId.isBlank()) ? bridgeNodeId : "anonymous-bridge";
        if (!rateLimiterService.allowRequest(rateLimitKey)) {
            return new IngestionResult(IngestionOutcome.RATE_LIMITED, null, "BRIDGE_RATE_LIMIT_EXCEEDED", null, null);
        }

        if (packet == null || packet.ciphertext() == null || packet.ciphertext().isBlank()) {
            return new IngestionResult(IngestionOutcome.INVALID, null, "EMPTY_CIPHERTEXT", null, null);
        }

        // 3. Compute immutable cryptographic packet hash
        String packetHash = cryptoService.computeSha256(packet.ciphertext());

        // 4. Idempotency Check (Atomic CAS)
        if (!idempotencyService.tryAcquire(packetHash)) {
            return new IngestionResult(IngestionOutcome.DUPLICATE_DROPPED, packetHash, "DUPLICATE_PACKET_DETECTED", null, null);
        }

        PaymentInstruction instruction;
        try {
            // 5. Decrypt Hybrid Cryptographic Envelope
            String decryptedJson = cryptoService.decrypt(packet.ciphertext(), serverKeyHolder.getPrivateKey());
            instruction = objectMapper.readValue(decryptedJson, PaymentInstruction.class);
        } catch (Exception e) {
            idempotencyService.markFailed(packetHash);
            settlementService.recordFailure(packet.packetId(), packetHash, null, "CIPHERTEXT_DECRYPTION_FAILED");
            return new IngestionResult(IngestionOutcome.INVALID, packetHash, "CIPHERTEXT_CORRUPTED_OR_TAMPERED", null, null);
        }

        // 6. Verify Device-Level Cryptographic Signature (secp256r1)
        if (instruction.devicePublicKey() != null && instruction.deviceSignature() != null) {
            String signingPayload = instruction.getSigningPayload();
            boolean validSig = signatureService.verify(instruction.devicePublicKey(), signingPayload, instruction.deviceSignature());
            if (!validSig) {
                idempotencyService.markFailed(packetHash);
                settlementService.recordFailure(packet.packetId(), packetHash, instruction, "DEVICE_SIGNATURE_VERIFICATION_FAILED");
                return new IngestionResult(IngestionOutcome.INVALID, packetHash, "FORGED_DEVICE_SIGNATURE", null, null);
            }
        }

        // 7. Verify Offline Spending Voucher & Nonce Sequentiality
        if (instruction.voucherId() != null && !instruction.voucherId().equals("NONE")) {
            OfflineVoucher voucher = voucherService.getVoucher(instruction.voucherId());
            if (voucher == null || !voucherService.verifyVoucher(voucher)) {
                idempotencyService.markFailed(packetHash);
                settlementService.recordFailure(packet.packetId(), packetHash, instruction, "INVALID_OR_EXPIRED_VOUCHER");
                return new IngestionResult(IngestionOutcome.INVALID, packetHash, "BANK_VOUCHER_VERIFICATION_FAILED", null, null);
            }

            // Monotonic sequence number check (prevents offline double spending!)
            if (!voucherService.validateAndIncrementSequence(instruction.senderVpa(), instruction.seqNo())) {
                idempotencyService.markFailed(packetHash);
                settlementService.recordFailure(packet.packetId(), packetHash, instruction, "OFFLINE_SEQUENCE_REPLAY_DETECTED");
                return new IngestionResult(IngestionOutcome.INVALID, packetHash, "DOUBLE_SPEND_OFFLINE_SEQUENCE_REPLAY", null, null);
            }
        }

        // 8. Settle Transaction Against Ledger
        Transaction tx = settlementService.settle(instruction, packet.packetId(), packetHash);
        if ("SETTLED".equals(tx.getStatus())) {
            idempotencyService.markSettled(packetHash, tx.getId());
            return new IngestionResult(IngestionOutcome.SETTLED, packetHash, null, tx.getId(), tx.getReceiptHash());
        } else {
            idempotencyService.markFailed(packetHash);
            return new IngestionResult(IngestionOutcome.INVALID, packetHash, tx.getFailureReason(), tx.getId(), null);
        }
    }
}
