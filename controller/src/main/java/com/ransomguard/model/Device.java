package com.ransomguard.model;

public class Device {
    private String deviceId;
    private String status;          // ONLINE / OFFLINE / QUARANTINED
    private boolean quarantined;
    private long lastSeenMs;

    public Device() {}

    public Device(String deviceId) {
        this.deviceId = deviceId;
        this.status = "ONLINE";
        this.quarantined = false;
        this.lastSeenMs = 0L;
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isQuarantined() { return quarantined; }
    public void setQuarantined(boolean quarantined) { this.quarantined = quarantined; }

    public long getLastSeenMs() { return lastSeenMs; }
    public void setLastSeenMs(long lastSeenMs) { this.lastSeenMs = lastSeenMs; }
}
