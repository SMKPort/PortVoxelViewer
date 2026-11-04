package com.port.audiobooster;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

/** Serviço foreground opt-in. Mantém efeitos e botão flutuante enquanto ativos. */
public final class AudioEffectsService extends Service {
    static final String ACTION_START="com.port.audiobooster.START";
    static final String ACTION_APPLY="com.port.audiobooster.APPLY";
    static final String ACTION_STOP="com.port.audiobooster.STOP";
    static final String ACTION_AB="com.port.audiobooster.AB";
    static final String CHANNEL_ID="port_audio_active";
    private final GlobalAudioEngine engine=new GlobalAudioEngine();
    private FloatingControls floating;
    private boolean active;
    private boolean engineStarted;

    @Override public void onCreate(){
        super.onCreate();
        NotificationChannel channel=new NotificationChannel(CHANNEL_ID,"Port Audio ativo",NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Efeitos experimentais e controles flutuantes do áudio.");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        floating=new FloatingControls(this);
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        String action=intent==null?ACTION_START:intent.getAction();
        SharedPreferences prefs=getSharedPreferences(GlobalAudioEngine.PREFS,MODE_PRIVATE);
        if(ACTION_STOP.equals(action)){
            prefs.edit().putBoolean("enabled",false).putString("status","Desligado").apply();
            engine.close();engineStarted=false;floating.hide();active=false;
            stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;
        }
        if(ACTION_AB.equals(action))
            prefs.edit().putBoolean("compare_original",!prefs.getBoolean("compare_original",false)).apply();
        boolean enabled=prefs.getBoolean("enabled",false);
        boolean overlay=prefs.getBoolean("floating",false);
        if(!enabled&&!overlay){
            floating.hide();engine.close();engineStarted=false;stopSelf();return START_NOT_STICKY;
        }
        if(!active){
            startForeground(12,makeNotification());active=true;
        }
        if(enabled){
            if(!engineStarted){engine.start();engineStarted=true;}
            engine.apply(prefs);
            prefs.edit().putString("status",engine.getStatus()).apply();
        }else if(engineStarted){
            engine.close();engineStarted=false;
            prefs.edit().putString("status","Processamento desligado (somente botão flutuante)").apply();
        }
        floating.sync();
        return START_STICKY;
    }
    private Notification makeNotification(){
        PendingIntent openPi=PendingIntent.getActivity(this,1,new Intent(this,MainActivity.class),
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stopPi=PendingIntent.getService(this,2,
            new Intent(this,AudioEffectsService.class).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent abPi=PendingIntent.getService(this,3,
            new Intent(this,AudioEffectsService.class).setAction(ACTION_AB),
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this,CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher).setContentTitle("Port Audio Booster 3.0")
            .setContentText("Equalização Android experimental • controles rápidos")
            .setContentIntent(openPi).setOngoing(true)
            .addAction(new Notification.Action.Builder(null,"A/B",abPi).build())
            .addAction(new Notification.Action.Builder(null,"Desligar",stopPi).build())
            .build();
    }
    @Override public void onDestroy(){if(floating!=null)floating.hide();engine.close();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
