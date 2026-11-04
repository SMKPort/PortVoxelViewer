package com.port.audiobooster;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Botão flutuante opcional com a permissão de sobreposição concedida manualmente. */
final class FloatingControls {
    private final Service service;
    private final SharedPreferences prefs;
    private WindowManager manager;
    private View view;
    private boolean expanded;
    FloatingControls(Service service) {
        this.service=service;
        prefs=service.getSharedPreferences(GlobalAudioEngine.PREFS,Context.MODE_PRIVATE);
    }
    private int dp(int n){return (int)(n*service.getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int color){
        GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(17));return g;
    }
    void sync(){
        if(!prefs.getBoolean("floating",false)||!Settings.canDrawOverlays(service)){hide();return;}
        if(view==null)show(false);
    }
    private WindowManager.LayoutParams params(boolean wide){
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(
            dp(wide?274:58),-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.END;p.x=dp(12);p.y=dp(130);return p;
    }
    private void show(boolean wide){
        hide(); expanded=wide;
        if(!Settings.canDrawOverlays(service))return;
        manager=(WindowManager)service.getSystemService(Context.WINDOW_SERVICE);
        LinearLayout panel=new LinearLayout(service);
        panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(8),dp(8),dp(8),dp(8));
        panel.setBackground(shape(Color.rgb(29,23,30)));
        if(!wide){
            TextView bubble=new TextView(service);bubble.setText("🔊");bubble.setTextSize(25);
            bubble.setGravity(Gravity.CENTER);
            bubble.setBackground(shape(Color.rgb(166,36,49)));panel.addView(bubble,
                new LinearLayout.LayoutParams(dp(46),dp(46)));
            bubble.setOnClickListener(v->show(true));
        }else{
            TextView head=new TextView(service);head.setText("PORT AUDIO   ✕");
            head.setTextColor(Color.rgb(231,183,75));head.setTextSize(16);head.setPadding(dp(8),dp(6),dp(8),dp(12));
            panel.addView(head);head.setOnClickListener(v->show(false));
            add(panel,"A/B: "+(prefs.getBoolean("compare_original",false)?"ORIGINAL":"COM EFEITOS"),()->{
                prefs.edit().putBoolean("compare_original",!prefs.getBoolean("compare_original",false)).apply();
                apply();show(true);
            });
            add(panel,"Trocar preset ("+prefs.getString("preset","Paredão")+")",()->{
                String[] names=AudioProfiles.NAMES;
                int current=0;for(int i=0;i<names.length-1;i++)if(names[i].equals(prefs.getString("preset","")))current=i;
                AudioProfiles.setPreset(prefs,names[(current+1)%(names.length-1)]);apply();show(true);
            });
            add(panel,"Graves +10%",()->{
                prefs.edit().putInt("bass",AudioProfiles.clamp(prefs.getInt("bass",35)+10,0,100)).apply();apply();
            });
            add(panel,"Volume -",()->volume(-1));
            add(panel,"Volume +",()->volume(1));
            add(panel,"Desligar efeito",()->{
                prefs.edit().putBoolean("enabled",false).apply();
                service.startService(new Intent(service,AudioEffectsService.class).setAction(AudioEffectsService.ACTION_STOP));
                hide();
            });
        }
        view=panel;
        try{manager.addView(view,params(wide));}
        catch(RuntimeException e){view=null;}
    }
    private void add(LinearLayout panel,String s,Runnable action){
        Button b=new Button(service);b.setAllCaps(false);b.setText(s);b.setTextSize(12);
        b.setTextColor(Color.WHITE);b.setBackground(shape(Color.rgb(66,44,51)));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(42));
        lp.bottomMargin=dp(4);panel.addView(b,lp);b.setOnClickListener(v->action.run());
    }
    private void volume(int delta) {
        AudioManager am=(AudioManager)service.getSystemService(Context.AUDIO_SERVICE);
        int value=am.getStreamVolume(AudioManager.STREAM_MUSIC);
        am.setStreamVolume(AudioManager.STREAM_MUSIC,
           AudioProfiles.clamp(value+delta,0,am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)),0);
    }
    private void apply() {
        service.startService(new Intent(service,AudioEffectsService.class).setAction(AudioEffectsService.ACTION_APPLY));
    }
    void hide() {
        if(manager!=null && view!=null)try{manager.removeView(view);}catch(RuntimeException ignored){}
        view=null;
    }
}
