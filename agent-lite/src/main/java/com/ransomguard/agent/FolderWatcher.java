package com.ransomguard.agent;

import com.ransomguard.agent.analyzer.EntropyCalculator;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Watches a device folder (recursively) and streams security events to the controller.
 *
 * Detection signals emitted:
 *  - file_created / file_modified / file_deleted  (raw activity)
 *  - file_renamed        (inferred: delete+create pair in the same event batch,
 *                         which is how WatchService reports an atomic move)
 *  - entropy_high        (Shannon entropy of file content >= threshold;
 *                         encrypted output is near 8.0 bits/byte)
 */
public class FolderWatcher implements Runnable {

    /** Encrypted/compressed content sits near 8.0; plain text is typically 3.5-5.5. */
    private static final double ENTROPY_THRESHOLD = 7.5;
    /** Skip entropy analysis for tiny files - too few bytes to be statistically meaningful. */
    private static final long MIN_SIZE_FOR_ENTROPY = 256;

    private final Path folder;
    private final String deviceId;
    private final String ingestUrl;

    private final Map<WatchKey, Path> keyToDir = new HashMap<>();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public FolderWatcher(Path folder, String deviceId, String ingestUrl) {
        this.folder = folder;
        this.deviceId = deviceId;
        this.ingestUrl = ingestUrl;
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {

            registerTree(watchService, folder);
            sendEvent("heartbeat", 1, "agent_started");

            while (true) {
                WatchKey key = watchService.take();
                Path dir = keyToDir.get(key);
                if (dir == null) { key.reset(); continue; }

                List<Path> created = new ArrayList<>();
                List<Path> deleted = new ArrayList<>();

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == OVERFLOW) continue;

                    Path rel = (Path) event.context();
                    Path abs = dir.resolve(rel);

                    if (event.kind() == ENTRY_CREATE) {
                        created.add(abs);
                        // new subdirectory -> start watching it too
                        if (Files.isDirectory(abs, LinkOption.NOFOLLOW_LINKS)) {
                            registerTree(watchService, abs);
                        }
                    } else if (event.kind() == ENTRY_DELETE) {
                        deleted.add(abs);
                    } else if (event.kind() == ENTRY_MODIFY) {
                        sendEvent("file_modified", 2, relPath(abs));
                        analyzeEntropy(abs);
                    }
                }

                // Rename inference: an atomic move surfaces as DELETE+CREATE in one batch.
                int renames = Math.min(created.size(), deleted.size());
                for (int i = 0; i < renames; i++) {
                    sendEvent("file_renamed", 3,
                            relPath(deleted.get(i)) + " -> " + relPath(created.get(i)));
                }
                for (int i = renames; i < created.size(); i++) {
                    Path p = created.get(i);
                    if (!Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)) {
                        sendEvent("file_created", 1, relPath(p));
                        analyzeEntropy(p);
                    }
                }
                for (int i = renames; i < deleted.size(); i++) {
                    sendEvent("file_deleted", 1, relPath(deleted.get(i)));
                }

                if (!key.reset()) {
                    keyToDir.remove(key);
                    if (keyToDir.isEmpty()) break;
                }
            }

        } catch (Exception e) {
            System.err.println("Watcher crashed for " + deviceId);
            e.printStackTrace();
        }
    }

    /** Register a directory and all its subdirectories. */
    private void registerTree(WatchService ws, Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes attrs) throws IOException {
                WatchKey k = d.register(ws, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
                keyToDir.put(k, d);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /** Compute Shannon entropy of the file head; report if it looks encrypted. */
    private void analyzeEntropy(Path file) {
        try {
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) return;
            if (Files.size(file) < MIN_SIZE_FOR_ENTROPY) return;

            double h = EntropyCalculator.entropy(file.toFile());
            if (h >= ENTROPY_THRESHOLD) {
                sendEvent("entropy_high", 4,
                        relPath(file) + " entropy=" + String.format("%.2f", h));
            }
        } catch (Exception ignored) {
            // file may already be gone (ransomware-like churn) - that's fine
        }
    }

    private String relPath(Path p) {
        try { return folder.relativize(p).toString(); }
        catch (Exception e) { return p.getFileName().toString(); }
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
            System.out.println("[AGENT] failed to send event for " + deviceId + " (" + ex.getClass().getSimpleName() + ")");
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
