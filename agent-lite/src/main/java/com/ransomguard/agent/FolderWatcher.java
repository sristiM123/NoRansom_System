package com.ransomguard.agent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;

import static java.nio.file.StandardWatchEventKinds.*;

public class FolderWatcher implements Runnable {

    private final Path folder;
    private final String deviceId;
    private final String ingestUrl;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // ✅ THIS CONSTRUCTOR MATCHES AgentMain
    public FolderWatcher(Path folder, String deviceId, String ingestUrl) {
        this.folder = folder;
        this.deviceId = deviceId;
        this.ingestUrl = ingestUrl;
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {

            folder.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);

            // Send one heartbeat so device appears immediately
            sendEvent("heartbeat", 1, "agent_started");

            while (true) {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == OVERFLOW) continue;

                    Path changed = (Path) event.context();
                    String eventType =
                            event.kind() == ENTRY_CREATE ? "file_created" :
                                    event.kind() == ENTRY_MODIFY ? "file_modified" :
                                            "file_deleted";

                    int severity = eventType.equals("file_modified") ? 2 : 1;
                    sendEvent(eventType, severity, changed.toString());
                }

                key.reset();
            }

        } catch (Exception e) {
            System.err.println("Watcher crashed for " + deviceId);
            e.printStackTrace();
        }
    }

    private void sendEvent(String eventType, int severity, String details) {
        try {
            long ts = System.currentTimeMillis();
            String json = "{"
                    + "\"deviceId\":\"" + esc(deviceId) + "\","
                    + "\"timestampMs\":" + ts + ","
                    + "\"eventType\":\"" + esc(eventType) + "\","
                    + "\"severity\":" + severity + ","
                    + "\"details\":\"" + esc(details) + "\""
                    + "}";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ingestUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> resp =
                    client.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                System.out.println("[AGENT] ingest failed " + resp.statusCode() + " -> " + resp.body());
            }
        } catch (Exception ex) {
            System.out.println("[AGENT] failed to send event for " + deviceId);
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
