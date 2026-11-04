package com.port.audiobooster;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

/** Mantém os efeitos globais experimentais enquanto Spotify está aberto. */
public final class AudioEffectsService extends Service {
    static final String ACTION_START = "com.port.audiobooster.START";
    static final String ACTION_APPLY = "com.port.audiobooster.APPLY";
    static final String ACTION_STOP = "com.port.audiobooster.STOP";
    static final String CHANNEL_ID = "port_audio_active";
    private final GlobalAudioEngine engine = new GlobalAudioEngine();
    private boolean active;

    @Override public void onCreate() {
        super.onCreate();
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
            "Processamento de áudio", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("O modo experimental usa efeitos de saída de áudio do Android.");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            getSharedPreferences(GlobalAudioEngine.PREFS, MODE_PRIVATE).edit()
                .putBoolean("enabled", false).putString("status", "Desligado").apply();
            engine.close();
            active = false;
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }
        SharedPreferences prefs = getSharedPreferences(GlobalAudioEngine.PREFS, MODE_PRIVATE);
        if (!prefs.getBoolean("enabled", false)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!active) {
            // Foreground dentro do prazo do Android após startForegroundService().
            startForeground(12, makeNotification());
            active = true;
            engine.start();
        }
        engine.apply(prefs);
        prefs.edit().putString("status", engine.getStatus()).apply();
        return START_STICKY;
    }

    private Notification makeNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(this, 1, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, AudioEffectsService.class).setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 2, stop,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Port Audio Booster")
            .setContentText("Efeitos globais experimentais ativos")
            .setContentIntent(openPi)
            .setOngoing(true)
            .addAction(new Notification.Action.Builder(null, "Desligar", stopPi).build())
            .build();
    }

    @Override public void onDestroy() {
        engine.close();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
