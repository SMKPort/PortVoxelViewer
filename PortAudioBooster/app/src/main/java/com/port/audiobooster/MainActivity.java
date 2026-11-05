package com.port.audiobooster;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.media.MediaMetadata;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de áudio Android com foco no Spotify.
 * EQ/Bass para Spotify é experimental: session 0 é legado e pode não funcionar.
 * Controles de reprodução, se autorizados, usam MediaSession, NÃO alteram áudio.
 */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(10, 9, 13);
    private static final int PANEL = Color.rgb(29, 23, 30);
    private static final int GOLD = Color.rgb(231, 183, 75);
    private static final int RED = Color.rgb(204, 38, 57);
    private static final int WHITE = Color.rgb(244, 239, 232);
    private static final int MUTED = Color.rgb(179, 165, 169);
    private static final int[] BAND_FREQ = {60, 230, 910, 3600, 14000};
    private static final String[] BAND_NAMES = {"SUB / GRAVE", "GRAVE", "MÉDIO", "PRESENÇA", "AGUDO"};
    private static final String[] TABS = {"SPOTIFY", "EQUALIZADOR", "BOOST", "AJUDA"};
    private static final String[] PRESETS = {"Paredão", "Grave Profundo", "Voz Destacada", "Rock", "Eletrônica", "Flat"};
    private static final int[][] PRESET_BANDS = {
        {7, 8, -1, 3, 1}, {9, 8, -1, 0, 1}, {-4, -2, 2, 5, 2},
        {3, 3, -2, 4, 3}, {6, 6, 0, 3, 5}, {0, 0, 0, 0, 0}
    };
    private static final int[] PRESET_BASS = {80, 90, 10, 35, 60, 0};
    private SharedPreferences prefs;
    private AudioManager audioManager;
    private int tab = 0;
    private LinearLayout root;
    private TextView sessionTitle;
    private TextView sessionArtist;
    private TextView connectionStatus;
    private TextView effectStatus;
    private TextView playButton;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (tab == 0) refreshSpotifyPanel();
            if (effectStatus != null) effectStatus.setText(prefs.getString("status", "Modo global desligado"));
            handler.postDelayed(this, 2500);
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(GlobalAudioEngine.PREFS, MODE_PRIVATE);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        if (prefs.getBoolean("enabled", false)) sendService(AudioEffectsService.ACTION_START);
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(refresh);
        handler.post(refresh);
    }
    @Override protected void onPause() {
        handler.removeCallbacks(refresh);
        super.onPause();
    }

    private int dp(int n) { return Math.round(getResources().getDisplayMetrics().density * n); }

    private GradientDrawable rounded(int color, int border) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(14));
        if (border != 0) g.setStroke(dp(1), border);
        return g;
    }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private void space(LinearLayout parent, int h) {
        View v = new View(this); parent.addView(v, new LinearLayout.LayoutParams(1, dp(h)));
    }
    private LinearLayout card(LinearLayout parent) {
        LinearLayout c = column();
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        c.setBackground(rounded(PANEL, 0));
        parent.addView(c, new LinearLayout.LayoutParams(-1, -2));
        space(parent, 12);
        return c;
    }
    private void title(LinearLayout parent, String title, String detail) {
        parent.addView(text(title, 20, WHITE, true));
        space(parent, 7);
        parent.addView(text(detail, 13, MUTED, false));
        space(parent, 15);
    }
    private Button button(String label, boolean primary, Runnable action) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(13);
        b.setTextColor(primary ? Color.BLACK : WHITE);
        b.setBackground(rounded(primary ? GOLD : Color.rgb(61, 42, 48), 0));
        b.setOnClickListener(v -> action.run());
        return b;
    }
    private void addButton(LinearLayout card, String s, boolean primary, Runnable action) {
        card.addView(button(s, primary, action), new LinearLayout.LayoutParams(-1, dp(46)));
        space(card, 7);
    }
    private void render() {
        sessionTitle = null; sessionArtist = null; connectionStatus = null;
        effectStatus = null; playButton = null;
        root = column(); root.setBackgroundColor(BG);
        root.setPadding(dp(15), dp(16), dp(15), dp(9));
        setContentView(root);
        TextView brand = text("P O R T   A U D I O   /   V 3.2", 11, GOLD, true);
        root.addView(brand);
        space(root, 4);
        root.addView(text("AUDIO BOOSTER", 27, WHITE, true));
        space(root, 4);
        root.addView(text("Controles do Spotify • Port Audio", 13, MUTED, false));
        space(root, 14);
        LinearLayout tabs = row();
        root.addView(tabs);
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            TextView x = text(TABS[i], 10, i == tab ? GOLD : MUTED, true);
            x.setGravity(Gravity.CENTER);
            x.setBackground(rounded(i == tab ? PANEL : BG, i == tab ? GOLD : 0));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(45), 1);
            lp.setMargins(dp(2), 0, dp(2), 0);
            tabs.addView(x, lp);
            x.setOnClickListener(v -> { tab = index; render(); });
        }
        space(root, 13);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout content = column();
        content.setPadding(0, 0, 0, dp(24));
        scroll.addView(content);
        if (tab == 0) renderSpotify(content);
        else if (tab == 1) renderEq(content);
        else if (tab == 2) renderBoost(content);
        else renderHelp(content);
    }

    private void renderSpotify(LinearLayout content) {
        LinearLayout c = card(content);
        title(c, "🎵 Spotify", "A música continua tocando pelo Spotify. Nosso app ajusta o modo de áudio experimental.");
        addButton(c, "Abrir Spotify", true, this::openSpotify);

        c.addView(text("A equalização é controlada pelo próprio Port Audio Booster. Volte ao painel Car Audio para alternar A/B.", 12, MUTED, false));

        LinearLayout connect = card(content);
        title(connect, "Controle de reprodução", "Opcional: controle as músicas sem sair do Port Audio Booster.");
        connectionStatus = text("Verificando acesso...", 12, MUTED, false);
        connect.addView(connectionStatus);
        space(connect, 10);
        sessionTitle = text("Nenhuma música detectada", 18, WHITE, true);
        sessionArtist = text("", 12, MUTED, false);
        connect.addView(sessionTitle); connect.addView(sessionArtist);
        space(connect, 8);
        LinearLayout actions = row(); connect.addView(actions);
        TextView back = actionButton("⏮", actions);
        playButton = actionButton("▶", actions);
        TextView next = actionButton("⏭", actions);
        back.setOnClickListener(v -> controlSpotify(0));
        playButton.setOnClickListener(v -> controlSpotify(1));
        next.setOnClickListener(v -> controlSpotify(2));
        space(connect, 8);
        addButton(connect, "Permitir controles do Spotify (opcional)", false, () -> {
            try { startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)); }
            catch (RuntimeException e) { toast("Não foi possível abrir configurações."); }
        });
        connect.addView(text("Esta permissão dá acesso às notificações do celular. Só ative se quiser controles de reprodução. Não é necessária para o equalizador.", 11, MUTED, false));

        LinearLayout global = card(content);
        title(global, "Saída global experimental", "Pode afetar o Spotify neste aparelho, mas a API do Android não garante o efeito.");
        LinearLayout line = row(); global.addView(line);
        TextView label = text("Ativar processamento", 16, WHITE, true);
        line.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        Switch on = new Switch(this);
        on.setChecked(prefs.getBoolean("enabled", false));
        on.setThumbTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}}, new int[]{GOLD, MUTED}));
        line.addView(on);
        on.setOnCheckedChangeListener((v, enabled) -> {
            prefs.edit().putBoolean("enabled", enabled)
                .putString("status", enabled ? "Iniciando teste de compatibilidade..." : "Desligado").apply();
            sendService(enabled ? AudioEffectsService.ACTION_START : AudioEffectsService.ACTION_STOP);
        });
        space(global, 12);
        effectStatus = text(prefs.getString("status", "Desligado"), 12, GOLD, false);
        global.addView(effectStatus);
        space(global, 9);
        global.addView(text("A confirmação 'OK' indica somente disponibilidade técnica da API, não prova processamento audível do Spotify.", 12, MUTED, false));
        refreshSpotifyPanel();
    }

    private TextView actionButton(String name, LinearLayout parent) {
        TextView btn = text(name, 25, GOLD, true); btn.setGravity(Gravity.CENTER);
        btn.setBackground(rounded(Color.rgb(49, 37, 44), 0));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(54), 1);
        lp.setMargins(dp(3), 0, dp(3), 0); parent.addView(btn, lp);
        return btn;
    }

    private void renderEq(LinearLayout content) {
        LinearLayout c = card(content);
        title(c, "Equalizador de 5 bandas", "Perfis mapeados para as bandas que o motor do Android disponibilizar (quando compatível).");
        for (int i = 0; i < BAND_FREQ.length; i++) {
            final int n = i;
            String freq = BAND_FREQ[i] >= 1000 ? String.format(Locale.getDefault(), "%.1f kHz", BAND_FREQ[i] / 1000f) : BAND_FREQ[i] + " Hz";
            addSlider(c, BAND_NAMES[i] + "  •  " + freq, -12, 12,
                prefs.getInt("band_" + i, 0), " dB", value -> {
                    prefs.edit().putInt("band_" + n, value).apply(); applyEngine();
                });
        }
        LinearLayout presets = card(content);
        title(presets, "Presets", "Selecione um perfil para alterar o equalizador e o Bass Boost.");
        for (int i = 0; i < PRESETS.length; i++) {
            final int idx = i;
            addButton(presets, PRESETS[i], i == 0, () -> {
                SharedPreferences.Editor editor = prefs.edit();
                for (int band = 0; band < BAND_FREQ.length; band++)
                    editor.putInt("band_" + band, PRESET_BANDS[idx][band]);
                editor.putInt("bass", PRESET_BASS[idx]);
                editor.putString("preset", PRESETS[idx]);
                editor.apply(); applyEngine(); render();
                toast("Preset " + PRESETS[idx] + " aplicado");
            });
        }
    }

    private interface ValueChange { void onValue(int n); }
    private void addSlider(LinearLayout parent, String label, int min, int max,
                           int initial, String units, ValueChange change) {
        LinearLayout row = row(); parent.addView(row);
        row.addView(text(label, 13, WHITE, false), new LinearLayout.LayoutParams(0, -2, 1));
        TextView val = text(initial + units, 13, GOLD, true); row.addView(val);
        SeekBar slider = new SeekBar(this);
        slider.setMax(max - min);
        slider.setProgress(initial - min);
        slider.setProgressTintList(ColorStateList.valueOf(RED));
        slider.setThumbTintList(ColorStateList.valueOf(GOLD));
        parent.addView(slider);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                int value = p + min; val.setText(value + units);
                if (user) change.onValue(value);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        space(parent, 13);
    }

    private void renderBoost(LinearLayout content) {
        LinearLayout c = card(content);
        title(c, "Bass Boost 🔊", "Graves mais presentes no modo global experimental.");
        addSlider(c, "Intensidade dos graves", 0, 100, prefs.getInt("bass", 35), "%", v -> {
            prefs.edit().putInt("bass", v).apply(); applyEngine();
        });
        addSlider(c, "Virtualização 3D", 0, 100, prefs.getInt("space", 0), "%", v -> {
            prefs.edit().putInt("space", v).apply(); applyEngine();
        });
        addSlider(c, "Ganho extra (pode distorcer)", 0, 3, prefs.getInt("gain", 0), " dB", v -> {
            prefs.edit().putInt("gain", v).apply(); applyEngine();
        });
        c.addView(text("Ganho extra só funciona se o LoudnessEnhancer global for aceito pelo aparelho. Não ultrapassa o limite físico do alto-falante.", 12, MUTED, false));

        LinearLayout volume = card(content);
        title(volume, "Volume multimídia do celular", "Este controle altera o volume padrão do Android, inclusive durante a reprodução pelo Spotify.");
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        addSlider(volume, "Volume Android", 0, max, current, " / " + max,
            v -> audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0));
        volume.addView(text("⚠️ Use ganhos baixos para evitar distorção e exposição a volume excessivo, especialmente com fones.", 12, GOLD, false));
    }

    private void renderHelp(LinearLayout content) {
        LinearLayout c = card(content);
        title(c, "Como usar com o Spotify", "O que é realmente possível no Android sem root.");
        c.addView(text(
            "1. Abra o Spotify e coloque uma música para tocar.\n\n" +
            "2. Ative o processamento global experimental.\n\n" +
            "3. Aplique o preset Paredão e ouça se há diferença ao ligar/desligar.\n\n" +
            "4. Caso não haja diferença, o Android provavelmente não permite efeitos globais neste aparelho. " +
            "Use o equalizador do próprio Spotify ou do sistema.\n\n" +
            "5. Os controles de reprodução exigem ativar manualmente o acesso às notificações. " +
            "Esse acesso NÃO permite modificar a saída de áudio.\n\n" +
            "Spotify Connect tocando em outro equipamento não passa pelo processamento de áudio deste celular.\n\n" +
            "Não há captura nem descriptografia do áudio do Spotify. O app não executa música baixada.",
            14, WHITE, false));
    }

    private void applyEngine() {
        if (prefs.getBoolean("enabled", false)) sendService(AudioEffectsService.ACTION_APPLY);
    }
    private void sendService(String action) {
        Intent service = new Intent(this, AudioEffectsService.class).setAction(action);
        try {
            if (AudioEffectsService.ACTION_START.equals(action) || AudioEffectsService.ACTION_APPLY.equals(action))
                startForegroundService(service);
            else startService(service);
        } catch (RuntimeException e) {
            prefs.edit().putString("status", "Serviço indisponível: " + e.getClass().getSimpleName()).apply();
            toast("Não foi possível acionar o modo global.");
        }
    }

    private void openSpotify() {
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage("com.spotify.music");
            if (launch != null) startActivity(launch);
            else startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.spotify.music")));
        } catch (RuntimeException e) {
            try { startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.spotify.music"))); }
            catch (RuntimeException ignored) { toast("Spotify não encontrado."); }
        }
    }
    private MediaController spotifyController() {
        try {
            MediaSessionManager manager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
            ComponentName listener = new ComponentName(this, SpotifyNotificationListener.class);
            List<MediaController> sessions = manager.getActiveSessions(listener);
            for (MediaController c : sessions)
                if ("com.spotify.music".equals(c.getPackageName())) return c;
        } catch (SecurityException ignored) { }
        catch (RuntimeException ignored) { }
        return null;
    }
    private void refreshSpotifyPanel() {
        if (connectionStatus == null || sessionTitle == null || sessionArtist == null) return;
        MediaController mc = spotifyController();
        if (mc == null) {
            connectionStatus.setText("Nenhuma sessão Spotify acessível. Ative o acesso opcional às notificações.");
            sessionTitle.setText("Spotify não conectado");
            sessionArtist.setText("Abra o Spotify e inicie uma música.");
            if (playButton != null) playButton.setText("▶");
            return;
        }
        connectionStatus.setText("✓ Sessão de reprodução do Spotify detectada");
        try {
            MediaMetadata meta = mc.getMetadata();
            String track = meta == null ? null : meta.getString(MediaMetadata.METADATA_KEY_TITLE);
            String artist = meta == null ? null : meta.getString(MediaMetadata.METADATA_KEY_ARTIST);
            sessionTitle.setText(track == null ? "Spotify conectado" : track);
            sessionArtist.setText(artist == null ? "" : artist);
            PlaybackState playback = mc.getPlaybackState();
            if (playButton != null) playButton.setText(playback != null && playback.getState() == PlaybackState.STATE_PLAYING ? "❚❚" : "▶");
        } catch (RuntimeException ignored) { }
    }
    private void controlSpotify(int action) {
        MediaController mc = spotifyController();
        if (mc == null) { toast("Permita os controles e abra uma música no Spotify."); return; }
        try {
            if (action == 0) mc.getTransportControls().skipToPrevious();
            if (action == 2) mc.getTransportControls().skipToNext();
            if (action == 1) {
                PlaybackState s = mc.getPlaybackState();
                if (s != null && s.getState() == PlaybackState.STATE_PLAYING)
                    mc.getTransportControls().pause();
                else mc.getTransportControls().play();
            }
            handler.postDelayed(this::refreshSpotifyPanel, 600);
        } catch (RuntimeException e) { toast("Spotify não aceitou este comando."); }
    }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
