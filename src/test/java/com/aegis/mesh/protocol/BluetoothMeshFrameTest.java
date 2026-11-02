package com.aegis.mesh.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BluetoothMeshFrameTest {
    @Test
    public void testRoundtripSerialization() {
        byte[] data = "PAYMENT_PAYLOAD".getBytes();
        BluetoothMeshFrame frame = new BluetoothMeshFrame(BluetoothMeshFrame.MSG_TYPE_PAYMENT_VOUCHER, 1042L, data);
        byte[] wire = frame.serialize();
        BluetoothMeshFrame restored = BluetoothMeshFrame.deserialize(wire);

        assertEquals(frame.getType(), restored.getType());
        assertEquals(frame.getSequenceNumber(), restored.getSequenceNumber());
        assertArrayEquals(data, restored.getPayload());
    }
}
