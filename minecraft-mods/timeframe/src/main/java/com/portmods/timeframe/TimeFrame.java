package com.portmods.timeframe;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/** Local recording and observed-position replay; never sends packets. */
@Mod(value = TimeFrame.ID, dist = Dist.CLIENT)
public final class TimeFrame {
    public static final String ID = "timeframe";
    private static final KeyMapping OPEN = new KeyMapping("key.timeframe.open", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F8, "key.categories.timeframe");
    private static final KeyMapping SAVE_CLIP = new KeyMapping("key.timeframe.clip", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F9, "key.categories.timeframe");
    private static final KeyMapping REPLAY = new KeyMapping("key.timeframe.replay", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F10, "key.categories.timeframe");
    private final ReplayBuffer replay = new ReplayBuffer();
    private TimeFrameConfig config;
    private ClipRecorder recorder;
    private boolean lastHasWorld;

    public TimeFrame(IEventBus bus) {
        bus.addListener(this::registerKeys);
        NeoForge.EVENT_BUS.register(this);
    }

    private void registerKeys(RegisterKeyMappingsEvent e) {
        e.register(OPEN);
        e.register(SAVE_CLIP);
        e.register(REPLAY);
    }
    private void ready() {
        if (config != null) return;
        config = TimeFrameConfig.load();
        recorder = new ClipRecorder(config);
    }
    public TimeFrameConfig config() { ready(); return config; }
    public ClipRecorder recorder() { ready(); return recorder; }
    public ReplayBuffer replay() { return replay; }

    @SubscribeEvent
    public void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        ready();
        replay.tick(mc);
        recorder.tick();
        boolean hasWorld = mc.level != null && mc.player != null;
        if (!hasWorld && lastHasWorld && recorder.recording()) recorder.stop();
        lastHasWorld = hasWorld;
        if (!hasWorld) return;
        while (OPEN.consumeClick()) mc.setScreen(new TimeFrameScreen(this));
        while (SAVE_CLIP.consumeClick()) recorder.saveClip("manual");
        while (REPLAY.consumeClick()) mc.setScreen(new TimeFrameReplayScreen(this));
        if (replay.takeDeathFlag() && config.autoDeathClip && recorder.recording()) recorder.saveClip("morte");
    }

    @SubscribeEvent
    public void hud(RenderGuiEvent.Post e) {
        if (config == null || !config.showHud) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;
        boolean rec = recorder.recording();
        String text = (rec ? "[REC] " : "[TimeFrame] ")
                + (rec ? recorder.elapsedSeconds() + "s | " : "")
                + "replay " + replay.durationSeconds() + "s | F8 painel F9 clipe F10 replay";
        e.getGuiGraphics().drawString(mc.font, text, 7, 7, rec ? 0xFF6677 : 0x8DD7FF, true);
    }
}
