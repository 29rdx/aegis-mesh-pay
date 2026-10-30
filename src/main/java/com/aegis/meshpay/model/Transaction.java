package com.aegis.meshpay.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String packetId;

    @Column(nullable = false, unique = true)
    private String packetHash;

    private String senderVpa;
    private String receiverVpa;
    private BigDecimal amount;

    @Column(nullable = false)
    private String status; // SETTLED, INVALID, DUPLICATE_DROPPED

    private String failureReason;

    private String previousReceiptHash;
    private String receiptHash;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Transaction() {}

    public Long getId() { return id; }
    public String getPacketId() { return packetId; }
    public void setPacketId(String packetId) { this.packetId = packetId; }
    public String getPacketHash() { return packetHash; }
    public void setPacketHash(String packetHash) { this.packetHash = packetHash; }
    public String getSenderVpa() { return senderVpa; }
    public void setSenderVpa(String senderVpa) { this.senderVpa = senderVpa; }
    public String getReceiverVpa() { return receiverVpa; }
    public void setReceiverVpa(String receiverVpa) { this.receiverVpa = receiverVpa; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getPreviousReceiptHash() { return previousReceiptHash; }
    public void setPreviousReceiptHash(String previousReceiptHash) { this.previousReceiptHash = previousReceiptHash; }
    public String getReceiptHash() { return receiptHash; }
    public void setReceiptHash(String receiptHash) { this.receiptHash = receiptHash; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
