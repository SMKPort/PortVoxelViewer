package dev.port.clientbooster;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

/**
 * Ajusta exclusivamente opcoes graficas do cliente.
 * Sem threads extras, Mixin, modificacoes no mundo ou gameplay.
 */
public final class AdaptivePerformanceController {
    private static final Logger LOGGER = LoggerFactory.getLogger("PortClientBooster");
    private final FrameMetrics frameMetrics = new FrameMetrics();
    private boolean environmentReported;
    private FrameMetrics.Snapshot lastSnapshot = new FrameMetrics.Snapshot(Double.NaN, Double.NaN, 0);
    private static final long EVALUATE_NS = 5_000_000_000L;
    private static final double STEP = 0.10D;
    private static final double EPS = 0.0001D;

    private long sampleStartedAt;
    private long lastEvaluationAt;
    private int frameCount;
    private double measuredFps;
    private double smoothedFps;
    private int slowWindows;
    private int fastWindows;

    private Double originalEntityScale;
    private Double lastAppliedEntityScale;
    private ParticleStatus originalParticles;
    private ParticleStatus lastAppliedParticles;

    @SubscribeEvent
    public void onFrame(RenderFrameEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!BoosterConfig.ENABLED.get() || mc.level == null || mc.screen != null || mc.isPaused()) {
            return;
        }
        if (sampleStartedAt == 0L) sampleStartedAt = System.nanoTime();
        frameCount++;
        frameMetrics.observe(System.nanoTime());
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !BoosterConfig.ENABLED.get()) {
            restore(mc);
            resetMeasurements();
            return;
        }
        if (mc.screen != null || mc.isPaused()) {
            resetMeasurements();
            return;
        }
        if (!environmentReported) {
            environmentReported = true;
            LOGGER.info("Port Client Booster: mods visuais detectados: {}", ModEnvironmentSummary.describe());
            LOGGER.info("Perfil conservador: sem alterar distancia de entidades ou particulas, a menos que o jogador habilite.");
        }
        if (originalEntityScale == null) {
            originalEntityScale = mc.options.entityDistanceScaling().get();
        }
        if (originalParticles == null) {
            originalParticles = mc.options.particles().get();
        }
        long now = System.nanoTime();
        if (sampleStartedAt == 0L) sampleStartedAt = now;
        if (lastEvaluationAt == 0L) lastEvaluationAt = now;
        if (now - lastEvaluationAt < EVALUATE_NS) return;

        long elapsedNs = now - sampleStartedAt;
        measuredFps = elapsedNs > 0L ? frameCount * 1_000_000_000D / elapsedNs : 0D;
        lastSnapshot = frameMetrics.snapshot();
        frameMetrics.reset();
        if (frameCount >= 15 && measuredFps > 0D) {
            smoothedFps = smoothedFps == 0D ? measuredFps : smoothedFps * 0.60D + measuredFps * 0.40D;
            evaluate(mc);
            if (BoosterConfig.LOG_DIAGNOSTICS.get()) {
                LOGGER.info("FPS={}; frametime_p95_ms={}; 1pct_low_fps={}; escala_entidades={}",
                    String.format(Locale.ROOT, "%.1f", smoothedFps),
                    String.format(Locale.ROOT, "%.2f", lastSnapshot.p95FrameMs()),
                    String.format(Locale.ROOT, "%.1f", lastSnapshot.onePercentLowFps()),
                    mc.options.entityDistanceScaling().get());
            }
        }
        sampleStartedAt = now;
        lastEvaluationAt = now;
        frameCount = 0;
    }

    private void evaluate(Minecraft mc) {
        final int target = BoosterConfig.TARGET_FPS.get();
        if (smoothedFps < target * 0.85D) {
            slowWindows++;
            fastWindows = 0;
        } else if (smoothedFps > target * 1.20D) {
            fastWindows++;
            slowWindows = 0;
        } else {
            slowWindows = 0;
            fastWindows = 0;
        }

        if (BoosterConfig.ADAPT_ENTITIES.get()) {
            double actual = mc.options.entityDistanceScaling().get();
            // Uma mudanca de outro mod/jogador define nova preferencia original.
            if (lastAppliedEntityScale != null && Math.abs(lastAppliedEntityScale - actual) > EPS) {
                originalEntityScale = actual;
                lastAppliedEntityScale = null;
            }
            if (slowWindows >= 2 || fastWindows >= 3) {
                double lower = Math.min(originalEntityScale, BoosterConfig.MIN_ENTITY_SCALE.get());
                double next = slowWindows >= 2 ? Math.max(lower, actual - STEP)
                    : Math.min(originalEntityScale, actual + STEP);
                next = Math.round(next * 20D) / 20D;
                if (Math.abs(next - actual) > EPS) {
                    mc.options.entityDistanceScaling().set(next);
                    lastAppliedEntityScale = next;
                }
            }
        } else {
            restoreEntities(mc);
        }

        if (BoosterConfig.ADAPT_PARTICLES.get()) {
            ParticleStatus current = mc.options.particles().get();
            if (lastAppliedParticles != null && current != lastAppliedParticles) {
                originalParticles = current;
                lastAppliedParticles = null;
            }
            ParticleStatus desired = originalParticles;
            if (slowWindows >= 2) {
                if (smoothedFps < target * 0.55D) desired = ParticleStatus.MINIMAL;
                else if (smoothedFps < target * 0.75D) desired = ParticleStatus.DECREASED;
            }
            if (fastWindows >= 3) desired = originalParticles;
            // Nunca elevar a quantidade acima da preferencia original.
            if (desired.ordinal() < originalParticles.ordinal()) desired = originalParticles;
            if (desired != current && (slowWindows >= 2 || fastWindows >= 3)) {
                mc.options.particles().set(desired);
                lastAppliedParticles = desired;
            }
        } else {
            restoreParticles(mc);
        }

        if (slowWindows >= 2) slowWindows = 0;
        if (fastWindows >= 3) fastWindows = 0;
    }

    @SubscribeEvent
    public void onHud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!BoosterConfig.SHOW_HUD.get() || mc.level == null || mc.options.hideGui || mc.screen != null) return;
        String fps = smoothedFps <= 0 ? "--" : String.format(Locale.ROOT, "%.0f", smoothedFps);
        String scale = String.format(Locale.ROOT, "%.0f%%", mc.options.entityDistanceScaling().get() * 100D);
        String low = Double.isNaN(lastSnapshot.onePercentLowFps()) ? "--"
            : String.format(Locale.ROOT, "%.0f", lastSnapshot.onePercentLowFps());
        event.getGuiGraphics().drawString(mc.font, "PORT BOOST | " + fps + " FPS | 1% low " + low + " | Entidades " + scale,
            6, 6, 0xFFD46B, true);
    }

    @SubscribeEvent
    public void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        restore(Minecraft.getInstance());
        resetMeasurements();
    }

    @SubscribeEvent
    public void onClose(GameShuttingDownEvent event) {
        restore(Minecraft.getInstance());
        Minecraft.getInstance().options.save();
    }

    private void restore(Minecraft mc) {
        restoreEntities(mc);
        restoreParticles(mc);
    }

    private void restoreEntities(Minecraft mc) {
        if (originalEntityScale == null) return;
        // Nao sobrescrever mudancas manuais ou feitas por outro mod.
        double actual = mc.options.entityDistanceScaling().get();
        if (lastAppliedEntityScale != null && Math.abs(actual - lastAppliedEntityScale) < EPS) {
            mc.options.entityDistanceScaling().set(originalEntityScale);
        }
        originalEntityScale = null;
        lastAppliedEntityScale = null;
    }

    private void restoreParticles(Minecraft mc) {
        if (originalParticles == null) return;
        if (lastAppliedParticles != null && mc.options.particles().get() == lastAppliedParticles) {
            mc.options.particles().set(originalParticles);
        }
        originalParticles = null;
        lastAppliedParticles = null;
    }

    private void resetMeasurements() {
        sampleStartedAt = 0L;
        lastEvaluationAt = 0L;
        frameCount = 0;
        slowWindows = 0;
        fastWindows = 0;
        measuredFps = 0D;
        smoothedFps = 0D;
        frameMetrics.reset();
        lastSnapshot = new FrameMetrics.Snapshot(Double.NaN, Double.NaN, 0);
    }
}
