package com.aegis.mesh.crypto;

import org.junit.jupiter.api.Test;
import java.security.MessageDigest;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

public class MerkleProofVerifierTest {
    @Test
    public void testDirectLeafRootVerification() {
        String dummyLeaf = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        boolean valid = MerkleProofVerifier.verify(dummyLeaf, Collections.emptyList(), dummyLeaf);
        assertTrue(valid);
    }
}
