package com.port.performance;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * Low-frequency process/thread CPU sampling. Does not pin threads, manage P/E cores,
 * claim GPU utilization, infer temperature or run shell commands.
 */
final class HardwareMonitor {
    private static final com.sun.management.OperatingSystemMXBean SYSTEM =
        ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean bean
            ? bean : null;
    private static final ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();
    private static final int LOGICAL = Math.max(1, Runtime.getRuntime().availableProcessors());
    private static long previousWall = -1L, previousProcess = -1L, previousClient = -1L;
    private static double processCpuPercent = -1, clientCpuPercent = -1;
    private static long freeSystemMb = -1, totalSystemMb = -1;
    private HardwareMonitor() {}

    static void sample() {
        long now = System.nanoTime();
        if (previousWall > 0 && now - previousWall < 1_000_000_000L) return;
        long processNs = SYSTEM == null ? -1 : SYSTEM.getProcessCpuTime();
        long clientNs = -1;
        if (THREADS.isCurrentThreadCpuTimeSupported()) {
            try {
                clientNs = THREADS.getCurrentThreadCpuTime();
            } catch (UnsupportedOperationException | SecurityException ignored) {}
        }
        if (SYSTEM != null) {
            // Physical RAM is shared with integrated graphics; this is not dedicated VRAM.
            long free = SYSTEM.getFreeMemorySize();
            long total = SYSTEM.getTotalMemorySize();
            freeSystemMb = free < 0 ? -1 : free / 1048576L;
            totalSystemMb = total < 0 ? -1 : total / 1048576L;
        }
        if (previousWall > 0) {
            long elapsed = now - previousWall;
            if (elapsed > 0 && previousProcess >= 0 && processNs >= previousProcess)
                processCpuPercent = clamp(100D * (processNs - previousProcess) / (elapsed * LOGICAL));
            if (elapsed > 0 && previousClient >= 0 && clientNs >= previousClient)
                clientCpuPercent = clamp(100D * (clientNs - previousClient) / elapsed);
        }
        previousWall = now;
        previousProcess = processNs;
        previousClient = clientNs;
    }
    private static double clamp(double d) {
        return Double.isFinite(d) ? Math.max(0, Math.min(100, d)) : -1;
    }
    static int logicalProcessors() { return LOGICAL; }
    static double processCpuPercent() { return processCpuPercent; }
    static double clientCpuPercent() { return clientCpuPercent; }
    static long freeSystemMb() { return freeSystemMb; }
    static long totalSystemMb() { return totalSystemMb; }
    static boolean memoryPressure() { return freeSystemMb >= 0 && freeSystemMb < 900; }
    static String hint() {
        if (memoryPressure()) return "RAM física baixa";
        if (clientCpuPercent > 85) return "thread do cliente muito ocupada";
        if (processCpuPercent > 85) return "CPU total do processo alta";
        return "sem gargalo confirmado";
    }
}
