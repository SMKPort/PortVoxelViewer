package com.port.audiobooster;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Botão flutuante exclusivo Port Audio Booster; exige permissão explícita de overlay. */
final class FloatingControls {
    private final Service service;
    private final SharedPreferences p;
    private WindowManager manager;
    private View view;
    FloatingControls(Service s){
        service=s;
        p=s.getSharedPreferences(GlobalAudioEngine.PREFS,Context.MODE_PRIVATE);
    }
    private int dp(int n){return (int)(n*service.getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int c){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(15));return g;}
    void sync(){
        if(!p.getBoolean("floating",false)||!Settings.canDrawOverlays(service)){hide();return;}
        if(view==null)show(false);
    }
    private void show(boolean expanded){
        hide();if(!Settings.canDrawOverlays(service))return;
        manager=(WindowManager)service.getSystemService(Context.WINDOW_SERVICE);
        LinearLayout panel=new LinearLayout(service);panel.setOrientation(1);panel.setPadding(dp(8),dp(8),dp(8),dp(8));
        panel.setBackground(bg(0xff2c222c));
        if(!expanded){
            TextView bubble=new TextView(service);bubble.setText("🔊");bubble.setTextSize(25);
            bubble.setGravity(Gravity.CENTER);bubble.setBackground(bg(0xffb73345));
            panel.addView(bubble,new LinearLayout.LayoutParams(dp(45),dp(45)));
            bubble.setOnClickListener(v->show(true));
        } else {
            TextView head=new TextView(service);head.setText("PORT AUDIO 3.2   ✕");
            head.setTextColor(0xffe2b25b);head.setTextSize(15);
            head.setPadding(dp(8),dp(7),dp(8),dp(9));panel.addView(head);
            head.setOnClickListener(v->show(false));
            String state=!p.getBoolean("enabled",false)?"DESLIGADO":p.getBoolean("compare_original",false)?"A ORIGINAL":"B COM EFEITOS";
            add(panel,"A/B: "+state,()->{
                if(!p.getBoolean("enabled",false)){openMain();return;}
                p.edit().putBoolean("compare_original",!p.getBoolean("compare_original",false)).apply();
                apply();show(true);
            });
            add(panel,"Preset: "+p.getString("car_preset","Paredão Controlado"),()->{
                int i=CarAudioPresets.find(p.getString("car_preset","Paredão Controlado"));
                CarAudioPresets.apply(p,CarAudioPresets.NAMES[(i+1)%CarAudioPresets.NAMES.length]);
                apply();show(true);
            });
            add(panel,"Proteção: "+(p.getBoolean("car_guard",true)?"ON":"OFF"),()->{
                p.edit().putBoolean("car_guard",!p.getBoolean("car_guard",true)).apply();
                apply();show(true);
            });
            add(panel,"Volume -",()->volume(-1));
            add(panel,"Volume +",()->volume(1));
            add(panel,"Abrir Port Audio",this::openMain);
            add(panel,"Ocultar botão",()->{
                p.edit().putBoolean("floating",false).apply();
                if(!p.getBoolean("enabled",false))
                    service.startService(new Intent(service,AudioEffectsService.class).setAction(AudioEffectsService.ACTION_STOP));
                hide();
            });
        }
        WindowManager.LayoutParams lp=new WindowManager.LayoutParams(
            dp(expanded?282:61),-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT);
        lp.gravity=Gravity.TOP|Gravity.END;lp.x=dp(12);lp.y=dp(135);
        view=panel;try{manager.addView(view,lp);}catch(RuntimeException e){view=null;}
    }
    private void add(LinearLayout parent,String name,Runnable run){
        Button b=new Button(service);b.setAllCaps(false);b.setText(name);b.setTextColor(Color.WHITE);b.setTextSize(13);
        b.setBackground(bg(0xff65414e));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(42));
        lp.bottomMargin=dp(5);parent.addView(b,lp);b.setOnClickListener(v->run.run());
    }
    private void openMain(){
        try{
            Intent i=new Intent(service,PortAudioActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            service.startActivity(i);show(false);
        }catch(RuntimeException ignored){}
    }
    private void apply(){
        if(p.getBoolean("enabled",false))try{
            service.startService(new Intent(service,AudioEffectsService.class).setAction(AudioEffectsService.ACTION_APPLY));
        }catch(RuntimeException ignored){}
    }
    private void volume(int delta){
        AudioManager a=(AudioManager)service.getSystemService(Context.AUDIO_SERVICE);
        int current=a.getStreamVolume(AudioManager.STREAM_MUSIC);
        a.setStreamVolume(AudioManager.STREAM_MUSIC,
            AudioProfiles.clamp(current+delta,0,a.getStreamMaxVolume(AudioManager.STREAM_MUSIC)),0);
    }
    void hide(){
        if(manager!=null&&view!=null)try{manager.removeView(view);}catch(RuntimeException ignored){}
        view=null;
    }
}
