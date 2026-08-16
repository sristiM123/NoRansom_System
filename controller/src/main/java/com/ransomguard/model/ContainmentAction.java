package com.ransomguard.model;

public class ContainmentAction {
    private String deviceId;
    private String action; // CONTAIN / RELEASE
    private long timestampMs;
    private String reason;

    public ContainmentAction() {}

    public ContainmentAction(String deviceId, String action, long timestampMs, String reason) {
        this.deviceId = deviceId;
        this.action = action;
        this.timestampMs = timestampMs;
        this.reason = reason;
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public long getTimestampMs() { return timestampMs; }
    public void setTimestampMs(long timestampMs) { this.timestampMs = timestampMs; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
