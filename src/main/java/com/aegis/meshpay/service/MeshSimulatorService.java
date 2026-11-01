package com.aegis.meshpay.service;

import com.aegis.meshpay.model.MeshPacket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MeshSimulatorService {

    private final BridgeIngestionService ingestionService;
    private final Map<String, VirtualDevice> devices = new ConcurrentHashMap<>();

    @Value("${aegis.mesh.bridge.auth-token:aegis-sec-bridge-token-2026-xyz}")
    private String bridgeToken;

    public MeshSimulatorService(BridgeIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    public void registerDevice(VirtualDevice device) {
        devices.put(device.getDeviceId(), device);
    }

    public VirtualDevice getDevice(String deviceId) {
        return devices.get(deviceId);
    }

    public Collection<VirtualDevice> getAllDevices() {
        return devices.values();
    }

    public void runGossipRound() {
        List<VirtualDevice> deviceList = new ArrayList<>(devices.values());
        for (VirtualDevice source : deviceList) {
            for (MeshPacket packet : source.getStoredPackets()) {
                if (packet.ttl() <= 0) continue;

                // Gossip to all other devices in proximity
                for (VirtualDevice target : deviceList) {
                    if (target.getDeviceId().equals(source.getDeviceId())) continue;

                    MeshPacket forwarded = packet.withHop(target.getDeviceId());
                    target.storePacket(forwarded);
                }
            }
        }
    }

    public List<BridgeIngestionService.IngestionResult> flushOnlineBridges() {
        List<BridgeIngestionService.IngestionResult> results = new ArrayList<>();
        for (VirtualDevice device : devices.values()) {
            if (device.hasInternetAccess()) {
                for (MeshPacket packet : device.getStoredPackets()) {
                    BridgeIngestionService.IngestionResult res = ingestionService.ingest(packet, device.getDeviceId(), bridgeToken);
                    results.add(res);
                }
            }
        }
        return results;
    }

    public void clearAllMeshStores() {
        for (VirtualDevice d : devices.values()) {
            d.clearPackets();
        }
    }
}
