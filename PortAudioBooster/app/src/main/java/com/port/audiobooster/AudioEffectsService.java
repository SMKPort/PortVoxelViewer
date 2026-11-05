package com.port.audiobooster;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

/** Serviço de processamento próprio. Sessão Android 0 segue experimental. */
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
        NotificationChannel channel=new NotificationChannel(CHANNEL_ID,"Port Audio Booster",NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Efeitos próprios e controles do Port Audio Booster.");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        floating=new FloatingControls(this);
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        String action=intent==null?ACTION_START:intent.getAction();
        SharedPreferences p=getSharedPreferences(GlobalAudioEngine.PREFS,MODE_PRIVATE);
        if(ACTION_STOP.equals(action)){
            p.edit().putBoolean("enabled",false).putBoolean("floating",false)
                .putString("status","Desligado").apply();
            engine.close();engineStarted=false;floating.hide();active=false;
            stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;
        }
        if(ACTION_AB.equals(action))
            p.edit().putBoolean("compare_original",!p.getBoolean("compare_original",false)).apply();
        boolean enabled=p.getBoolean("enabled",false);
        boolean overlay=p.getBoolean("floating",false);
        if(!enabled&&!overlay){
            floating.hide();engine.close();engineStarted=false;
            stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;
        }
        if(!active){startForeground(12,makeNotification());active=true;}
        if(enabled){
            if(!engineStarted){engine.start();engineStarted=true;}
            engine.apply(p);
            p.edit().putString("status",engine.getStatus()).apply();
        }else if(engineStarted){
            engine.close();engineStarted=false;
            p.edit().putString("status","Somente controles flutuantes").apply();
        }
        floating.sync();
        return START_STICKY;
    }
    private Notification makeNotification(){
        PendingIntent open=PendingIntent.getActivity(this,1,
            new Intent(this,PortAudioActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop=PendingIntent.getService(this,2,
            new Intent(this,AudioEffectsService.class).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent compare=PendingIntent.getService(this,3,
            new Intent(this,AudioEffectsService.class).setAction(ACTION_AB),
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this,CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Port Audio Booster 3.2 • Car Audio")
            .setContentText("EQ próprio em execução • A/B e proteção preventiva")
            .setContentIntent(open).setOngoing(true)
            .addAction(new Notification.Action.Builder(null,"A/B",compare).build())
            .addAction(new Notification.Action.Builder(null,"Desligar",stop).build())
            .build();
    }
    @Override public void onDestroy(){
        if(floating!=null)floating.hide();engine.close();super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}
