package com.aegis.meshpay.service;

import com.aegis.meshpay.model.MeshPacket;
import com.aegis.meshpay.voucher.OfflineVoucher;

import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VirtualDevice {

    private final String deviceId;
    private final String ownerVpa;
    private final KeyPair keyPair;
    private OfflineVoucher offlineVoucher;
    private long currentSequence = 0;
    private boolean hasInternetAccess;

    private final Map<String, MeshPacket> packetStore = new ConcurrentHashMap<>();

    public VirtualDevice(String deviceId, String ownerVpa, KeyPair keyPair, boolean hasInternetAccess) {
        this.deviceId = deviceId;
        this.ownerVpa = ownerVpa;
        this.keyPair = keyPair;
        this.hasInternetAccess = hasInternetAccess;
    }

    public String getDeviceId() { return deviceId; }
    public String getOwnerVpa() { return ownerVpa; }
    public KeyPair getKeyPair() { return keyPair; }
    public OfflineVoucher getOfflineVoucher() { return offlineVoucher; }
    public void setOfflineVoucher(OfflineVoucher offlineVoucher) { this.offlineVoucher = offlineVoucher; }
    public synchronized long nextSequence() { return ++currentSequence; }
    public boolean hasInternetAccess() { return hasInternetAccess; }
    public void setHasInternetAccess(boolean hasInternetAccess) { this.hasInternetAccess = hasInternetAccess; }

    public void storePacket(MeshPacket packet) {
        packetStore.put(packet.packetId(), packet);
    }

    public Collection<MeshPacket> getStoredPackets() {
        return Collections.unmodifiableCollection(packetStore.values());
    }

    public void clearPackets() {
        packetStore.clear();
    }
}
