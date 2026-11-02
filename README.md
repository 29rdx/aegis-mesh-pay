# 🛡️ Aegis Mesh Pay
### Zero-Connectivity Cryptographic Mesh Payment Gateway (Java 17 / Spring Boot)

[![CI](https://github.com/29rdx/aegis-mesh-pay/actions/workflows/ci.yml/badge.svg)](https://github.com/29rdx/aegis-mesh-pay/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://adoptium.net/)

## 📌 Architecture
- **Offline UPI Vouchers**: ECDSA-signed bank spending vouchers.
- **Merkle Proof Verification**: Merkle Tree root audit trail for asynchronous reconciliation.
- **Atomic Velocity Governor**: Lock-free CAS spending velocity limits.
- **BLE Mesh Framing**: Binary framing protocol with anti-replay sequence tracking.
- **Bloom Filter Revocation**: High-density in-memory revocation list for stolen device keys.

## 🧪 Build and Test
```bash
./mvnw clean test
./mvnw spring-boot:run
```
