package com.ransomguard.sim;

import java.nio.file.Path;

public class SafePath {
    // Safety guard: simulator will only run if folder path contains these tokens
    public static void assertSafe(Path target) {
        String p = target.toAbsolutePath().toString().toLowerCase();

        if (!p.contains("iot_test") && !p.contains("iot-test")) {
            throw new IllegalStateException("SAFETY STOP: target is not inside iot_test / iot-test");
        }
        if (!p.contains("devicea")) {
            throw new IllegalStateException("SAFETY STOP: target is not DeviceA");
        }
    }
}
