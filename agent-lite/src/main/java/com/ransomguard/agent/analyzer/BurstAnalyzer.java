package com.ransomguard.agent.analyzer;

import java.util.concurrent.atomic.AtomicInteger;

public class BurstAnalyzer {

    private final AtomicInteger writes = new AtomicInteger();
    private final AtomicInteger creates = new AtomicInteger();

    public void incWrite() {
        writes.incrementAndGet();
    }

    public void incCreate() {
        creates.incrementAndGet();
    }

    public int flushWrites() {
        return writes.getAndSet(0);
    }

    public int flushCreates() {
        return creates.getAndSet(0);
    }
}
