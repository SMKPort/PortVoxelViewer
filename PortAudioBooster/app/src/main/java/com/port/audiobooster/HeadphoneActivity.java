package com.port.audiobooster;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Modo Xiaomi Buds. Sem MusicFX ou controle proprietário de ANC/firmware. */
public final class HeadphoneActivity extends Activity {
    private static final int BG=0xff0c0a0e,PANEL=0xff251d25,GOLD=0xffe2b25b,
        WHITE=0xfff7f1e9,MUTED=0xffc4b6bb,RED=0xffbf3045;
    private static final int BT_PERMISSION=303;
    private SharedPreferences prefs;
    private AudioManager audio;
    private LinearLayout body;
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        prefs=getSharedPreferences(GlobalAudioEngine.PREFS,MODE_PRIVATE);
        audio=(AudioManager)getSystemService(AUDIO_SERVICE);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        selectHeadphoneMode();
        render();
    }
    @Override protected void onResume(){super.onResume();render();}
    private void selectHeadphoneMode(){
        if(!"headphones".equals(prefs.getString("audio_mode","car")))
            HeadphonePresets.apply(prefs,prefs.getString("headphone_preset","Xiaomi Buds Equilibrado"));
        prefs.edit().putString("audio_mode","headphones").putBoolean("native_mode",false).apply();
        apply();
    }
    private void send(String action){
        try{
            Intent i=new Intent(this,AudioEffectsService.class).setAction(action);
            if(AudioEffectsService.ACTION_START.equals(action)||AudioEffectsService.ACTION_APPLY.equals(action))
                startForegroundService(i);
            else startService(i);
        }catch(RuntimeException e){toast("Android bloqueou a inicialização dos efeitos.");}
    }
    private void apply(){if(prefs.getBoolean("enabled",false))send(AudioEffectsService.ACTION_APPLY);}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_SHORT).show();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable bg(int color){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(13));return d;
    }
    private TextView text(String value,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextColor(color);t.setTextSize(size);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private void spacer(LinearLayout l,int h){l.addView(new View(this),new LinearLayout.LayoutParams(1,dp(h)));}
    private LinearLayout card(String title,String note){
        LinearLayout c=col();c.setPadding(dp(15),dp(15),dp(15),dp(15));
        c.setBackground(bg(PANEL));body.addView(c,new LinearLayout.LayoutParams(-1,-2));spacer(body,12);
        c.addView(text(title,19,WHITE,true));spacer(c,6);
        if(note!=null){c.addView(text(note,12,MUTED,false));spacer(c,12);}return c;
    }
    private void button(LinearLayout parent,String title,boolean primary,Runnable action){
        Button b=new Button(this);b.setAllCaps(false);b.setText(title);
        b.setTextColor(primary?Color.BLACK:WHITE);
        b.setBackground(bg(primary?GOLD:0xff5a3845));
        parent.addView(b,new LinearLayout.LayoutParams(-1,dp(47)));spacer(parent,6);
        b.setOnClickListener(v->action.run());
    }
    private void switcher(LinearLayout parent,String title,String key,boolean def){
        LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);parent.addView(line);
        line.addView(text(title,14,WHITE,false),new LinearLayout.LayoutParams(0,-2,1));
        Switch s=new Switch(this);s.setChecked(prefs.getBoolean(key,def));s.setThumbTintList(ColorStateList.valueOf(GOLD));line.addView(s);
        s.setOnCheckedChangeListener((v,on)->{
            if(key.equals("headphone_auto")&&on&&Build.VERSION.SDK_INT>=31&&
               checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){
                s.setChecked(false);
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},BT_PERMISSION);
                return;
            }
            prefs.edit().putBoolean(key,on).apply();
            if(key.equals("headphone_guard"))apply();
        });
        spacer(parent,9);
    }
    private String route(){
        try{
            StringBuilder result=new StringBuilder();
            for(AudioDeviceInfo info:audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)){
                int type=info.getType();
                if(type==AudioDeviceInfo.TYPE_BLUETOOTH_A2DP||
                   type==AudioDeviceInfo.TYPE_BLUETOOTH_SCO||
                   (Build.VERSION.SDK_INT>=31&&
                     (type==AudioDeviceInfo.TYPE_BLE_HEADSET||type==AudioDeviceInfo.TYPE_BLE_SPEAKER))){
                    if(result.length()>0)result.append(", ");
                    result.append(info.getProductName());
                }
            }
            return result.length()==0?"Nenhuma saída Bluetooth identificada":result.toString();
        }catch(RuntimeException e){return "Saída não disponível";}
    }
    private void render(){
        LinearLayout root=col();root.setPadding(dp(14),dp(16),dp(14),dp(8));root.setBackgroundColor(BG);
        setContentView(root);
        root.addView(text("P O R T   A U D I O  /  3.3",11,GOLD,true));spacer(root,7);
        root.addView(text("🎧 XIAOMI BUDS",27,WHITE,true));spacer(root,4);
        root.addView(text("Modo fones • Bluetooth • Spotify",12,MUTED,false));spacer(root,13);
        ScrollView scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        body=col();body.setPadding(0,0,0,dp(24));scroll.addView(body);

        boolean active=prefs.getBoolean("enabled",false);
        boolean original=prefs.getBoolean("compare_original",false);
        LinearLayout header=card("A/B • Efeito dos fones","A: áudio original. B: seus ajustes do Port Audio Booster, se o Android aceitar a sessão global.");
        TextView status=text(!active?"● DESLIGADO":original?"A  ORIGINAL":"B  COM EFEITOS",
            24,!active?MUTED:original?0xffffc2b4:0xffd0f3bc,true);
        status.setGravity(Gravity.CENTER);status.setPadding(dp(10),dp(16),dp(10),dp(16));
        status.setBackground(bg(!active?0xff393039:original?0xff612b31:0xff294334));
        header.addView(status);spacer(header,9);
        button(header,active?"Desligar efeitos":"Ativar efeitos",true,()->{
            boolean enable=!prefs.getBoolean("enabled",false);
            prefs.edit().putBoolean("enabled",enable).putBoolean("compare_original",false).apply();
            send(enable?AudioEffectsService.ACTION_START:AudioEffectsService.ACTION_STOP);render();
        });
        button(header,original?"Mudar para B: efeitos":"Mudar para A: original",false,()->{
            if(!prefs.getBoolean("enabled",false)){toast("Ative os efeitos primeiro.");return;}
            prefs.edit().putBoolean("compare_original",!prefs.getBoolean("compare_original",false)).apply();
            apply();render();
        });

        LinearLayout profile=card("Presets para Xiaomi Buds",
            "Ajustes moderados para fones. Escolha o equilíbrio entre graves e clareza sem aplicar o preset pesado do paredão.");
        profile.addView(text("Atual: "+prefs.getString("headphone_preset","Xiaomi Buds Equilibrado"),15,GOLD,true));
        spacer(profile,12);
        for(String name:HeadphonePresets.NAMES){
            button(profile,name,name.equals(prefs.getString("headphone_preset","Xiaomi Buds Equilibrado")),()->{
                HeadphonePresets.apply(prefs,name);apply();render();toast(name+" aplicado");
            });
        }

        LinearLayout protection=card("🛡️ Proteção dos fones",
            "Evita amplificação extra e limita o reforço dos graves, sem afirmar que o volume é seguro para a audição.");
        switcher(protection,"Ganho protegido", "headphone_guard",true);
        protection.addView(text("Com proteção: ganho digital extra 0 dB, EQ limitado a +2 dB, Bass Boost até 10% e espacialização opcional limitada.",12,MUTED,false));

        LinearLayout volume=card("Volume para fones",
            "Controle o volume do Android. Ouvir por muito tempo em volumes altos pode prejudicar a audição.");
        int max=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current=audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        TextView label=text("Volume "+current+" / "+max,14,GOLD,true);volume.addView(label);
        SeekBar slider=new SeekBar(this);slider.setMax(max);slider.setProgress(current);
        slider.setProgressTintList(ColorStateList.valueOf(RED));slider.setThumbTintList(ColorStateList.valueOf(GOLD));
        volume.addView(slider);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int n,boolean user){
                label.setText("Volume "+n+" / "+max);
                if(user)audio.setStreamVolume(AudioManager.STREAM_MUSIC,n,0);
            }
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });
        button(volume,"Reduzir volume para começar o teste",false,()->{
            int target=Math.max(1,Math.round(max*0.35f));
            audio.setStreamVolume(AudioManager.STREAM_MUSIC,target,0);render();
        });
        volume.addView(text("35% é somente um ponto de partida baixo. Volume realmente seguro depende da sensibilidade dos fones, ambiente e tempo de uso.",12,MUTED,false));

        LinearLayout bluetooth=card("Xiaomi Buds conectados",
            "Saída detectada: "+route());
        switcher(bluetooth,"Selecionar perfil automaticamente ao conectar Xiaomi/Redmi Buds","headphone_auto",false);
        bluetooth.addView(text("O reconhecimento depende do nome Bluetooth; conexões renomeadas podem exigir seleção manual. O Android pode restringir a ativação em segundo plano.",12,MUTED,false));

        LinearLayout extras=card("Outras ferramentas","Preserve a equalização avançada e o perfil Car Audio no mesmo aplicativo.");
        button(extras,"EQ profissional • 5/10/31 bandas",false,()->startActivity(new Intent(this,StudioActivity.class)));
        button(extras,"Voltar ao modo Paredão",true,()->{
            CarAudioPresets.apply(prefs,prefs.getString("car_preset","Paredão Controlado"));
            apply();
            startActivity(new Intent(this,PortAudioActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));finish();
        });
        button(extras,"Abrir Spotify",false,()->{
            Intent launch=getPackageManager().getLaunchIntentForPackage("com.spotify.music");
            if(launch!=null)startActivity(launch);
            else toast("Spotify não está disponível neste aparelho.");
        });
        extras.addView(text("Controles como ANC, transparência, gestos e firmware dos Buds não estão disponíveis no Port Audio Booster. Eles dependem do modelo e do app da Xiaomi.",12,MUTED,false));
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(code,permissions,grantResults);render();
    }
}
