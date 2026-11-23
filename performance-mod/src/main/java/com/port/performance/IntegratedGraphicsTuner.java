package com.port.performance;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;

/** Optional iGPU-friendly visual settings. Player changes are never overwritten. */
final class IntegratedGraphicsTuner {
    private static int originalBlend = -1, appliedBlend = -1, previousBlend = -1;
    private static CloudStatus originalClouds, appliedClouds, previousClouds;
    private IntegratedGraphicsTuner() {}

    static boolean tryBlend(Minecraft mc) {
        if (!PerformanceConfig.boosterAdjustBiomeBlend) return false;
        int now = mc.options.biomeBlendRadius().get();
        int baseline = originalBlend >= 0 ? originalBlend : now;
        int floor = Math.max(0, baseline - 2);
        if (now <= floor) return false;
        if (originalBlend < 0) originalBlend = now;
        previousBlend = now;
        appliedBlend = now - 1;
        PerformanceConfig.notePendingBlendRestore(originalBlend, appliedBlend);
        mc.options.biomeBlendRadius().set(appliedBlend);
        mc.options.save();
        return true;
    }

    static boolean tryClouds(Minecraft mc) {
        if (!PerformanceConfig.boosterAdjustClouds) return false;
        CloudStatus now = mc.options.cloudStatus().get();
        CloudStatus next = now == CloudStatus.FANCY ? CloudStatus.FAST :
            now == CloudStatus.FAST ? CloudStatus.OFF : now;
        if (next == now) return false;
        if (originalClouds == null) originalClouds = now;
        previousClouds = now;
        appliedClouds = next;
        PerformanceConfig.notePendingCloudRestore(originalClouds.name(), appliedClouds.name());
        mc.options.cloudStatus().set(appliedClouds);
        mc.options.save();
        return true;
    }

    static void undoLast(Minecraft mc, String type) {
        if ("blend".equals(type) && appliedBlend >= 0 &&
            mc.options.biomeBlendRadius().get() == appliedBlend) {
            mc.options.biomeBlendRadius().set(previousBlend);
            appliedBlend = previousBlend == originalBlend ? -1 : previousBlend;
            if (appliedBlend < 0) {
                originalBlend = -1;
                PerformanceConfig.clearPendingBlendRestore();
            } else PerformanceConfig.notePendingBlendRestore(originalBlend, appliedBlend);
            mc.options.save();
        } else if ("clouds".equals(type) && appliedClouds != null &&
            mc.options.cloudStatus().get() == appliedClouds) {
            mc.options.cloudStatus().set(previousClouds);
            appliedClouds = previousClouds == originalClouds ? null : previousClouds;
            if (appliedClouds == null) {
                originalClouds = null;
                PerformanceConfig.clearPendingCloudRestore();
            } else PerformanceConfig.notePendingCloudRestore(originalClouds.name(), appliedClouds.name());
            mc.options.save();
        }
    }

    static boolean restoreOne(Minecraft mc) {
        if (appliedClouds != null && originalClouds != null &&
            mc.options.cloudStatus().get() == appliedClouds) {
            CloudStatus next = appliedClouds == CloudStatus.OFF && originalClouds != CloudStatus.OFF
                ? CloudStatus.FAST : originalClouds;
            mc.options.cloudStatus().set(next);
            appliedClouds = next == originalClouds ? null : next;
            if (appliedClouds == null) {
                originalClouds = null;
                PerformanceConfig.clearPendingCloudRestore();
            } else PerformanceConfig.notePendingCloudRestore(originalClouds.name(), appliedClouds.name());
            mc.options.save();
            return true;
        }
        if (appliedBlend >= 0 && originalBlend >= 0 &&
            mc.options.biomeBlendRadius().get() == appliedBlend) {
            int next = Math.min(originalBlend, appliedBlend + 1);
            mc.options.biomeBlendRadius().set(next);
            appliedBlend = next == originalBlend ? -1 : next;
            if (appliedBlend < 0) {
                originalBlend = -1;
                PerformanceConfig.clearPendingBlendRestore();
            } else PerformanceConfig.notePendingBlendRestore(originalBlend, appliedBlend);
            mc.options.save();
            return true;
        }
        return false;
    }

    static void checkManual(Minecraft mc) {
        if (appliedBlend >= 0 && mc.options.biomeBlendRadius().get() != appliedBlend) {
            originalBlend = appliedBlend = previousBlend = -1;
            PerformanceConfig.clearPendingBlendRestore();
        }
        if (appliedClouds != null && mc.options.cloudStatus().get() != appliedClouds) {
            originalClouds = appliedClouds = previousClouds = null;
            PerformanceConfig.clearPendingCloudRestore();
        }
    }

    static boolean hasChanges() { return appliedBlend >= 0 || appliedClouds != null; }
    static int blend() { return appliedBlend; }
    static String cloudChange() { return appliedClouds == null ? "none" : appliedClouds.name(); }

    static void restore(Minecraft mc) {
        if (mc.options != null) {
            boolean modified = false;
            if (appliedBlend >= 0 && originalBlend >= 0 &&
                mc.options.biomeBlendRadius().get() == appliedBlend) {
                mc.options.biomeBlendRadius().set(originalBlend);
                modified = true;
            }
            if (appliedClouds != null && originalClouds != null &&
                mc.options.cloudStatus().get() == appliedClouds) {
                mc.options.cloudStatus().set(originalClouds);
                modified = true;
            }
            if (modified) mc.options.save();
        }
        originalBlend = appliedBlend = previousBlend = -1;
        originalClouds = appliedClouds = previousClouds = null;
        PerformanceConfig.clearPendingBlendRestore();
        PerformanceConfig.clearPendingCloudRestore();
    }

    static void recover(Minecraft mc) {
        boolean modified = false;
        if (PerformanceConfig.pendingBlendOriginal >= 0 &&
            mc.options.biomeBlendRadius().get() == PerformanceConfig.pendingBlendApplied) {
            mc.options.biomeBlendRadius().set(PerformanceConfig.pendingBlendOriginal);
            modified = true;
        }
        PerformanceConfig.clearPendingBlendRestore();

        if (!PerformanceConfig.pendingCloudOriginal.isBlank() &&
            !PerformanceConfig.pendingCloudApplied.isBlank()) {
            try {
                CloudStatus original = CloudStatus.valueOf(PerformanceConfig.pendingCloudOriginal);
                CloudStatus applied = CloudStatus.valueOf(PerformanceConfig.pendingCloudApplied);
                if (mc.options.cloudStatus().get() == applied) {
                    mc.options.cloudStatus().set(original);
                    modified = true;
                }
            } catch (IllegalArgumentException ignored) {}
        }
        PerformanceConfig.clearPendingCloudRestore();
        if (modified) mc.options.save();
    }
}
