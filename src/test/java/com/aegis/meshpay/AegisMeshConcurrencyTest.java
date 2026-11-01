package com.aegis.meshpay;

import com.aegis.meshpay.crypto.DeviceSignatureService;
import com.aegis.meshpay.crypto.HybridCryptoService;
import com.aegis.meshpay.crypto.ServerKeyHolder;
import com.aegis.meshpay.model.Account;
import com.aegis.meshpay.model.AccountRepository;
import com.aegis.meshpay.model.MeshPacket;
import com.aegis.meshpay.service.BridgeIngestionService;
import com.aegis.meshpay.service.DemoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class AegisMeshConcurrencyTest {

    @Autowired
    private BridgeIngestionService ingestionService;

    @Autowired
    private DemoService demoService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ServerKeyHolder serverKeyHolder;

    @Autowired
    private HybridCryptoService cryptoService;

    @Value("${aegis.mesh.bridge.auth-token}")
    private String bridgeToken;

    @BeforeEach
    public void setup() {
        demoService.init();
    }

    @Test
    public void singlePacketDeliveredByThreeBridgesSettlesExactlyOnce() throws Exception {
        Account aliceBefore = accountRepository.findByVpa("alice@aegis").orElseThrow();
        BigDecimal balanceBefore = aliceBefore.getBalance();

        BigDecimal sendAmount = new BigDecimal("500.00");
        MeshPacket packet = demoService.createAndInjectPayment("alice@aegis", "bob@aegis", sendAmount, "1234");

        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<BridgeIngestionService.IngestionResult>> futures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final String bridgeId = "phone-bridge-" + (i + 1);
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await(); // Simultaneous firing
                return ingestionService.ingest(packet, bridgeId, bridgeToken);
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Release threads
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        long settledCount = 0;
        long duplicateCount = 0;

        for (Future<BridgeIngestionService.IngestionResult> f : futures) {
            BridgeIngestionService.IngestionResult res = f.get();
            if (res.outcome() == BridgeIngestionService.IngestionOutcome.SETTLED) {
                settledCount++;
                assertThat(res.receiptHash()).isNotNull();
            } else if (res.outcome() == BridgeIngestionService.IngestionOutcome.DUPLICATE_DROPPED) {
                duplicateCount++;
            }
        }

        assertThat(settledCount).isEqualTo(1);
        assertThat(duplicateCount).isEqualTo(2);

        Account aliceAfter = accountRepository.findByVpa("alice@aegis").orElseThrow();
        assertThat(aliceAfter.getBalance()).isEqualByComparingTo(balanceBefore.subtract(sendAmount));
    }

    @Test
    public void tamperedCiphertextIsRejected() {
        MeshPacket fakePacket = new MeshPacket(
                "tamper-123",
                2,
                1,
                List.of("rogue-device"),
                System.currentTimeMillis(),
                "QUVHLVNFQ1VSRVRBTVBFUkVEQkxPQi1GQUtF"
        );

        BridgeIngestionService.IngestionResult res = ingestionService.ingest(fakePacket, "bridge-test", bridgeToken);
        assertThat(res.outcome()).isEqualTo(BridgeIngestionService.IngestionOutcome.INVALID);
    }

    @Test
    public void unauthorizedBridgeTokenIsRejected() throws Exception {
        MeshPacket packet = demoService.createAndInjectPayment("alice@aegis", "bob@aegis", new BigDecimal("100.00"), "1234");
        BridgeIngestionService.IngestionResult res = ingestionService.ingest(packet, "rogue-bridge", "wrong-token-abc");
        assertThat(res.outcome()).isEqualTo(BridgeIngestionService.IngestionOutcome.UNAUTHORIZED_BRIDGE);
    }
}
