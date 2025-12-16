package com.ransomguard.api;

import com.ransomguard.model.Device;
import com.ransomguard.model.SecurityEvent;
import com.ransomguard.service.CorrelationService;
import com.ransomguard.service.ScoringService;
import com.ransomguard.store.DeviceStore;
import com.ransomguard.store.EventStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class IngestController {

    private final DeviceStore deviceStore;
    private final EventStore eventStore;
    private final ScoringService scoringService;
    private final CorrelationService correlationService;

    public IngestController(DeviceStore deviceStore, EventStore eventStore,
                            ScoringService scoringService, CorrelationService correlationService) {
        this.deviceStore = deviceStore;
        this.eventStore = eventStore;
        this.scoringService = scoringService;
        this.correlationService = correlationService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<?> ingest(@RequestBody SecurityEvent e) {
        long now = System.currentTimeMillis();

        if (e == null || e.getDeviceId() == null || e.getDeviceId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "deviceId missing"));
        }
        if (e.getTimestampMs() <= 0) e.setTimestampMs(now);
        if (e.getEventType() == null || e.getEventType().isBlank()) e.setEventType("unknown");
        if (e.getDetails() == null) e.setDetails("");

        // ✅ Ensure device exists and lastSeen updates
        Device d = deviceStore.touch(e.getDeviceId(), e.getTimestampMs());

        // If quarantined, keep status
        if (d != null && d.isQuarantined()) {
            d.setStatus("QUARANTINED");
        }

        // Store event
        eventStore.add(e);

        // Score + correlate
        int score = scoringService.updateAndScore(e);
        correlationService.maybeCreateAlert(e, score);

        return ResponseEntity.ok(Map.of("ok", true));
    }
}
