package com.aegis.meshpay.model;

import java.util.List;

public record MeshPacket(
        String packetId,
        int ttl,
        int hopCount,
        List<String> routeTrace,
        long createdAt,
        String ciphertext
) {
    public MeshPacket withHop(String nodeId) {
        var newTrace = new java.util.ArrayList<>(routeTrace != null ? routeTrace : List.of());
        newTrace.add(nodeId);
        return new MeshPacket(packetId, ttl - 1, hopCount + 1, newTrace, createdAt, ciphertext);
    }
}
