package com.port.performance;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

/**
 * Conservative client-only, continuously evaluated adaptive renderer profile.
 * Never changes server rules, shaders, cosmetics, particles or third-party config.
 */
final class BoosterEngine {
    private static final FrameTimeMonitor MONITOR = new FrameTimeMonitor();
    private static final long WINDOW_NS = 5_000_000_000L;
    private static final long CHANGE_COOLDOWN_NS = 12_000_000_000L;
    private static final long REJECT_COOLDOWN_NS = 30_000_000_000L;
    private static FrameTimeMonitor.Snapshot last = FrameTimeMonitor.Snapshot.EMPTY;

    private static String mode = "off", phase = "idle", lastAction = "Booster desligado.";
    private static long windowStartNs, lastChangeNs, rejectedUntilNs;
    private static int slowWindows, fastWindows, windows;
    private static int originalChunks = -1, appliedChunks = -1, trialPreviousChunks = -1;
    private static double originalEntities = -1, appliedEntities = -1, trialPreviousEntities = -1;
    private static String trialType = "";
    private static double baselineAvg, trialAvg, baselineP95, differencePct;
    private static boolean checkedCrashRestore = false;
    private BoosterEngine() {}

    static void onFrame() {
        Minecraft mc = Minecraft.getInstance();
        MONITOR.onFrame(mc.level != null && mc.player != null && mc.screen == null &&
            !mc.isPaused() && mc.isWindowActive());
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        recoverPreviousSession(mc);
        if (mc.player == null || mc.level == null) {
            if (!"off".equals(mode)) disable();
            MONITOR.reset();
            windowStartNs = 0;
            return;
        }
        if (mc.screen != null || mc.isPaused() || !mc.isWindowActive()) {
            MONITOR.reset();
            windowStartNs = 0;
            return;
        }
        long now = System.nanoTime();
        if (windowStartNs == 0) {
            windowStartNs = now;
            return;
        }
        if (now - windowStartNs < WINDOW_NS) return;
        windowStartNs = now;
        FrameTimeMonitor.Snapshot sample = MONITOR.takeWindow();
        if (sample.frames() < 30) return;
        last = sample;
        windows++;
        if (!"adaptive".equals(mode)) {
            if ("observe".equals(mode)) phase = "observing";
            return;
        }
        checkManualChanges(mc);
        if (!trialType.isEmpty()) {
            finishTrial(mc, sample, now);
            return;
        }
        int target = PerformanceConfig.boosterTargetFps;
        boolean struggling = sample.averageFps() < target * 0.90 ||
            (sample.averageFps() < target && sample.low1Fps() < target * 0.60);
        boolean comfortable = sample.averageFps() > target * 1.18 &&
            sample.low1Fps() > target * 0.78 && sample.stutterPercent() < 3.0;
        slowWindows = struggling ? slowWindows + 1 : 0;
        fastWindows = comfortable ? fastWindows + 1 : 0;
        if (now - lastChangeNs < CHANGE_COOLDOWN_NS) return;
        if (slowWindows >= 2 && now >= rejectedUntilNs) {
            if (reduceOneStep(mc, sample, now)) {
                slowWindows = 0;
                fastWindows = 0;
            } else {
                phase = "no-change";
                lastAction = "Limite de otimização segura atingido. Magias e modelos preservados.";
            }
        } else if (fastWindows >= 3 && hasChanges()) {
            restoreOneStep(mc);
            fastWindows = 0;
            slowWindows = 0;
            lastChangeNs = now;
            phase = "kept";
            lastAction = "FPS estável: recuperando qualidade visual gradualmente.";
        } else if (!struggling) {
            phase = "target-met";
        }
    }

    private static boolean reduceOneStep(Minecraft mc, FrameTimeMonitor.Snapshot before, long now) {
        if (PerformanceConfig.boosterAdjustChunks) {
            int current = mc.options.renderDistance().get();
            int base = originalChunks > 0 ? originalChunks : current;
            int floor = Math.max(Math.min(base, PerformanceConfig.boosterMinRenderDistance), base - 4);
            if (current > floor) {
                if (originalChunks < 0) originalChunks = current;
                trialPreviousChunks = current;
                appliedChunks = current - 1;
                PerformanceConfig.notePendingRenderRestore(originalChunks, appliedChunks);
                mc.options.renderDistance().set(appliedChunks);
                mc.options.save();
                beginTrial("chunks", before, now, "Ajuste de chunks: " + current + " para " + appliedChunks + ".");
                return true;
            }
        }
        // Optional only: distant custom armour and player models remain visible by default.
        if (PerformanceConfig.boosterAdjustEntities) {
            double current = mc.options.entityDistanceScaling().get();
            double base = originalEntities > 0 ? originalEntities : current;
            double floor = Math.max(0.75, base * 0.80);
            if (current > floor + 0.025) {
                if (originalEntities < 0) originalEntities = current;
                trialPreviousEntities = current;
                appliedEntities = Math.max(floor, Math.round((current - 0.05) * 100D) / 100D);
                PerformanceConfig.notePendingRestore(originalEntities, appliedEntities);
                mc.options.entityDistanceScaling().set(appliedEntities);
                mc.options.save();
                beginTrial("entities", before, now, "Ajuste opcional de entidades: " +
                    Math.round(current * 100) + "% para " + Math.round(appliedEntities * 100) + "%.");
                return true;
            }
        }
        return false;
    }

    private static void beginTrial(String type, FrameTimeMonitor.Snapshot before, long now, String action) {
        trialType = type;
        baselineAvg = before.averageFps();
        baselineP95 = before.p95Ms();
        trialAvg = 0;
        differencePct = 0;
        phase = "trial";
        lastAction = action + " Avaliando benefício com frametimes reais.";
        lastChangeNs = now;
    }

    private static void finishTrial(Minecraft mc, FrameTimeMonitor.Snapshot after, long now) {
        trialAvg = after.averageFps();
        differencePct = baselineAvg > 0 ? 100D * (trialAvg - baselineAvg) / baselineAvg : 0;
        boolean improved = trialAvg >= baselineAvg * 1.035 ||
            (baselineP95 > 0 && after.p95Ms() <= baselineP95 * 0.92);
        if (improved) {
            phase = "kept";
            lastAction = "Melhora observada de " + Math.round(differencePct) +
                "% em FPS médio (p95 " + Math.round(after.p95Ms()) + " ms). Ajuste mantido.";
        } else {
            undoTrial(mc);
            phase = "reverted";
            lastAction = "Mudança sem melhora consistente. Último ajuste revertido.";
            rejectedUntilNs = now + REJECT_COOLDOWN_NS;
        }
        trialType = "";
        slowWindows = 0;
        fastWindows = 0;
        lastChangeNs = now;
    }

    private static void undoTrial(Minecraft mc) {
        if ("chunks".equals(trialType) && appliedChunks > 0 &&
            mc.options.renderDistance().get() == appliedChunks) {
            mc.options.renderDistance().set(trialPreviousChunks);
            appliedChunks = trialPreviousChunks == originalChunks ? -1 : trialPreviousChunks;
            if (appliedChunks < 0) {
                originalChunks = -1;
                PerformanceConfig.clearPendingRenderRestore();
            } else PerformanceConfig.notePendingRenderRestore(originalChunks, appliedChunks);
            mc.options.save();
        } else if ("entities".equals(trialType) && appliedEntities > 0 &&
            Math.abs(mc.options.entityDistanceScaling().get() - appliedEntities) < 0.001) {
            mc.options.entityDistanceScaling().set(trialPreviousEntities);
            appliedEntities = Math.abs(trialPreviousEntities - originalEntities) < 0.001 ? -1 : trialPreviousEntities;
            if (appliedEntities < 0) {
                originalEntities = -1;
                PerformanceConfig.clearPendingRestore();
            } else PerformanceConfig.notePendingRestore(originalEntities, appliedEntities);
            mc.options.save();
        }
    }

    private static void restoreOneStep(Minecraft mc) {
        if (appliedEntities > 0 && originalEntities > 0) {
            if (Math.abs(mc.options.entityDistanceScaling().get() - appliedEntities) < 0.001) {
                double value = Math.min(originalEntities, appliedEntities + 0.05);
                mc.options.entityDistanceScaling().set(value);
                appliedEntities = Math.abs(value - originalEntities) < 0.001 ? -1 : value;
                if (appliedEntities < 0) {
                    originalEntities = -1;
                    PerformanceConfig.clearPendingRestore();
                } else PerformanceConfig.notePendingRestore(originalEntities, appliedEntities);
                mc.options.save();
                return;
            }
        }
        if (appliedChunks > 0 && originalChunks > 0 &&
            mc.options.renderDistance().get() == appliedChunks) {
            int value = Math.min(originalChunks, appliedChunks + 1);
            mc.options.renderDistance().set(value);
            appliedChunks = value == originalChunks ? -1 : value;
            if (appliedChunks < 0) {
                originalChunks = -1;
                PerformanceConfig.clearPendingRenderRestore();
            } else PerformanceConfig.notePendingRenderRestore(originalChunks, appliedChunks);
            mc.options.save();
        }
    }

    private static void checkManualChanges(Minecraft mc) {
        if (appliedChunks > 0 && mc.options.renderDistance().get() != appliedChunks) {
            originalChunks = -1;
            appliedChunks = -1;
            PerformanceConfig.clearPendingRenderRestore();
            if ("chunks".equals(trialType)) trialType = "";
        }
        if (appliedEntities > 0 &&
            Math.abs(mc.options.entityDistanceScaling().get() - appliedEntities) > 0.001) {
            originalEntities = -1;
            appliedEntities = -1;
            PerformanceConfig.clearPendingRestore();
            if ("entities".equals(trialType)) trialType = "";
        }
    }

    private static boolean hasChanges() {
        return appliedChunks > 0 || appliedEntities > 0;
    }

    private static void recoverPreviousSession(Minecraft mc) {
        if (checkedCrashRestore) return;
        checkedCrashRestore = true;
        boolean changed = false;
        if (PerformanceConfig.pendingRestoreOriginal > 0) {
            if (Math.abs(mc.options.entityDistanceScaling().get() -
                PerformanceConfig.pendingRestoreApplied) < 0.001) {
                mc.options.entityDistanceScaling().set(PerformanceConfig.pendingRestoreOriginal);
                changed = true;
            }
            PerformanceConfig.clearPendingRestore();
        }
        if (PerformanceConfig.pendingRenderOriginal > 0) {
            if (mc.options.renderDistance().get() == PerformanceConfig.pendingRenderApplied) {
                mc.options.renderDistance().set(PerformanceConfig.pendingRenderOriginal);
                changed = true;
            }
            PerformanceConfig.clearPendingRenderRestore();
        }
        if (changed) mc.options.save();
    }

    static void observe() {
        restore("Restauradas as configurações do booster.");
        mode = "observe";
        phase = "observing";
        resetCounters();
        lastAction = "Monitoramento real de frametimes, sem alterações gráficas.";
    }
    static void adaptive() {
        restore("Restauradas as configurações do booster.");
        mode = "adaptive";
        phase = "baseline";
        resetCounters();
        lastAction = "Otimização contínua ativada. Chunks adaptativos; entidades opcionais.";
    }
    static void disable() {
        restore("Ajustes revertidos.");
        mode = "off";
        phase = "idle";
        resetCounters();
        lastAction = "Booster desligado e configurações originais restauradas.";
    }
    static void reset() {
        disable();
        MONITOR.reset();
        last = FrameTimeMonitor.Snapshot.EMPTY;
        lastAction = "Histórico reiniciado e ajustes restaurados.";
    }
    static void setTarget(int target) {
        PerformanceConfig.boosterTargetFps = Math.max(30, Math.min(240, target));
        PerformanceConfig.save();
        slowWindows = fastWindows = 0;
    }
    static void allowChunks(boolean allow) {
        PerformanceConfig.boosterAdjustChunks = allow;
        PerformanceConfig.save();
        if ("adaptive".equals(mode)) adaptive();
    }
    static void allowEntities(boolean allow) {
        PerformanceConfig.boosterAdjustEntities = allow;
        PerformanceConfig.save();
        if ("adaptive".equals(mode)) adaptive();
    }

    static void webAction(String action, Integer target) {
        if (!PerformanceConfig.webControlEnabled) return;
        if (target != null) setTarget(target);
        if (action == null) return;
        switch (action) {
            case "observe" -> observe();
            case "auto" -> adaptive();
            case "off" -> disable();
            case "reset" -> reset();
            default -> {}
        }
    }

    private static void restore(String note) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            boolean changed = false;
            if (appliedChunks > 0 && originalChunks > 0 &&
                mc.options.renderDistance().get() == appliedChunks) {
                mc.options.renderDistance().set(originalChunks);
                changed = true;
            }
            if (appliedEntities > 0 && originalEntities > 0 &&
                Math.abs(mc.options.entityDistanceScaling().get() - appliedEntities) < 0.001) {
                mc.options.entityDistanceScaling().set(originalEntities);
                changed = true;
            }
            if (changed) mc.options.save();
        }
        PerformanceConfig.clearPendingRestore();
        PerformanceConfig.clearPendingRenderRestore();
        originalChunks = appliedChunks = trialPreviousChunks = -1;
        originalEntities = appliedEntities = trialPreviousEntities = -1;
        trialType = "";
        lastAction = note;
    }

    private static void resetCounters() {
        slowWindows = fastWindows = windows = 0;
        baselineAvg = trialAvg = baselineP95 = differencePct = 0;
        windowStartNs = lastChangeNs = rejectedUntilNs = 0;
        MONITOR.reset();
    }

    static String status() {
        return "Modo " + mode + " | Fase " + phase + " | Meta " +
            PerformanceConfig.boosterTargetFps + " FPS | " + lastAction;
    }
    static double low1() { return last.low1Fps(); }
    static double p95() { return last.p95Ms(); }
    static double stutters() { return last.stutterPercent(); }
    static int currentChunks() {
        Minecraft mc = Minecraft.getInstance();
        return mc.options == null ? 0 : mc.options.renderDistance().get();
    }

    static void snapshot(JsonObject json) {
        JsonObject b = new JsonObject();
        b.addProperty("mode", mode);
        b.addProperty("phase", phase);
        b.addProperty("targetFps", PerformanceConfig.boosterTargetFps);
        b.addProperty("webControlEnabled", PerformanceConfig.webControlEnabled);
        b.addProperty("baselineAvgFps", round(baselineAvg));
        b.addProperty("trialAvgFps", round(trialAvg));
        b.addProperty("differencePct", round(differencePct));
        b.addProperty("secondsInPhase", windows * 5);
        b.addProperty("originalEntityScale", originalEntities > 0 ? originalEntities : 1);
        b.addProperty("appliedEntityScale", appliedEntities);
        b.addProperty("changedOption", hasChanges());
        b.addProperty("low1SampledFps", round(last.low1Fps()));
        b.addProperty("realLow1Fps", round(last.low1Fps()));
        b.addProperty("frameP95Ms", round(last.p95Ms()));
        b.addProperty("frameP99Ms", round(last.p99Ms()));
        b.addProperty("stutterPercent", round(last.stutterPercent()));
        b.addProperty("measuredAvgFps", round(last.averageFps()));
        b.addProperty("renderDistance", currentChunks());
        b.addProperty("changedRenderDistance", appliedChunks > 0);
        b.addProperty("originalRenderDistance", originalChunks);
        b.addProperty("allowChunkScaling", PerformanceConfig.boosterAdjustChunks);
        b.addProperty("allowEntityScaling", PerformanceConfig.boosterAdjustEntities);
        b.addProperty("lastAction", lastAction);
        json.add("booster", b);
    }
    private static double round(double d) {
        return Double.isFinite(d) ? Math.round(d * 10D) / 10D : 0;
    }
}
