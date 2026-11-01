# AegisMeshPay 🛡️
### Zero-Connectivity Cryptographic Mesh Payment Gateway with Hardware Vouchers & Multi-Hop Relay

[![Spring Boot: 3.3.4](https://img.shields.io/badge/Spring_Boot-3.3.4-brightgreen.svg?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java: 17+](https://img.shields.io/badge/Java-17%2B-orange.svg?logo=openjdk&logoColor=white)](https://openjdk.org)
[![Security: Hybrid RSA + AES--256--GCM + ECDSA](https://img.shields.io/badge/Security-RSA--2048%20%7C%20AES--256--GCM%20%7C%20ECDSA-blue.svg)](https://en.wikipedia.org/wiki/Galois/Counter_Mode)
[![Idempotency: Atomic CAS Engine](https://img.shields.io/badge/Idempotency-Atomic_CAS_100%25-brightgreen.svg)](#)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 🌟 Executive Overview

**AegisMeshPay** is a production-grade, zero-connectivity offline payment system. It solves the hardest problem in fintech: **How to securely transact money in a basement, subway, or disaster zone with zero cellular or internet connectivity, route the payment through untrusted intermediary devices, and settle exactly once on the core banking ledger without risk of double-spending or tampering.**

```
Offline Sender (Basement)       Untrusted Mesh Intermediary           Online Bridge (Surface)         Aegis Banking Gateway
┌─────────────────────────┐     ┌─────────────────────────────┐       ┌───────────────────────┐       ┌───────────────────────┐
│ • Device ECDSA Sign     │     │ • Bluetooth Low Energy (BLE)│       │ • Captures Packet via │       │ • Bridge Token Auth   │
│ • Bank Voucher Validate │────▶│ • Store-and-Forward Gossip  │──────▶│   Local Mesh          │──────▶│ • Atomic CAS Dedupe   │
│ • RSA + AES-GCM Encrypt │     │ • Zero Access to Plaintext  │       │ • Forwards to 5G/WiFi │       │ • Enforce Monotonicity│
└─────────────────────────┘     └─────────────────────────────┘       └───────────────────────┘       │ • Merkle Ledger Settle│
                                                                                                      └───────────────────────┘
```

---

## 🛡️ Critical Security Gaps in Upstream & How Aegis Solves Them

| Dimension | Standard / Upstream Flaw | AegisMeshPay Architectural Defense |
| :--- | :--- | :--- |
| **Offline Insolvency & Double Spending** | Upstream admits: *"The receiver has no way to verify sender funds... a sender can double spend offline."* | **Bank-Signed Monotonic Offline Vouchers**: Bank pre-signs a hardware credit allowance voucher with monotonic sequence counter (`seqNo`). Offline receivers cryptographically verify bank signature before accepting goods. Double spending is impossible. |
| **Identity Forgery** | Upstream only encrypted with server RSA key; anyone with public key could forge payment instructions. | **Hardware ECDSA Signatures (secp256r1)**: Every instruction is digitally signed by the sender device's secure enclave private key. Non-repudiation and sender authenticity are guaranteed. |
| **Bridge Ingestion Exploits** | Upstream `/api/bridge/ingest` had no authentication or velocity controls. | **Cryptographic Bridge Auth & Token-Bucket Rate Limiter**: All bridge nodes authenticate with HMAC tokens (`X-Bridge-Token`) and pass through sliding-window rate limiters to prevent DoS floods. |
| **Ledger Auditability** | Standard flat database entries without tamper evidence. | **Merkle Receipt Hash Chain**: Every settled transaction generates a receipt hash cryptographically chained to the previous block: `SHA-256(prevHash + packetHash + amount + timestamp)`. |

---

## 🚀 Key Engineering Capabilities

1. **Hybrid Envelope Cryptography**:
   - Ephemeral **AES-256-GCM** key generated per transaction (authenticated encryption with 128-bit integrity tag).
   - Session key encrypted via **RSA-2048 with OAEP-SHA256 padding**.
   - Zero plaintext exposure across mesh hops; intermediaries only see encrypted binary blobs.
2. **Atomic CAS Idempotency**:
   - Computes unique SHA-256 digest of ciphertext envelope.
   - Atomic Compare-and-Set state engine ensures that when 5 different bridge phones deliver the same mesh packet simultaneously, **exactly 1 settles and 4 are dropped as duplicates**.
3. **Simulated Multi-Hop BLE Gossip Protocol**:
   - Full in-memory virtual device mesh simulator modeling physical radio range, hop counts, TTL decrementing, and bridge node connectivity.
4. **Cyberpunk Operator Dashboard**:
   - In-browser management console at `http://localhost:8080` with interactive payment injector, BLE gossip trigger, bridge flush, live accounts table, and real-time security attack simulators.

---

## 💻 Quick Start & Running Locally

### Prerequisites
- JDK 17 or higher
- Git

### Build & Run
```bash
git clone https://github.com/29rdx/aegis-mesh-pay.git
cd aegis-mesh-pay
./mvnw spring-boot:run
```

Once started, open **`http://localhost:8080`** in your browser.

---

## 🧪 Comprehensive Concurrency & Security Tests

Run the full automated test suite:
```bash
./mvnw test
```

### Verified Test Suite:
- **`singlePacketDeliveredByThreeBridgesSettlesExactlyOnce`**: Fires 3 concurrent worker threads delivering the identical packet simultaneously through different simulated bridge gateways. Asserts exactly 1 `SETTLED`, 2 `DUPLICATE_DROPPED`, and zero double-debiting.
- **`tamperedCiphertextIsRejected`**: Verifies that bit-flipped or corrupted ciphertext envelopes are rejected as `INVALID` by GCM tag verification before touching the ledger.
- **`unauthorizedBridgeTokenIsRejected`**: Asserts unauthorized mesh bridges without valid credentials return `UNAUTHORIZED_BRIDGE`.

---

## 📊 API Reference

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/` | Cyberpunk Operator Dashboard |
| `GET` | `/api/server-key` | Server master RSA public key (X.509 Base64) |
| `GET` | `/api/accounts` | Core ledger accounts & offline allowances |
| `GET` | `/api/transactions` | Real-time audit stream & Merkle receipts |
| `GET` | `/api/mesh/state` | Live virtual device statuses & packet queues |
| `POST` | `/api/demo/send` | Simulate offline payment injection on device |
| `POST` | `/api/mesh/gossip` | Execute multi-hop BLE gossip round |
| `POST` | `/api/mesh/flush` | Flush online bridge gateways to backend |
| `POST` | `/api/bridge/ingest` | Production ingestion gateway for bridge nodes |
| `POST` | `/api/voucher/issue` | Issue bank-signed offline spending voucher |

---

## 📄 License
Released under the [MIT License](LICENSE). Engineered by **Rupraj Datta** (`@29rdx`).
