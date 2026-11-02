package com.aegis.mesh.policy;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

public class SpendingVelocityGovernorTest {
    @Test
    public void testEnforceVelocityLimits() {
        SpendingVelocityGovernor governor = new SpendingVelocityGovernor(
            new BigDecimal("5000.00"),
            new BigDecimal("10000.00")
        );
        assertTrue(governor.validateAndRecord("user-1", new BigDecimal("4000.00")));
        assertTrue(governor.validateAndRecord("user-1", new BigDecimal("4000.00")));
        // Third purchase exceeds 10,000 daily threshold
        assertFalse(governor.validateAndRecord("user-1", new BigDecimal("3000.00")));
        // Single purchase exceeds single transaction max
        assertFalse(governor.validateAndRecord("user-1", new BigDecimal("6000.00")));
    }
}
