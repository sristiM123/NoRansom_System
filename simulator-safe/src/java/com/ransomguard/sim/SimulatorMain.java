package com.ransomguard.sim;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Random;

public class SimulatorMain {

    private static final String ROOT = "iot_test";
    private static final List<String> DEVICES = List.of("DeviceA", "DeviceB", "DeviceC", "DeviceD", "DeviceE");

    public static void main(String[] args) throws Exception {
        Path root = Paths.get(ROOT);
        Files.createDirectories(root);

        for (String d : DEVICES) {
            Files.createDirectories(root.resolve(d));
        }

        System.out.println("Simulator root: " + root.toAbsolutePath());
        System.out.println("Devices: " + DEVICES);

        Random r = new Random();

        // Phase 1: normal workload
        System.out.println("\n=== Phase 1: NORMAL workload (12 seconds) ===");
        long phase1End = System.currentTimeMillis() + 12_000;
        while (System.currentTimeMillis() < phase1End) {
            String dev = DEVICES.get(r.nextInt(DEVICES.size()));
            Path f = root.resolve(dev).resolve("normal_" + (r.nextInt(50)) + ".txt");
            Files.writeString(f, "hello " + System.nanoTime(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Thread.sleep(200);
        }

        // Phase 2: ransom-like burst
        System.out.println("\n=== Phase 2: RANSOM-LIKE workload (12 seconds) ===");
        long phase2End = System.currentTimeMillis() + 12_000;
        while (System.currentTimeMillis() < phase2End) {
            String dev = DEVICES.get(r.nextInt(DEVICES.size()));
            Path dir = root.resolve(dev);

            // burst writes
            for (int i = 0; i < 5; i++) {
                Path f = dir.resolve("enc_" + (r.nextInt(2000)) + ".locked");
                Files.writeString(f, "ENCRYPTED " + System.nanoTime(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }

            // rename spike
            Path a = dir.resolve("a_" + r.nextInt(1000) + ".tmp");
            Files.writeString(a, "tmp", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Path b = dir.resolve("b_" + r.nextInt(1000) + ".tmp");
            Files.move(a, b, StandardCopyOption.REPLACE_EXISTING);

            Thread.sleep(150);
        }

        System.out.println("\nDone. Open dashboard to see devices/events.");
    }
}
