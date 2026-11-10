package dev.port.clientbooster;

import java.util.Arrays;

/** Medicoes locais de frames (sem consultar o servidor ou o renderizador de outros mods). */
public final class FrameMetrics {
    private static final int CAPACITY = 1200;
    private static final long MAX_VALID_FRAME_NS = 500_000_000L;
    private final long[] durationsNs = new long[CAPACITY];
    private int count;
    private int next;
    private long lastFrameNs;

    public void observe(long nowNs) {
        if (lastFrameNs > 0L) {
            long elapsed = nowNs - lastFrameNs;
            // Evita contabilizar longas pausas ao alternar entre aplicativos.
            if (elapsed > 0L && elapsed <= MAX_VALID_FRAME_NS) {
                durationsNs[next] = elapsed;
                next = (next + 1) % CAPACITY;
                if (count < CAPACITY) count++;
            }
        }
        lastFrameNs = nowNs;
    }

    public Snapshot snapshot() {
        if (count < 20) return new Snapshot(Double.NaN, Double.NaN, count);
        long[] copy = Arrays.copyOf(durationsNs, count);
        Arrays.sort(copy);
        int p95Index = (int) Math.ceil(count * 0.95D) - 1;
        double p95Ms = copy[Math.max(0, p95Index)] / 1_000_000.0D;
        int slowSamples = Math.max(1, (int) Math.ceil(count * 0.01D));
        double slowSum = 0D;
        for (int i = count - slowSamples; i < count; i++) slowSum += copy[i];
        double onePercentLowFps = (1_000_000_000.0D * slowSamples) / slowSum;
        return new Snapshot(p95Ms, onePercentLowFps, count);
    }

    public void reset() {
        count = 0;
        next = 0;
        lastFrameNs = 0L;
    }

    public record Snapshot(double p95FrameMs, double onePercentLowFps, int frames) {}
}
