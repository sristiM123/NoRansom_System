package com.ransomguard.sim.workload;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

public class RansomLikeWorkload {

    private final Random rnd = new Random();

    public void run(Path baseDir, int seconds) throws Exception {
        // All attack activity stays inside this folder (safe containment)
        Path attackDir = baseDir.resolve("_sim_attack");
        Files.createDirectories(attackDir);

        long end = System.currentTimeMillis() + seconds * 1000L;

        int fileCounter = 0;

        while (System.currentTimeMillis() < end) {
            // 1) burst create/write
            for (int k = 0; k < 15; k++) {
                Path f = attackDir.resolve("file_" + (fileCounter++) + ".bin");

                // write high-entropy bytes (random) — simulates encrypted-like output
                byte[] data = new byte[4096];
                rnd.nextBytes(data);
                Files.write(f, data);
            }

            // 2) rename spike (ransomware often renames extensions)
            // Rename a few of the recent files
            for (int r = 0; r < 6; r++) {
                int pick = Math.max(0, fileCounter - 1 - rnd.nextInt(20));
                Path from = attackDir.resolve("file_" + pick + ".bin");
                if (Files.exists(from)) {
                    Path to = attackDir.resolve("file_" + pick + ".locked");
                    try {
                        Files.move(from, to);
                    } catch (Exception ignored) {
                        // ignore if file is busy
                    }
                }
            }

            // tiny pause so bursts come in waves
            Thread.sleep(250);
        }
    }
}
