package com.ransomguard.api;

import com.ransomguard.model.SecurityEvent;
import com.ransomguard.store.EventStore;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class EventsController {

    private final EventStore eventStore;

    public EventsController(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    /**
     * Backwards-compatible:
     * - /api/events with no params returns a plain list (like before).
     * Efficient upgrades:
     * - limit (default 50)
     * - deviceId filter
     * - type filter
     * - sinceMs filter
     * - format=list (default) OR format=full (items + summary)
     */
    @GetMapping("/events")
    public Object events(
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @RequestParam(value = "deviceId", required = false) String deviceId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "sinceMs", required = false) Long sinceMs,
            @RequestParam(value = "format", defaultValue = "list") String format
    ) {
        limit = Math.max(1, Math.min(limit, 1000));

        List<SecurityEvent> items = eventStore.latestAll(limit);

        if (deviceId != null && !deviceId.isBlank()) {
            String did = deviceId.trim();
            items = items.stream().filter(e -> did.equals(e.getDeviceId())).collect(Collectors.toList());
        }
        if (type != null && !type.isBlank()) {
            String tt = type.trim().toLowerCase(Locale.ROOT);
            items = items.stream().filter(e -> e.getEventType() != null && e.getEventType().toLowerCase(Locale.ROOT).contains(tt))
                    .collect(Collectors.toList());
        }
        if (sinceMs != null && sinceMs > 0) {
            long s = sinceMs;
            items = items.stream().filter(e -> e.getTimestampMs() >= s).collect(Collectors.toList());
        }

        // Old behavior preserved
        if (!"full".equalsIgnoreCase(format)) {
            return items;
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("count", items.size());

        summary.put("byType", items.stream()
                .collect(Collectors.groupingBy(e -> safe(e.getEventType()), Collectors.counting())));

        summary.put("bySeverity", items.stream()
                .collect(Collectors.groupingBy(SecurityEvent::getSeverity, Collectors.counting())));

        summary.put("topDevices", items.stream()
                .collect(Collectors.groupingBy(e -> safe(e.getDeviceId()), Collectors.counting()))
                .entrySet().stream()
                .sorted((x, y) -> Long.compare(y.getValue(), x.getValue()))
                .limit(5)
                .collect(Collectors.toList()));

        long suspicious = items.stream().filter(e -> {
            String t = safe(e.getEventType()).toLowerCase(Locale.ROOT);
            return t.contains("entropy") || t.contains("burst") || t.contains("rename") || t.contains("file_deleted");
        }).count();
        summary.put("suspiciousSignalsInBatch", suspicious);

        return Map.of("items", items, "summary", summary);
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "unknown" : s.trim();
    }
}
