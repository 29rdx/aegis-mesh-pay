package com.aegis.mesh.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class MerkleProofVerifier {
    public static boolean verify(String leafHashHex, List<String> proofPath, String expectedRootHex) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] current = hexStringToByteArray(leafHashHex);
            for (String siblingHex : proofPath) {
                byte[] sibling = hexStringToByteArray(siblingHex);
                digest.reset();
                digest.update(current);
                digest.update(sibling);
                current = digest.digest();
            }
            String calculatedRoot = bytesToHex(current);
            return calculatedRoot.equalsIgnoreCase(expectedRootHex);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm missing", e);
        }
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
