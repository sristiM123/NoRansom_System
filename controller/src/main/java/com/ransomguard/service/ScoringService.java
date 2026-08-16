package com.ransomguard.service;

import com.ransomguard.model.SecurityEvent;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rolling scoring engine (per device).
 *
 * Why this fixes your issue:
 * - CorrelationService auto-alerts when score >= AUTO_ALERT_THRESHOLD (often 10).
 * - Your old scoring returned only per-event small values, so threshold was never reached.
 * - This scoring keeps a rolling 2-minute window per device and sums points,
 *   so ransomware-like bursts quickly cross threshold -> auto alerts fire.
 */
@Service
public class ScoringService {

    // 2 minutes rolling window (matches what your UI/report talks about)
    private static final long WINDOW_MS = 120_000L;

    // per-device rolling window of scored samples
    private final ConcurrentHashMap<String, Deque<ScoredSample>> windows = new ConcurrentHashMap<>();

    /**
     * Update rolling window with this event and return the CURRENT window score for that device.
     * This is what IngestController passes to CorrelationService.
     */
    public int updateAndScore(SecurityEvent e) {
        if (e == null) return 0;

        final String deviceId = safe(e.getDeviceId());
        if (deviceId.isEmpty()) return 0;

        final long ts = (e.getTimestampMs() > 0) ? e.getTimestampMs() : System.currentTimeMillis();
        final String type = safe(e.getEventType()).toLowerCase();
        final String details = safe(e.getDetails());

        int points = pointsFor(type, details);

        // add +small boost if the simulator tag is present (optional but helps demos)
        if (details.contains("_sim_attack")) points += 2;

        final Deque<ScoredSample> dq = windows.computeIfAbsent(deviceId, k -> new ArrayDeque<>());

        synchronized (dq) {
            dq.addLast(new ScoredSample(ts, points, type));
            pruneOld(dq, ts);
            return sum(dq);
        }
    }

    /**
     * Optional helper (in case any other class uses it later).
     * Stateless score of a list (no memory).
     */
    public int score(List<SecurityEvent> events) {
        if (events == null || events.isEmpty()) return 0;
        int total = 0;
        for (SecurityEvent e : events) {
            if (e == null) continue;
            String type = safe(e.getEventType()).toLowerCase();
            String details = safe(e.getDetails());
            int points = pointsFor(type, details);
            if (details.contains("_sim_attack")) points += 2;
            total += points;
        }
        return total;
    }

    // ----------------- scoring rules -----------------

    private int pointsFor(String type, String details) {
        // File activity
        if (type.contains("file_deleted")) return 3;
        if (type.contains("file_modified")) return 2;
        if (type.contains("file_created")) return 1;

        // Ransomware-y patterns (rename storms / burst / entropy)
        if (type.contains("mass_rename") || type.contains("rename_storm")) return 6;
        if (type.contains("rename")) return 4;
        if (type.contains("burst")) return 4;
        if (type.contains("entropy")) return 5;
        if (type.contains("ransom")) return 6;

        // If your agent uses generic types but puts keywords in details
        String d = safe(details).toLowerCase();
        if (d.contains("entropy")) return 5;
        if (d.contains("rename")) return 4;
        if (d.contains("burst")) return 4;

        return 0;
    }

    // ----------------- window mgmt -----------------

    private void pruneOld(Deque<ScoredSample> dq, long now) {
        long cutoff = now - WINDOW_MS;
        while (!dq.isEmpty() && dq.peekFirst().ts < cutoff) {
            dq.removeFirst();
        }
    }

    private int sum(Deque<ScoredSample> dq) {
        int s = 0;
        for (ScoredSample x : dq) s += x.points;
        return s;
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private static final class ScoredSample {
        final long ts;
        final int points;
        final String type;

        ScoredSample(long ts, int points, String type) {
            this.ts = ts;
            this.points = points;
            this.type = Objects.requireNonNullElse(type, "");
        }
    }
}
