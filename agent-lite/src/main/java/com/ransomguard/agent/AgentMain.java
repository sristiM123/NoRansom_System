package com.ransomguard.agent;

import java.nio.file.*;
import java.util.List;

/**
 * NoRansom agent entry point.
 *
 * Usage:
 *   java -jar agent-lite.jar [rootDir] [controllerIngestUrl]
 *
 * Defaults:
 *   rootDir  = iot_test          (one subfolder per simulated device)
 *   ingestUrl = http://localhost:9004/api/ingest
 */
public class AgentMain {

    private static final List<String> DEVICES = List.of("DeviceA","DeviceB","DeviceC","DeviceD","DeviceE");

    public static void main(String[] args) throws Exception {
        String rootDir = args.length > 0 ? args[0] : "iot_test";
        String ingestUrl = args.length > 1 ? args[1] : "http://localhost:9004/api/ingest";

        Path root = Paths.get(rootDir);
        Files.createDirectories(root);
        for (String d : DEVICES) {
            Files.createDirectories(root.resolve(d));
        }

        System.out.println("NoRansom agent watching: " + root.toAbsolutePath());
        System.out.println("Devices: " + DEVICES);
        System.out.println("Controller ingest: " + ingestUrl);

        for (String deviceId : DEVICES) {
            Thread t = new Thread(new FolderWatcher(root.resolve(deviceId), deviceId, ingestUrl),
                    "watcher-" + deviceId);
            t.setDaemon(true);
            t.start();
        }

        while (true) Thread.sleep(10_000);
    }
}
