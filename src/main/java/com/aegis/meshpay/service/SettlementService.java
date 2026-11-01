package com.aegis.meshpay.service;

import com.aegis.meshpay.crypto.HybridCryptoService;
import com.aegis.meshpay.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
public class SettlementService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final HybridCryptoService cryptoService;

    public SettlementService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            HybridCryptoService cryptoService
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.cryptoService = cryptoService;
    }

    @Transactional
    public Transaction settle(PaymentInstruction instruction, String packetId, String packetHash) {
        Account sender = accountRepository.findByVpa(instruction.senderVpa())
                .orElseThrow(() -> new IllegalArgumentException("Sender VPA not found: " + instruction.senderVpa()));

        Account receiver = accountRepository.findByVpa(instruction.receiverVpa())
                .orElseThrow(() -> new IllegalArgumentException("Receiver VPA not found: " + instruction.receiverVpa()));

        // Verify PIN hash (SHA-256 of PIN)
        String enteredPinHash = cryptoService.computeSha256(instruction.pin());
        if (!sender.getPinHash().equalsIgnoreCase(enteredPinHash)) {
            return recordFailure(packetId, packetHash, instruction, "INVALID_PIN");
        }

        // Verify balance
        if (sender.getBalance().compareTo(instruction.amount()) < 0) {
            return recordFailure(packetId, packetHash, instruction, "INSUFFICIENT_FUNDS");
        }

        // Execute transfer
        sender.setBalance(sender.getBalance().subtract(instruction.amount()));
        receiver.setBalance(receiver.getBalance().add(instruction.amount()));
        accountRepository.save(sender);
        accountRepository.save(receiver);

        // Fetch previous transaction receipt hash for the Merkle chain
        Optional<Transaction> latest = transactionRepository.findTopByOrderByIdDesc();
        String prevHash = latest.map(Transaction::getReceiptHash).orElse("GENESIS_BLOCK_0000000000000000");

        String receiptPayload = prevHash + ":" + packetHash + ":" + instruction.amount().toPlainString() +
                ":" + instruction.senderVpa() + ":" + instruction.receiverVpa() + ":" + Instant.now().toEpochMilli();
        String currentReceiptHash = cryptoService.computeSha256(receiptPayload);

        Transaction tx = new Transaction();
        tx.setPacketId(packetId);
        tx.setPacketHash(packetHash);
        tx.setSenderVpa(instruction.senderVpa());
        tx.setReceiverVpa(instruction.receiverVpa());
        tx.setAmount(instruction.amount());
        tx.setStatus("SETTLED");
        tx.setPreviousReceiptHash(prevHash);
        tx.setReceiptHash(currentReceiptHash);

        return transactionRepository.save(tx);
    }

    public Transaction recordFailure(String packetId, String packetHash, PaymentInstruction instruction, String reason) {
        Transaction tx = new Transaction();
        tx.setPacketId(packetId);
        tx.setPacketHash(packetHash);
        if (instruction != null) {
            tx.setSenderVpa(instruction.senderVpa());
            tx.setReceiverVpa(instruction.receiverVpa());
            tx.setAmount(instruction.amount());
        } else {
            tx.setAmount(BigDecimal.ZERO);
        }
        tx.setStatus("INVALID");
        tx.setFailureReason(reason);
        return transactionRepository.save(tx);
    }
}
