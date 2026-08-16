package com.ransomguard.sim.workload;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Random;

public class NormalWorkload {

    private final Random rnd = new Random();

    public void run(Path baseDir, int seconds) throws Exception {
        Path normalDir = baseDir.resolve("_sim_normal");
        Files.createDirectories(normalDir);

        long end = System.currentTimeMillis() + seconds * 1000L;
        int i = 0;

        while (System.currentTimeMillis() < end) {
            // small periodic “sensor log”
            String name = "log_" + (i++) + ".txt";
            Path f = normalDir.resolve(name);

            String content = "timestamp=" + Instant.now() + "\n"
                    + "temp=" + (20 + rnd.nextInt(10)) + "\n"
                    + "status=OK\n";

            Files.writeString(f, content, StandardCharsets.UTF_8);

            // small update to existing file
            if (i % 4 == 0) {
                Files.writeString(f, content + "updated=true\n", StandardCharsets.UTF_8);
            }

            Thread.sleep(350); // slow and normal
        }
    }
}
