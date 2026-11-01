package com.aegis.meshpay.service;

import com.aegis.meshpay.crypto.DeviceSignatureService;
import com.aegis.meshpay.crypto.HybridCryptoService;
import com.aegis.meshpay.crypto.ServerKeyHolder;
import com.aegis.meshpay.model.Account;
import com.aegis.meshpay.model.AccountRepository;
import com.aegis.meshpay.model.MeshPacket;
import com.aegis.meshpay.model.PaymentInstruction;
import com.aegis.meshpay.voucher.OfflineVoucher;
import com.aegis.meshpay.voucher.VoucherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.KeyPair;
import java.util.List;
import java.util.UUID;

@Service
public class DemoService {

    private final AccountRepository accountRepository;
    private final ServerKeyHolder serverKeyHolder;
    private final HybridCryptoService cryptoService;
    private final DeviceSignatureService signatureService;
    private final VoucherService voucherService;
    private final MeshSimulatorService meshSimulatorService;
    private final ObjectMapper objectMapper;

    public DemoService(
            AccountRepository accountRepository,
            ServerKeyHolder serverKeyHolder,
            HybridCryptoService cryptoService,
            DeviceSignatureService signatureService,
            VoucherService voucherService,
            MeshSimulatorService meshSimulatorService,
            ObjectMapper objectMapper
    ) {
        this.accountRepository = accountRepository;
        this.serverKeyHolder = serverKeyHolder;
        this.cryptoService = cryptoService;
        this.signatureService = signatureService;
        this.voucherService = voucherService;
        this.meshSimulatorService = meshSimulatorService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        // 1. Seed demo banking accounts
        if (accountRepository.findByVpa("alice@aegis").isEmpty()) {
            accountRepository.save(new Account("alice@aegis", "Alice Sharma", new BigDecimal("10000.00"), new BigDecimal("3000.00"), cryptoService.computeSha256("1234")));
        }
        if (accountRepository.findByVpa("bob@aegis").isEmpty()) {
            accountRepository.save(new Account("bob@aegis", "Bob Verma", new BigDecimal("2500.00"), BigDecimal.ZERO, cryptoService.computeSha256("5678")));
        }
        if (accountRepository.findByVpa("carol@aegis").isEmpty()) {
            accountRepository.save(new Account("carol@aegis", "Carol Roy", new BigDecimal("500.00"), BigDecimal.ZERO, cryptoService.computeSha256("9999")));
        }

        // 2. Issue Bank-Signed Offline Spending Vouchers
        OfflineVoucher aliceVoucher = voucherService.issueVoucher("alice@aegis", new BigDecimal("3000.00"));

        // 3. Register Simulated Mobile Devices
        KeyPair aliceKeys = signatureService.generateDeviceKeyPair();
        VirtualDevice aliceDevice = new VirtualDevice("phone-alice", "alice@aegis", aliceKeys, false);
        aliceDevice.setOfflineVoucher(aliceVoucher);

        KeyPair bobKeys = signatureService.generateDeviceKeyPair();
        VirtualDevice bobDevice = new VirtualDevice("phone-bob", "bob@aegis", bobKeys, false);

        KeyPair relayKeys = signatureService.generateDeviceKeyPair();
        VirtualDevice relayDevice = new VirtualDevice("phone-mesh-relay", "relay@aegis", relayKeys, false);

        KeyPair bridge1Keys = signatureService.generateDeviceKeyPair();
        VirtualDevice bridge1 = new VirtualDevice("phone-bridge-1", "gateway1@aegis", bridge1Keys, true);

        KeyPair bridge2Keys = signatureService.generateDeviceKeyPair();
        VirtualDevice bridge2 = new VirtualDevice("phone-bridge-2", "gateway2@aegis", bridge2Keys, true);

        meshSimulatorService.registerDevice(aliceDevice);
        meshSimulatorService.registerDevice(bobDevice);
        meshSimulatorService.registerDevice(relayDevice);
        meshSimulatorService.registerDevice(bridge1);
        meshSimulatorService.registerDevice(bridge2);
    }

    public MeshPacket createAndInjectPayment(String senderVpa, String receiverVpa, BigDecimal amount, String pin) throws Exception {
        VirtualDevice senderDevice = meshSimulatorService.getAllDevices().stream()
                .filter(d -> senderVpa.equals(d.getOwnerVpa()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No virtual device found for sender: " + senderVpa));

        long seqNo = senderDevice.nextSequence();
        String nonce = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        OfflineVoucher voucher = senderDevice.getOfflineVoucher();
        String voucherId = (voucher != null) ? voucher.voucherId() : "NONE";

        String devicePubKey = signatureService.encodePublicKey(senderDevice.getKeyPair().getPublic());
        String signingPayload = voucherId + ":" + seqNo + ":" + senderVpa + ":" + receiverVpa + ":" + amount.toPlainString() + ":" + nonce + ":" + now;
        String deviceSig = signatureService.sign(senderDevice.getKeyPair().getPrivate(), signingPayload);

        PaymentInstruction instruction = new PaymentInstruction(
                voucherId,
                seqNo,
                senderVpa,
                receiverVpa,
                amount,
                pin,
                nonce,
                now,
                devicePubKey,
                deviceSig
        );

        String jsonPayload = objectMapper.writeValueAsString(instruction);
        String ciphertext = cryptoService.encrypt(jsonPayload, serverKeyHolder.getPublicKey());

        String packetId = UUID.randomUUID().toString();
        MeshPacket packet = new MeshPacket(packetId, 5, 0, List.of(senderDevice.getDeviceId()), now, ciphertext);

        senderDevice.storePacket(packet);
        return packet;
    }
}
