package org.megoru.entity.api;

import java.time.LocalDateTime;

public class Clients {

    private String id;
    private String name;
    private boolean enabled;
    private String address;
    private String publicKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String persistentKeepalive;
    private LocalDateTime latestHandshakeAt;
    private long transferRx;
    private long transferTx;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPersistentKeepalive() {
        return persistentKeepalive;
    }

    public void setPersistentKeepalive(String persistentKeepalive) {
        this.persistentKeepalive = persistentKeepalive;
    }

    public LocalDateTime getLatestHandshakeAt() {
        return latestHandshakeAt;
    }

    public void setLatestHandshakeAt(LocalDateTime latestHandshakeAt) {
        this.latestHandshakeAt = latestHandshakeAt;
    }

    public long getTransferRx() {
        return transferRx;
    }

    public void setTransferRx(long transferRx) {
        this.transferRx = transferRx;
    }

    public long getTransferTx() {
        return transferTx;
    }

    public void setTransferTx(long transferTx) {
        this.transferTx = transferTx;
    }
}
