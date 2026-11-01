# 🛡️ AegisMesh Security Policy & Zero-Trust Threat Model

## 1. Executive Security Overview
**AegisMesh** operates under a zero-trust execution model. Every payment instruction generated in an offline mesh environment is considered hostile until verified against multiple independent cryptographic proofs:
1. **Device Integrity**: Authenticated via hardware-level ECDSA signatures (`secp256r1`).
2. **Encrypted Payload Confidentiality**: Sealed using RSA-2048 with optimal asymmetric encryption padding and AES-256-GCM authenticated ciphertext.
3. **Double-Spending Prevention**: Enforced via Bank-Signed Offline Spending Vouchers with monotonic sequence counters and balance reservation ceilings.
4. **Idempotency & Replay Resistance**: Guarantees exactly-once settlement across distributed mesh bridges via Atomic CAS and SHA-256 deduplication windows.
5. **Auditable Settlement**: Immutable Merkle receipt hash chain (`SHA256(prevHash + packetHash + amount + timestamp)`).

---

## 2. Threat Vector Matrix & Mitigations

| Threat Vector | Attack Scenario | AegisMesh Defensive Mitigation |
| :--- | :--- | :--- |
| **Offline Double-Spending** | Malicious sender attempts to broadcast identical payment packets to multiple offline merchants. | Bank-signed vouchers reserve spendable funds upstream; settlement engine rejects packets whose aggregate spend exceeds the voucher ceiling. |
| **Packet Forgery** | Attacker intercepts mesh gossip and attempts to forge recipient or amount fields. | Device ECDSA signature covers the canonical payment instruction tuple `(sender, receiver, amount, timestamp, counter)`. Any byte mutation invalidates verification. |
| **Replay & Sybil Flood** | Attacker replays historical valid packets to drain recipient or flood bridge ingestion. | Centralized idempotency service enforces atomic CAS check-and-set with a rolling 24-hour SHA-256 packet hash ring buffer. |
| **Bridge Ingestion Abuse** | Compromised bridge node floods settlement endpoint with fabricated traffic. | Token bucket rate limiter restricts throughput per bridge token to 50 burst / 10 refill/sec. Unauthorized tokens receive HTTP 401. |

---

## 3. Reporting a Vulnerability
If you discover a potential vulnerability within the AegisMesh cryptographic or settlement protocols, please submit a responsible disclosure report directly to the security team at **`dattarupraj@gmail.com`**.
