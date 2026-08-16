package com.ransomguard.api;

import com.ransomguard.model.Alert;
import com.ransomguard.store.AlertStore;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AlertsController {

    private final AlertStore alertStore;

    public AlertsController(AlertStore alertStore) {
        this.alertStore = alertStore;
    }

    /**
     * Backwards-compatible:
     * - /api/alerts with no params returns a plain list (like before).
     *
     * Efficient upgrades:
     * - limit (default 50)
     * - deviceId filter
     * - type filter
     * - sinceMs filter (works if Alert has getTimestampMs OR getTimeMs OR getTimestamp)
     * - format=list (default) OR format=full (items + summary)
     */
    @GetMapping("/alerts")
    public Object alerts(
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @RequestParam(value = "deviceId", required = false) String deviceId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "sinceMs", required = false) Long sinceMs,
            @RequestParam(value = "format", defaultValue = "list") String format
    ) {
        limit = Math.max(1, Math.min(limit, 500));

        List<Alert> items = alertStore.latest(limit);

        if (deviceId != null && !deviceId.isBlank()) {
            String did = deviceId.trim();
            items = items.stream().filter(a -> did.equals(a.getDeviceId())).collect(Collectors.toList());
        }
        if (type != null && !type.isBlank()) {
            String tt = type.trim().toLowerCase(Locale.ROOT);
            items = items.stream().filter(a -> a.getType() != null && a.getType().toLowerCase(Locale.ROOT).contains(tt))
                    .collect(Collectors.toList());
        }
        if (sinceMs != null && sinceMs > 0) {
            long s = sinceMs;
            items = items.stream().filter(a -> getAlertTimeMs(a) >= s).collect(Collectors.toList());
        }

        // Old behavior preserved
        if (!"full".equalsIgnoreCase(format)) {
            return items;
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("count", items.size());

        summary.put("byType", items.stream()
                .collect(Collectors.groupingBy(a -> safe(a.getType()), Collectors.counting())));

        summary.put("bySeverity", items.stream()
                .collect(Collectors.groupingBy(Alert::getSeverity, Collectors.counting())));

        summary.put("topDevices", items.stream()
                .collect(Collectors.groupingBy(a -> safe(a.getDeviceId()), Collectors.counting()))
                .entrySet().stream()
                .sorted((x, y) -> Long.compare(y.getValue(), x.getValue()))
                .limit(5)
                .collect(Collectors.toList()));

        return Map.of("items", items, "summary", summary);
    }

    // Works with getTimestampMs() OR getTimeMs() OR getTimestamp()
    private long getAlertTimeMs(Alert a) {
        if (a == null) return 0L;

        String[] candidates = {"getTimestampMs", "getTimeMs", "getTimestamp"};
        for (String m : candidates) {
            try {
                Method method = a.getClass().getMethod(m);
                Object v = method.invoke(a);
                if (v instanceof Number) return ((Number) v).longValue();
            } catch (Exception ignored) { }
        }
        return 0L;
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "unknown" : s.trim();
    }
}
