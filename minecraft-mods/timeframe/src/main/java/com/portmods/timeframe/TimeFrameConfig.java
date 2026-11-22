package com.portmods.timeframe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Settings are stored only on the local user's computer. */
public final class TimeFrameConfig {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    public String ffmpeg = "ffmpeg";
    public String microphone = "";
    public String gameAudio = "";
    public boolean microphoneEnabled = false;
    public boolean gameAudioEnabled = false;
    public boolean separateTracks = false;
    public boolean microphoneNoiseFilter = false;
    public boolean autoDeathClip = true;
    public boolean autoCombatClip = true;
    public boolean showHud = true;
    public int bufferSeconds = 300;
    public int fps = 30;
    public int microphoneVolume = 100;
    public int gameVolume = 100;

    public void normalize() {
        ffmpeg = ffmpeg == null || ffmpeg.isBlank() ? "ffmpeg" : ffmpeg.trim();
        microphone = microphone == null ? "" : microphone;
        gameAudio = gameAudio == null ? "" : gameAudio;
        bufferSeconds = Math.min(600, Math.max(30, bufferSeconds));
        fps = Math.min(60, Math.max(15, fps));
        microphoneVolume = Math.min(150, Math.max(0, microphoneVolume));
        gameVolume = Math.min(150, Math.max(0, gameVolume));
    }

    public static Path folder() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("timeframe");
    }
    public static Path path() { return folder().resolve("settings.json"); }

    public static TimeFrameConfig load() {
        try {
            if (Files.exists(path())) {
                TimeFrameConfig value = JSON.fromJson(Files.readString(path()), TimeFrameConfig.class);
                if (value != null) {
                    value.normalize();
                    return value;
                }
            }
        } catch (Exception e) {
            System.err.println("[TimeFrame] Cannot load config: " + e);
        }
        return new TimeFrameConfig();
    }

    public void save() {
        normalize();
        try {
            Files.createDirectories(folder());
            Files.writeString(path(), JSON.toJson(this), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[TimeFrame] Cannot save settings: " + e);
        }
    }
}
