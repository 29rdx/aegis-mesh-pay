package com.aegis.mesh.revocation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BloomFilterRevocationListTest {
    @Test
    public void testRevocationLookups() {
        BloomFilterRevocationList list = new BloomFilterRevocationList(10000);
        list.revoke("device-token-abc");
        assertTrue(list.isRevoked("device-token-abc"));
        assertFalse(list.isRevoked("device-token-clean"));
    }
}
