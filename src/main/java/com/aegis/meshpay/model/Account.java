package com.aegis.meshpay.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String vpa;

    @Column(nullable = false)
    private String holderName;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private BigDecimal offlineCommitted;

    @Column(nullable = false)
    private String pinHash;

    public Account() {}

    public Account(String vpa, String holderName, BigDecimal balance, BigDecimal offlineCommitted, String pinHash) {
        this.vpa = vpa;
        this.holderName = holderName;
        this.balance = balance;
        this.offlineCommitted = offlineCommitted;
        this.pinHash = pinHash;
    }

    public Long getId() { return id; }
    public String getVpa() { return vpa; }
    public String getHolderName() { return holderName; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public BigDecimal getOfflineCommitted() { return offlineCommitted; }
    public void setOfflineCommitted(BigDecimal offlineCommitted) { this.offlineCommitted = offlineCommitted; }
    public String getPinHash() { return pinHash; }
}
