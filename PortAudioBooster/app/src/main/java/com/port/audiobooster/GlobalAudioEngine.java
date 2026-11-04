package com.port.audiobooster;

import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.Virtualizer;
import android.content.SharedPreferences;

import java.util.Locale;

/**
 * Modo experimental: audio session 0 (global mix). O Android descontinuou essa
 * interface, e o sucesso na criação do efeito NÃO comprova alteração do Spotify.
 * Sem captura ou redirecionamento de áudio de aplicativos terceiros.
 */
final class GlobalAudioEngine {
    static final String PREFS = "audio_global_settings";
    static final int[] ANCHORS = {60, 230, 910, 3600, 14000};
    private Equalizer eq;
    private BassBoost bass;
    private Virtualizer space;
    private LoudnessEnhancer loudness;
    private String status = "Inativo";

    String getStatus() { return status; }

    void start() {
        close();
        try {
            eq = new Equalizer(100, 0);
            eq.setEnabled(true);
        } catch (RuntimeException | LinkageError e) { releaseEq(); }
        try { bass = new BassBoost(100, 0); }
        catch (RuntimeException | LinkageError e) { releaseBass(); }
        try { space = new Virtualizer(100, 0); }
        catch (RuntimeException | LinkageError e) { releaseSpace(); }
        try { loudness = new LoudnessEnhancer(0); }
        catch (RuntimeException | LinkageError e) { releaseLoudness(); }
        updateStatus();
    }

    void apply(SharedPreferences prefs) {
        if (eq != null) {
            try {
                short[] bounds = eq.getBandLevelRange();
                int count = eq.getNumberOfBands();
                for (short band = 0; band < count; band++) {
                    double hz = eq.getCenterFreq(band) / 1000.0;
                    double db = interpolateDb(prefs, hz);
                    int mb = (int) Math.round(db * 100.0);
                    eq.setBandLevel(band, (short) Math.min(bounds[1], Math.max(bounds[0], mb)));
                }
                eq.setEnabled(true);
            } catch (RuntimeException | LinkageError ignored) {}
        }
        if (bass != null) {
            try {
                int percent = clamp(prefs.getInt("bass", 35), 0, 100);
                bass.setStrength((short) (percent * 10));
                bass.setEnabled(percent > 0);
            } catch (RuntimeException | LinkageError ignored) {}
        }
        if (space != null) {
            try {
                int percent = clamp(prefs.getInt("space", 0), 0, 100);
                space.setStrength((short) (percent * 10));
                space.setEnabled(percent > 0);
            } catch (RuntimeException | LinkageError ignored) {}
        }
        if (loudness != null) {
            try {
                int db = clamp(prefs.getInt("gain", 0), 0, 3);
                loudness.setTargetGain(db * 100);
                loudness.setEnabled(db > 0);
            } catch (RuntimeException | LinkageError ignored) {}
        }
        updateStatus();
    }

    private static int clamp(int v, int lo, int hi) { return Math.min(hi, Math.max(lo, v)); }

    private static double interpolateDb(SharedPreferences prefs, double hz) {
        if (hz <= ANCHORS[0]) return prefs.getInt("band_0", 0);
        if (hz >= ANCHORS[ANCHORS.length - 1]) return prefs.getInt("band_4", 0);
        for (int i = 0; i < ANCHORS.length - 1; i++) {
            if (hz <= ANCHORS[i + 1]) {
                double logLo = Math.log(ANCHORS[i]);
                double logHi = Math.log(ANCHORS[i + 1]);
                double weight = (Math.log(hz) - logLo) / (logHi - logLo);
                return (1 - weight) * prefs.getInt("band_" + i, 0)
                    + weight * prefs.getInt("band_" + (i + 1), 0);
            }
        }
        return 0;
    }

    private void updateStatus() {
        boolean any = (eq != null && eq.hasControl()) || (bass != null && bass.hasControl())
            || (space != null && space.hasControl()) || (loudness != null && loudness.hasControl());
        if (!any) {
            status = "Este dispositivo não disponibilizou um efeito global controlável.";
            return;
        }
        status = String.format(Locale.US,
            "Efeitos criados: EQ %s | Grave %s | 3D %s | Ganho %s. Teste auditivo necessário.",
            marker(eq != null && eq.hasControl()), marker(bass != null && bass.hasControl()),
            marker(space != null && space.hasControl()), marker(loudness != null && loudness.hasControl()));
    }

    private String marker(boolean state) { return state ? "OK" : "N/D"; }
    private void releaseEq() { if (eq != null) { try { eq.release(); } catch (RuntimeException ignored) {} eq = null; } }
    private void releaseBass() { if (bass != null) { try { bass.release(); } catch (RuntimeException ignored) {} bass = null; } }
    private void releaseSpace() { if (space != null) { try { space.release(); } catch (RuntimeException ignored) {} space = null; } }
    private void releaseLoudness() { if (loudness != null) { try { loudness.release(); } catch (RuntimeException ignored) {} loudness = null; } }

    void close() {
        releaseEq(); releaseBass(); releaseSpace(); releaseLoudness();
        status = "Inativo";
    }
}
