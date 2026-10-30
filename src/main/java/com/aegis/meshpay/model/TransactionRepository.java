package com.aegis.meshpay.model;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByPacketHash(String packetHash);
    Optional<Transaction> findTopByOrderByIdDesc();
    List<Transaction> findTop20ByOrderByIdDesc();
}
