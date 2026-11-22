package com.portmods.timeframe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Lightweight in-game controls; devices are detected from the host OS. */
public final class TimeFrameScreen extends Screen {
    private final TimeFrame mod;
    private final TimeFrameConfig settings;
    private final ClipRecorder recorder;
    private EditBox ffmpeg;
    private String message = "Selecione as fontes de audio e clique em Iniciar.";
    private int x, gap, col, left, row;

    public TimeFrameScreen(TimeFrame mod) {
        super(Component.literal("TimeFrame - captura e audio"));
        this.mod = mod;
        this.settings = mod.config();
        this.recorder = mod.recorder();
    }

    @Override protected void init() {
        left = 12;
        col = Math.max(80, (width - 32) / 3);
        gap = 4;
        x = left;
        int y = 46;

        button(0, y, "Iniciar / Parar", b -> {
            settings.save();
            if (recorder.recording()) recorder.stop();
            else recorder.start();
        });
        button(1, y, "Salvar clipe F9", b -> recorder.saveClip("manual"));
        button(2, y, "Replay 3D F10", b -> Minecraft.getInstance().setScreen(new TimeFrameReplayScreen(mod)));
        y += 25;
        button(0, y, "Detectar audio", b -> recorder.refreshDevices());
        button(1, y, label("Microfone", settings.microphone), b -> {
            settings.microphone = next(recorder.devices(), settings.microphone);
            b.setMessage(Component.literal(label("Microfone", settings.microphone)));
            settings.save();
        });
        button(2, y, label("Som do PC", settings.gameAudio), b -> {
            settings.gameAudio = next(recorder.devices(), settings.gameAudio);
            b.setMessage(Component.literal(label("Som do PC", settings.gameAudio)));
            settings.save();
        });
        y += 25;
        button(0, y, "Microfone: " + on(settings.microphoneEnabled), b -> {
            settings.microphoneEnabled = !settings.microphoneEnabled;
            b.setMessage(Component.literal("Microfone: " + on(settings.microphoneEnabled)));
            settings.save();
        });
        button(1, y, "Audio jogo: " + on(settings.gameAudioEnabled), b -> {
            settings.gameAudioEnabled = !settings.gameAudioEnabled;
            b.setMessage(Component.literal("Audio jogo: " + on(settings.gameAudioEnabled)));
            settings.save();
        });
        button(2, y, "Faixas: " + (settings.separateTracks ? "separadas" : "mixadas"), b -> {
            settings.separateTracks = !settings.separateTracks;
            b.setMessage(Component.literal("Faixas: " + (settings.separateTracks ? "separadas" : "mixadas")));
            settings.save();
        });
        y += 25;
        button(0, y, "Ruido: " + on(settings.microphoneNoiseFilter), b -> {
            settings.microphoneNoiseFilter = !settings.microphoneNoiseFilter;
            b.setMessage(Component.literal("Ruido: " + on(settings.microphoneNoiseFilter)));
            settings.save();
        });
        button(1, y, "Vol Mic: " + settings.microphoneVolume + "%", b -> {
            settings.microphoneVolume = settings.microphoneVolume >= 150 ? 0 : settings.microphoneVolume + 10;
            b.setMessage(Component.literal("Vol Mic: " + settings.microphoneVolume + "%"));
            settings.save();
        });
        button(2, y, "Vol Jogo: " + settings.gameVolume + "%", b -> {
            settings.gameVolume = settings.gameVolume >= 150 ? 0 : settings.gameVolume + 10;
            b.setMessage(Component.literal("Vol Jogo: " + settings.gameVolume + "%"));
            settings.save();
        });
        y += 25;
        button(0, y, "Buffer: " + settings.bufferSeconds + "s", b -> {
            settings.bufferSeconds = settings.bufferSeconds == 600 ? 30 : settings.bufferSeconds + 30;
            b.setMessage(Component.literal("Buffer: " + settings.bufferSeconds + "s"));
            settings.save();
        });
        button(1, y, "FPS: " + settings.fps, b -> {
            settings.fps = settings.fps >= 60 ? 15 : settings.fps + 15;
            b.setMessage(Component.literal("FPS: " + settings.fps));
            settings.save();
        });
        button(2, y, "Auto clipes: " + on(settings.autoDeathClip && settings.autoCombatClip), b -> {
            boolean active = !(settings.autoDeathClip && settings.autoCombatClip);
            settings.autoDeathClip = active;
            settings.autoCombatClip = active;
            b.setMessage(Component.literal("Auto clipes: " + on(active)));
            settings.save();
        });
        y += 25;
        button(0, y, "Teste Mic (3s)", b -> recorder.testMicrophone());
        button(1, y, "Ouvir teste", b -> playTest());
        button(2, y, "Abrir clipes", b -> recorder.openClipsFolder());
        y += 28;
        int fieldWidth = Math.max(85, width - 129);
        ffmpeg = new EditBox(font, 104, y, fieldWidth, 20, Component.literal("FFmpeg executavel"));
        ffmpeg.setMaxLength(400);
        ffmpeg.setValue(settings.ffmpeg);
        addRenderableWidget(ffmpeg);
        final int saveY = Math.min(height - 28, y + 28);
        addRenderableWidget(Button.builder(Component.literal("Salvar configuracoes"), b -> {
            settings.ffmpeg = ffmpeg.getValue().trim();
            settings.save();
            message = "Configuracoes salvas. Reinicie a captura para aplicar.";
        }).bounds(12, saveY, Math.min(width - 24, 170), 20).build());
        addRenderableWidget(Button.builder(Component.literal("Replay salvo"), b -> {
            mod.replay().saveReplay("manual");
            message = "Replay 3D salvo em timeframe/replays";
        }).bounds(Math.max(190, width - 152), saveY, 140, 20).build());
    }
    private void button(int column, int y, String label, java.util.function.Consumer<Button> cb) {
        addRenderableWidget(Button.builder(Component.literal(label), cb::accept)
                .bounds(left + column * col, y, col - gap, 20).build());
    }
    private static String on(boolean enabled) { return enabled ? "SIM" : "NAO"; }
    private static String label(String title, String device) {
        String name = device.isBlank() ? "(nenhum)" : device;
        if (name.length() > 18) name = name.substring(0, 18) + "...";
        return title + ": " + name;
    }
    private static String next(List<String> devices, String current) {
        if (devices.isEmpty()) return "";
        int pos = devices.indexOf(current);
        return pos + 1 >= devices.size() ? "" : devices.get(pos + 1);
    }
    private void playTest() {
        try {
            Path path = TimeFrameConfig.folder().resolve("microphone-test.wav");
            if (!Files.isRegularFile(path)) { message = "Grave o teste antes de ouvir."; return; }
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(path.toFile());
            else message = "O arquivo esta em: " + path;
        } catch (Exception e) { message = "Falha abrindo WAV: " + e.getMessage(); }
    }
    @Override public void render(GuiGraphics graphics, int mx, int my, float partial) {
        graphics.fill(0, 0, width, height, 0xF0161724);
        graphics.drawCenteredString(font, "TIMEFRAME | Gravacao local + replay observacional", width / 2, 10, 0xE8B769);
        graphics.drawString(font, "F8 painel  |  F9 salvar MP4  |  F10 replay", 12, 28, 0xAAC5DF);
        graphics.drawString(font, "FFmpeg:", 12, 207, 0xFFFFFF);
        int statusY = Math.min(height - 55, 262);
        String status = recorder.status();
        if (status.length() > 100) status = status.substring(0, 100) + "...";
        graphics.drawString(font, "Estado: " + status, 12, statusY, 0xFFCF8B);
        graphics.drawString(font, message, 12, Math.min(height - 41, statusY + 12), 0x98D8EE);
        graphics.drawString(font, "Windows: Stereo Mix / cabo virtual | Linux: fonte .monitor", 12, height - 12, 0x9CB0C5);
        super.render(graphics, mx, my, partial);
    }
    @Override public void onClose() {
        if (ffmpeg != null) settings.ffmpeg = ffmpeg.getValue().trim();
        settings.save();
        super.onClose();
    }
    @Override public boolean isPauseScreen() { return false; }
}
