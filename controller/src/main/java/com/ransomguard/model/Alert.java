package com.ransomguard.model;

public class Alert {
    private long timestampMs;
    private String deviceId;
    private String type;      // ransomware / quarantine / release
    private int severity;     // 1..10
    private String message;

    public Alert() {}

    public Alert(long timestampMs, String deviceId, String type, int severity, String message) {
        this.timestampMs = timestampMs;
        this.deviceId = deviceId;
        this.type = type;
        this.severity = severity;
        this.message = message;
    }

    public long getTimestampMs() { return timestampMs; }
    public void setTimestampMs(long timestampMs) { this.timestampMs = timestampMs; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getSeverity() { return severity; }
    public void setSeverity(int severity) { this.severity = severity; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
