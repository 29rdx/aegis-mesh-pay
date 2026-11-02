package com.aegis.mesh.revocation;

import java.util.BitSet;

public class BloomFilterRevocationList {
    private final BitSet bitSet;
    private final int size;

    public BloomFilterRevocationList(int bitSize) {
        this.size = bitSize;
        this.bitSet = new BitSet(bitSize);
    }

    private int hash1(String token) {
        return Math.abs(token.hashCode()) % size;
    }

    private int hash2(String token) {
        int h = 0;
        for (char c : token.toCharArray()) {
            h = 31 * h + c;
        }
        return Math.abs(h) % size;
    }

    public void revoke(String tokenId) {
        bitSet.set(hash1(tokenId));
        bitSet.set(hash2(tokenId));
    }

    public boolean isRevoked(String tokenId) {
        return bitSet.get(hash1(tokenId)) && bitSet.get(hash2(tokenId));
    }
}
