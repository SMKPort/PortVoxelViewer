package com.port.audiobooster;

import android.app.Activity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Tela principal independente de qualquer painel de equalização externo. */
public final class PortAudioActivity extends Activity {
    private static final int BG=0xff0c0a0e, PANEL=0xff251d25, GOLD=0xffe2b25b,
            RED=0xffbf3045, WHITE=0xfff7f1e9, MUTED=0xffc4b6bb;
    private SharedPreferences prefs;
    private LinearLayout body;
    private TextView currentStatus;
    private AudioManager audio;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable refresh=new Runnable(){
        @Override public void run(){
            if(currentStatus!=null)currentStatus.setText(
                "Estado do motor: "+prefs.getString("status","Não iniciado")+
                "\n"+CarAudioPresets.estimatedRisk(prefs));
            handler.postDelayed(this,2500);
        }
    };
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences(GlobalAudioEngine.PREFS,MODE_PRIVATE);
        audio=(AudioManager)getSystemService(AUDIO_SERVICE);
        // Atualizações anteriores podiam ter selecionado modo nativo.
        prefs.edit().putBoolean("native_mode",false).apply();
        if(!prefs.getBoolean("car_v32_migrated",false)){
            boolean previous=prefs.getBoolean("enabled",false);
            prefs.edit().putBoolean("car_v32_migrated",true)
                .putBoolean("enabled",false)
                .putBoolean("compare_original",false)
                .putBoolean("car_guard",true)
                .putInt("gain",0)
                .putBoolean("safe_gain",true).apply();
            if(previous)try {
                startService(new Intent(this,AudioEffectsService.class).setAction(AudioEffectsService.ACTION_STOP));
            }catch(RuntimeException ignored){}
        }
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        render();
    }
    @Override protected void onResume(){
        super.onResume();
        if(prefs.getBoolean("enabled",false)||prefs.getBoolean("floating",false))runService(AudioEffectsService.ACTION_START);
        render();handler.removeCallbacks(refresh);handler.post(refresh);
    }
    @Override protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] result){
        super.onRequestPermissionsResult(request,permissions,result);render();
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable style(int color){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(14));return d;
    }
    private TextView label(String s,int size,int color,boolean bold){
        TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;
    }
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private void gap(LinearLayout p,int h){p.addView(new View(this),new LinearLayout.LayoutParams(1,dp(h)));}
    private LinearLayout card(String title,String description){
        LinearLayout c=column();c.setPadding(dp(16),dp(16),dp(16),dp(16));c.setBackground(style(PANEL));
        body.addView(c,new LinearLayout.LayoutParams(-1,-2));gap(body,13);
        c.addView(label(title,19,WHITE,true));gap(c,7);
        if(description!=null){c.addView(label(description,12,MUTED,false));gap(c,13);}
        return c;
    }
    private void button(LinearLayout c,String name,Runnable fn,boolean main){
        Button b=new Button(this);b.setAllCaps(false);b.setText(name);b.setTextSize(14);
        b.setTextColor(main?Color.BLACK:WHITE);b.setBackground(style(main?GOLD:0xff5d3844));
        c.addView(b,new LinearLayout.LayoutParams(-1,dp(46)));gap(c,7);
        b.setOnClickListener(v->fn.run());
    }
    private void runService(String action){
        try{
            Intent i=new Intent(this,AudioEffectsService.class).setAction(action);
            if(AudioEffectsService.ACTION_START.equals(action)||AudioEffectsService.ACTION_APPLY.equals(action))
                startForegroundService(i);else startService(i);
        }catch(RuntimeException e){toast("Android impediu iniciar os efeitos. Abra o aplicativo novamente.");}
    }
    private void apply(){
        if(prefs.getBoolean("enabled",false))runService(AudioEffectsService.ACTION_APPLY);
    }
    private void toast(String msg){Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();}
    private void render(){
        LinearLayout root=column();root.setBackgroundColor(BG);
        root.setPadding(dp(15),dp(15),dp(15),dp(10));setContentView(root);
        root.addView(label("P O R T  A U D I O  /  3.3",11,GOLD,true));
        gap(root,5);root.addView(label("CAR AUDIO EDITION",26,WHITE,true));
        gap(root,4);root.addView(label("Equalizador próprio • Spotify • Paredão e Xiaomi Buds",12,MUTED,false));
        gap(root,15);
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        body=column();body.setPadding(0,0,0,dp(24));scroll.addView(body);

        boolean on=prefs.getBoolean("enabled",false);
        boolean a=prefs.getBoolean("compare_original",false);
        LinearLayout indicator=card("CONTROLE A/B",
            "A = original (efeitos desativados). B = com efeitos do Port Audio Booster. O Android pode limitar o processamento de apps terceiros.");
        TextView big=label(!on?"● DESLIGADO":a?"A  ORIGINAL":"B  COM EFEITOS",25,
            !on?MUTED:a?0xffffba9b:0xffd0f3bc,true);
        big.setGravity(Gravity.CENTER);big.setBackground(style(!on?0xff3b323a:a?0xff612a30:0xff294333));
        big.setPadding(dp(8),dp(18),dp(8),dp(18));indicator.addView(big);gap(indicator,10);
        button(indicator,on?"Desligar processamento":"Ativar processamento",()->{
            boolean enable=!prefs.getBoolean("enabled",false);
            prefs.edit().putBoolean("enabled",enable).putBoolean("compare_original",false).apply();
            runService(enable?AudioEffectsService.ACTION_START:AudioEffectsService.ACTION_STOP);render();
        },true);
        button(indicator,a?"Mudar para B • efeitos":"Mudar para A • original",()->{
            if(!prefs.getBoolean("enabled",false)){toast("Ative o processamento primeiro.");return;}
            prefs.edit().putBoolean("compare_original",!prefs.getBoolean("compare_original",false)).apply();
            apply();render();
        },false);

        LinearLayout headphones=card("🎧 Xiaomi Buds",
            "Perfis de fones Bluetooth separados do paredão, com ganho moderado e graves mais suaves.");
        headphones.addView(label("Modo atual: "+("headphones".equals(prefs.getString("audio_mode","car"))?"FONES":"CAR AUDIO"),14,GOLD,true));
        gap(headphones,10);
        button(headphones,"Abrir Modo Xiaomi Buds",()->startActivity(new Intent(this,HeadphoneActivity.class)),true);
        if("headphones".equals(prefs.getString("audio_mode","car"))){
            headphones.addView(label("O perfil de fones está ativo. Se for usar o som do carro, escolha um preset Car Audio abaixo.",12,MUTED,false));
        }
        LinearLayout car=card("🚗 Car Audio",
            "Regulagens mais moderadas para graves fortes sem exagerar no sinal digital.");
        TextView carName=label(prefs.getString("car_preset","Paredão Controlado"),17,GOLD,true);
        car.addView(carName);gap(car,10);
        for(String s:CarAudioPresets.NAMES){
            button(car,s,()->{
                CarAudioPresets.apply(prefs,s);
                apply();render();toast("Preset "+s+" aplicado");
            },s.equals(prefs.getString("car_preset","Paredão Controlado")));
        }

        LinearLayout protect=card("🛡️ Proteção preventiva",
            "Limita reforços digitais e zera o ganho extra no modo Car Audio. Não detecta clipping do módulo.");
        LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);protect.addView(line);
        line.addView(label("Reduzir risco de saturação",14,WHITE,false),new LinearLayout.LayoutParams(0,-2,1));
        Switch guard=new Switch(this);guard.setChecked(prefs.getBoolean("car_guard",true));
        guard.setThumbTintList(ColorStateList.valueOf(GOLD));line.addView(guard);
        guard.setOnCheckedChangeListener((v,enabled)->{
            prefs.edit().putBoolean("car_guard",enabled).apply();apply();render();
        });
        gap(protect,12);
        currentStatus=label("",12,GOLD,false);protect.addView(currentStatus);
        currentStatus.setText("Estado do motor: "+prefs.getString("status","Não iniciado")+"\n"+CarAudioPresets.estimatedRisk(prefs));
        gap(protect,12);
        protect.addView(label("O indicador estima risco a partir das regulagens. Não mede distorção, corrente ou temperatura dos amplificadores.",12,MUTED,false));

        LinearLayout btCard=card("📡 Bluetooth do paredão",
            "Ao conectar um dispositivo de áudio, tentar aplicar o preset próprio escolhido. O Android pode bloquear início em segundo plano.");
        LinearLayout btRow=new LinearLayout(this);btRow.setGravity(Gravity.CENTER_VERTICAL);btCard.addView(btRow);
        btRow.addView(label("Ativação automática",14,WHITE,false),new LinearLayout.LayoutParams(0,-2,1));
        Switch btSwitch=new Switch(this);btSwitch.setChecked(prefs.getBoolean("auto_bt",false));btRow.addView(btSwitch);
        btSwitch.setOnCheckedChangeListener((v,onBt)->{
            if(onBt&&Build.VERSION.SDK_INT>=31
                &&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){
                btSwitch.setChecked(false);
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},302);return;
            }
            prefs.edit().putBoolean("auto_bt",onBt).apply();
        });
        gap(btCard,10);
        btCard.addView(label("Preset padrão Bluetooth: "+
            prefs.getString("bt_car_default","Paredão Controlado"),13,GOLD,true));
        gap(btCard,8);
        for(String name:CarAudioPresets.NAMES){
            button(btCard,name,()->{
                prefs.edit().putString("bt_car_default",name).apply();render();
            },name.equals(prefs.getString("bt_car_default","Paredão Controlado")));
        }
        LinearLayout tools=card("Ferramentas e diagnóstico",
            "Acesse o equalizador avançado, medições e controles flutuantes.");
        button(tools,"Equalizador profissional e Bass Boost",()->startActivity(new Intent(this,StudioActivity.class)),false);
        button(tools,"Meu Paredão • calcular ligações",()->startActivity(new Intent(this,CarSystemActivity.class)),false);
        button(tools,"Modo Xiaomi Buds • fones",()->startActivity(new Intent(this,HeadphoneActivity.class)),false);
        button(tools,"Abrir Spotify",()->{
            Intent i=getPackageManager().getLaunchIntentForPackage("com.spotify.music");
            if(i!=null)startActivity(i);else toast("Spotify não está instalado.");
        },false);
        if(!Settings.canDrawOverlays(this)){
            button(tools,"Autorizar botão flutuante",()->{
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:"+getPackageName())));
            },false);
        } else {
            button(tools,prefs.getBoolean("floating",false)?"Desligar botão flutuante":"Ativar botão flutuante",()->{
                boolean show=!prefs.getBoolean("floating",false);
                prefs.edit().putBoolean("floating",show).apply();
                if(show||prefs.getBoolean("enabled",false))
                    runService(AudioEffectsService.ACTION_START);
                else runService(AudioEffectsService.ACTION_STOP);
                render();
            },false);
        }
        LinearLayout volume=card("Volume multimídia",
            "Controle do volume do Android. Não eleva a potência nominal dos módulos.");
        int max=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current=audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        TextView volumeLabel=label("Volume "+current+" / "+max,14,GOLD,true);volume.addView(volumeLabel);
        SeekBar bar=new SeekBar(this);bar.setMax(max);bar.setProgress(current);volume.addView(bar);
        bar.setProgressTintList(ColorStateList.valueOf(RED));bar.setThumbTintList(ColorStateList.valueOf(GOLD));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int v,boolean user){
                volumeLabel.setText("Volume "+v+" / "+max);
                if(user)audio.setStreamVolume(AudioManager.STREAM_MUSIC,v,0);
            }
            public void onStartTrackingTouch(SeekBar s){}
            public void onStopTrackingTouch(SeekBar s){}
        });
        gap(volume,10);
        volume.addView(label("Pare de aumentar o volume se houver distorção. O amplificador pode estar clipando, mesmo com ganhos digitais moderados.",12,MUTED,false));
    }
}
