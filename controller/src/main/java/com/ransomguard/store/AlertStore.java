package com.ransomguard.store;

import com.ransomguard.model.Alert;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class AlertStore {
    private final List<Alert> alerts = new CopyOnWriteArrayList<>();

    public void add(Alert a) {
        alerts.add(a);
        if (alerts.size() > 2000) {
            alerts.subList(0, 500).clear();
        }
    }

    public List<Alert> latest(int n) {
        int size = alerts.size();
        int from = Math.max(0, size - n);
        return new ArrayList<>(alerts.subList(from, size));
    }
}
