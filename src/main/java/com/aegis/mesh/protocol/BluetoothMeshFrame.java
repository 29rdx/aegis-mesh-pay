package com.aegis.mesh.protocol;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class BluetoothMeshFrame {
    public static final byte FRAME_MAGIC = (byte) 0xAE;
    public static final byte MSG_TYPE_PAYMENT_VOUCHER = 0x01;
    public static final byte MSG_TYPE_SETTLEMENT_ACK = 0x02;

    private final byte type;
    private final long sequenceNumber;
    private final byte[] payload;

    public BluetoothMeshFrame(byte type, long sequenceNumber, byte[] payload) {
        this.type = type;
        this.sequenceNumber = sequenceNumber;
        this.payload = payload;
    }

    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(1 + 1 + 8 + 4 + payload.length);
        buffer.put(FRAME_MAGIC);
        buffer.put(type);
        buffer.putLong(sequenceNumber);
        buffer.putInt(payload.length);
        buffer.put(payload);
        return buffer.array();
    }

    public static BluetoothMeshFrame deserialize(byte[] bytes) {
        if (bytes.length < 14 || bytes[0] != FRAME_MAGIC) {
            throw new IllegalArgumentException("Invalid Bluetooth Mesh Frame header");
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.get(); // Magic
        byte type = buffer.get();
        long seq = buffer.getLong();
        int len = buffer.getInt();
        byte[] payload = new byte[len];
        buffer.get(payload);
        return new BluetoothMeshFrame(type, seq, payload);
    }

    public byte getType() { return type; }
    public long getSequenceNumber() { return sequenceNumber; }
    public byte[] getPayload() { return payload; }
}
