package com.aegis.meshpay.controller;

import com.aegis.meshpay.crypto.ServerKeyHolder;
import com.aegis.meshpay.model.*;
import com.aegis.meshpay.service.*;
import com.aegis.meshpay.voucher.OfflineVoucher;
import com.aegis.meshpay.voucher.VoucherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

    private final ServerKeyHolder serverKeyHolder;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final MeshSimulatorService meshSimulatorService;
    private final BridgeIngestionService ingestionService;
    private final DemoService demoService;
    private final VoucherService voucherService;
    private final IdempotencyService idempotencyService;

    public ApiController(
            ServerKeyHolder serverKeyHolder,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            MeshSimulatorService meshSimulatorService,
            BridgeIngestionService ingestionService,
            DemoService demoService,
            VoucherService voucherService,
            IdempotencyService idempotencyService
    ) {
        this.serverKeyHolder = serverKeyHolder;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.meshSimulatorService = meshSimulatorService;
        this.ingestionService = ingestionService;
        this.demoService = demoService;
        this.voucherService = voucherService;
        this.idempotencyService = idempotencyService;
    }

    @GetMapping("/server-key")
    public Map<String, String> getServerKey() {
        return Map.of(
                "algorithm", "RSA-2048",
                "format", "X.509 Base64",
                "publicKey", serverKeyHolder.getPublicKeyBase64()
        );
    }

    @GetMapping("/accounts")
    public List<Account> getAccounts() {
        return accountRepository.findAll();
    }

    @GetMapping("/transactions")
    public List<Transaction> getTransactions() {
        return transactionRepository.findTop20ByOrderByIdDesc();
    }

    @GetMapping("/mesh/state")
    public List<Map<String, Object>> getMeshState() {
        List<Map<String, Object>> state = new ArrayList<>();
        for (VirtualDevice d : meshSimulatorService.getAllDevices()) {
            state.add(Map.of(
                    "deviceId", d.getDeviceId(),
                    "ownerVpa", d.getOwnerVpa(),
                    "hasInternet", d.hasInternetAccess(),
                    "storedPacketsCount", d.getStoredPackets().size(),
                    "packets", d.getStoredPackets()
            ));
        }
        return state;
    }

    public record SendRequest(String senderVpa, String receiverVpa, BigDecimal amount, String pin) {}

    @PostMapping("/demo/send")
    public ResponseEntity<?> sendPayment(@RequestBody SendRequest req) {
        try {
            MeshPacket packet = demoService.createAndInjectPayment(req.senderVpa(), req.receiverVpa(), req.amount(), req.pin());
            return ResponseEntity.ok(Map.of("status", "INJECTED_INTO_DEVICE", "packet", packet));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/mesh/gossip")
    public Map<String, Object> runGossip() {
        meshSimulatorService.runGossipRound();
        return Map.of("status", "GOSSIP_ROUND_COMPLETED", "devices", getMeshState());
    }

    @PostMapping("/mesh/flush")
    public Map<String, Object> flushBridges() {
        List<BridgeIngestionService.IngestionResult> results = meshSimulatorService.flushOnlineBridges();
        return Map.of("status", "BRIDGES_FLUSHED", "outcomes", results);
    }

    @PostMapping("/mesh/reset")
    public Map<String, String> resetMesh() {
        meshSimulatorService.clearAllMeshStores();
        idempotencyService.clear();
        return Map.of("status", "RESET_COMPLETE");
    }

    @PostMapping("/bridge/ingest")
    public ResponseEntity<BridgeIngestionService.IngestionResult> ingest(
            @RequestBody MeshPacket packet,
            @RequestHeader(value = "X-Bridge-Node-Id", required = false, defaultValue = "external-bridge") String bridgeId,
            @RequestHeader(value = "X-Bridge-Token", required = false) String bridgeToken
    ) {
        BridgeIngestionService.IngestionResult result = ingestionService.ingest(packet, bridgeId, bridgeToken);
        return ResponseEntity.ok(result);
    }

    public record VoucherIssueRequest(String vpa, BigDecimal allowance) {}

    @PostMapping("/voucher/issue")
    public ResponseEntity<OfflineVoucher> issueVoucher(@RequestBody VoucherIssueRequest req) {
        OfflineVoucher voucher = voucherService.issueVoucher(req.vpa(), req.allowance());
        return ResponseEntity.ok(voucher);
    }
}
