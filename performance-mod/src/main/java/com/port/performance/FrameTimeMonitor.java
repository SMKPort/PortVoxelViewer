package com.port.performance;

import java.util.Arrays;

/** Frame timings measured after each rendered frame, not inferred from one FPS reading per second. */
final class FrameTimeMonitor {
    private static final int CAPACITY = 2400;
    private static final long MAX_FRAME_NS = 500_000_000L;
    private final long[] intervals = new long[CAPACITY];
    private int count, next;
    private long previousNs;

    void onFrame(boolean active) {
        if (!active) {
            previousNs = 0;
            return;
        }
        long now = System.nanoTime();
        if (previousNs != 0) {
            long dt = now - previousNs;
            if (dt > 0 && dt <= MAX_FRAME_NS) {
                intervals[next] = dt;
                next = (next + 1) % CAPACITY;
                if (count < CAPACITY) count++;
            }
        }
        previousNs = now;
    }

    void reset() {
        count = 0;
        next = 0;
        previousNs = 0;
    }

    Snapshot takeWindow() {
        int n = count;
        if (n < 30) {
            reset();
            return Snapshot.EMPTY;
        }
        long[] sorted = Arrays.copyOf(intervals, n);
        Arrays.sort(sorted);
        double total = 0;
        int stutters = 0;
        for (long ns : sorted) {
            total += ns;
            if (ns > 50_000_000L) stutters++;
        }
        int lowCount = Math.max(1, (int)Math.ceil(n * 0.01));
        double slowNs = 0;
        for (int i = n - lowCount; i < n; i++) slowNs += sorted[i];
        Snapshot result = new Snapshot(
            1_000_000_000D * n / total,
            sorted[Math.max(0, (int)Math.ceil(n * 0.95) - 1)] / 1_000_000D,
            sorted[Math.max(0, (int)Math.ceil(n * 0.99) - 1)] / 1_000_000D,
            1_000_000_000D * lowCount / slowNs,
            100D * stutters / n, n
        );
        reset();
        return result;
    }

    record Snapshot(double averageFps, double p95Ms, double p99Ms,
                    double low1Fps, double stutterPercent, int frames) {
        static final Snapshot EMPTY = new Snapshot(0, 0, 0, 0, 0, 0);
    }
}
