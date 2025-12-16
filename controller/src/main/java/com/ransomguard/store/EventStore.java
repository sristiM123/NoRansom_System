package com.ransomguard.store;

import com.ransomguard.model.SecurityEvent;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class EventStore {

    private final List<SecurityEvent> events = new CopyOnWriteArrayList<>();

    public void add(SecurityEvent e) {
        events.add(e);
        // keep memory bounded
        if (events.size() > 5000) {
            events.subList(0, 1000).clear();
        }
    }

    public List<SecurityEvent> latestAll(int n) {
        int size = events.size();
        int from = Math.max(0, size - n);
        return new ArrayList<>(events.subList(from, size));
    }

    public List<SecurityEvent> lastEventsForDevice(String deviceId, int n) {
        List<SecurityEvent> out = new ArrayList<>();
        for (int i = events.size() - 1; i >= 0 && out.size() < n; i--) {
            SecurityEvent e = events.get(i);
            if (deviceId.equals(e.getDeviceId())) out.add(e);
        }
        Collections.reverse(out);
        return out;
    }

    public int maxSeverityInLastMs(long windowMs) {
        long now = System.currentTimeMillis();
        int max = 0;
        for (int i = events.size() - 1; i >= 0; i--) {
            SecurityEvent e = events.get(i);
            if (now - e.getTimestampMs() > windowMs) break;
            max = Math.max(max, e.getSeverity());
        }
        return max;
    }

    public int countSignalsInLastMs(long windowMs) {
        long now = System.currentTimeMillis();
        int count = 0;
        for (int i = events.size() - 1; i >= 0; i--) {
            SecurityEvent e = events.get(i);
            if (now - e.getTimestampMs() > windowMs) break;
            if (e.getSeverity() >= 7) count++;
        }
        return count;
    }
}
