package com.ransomguard.service;

import com.ransomguard.model.Alert;
import com.ransomguard.model.SecurityEvent;
import com.ransomguard.store.AlertStore;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CorrelationService (auto-alerting)
 * Safe upgrades:
 * - Keeps SAME public methods used by your controllers
 * - Fixes "severity always 10"
 * - Makes alerts more useful (type + message includes evidence + actions)
 * - Adds cooldown + dedupe so you don't get spammy repeats
 */
@Service
public class CorrelationService {

    private final AlertStore alertStore;

    // Analyze last 10 seconds per device
    private static final long WINDOW_MS = 10_000L;

    // Prevent duplicate/spammy auto alerts
    private static final long ALERT_COOLDOWN_MS = 20_000L;

    // Burst thresholds (aggregated score in WINDOW_MS)
    private static final int THRESH_WARN = 8;       // early warning
    private static final int THRESH_HIGH = 12;      // strong suspicion
    private static final int THRESH_CRITICAL = 18;  // ransomware-like burst

    // Per-device rolling window
    private final Map<String, Deque<MiniEvent>> windows = new ConcurrentHashMap<>();

    // Cooldown/dedupe
    private final Map<String, Long> lastAutoAlertAt = new ConcurrentHashMap<>();
    private final Map<String, String> lastAutoSignature = new ConcurrentHashMap<>();

    public CorrelationService(AlertStore alertStore) {
        this.alertStore = alertStore;
    }

    /**
     * Called from IngestController:
     *   int score = scoringService.updateAndScore(e);
     *   correlationService.maybeCreateAlert(e, score);
     *
     * This accumulates per-event scores in a short window to detect ransomware-like bursts.
     */
    public void maybeCreateAlert(SecurityEvent e, int score) {
        if (e == null) return;
        String deviceId = safe(e.getDeviceId());
        if (deviceId.isEmpty()) return;

        long now = System.currentTimeMillis();
        long ts = (e.getTimestampMs() > 0) ? e.getTimestampMs() : now;

        Deque<MiniEvent> dq = windows.computeIfAbsent(deviceId, k -> new ArrayDeque<>());

        synchronized (dq) {
            dq.addLast(new MiniEvent(ts, safe(e.getEventType()), safe(e.getDetails()), score));
            prune(dq, now);

            BurstFeatures f = computeFeatures(dq);

            // Not suspicious enough → no alert
            if (f.burstScore < THRESH_WARN) return;

            // Cooldown
            long last = lastAutoAlertAt.getOrDefault(deviceId, 0L);
            if (now - last < ALERT_COOLDOWN_MS) return;

            // Dedupe identical bursts
            String sig = f.signature();
            String prevSig = lastAutoSignature.getOrDefault(deviceId, "");
            if (sig.equals(prevSig)) return;

            // ✅ FIX: severity is NOT always 10 anymore
            int severity = severityFromBurstScore(f.burstScore);

            // More informative type than just "ransomware"
            String type = classify(f);

            // Evidence + readiness steps (still just a message string → no model changes)
            String msg = buildMessage(f);

            alertStore.add(new Alert(
                    now,
                    deviceId,
                    type,
                    severity,
                    msg
            ));

            lastAutoAlertAt.put(deviceId, now);
            lastAutoSignature.put(deviceId, sig);
        }
    }

    // Manual action alerts (keep exactly as your system expects)
    public void quarantineAlert(String deviceId, String reason) {
        alertStore.add(new Alert(
                System.currentTimeMillis(),
                safe(deviceId),
                "quarantine",
                8,
                "Device quarantined: " + safe(reason)
        ));
    }

    public void releaseAlert(String deviceId, String reason) {
        alertStore.add(new Alert(
                System.currentTimeMillis(),
                safe(deviceId),
                "release",
                3,
                "Device released: " + safe(reason)
        ));
    }

    // ---------------- helpers ----------------

    private void prune(Deque<MiniEvent> dq, long now) {
        long cutoff = now - WINDOW_MS;
        while (!dq.isEmpty() && dq.peekFirst().ts < cutoff) {
            dq.removeFirst();
        }
    }

    private BurstFeatures computeFeatures(Deque<MiniEvent> dq) {
        BurstFeatures f = new BurstFeatures();

        // Rough estimate: how many different files were touched
        Set<Integer> uniqueFiles = new HashSet<>();

        for (MiniEvent me : dq) {
            f.burstScore += Math.max(0, me.score);

            String t = me.type.toLowerCase(Locale.ROOT);

            if (t.contains("file_modified")) f.modified++;
            else if (t.contains("file_created")) f.created++;
            else if (t.contains("file_deleted")) f.deleted++;
            else if (t.contains("rename")) f.renamed++;
            else if (t.contains("entropy")) f.entropySignals++;
            else if (t.contains("burst")) f.burstSignals++;

            if (!me.details.isEmpty()) uniqueFiles.add(me.details.hashCode());
        }

        f.uniqueFiles = uniqueFiles.size();
        f.eventCount = dq.size();
        return f;
    }

    private String classify(BurstFeatures f) {
        // More “preparedness-friendly” types
        if (f.entropySignals > 0 && f.modified > 0) return "entropy_spike";
        if (f.renamed >= 3) return "rename_storm";
        if (f.deleted >= 3) return "mass_deletion";
        if (f.burstSignals > 0) return "burst_activity";

        // Generic ransomware burst class
        if (f.burstScore >= THRESH_CRITICAL) return "ransomware_critical";
        if (f.burstScore >= THRESH_HIGH) return "ransomware_high";
        return "ransomware_warning";
    }

    private int severityFromBurstScore(int burstScore) {
        // ✅ THIS is the key fix for “all severity = 10”
        if (burstScore >= THRESH_CRITICAL) return 10;
        if (burstScore >= THRESH_HIGH) return 8;
        if (burstScore >= THRESH_WARN) return 6;
        return 5;
    }

    private String buildMessage(BurstFeatures f) {
        StringBuilder sb = new StringBuilder();
        sb.append("Suspicious burst in last ").append(WINDOW_MS / 1000).append("s: ")
                .append("score=").append(f.burstScore)
                .append(", events=").append(f.eventCount)
                .append(", uniqueFiles≈").append(f.uniqueFiles).append(". ");

        sb.append("Ops[mod=").append(f.modified)
                .append(", create=").append(f.created)
                .append(", del=").append(f.deleted)
                .append(", rename=").append(f.renamed).append("]. ");

        if (f.entropySignals > 0) sb.append("Signals[entropy=").append(f.entropySignals).append("]. ");
        if (f.burstSignals > 0) sb.append("Signals[burst=").append(f.burstSignals).append("]. ");

        // “preparedness” steps (SOC-style)
        sb.append("Recommended: quarantine device, stop file shares, preserve logs, verify backups.");
        return sb.toString();
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    // ---------------- small types ----------------

    private static final class MiniEvent {
        final long ts;
        final String type;
        final String details;
        final int score;

        MiniEvent(long ts, String type, String details, int score) {
            this.ts = ts;
            this.type = (type == null) ? "" : type;
            this.details = (details == null) ? "" : details;
            this.score = score;
        }
    }

    private static final class BurstFeatures {
        int burstScore;
        int eventCount;
        int uniqueFiles;

        int modified;
        int created;
        int deleted;
        int renamed;

        int entropySignals;
        int burstSignals;

        String signature() {
            // fingerprint for dedupe
            return burstScore + "|" + eventCount + "|" + uniqueFiles + "|"
                    + modified + "|" + created + "|" + deleted + "|" + renamed + "|"
                    + entropySignals + "|" + burstSignals;
        }
    }
}
