package com.aegis.meshpay.crypto;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

@Service
public class HybridCryptoService {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128; // in bits

    public String encrypt(String plaintext, PublicKey serverPublicKey) throws GeneralSecurityException {
        // 1. Generate ephemeral AES-256 key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey aesKey = keyGen.generateKey();

        // 2. Encrypt plaintext with AES-GCM
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
        byte[] ciphertextWithTag = aesCipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // 3. Encrypt AES key with Server RSA Public Key (RSA/ECB/OAEPWithSHA-256AndMGF1Padding)
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        OAEPParameterSpec oaepSpec = new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT
        );
        rsaCipher.init(Cipher.ENCRYPT_MODE, serverPublicKey, oaepSpec);
        byte[] encryptedAesKey = rsaCipher.doFinal(aesKey.getEncoded());

        // 4. Combine: [2 bytes: rsaKeyLen] + [rsaKey] + [12 bytes: iv] + [ciphertext+tag]
        ByteBuffer buffer = ByteBuffer.allocate(2 + encryptedAesKey.length + GCM_IV_LENGTH + ciphertextWithTag.length);
        buffer.putShort((short) encryptedAesKey.length);
        buffer.put(encryptedAesKey);
        buffer.put(iv);
        buffer.put(ciphertextWithTag);

        return Base64.getEncoder().encodeToString(buffer.array());
    }

    public String decrypt(String envelopeBase64, PrivateKey serverPrivateKey) throws GeneralSecurityException {
        byte[] envelope = Base64.getDecoder().decode(envelopeBase64);
        ByteBuffer buffer = ByteBuffer.wrap(envelope);

        // 1. Read RSA Encrypted AES Key
        int rsaKeyLen = buffer.getShort() & 0xFFFF;
        byte[] encryptedAesKey = new byte[rsaKeyLen];
        buffer.get(encryptedAesKey);

        // 2. Decrypt AES key with Server Private Key
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        OAEPParameterSpec oaepSpec = new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT
        );
        rsaCipher.init(Cipher.DECRYPT_MODE, serverPrivateKey, oaepSpec);
        byte[] rawAesKey = rsaCipher.doFinal(encryptedAesKey);
        SecretKey aesKey = new SecretKeySpec(rawAesKey, "AES");

        // 3. Read IV and AES-GCM Ciphertext
        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);

        byte[] ciphertextWithTag = new byte[buffer.remaining()];
        buffer.get(ciphertextWithTag);

        // 4. Decrypt Plaintext
        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);
        byte[] plaintextBytes = aesCipher.doFinal(ciphertextWithTag);

        return new String(plaintextBytes, StandardCharsets.UTF_8);
    }

    public String computeSha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
