# Offline Payment State Machine

\`\`\`
[Device A: Payer]                           [Device B: Merchant]
       |                                             |
  1. Generate Signed Voucher                         |
     (ECDSA + AES-GCM Payload)                       |
       | ------ BLE Mesh Frame (Type 0x01) --------> |
       |                                       2. Verify Voucher
       |                                          - Signature Check
       |                                          - Merkle Proof Check
       |                                          - Revocation Bloom Filter
       |                                          - Velocity Governor Check
       | <----- BLE Mesh Settlement ACK (0x02) ----- |
  3. Store ACK in Local Storage                      |
\`\`\`
