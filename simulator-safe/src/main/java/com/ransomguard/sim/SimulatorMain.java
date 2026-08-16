package com.ransomguard.sim;

import com.ransomguard.sim.workload.NormalWorkload;
import com.ransomguard.sim.workload.RansomLikeWorkload;

import java.nio.file.*;

/**
 * Safe two-phase workload generator.
 *
 * Phase 1 - NORMAL: slow, low-entropy "sensor log" writes on DeviceA.
 * Phase 2 - RANSOM-LIKE: fast bursts of high-entropy files plus rename storms,
 *           contained to iot_test/DeviceA/_sim_attack (enforced by SafePath).
 *
 * Usage:
 *   java -jar simulator-safe.jar [rootDir] [normalSeconds] [attackSeconds]
 */
public class SimulatorMain {

    public static void main(String[] args) throws Exception {
        String rootDir = args.length > 0 ? args[0] : "iot_test";
        int normalSeconds = args.length > 1 ? Integer.parseInt(args[1]) : 15;
        int attackSeconds = args.length > 2 ? Integer.parseInt(args[2]) : 15;

        Path target = Paths.get(rootDir).resolve("DeviceA");
        Files.createDirectories(target);

        // Hard safety guard: refuse to run outside the sandbox folder.
        SafePath.assertSafe(target);

        System.out.println("Simulator target: " + target.toAbsolutePath());

        System.out.println("\n=== Phase 1: NORMAL workload (" + normalSeconds + "s) ===");
        new NormalWorkload().run(target, normalSeconds);

        System.out.println("\n=== Phase 2: RANSOM-LIKE workload (" + attackSeconds + "s) ===");
        new RansomLikeWorkload().run(target, attackSeconds);

        System.out.println("\nDone. Open the dashboard at http://localhost:9004 to see alerts.");
    }
}
