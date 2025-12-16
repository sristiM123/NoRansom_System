package com.ransomguard.agent;

import java.nio.file.*;
import java.util.List;

public class AgentMain {

    private static final String ROOT = "iot_test";
    private static final List<String> DEVICES = List.of("DeviceA","DeviceB","DeviceC","DeviceD","DeviceE");
    private static final String INGEST_URL = "http://localhost:9004/api/ingest";

    public static void main(String[] args) throws Exception {
        Path root = Paths.get(ROOT);
        Files.createDirectories(root);

        for (String d : DEVICES) {
            Files.createDirectories(root.resolve(d));
        }

        System.out.println("Agent watching: " + root.toAbsolutePath());
        System.out.println("Devices: " + DEVICES);

        for (String deviceId : DEVICES) {
            Path devicePath = root.resolve(deviceId);
            Thread t = new Thread(new FolderWatcher(devicePath, deviceId, INGEST_URL));
            t.setDaemon(true);
            t.start();
        }

        // Keep alive
        while (true) Thread.sleep(10_000);
    }
}
