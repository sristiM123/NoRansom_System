package com.ransomguard.api;

import com.ransomguard.service.CorrelationService;
import com.ransomguard.store.DeviceStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ActionController {

    private final DeviceStore deviceStore;
    private final CorrelationService correlationService;

    public ActionController(DeviceStore deviceStore, CorrelationService correlationService) {
        this.deviceStore = deviceStore;
        this.correlationService = correlationService;
    }

    @PostMapping("/quarantine/{deviceId}")
    public ResponseEntity<?> quarantine(@PathVariable String deviceId,
                                        @RequestBody(required = false) Map<String, Object> body) {
        String reason = body != null && body.get("reason") != null ? String.valueOf(body.get("reason")) : "manual";
        deviceStore.setQuarantine(deviceId, true);
        correlationService.quarantineAlert(deviceId, reason);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/release/{deviceId}")
    public ResponseEntity<?> release(@PathVariable String deviceId,
                                     @RequestBody(required = false) Map<String, Object> body) {
        String reason = body != null && body.get("reason") != null ? String.valueOf(body.get("reason")) : "manual";
        deviceStore.setQuarantine(deviceId, false);
        correlationService.releaseAlert(deviceId, reason);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
