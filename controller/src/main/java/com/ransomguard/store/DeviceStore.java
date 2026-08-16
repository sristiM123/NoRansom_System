package com.ransomguard.store;

import com.ransomguard.model.Device;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DeviceStore {

    private final Map<String, Device> devices = new ConcurrentHashMap<>();

    // Create device if missing, always return it
    public Device upsert(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) return null;

        return devices.computeIfAbsent(deviceId, id -> {
            Device d = new Device();
            d.setDeviceId(id);
            d.setStatus("OFFLINE");
            d.setQuarantined(false);
            d.setLastSeenMs(0L);
            return d;
        });
    }

    public List<Device> list() {
        List<Device> out = new ArrayList<>(devices.values());
        out.sort(Comparator.comparing(Device::getDeviceId));
        return out;
    }

    public Device get(String deviceId) {
        return devices.get(deviceId);
    }

    // ✅ THIS FIXES: deviceStore.touch(...) missing
    public Device touch(String deviceId, long lastSeenMs) {
        Device d = upsert(deviceId);
        if (d == null) return null;
        d.setLastSeenMs(lastSeenMs);
        if (!d.isQuarantined()) d.setStatus("ONLINE");
        return d;
    }

    // ✅ THIS FIXES: deviceStore.setQuarantine(...) missing
    public void setQuarantine(String deviceId, boolean quarantined) {
        Device d = upsert(deviceId);
        if (d == null) return;
        d.setQuarantined(quarantined);
        d.setStatus(quarantined ? "QUARANTINED" : "ONLINE");
        d.setLastSeenMs(System.currentTimeMillis());
    }
}
