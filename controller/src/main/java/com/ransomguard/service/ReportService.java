package com.ransomguard.service;

import com.ransomguard.store.AlertStore;
import com.ransomguard.store.DeviceStore;
import com.ransomguard.store.EventStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    private final DeviceStore deviceStore;
    private final EventStore eventStore;
    private final AlertStore alertStore;

    public ReportService(DeviceStore deviceStore, EventStore eventStore, AlertStore alertStore) {
        this.deviceStore = deviceStore;
        this.eventStore = eventStore;
        this.alertStore = alertStore;
    }

    public Map<String, Object> buildQuickReport() {
        Map<String, Object> r = new HashMap<>();
        r.put("devices", deviceStore.list());
        r.put("latestEvents", eventStore.latestAll(200));
        r.put("latestAlerts", alertStore.latest(50));
        r.put("generatedAtMs", System.currentTimeMillis());

        Map<String, Object> risk = new HashMap<>();
        risk.put("maxSeverity2m", eventStore.maxSeverityInLastMs(120_000));
        risk.put("ransomSignals", eventStore.countSignalsInLastMs(120_000));
        r.put("risk", risk);

        return r;
    }
}
