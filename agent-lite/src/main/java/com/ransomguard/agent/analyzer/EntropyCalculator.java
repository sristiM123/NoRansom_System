package com.ransomguard.agent.analyzer;

import java.io.File;
import java.io.FileInputStream;

public class EntropyCalculator {

    public static double entropy(File f) {
        try (FileInputStream in = new FileInputStream(f)) {
            int[] freq = new int[256];
            int b;
            int total = 0;

            while ((b = in.read()) != -1 && total < 4096) {
                freq[b & 0xff]++;
                total++;
            }

            double entropy = 0.0;
            for (int c : freq) {
                if (c == 0) continue;
                double p = (double) c / total;
                entropy -= p * (Math.log(p) / Math.log(2));
            }
            return entropy;
        } catch (Exception e) {
            return 0.0;
        }
    }
}
