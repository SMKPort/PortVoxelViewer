package com.port.audiobooster;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * MusicFX-first: atalhos e guias de ajustes, nunca simula escrita das bandas
 * em outro aplicativo. Spotify pode usar preferências por sessão/aplicativo.
 */
public final class NativeMusicFxActivity extends Activity {
    static final String OPEN_MUSICFX="com.port.audiobooster.OPEN_MUSICFX";
    private static final int BG=0xff0b090e, CARD=0xff241c25, GOLD=0xffe5b65b;
    private static final int RED=0xffc82942, WHITE=0xfff6f0e8, MUTED=0xffbdb1b9;
    private static final String[] TABS={"INÍCIO","PRESETS","BLUETOOTH","CONTROLES"};
    private static final int REQUEST_BT=421;
    private SharedPreferences prefs;
    private AudioManager audio;
    private LinearLayout content;
    private int tab=0;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences(GlobalAudioEngine.PREFS,MODE_PRIVATE);
        audio=(AudioManager)getSystemService(AUDIO_SERVICE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        if(!prefs.getBoolean("v31_migrated",false)){
            boolean wasEnabled=prefs.getBoolean("enabled",false);
            prefs.edit().putBoolean("native_mode",true)
                .putBoolean("enabled",false).putBoolean("compare_original",false)
                .putBoolean("v31_migrated",true).apply();
            if(wasEnabled)try{
                startService(new Intent(this,AudioEffectsService.class).setAction(AudioEffectsService.ACTION_STOP));
            }catch(RuntimeException ignored){}
        }
        if(OPEN_MUSICFX.equals(getIntent().getAction()))openMusicFx();
        render();
    }
    @Override protected void onResume(){
        super.onResume();
        if(prefs!=null) {
            checkAudioRoute();
            render();
        }
    }
    private int dp(int n){return Math.round(getResources().getDisplayMetrics().density*n);}
    private GradientDrawable shape(int color,boolean outlined){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(14));
        if(outlined)d.setStroke(dp(1),GOLD);return d;
    }
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private void gap(LinearLayout p,int h){p.addView(new View(this),new LinearLayout.LayoutParams(1,dp(h)));}
    private TextView text(String s,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private LinearLayout card(String title,String sub){
        LinearLayout c=col();c.setPadding(dp(16),dp(16),dp(16),dp(16));
        c.setBackground(shape(CARD,false));content.addView(c,new LinearLayout.LayoutParams(-1,-2));
        gap(content,13);c.addView(text(title,19,WHITE,true));gap(c,5);
        if(sub!=null&&!sub.isEmpty()){c.addView(text(sub,12,MUTED,false));gap(c,13);}
        return c;
    }
    private void button(LinearLayout parent,String label,boolean primary,Runnable action){
        Button b=new Button(this);b.setAllCaps(false);b.setText(label);b.setTextSize(13);
        b.setTextColor(primary?Color.BLACK:WHITE);b.setBackground(shape(primary?GOLD:0xff583845,false));
        parent.addView(b,new LinearLayout.LayoutParams(-1,dp(47)));gap(parent,7);
        b.setOnClickListener(v->action.run());
    }
    private void switchControl(LinearLayout parent,String label,String key){
        LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);parent.addView(line);
        line.addView(text(label,13,WHITE,false),new LinearLayout.LayoutParams(0,-2,1));
        Switch s=new Switch(this);s.setChecked(prefs.getBoolean(key,false));
        s.setThumbTintList(ColorStateList.valueOf(GOLD));line.addView(s);
        s.setOnCheckedChangeListener((button,checked)->{
            if(key.equals("native_auto")&&checked&&Build.VERSION.SDK_INT>=31
                &&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){
                s.setChecked(false);
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},REQUEST_BT);
                return;
            }
            prefs.edit().putBoolean(key,checked).apply();
            if(key.equals("floating")){
                if(checked&&!Settings.canDrawOverlays(this)){
                    prefs.edit().putBoolean("floating",false).apply();
                    s.setChecked(false);requestOverlay();return;
                }
                refreshOverlay();
            }
            if(key.equals("native_auto")&&checked)checkAudioRoute();
        });
        gap(parent,12);
    }
    private void render(){
        LinearLayout root=col();root.setBackgroundColor(BG);
        root.setPadding(dp(15),dp(16),dp(15),dp(10));setContentView(root);
        root.addView(text("P O R T    A U D I O  /  3.1",11,GOLD,true));gap(root,7);
        root.addView(text("MUSICFX CONTROL",26,WHITE,true));gap(root,4);
        root.addView(text("Spotify • equalizador nativo do Android",12,MUTED,false));gap(root,14);
        HorizontalScrollView sc=new HorizontalScrollView(this);sc.setHorizontalScrollBarEnabled(false);
        root.addView(sc);LinearLayout tabs=new LinearLayout(this);sc.addView(tabs);
        for(int i=0;i<TABS.length;i++){
            int idx=i;
            TextView t=text(TABS[i],11,i==tab?GOLD:MUTED,true);t.setGravity(Gravity.CENTER);
            t.setBackground(shape(i==tab?0xff372534:BG,i==tab));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(110),dp(44));
            lp.rightMargin=dp(5);tabs.addView(t,lp);
            t.setOnClickListener(v->{tab=idx;render();});
        }
        gap(root,14);
        ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        content=col();content.setPadding(0,0,0,dp(28));scroll.addView(content);
        switch(tab){
            case 0:home();break;
            case 1:presets();break;
            case 2:bluetooth();break;
            default:controls();break;
        }
    }
    private void home(){
        LinearLayout status=card("🎧 MusicFX primeiro","O equalizador nativo já apresentou diferença audível no Spotify. Este passa a ser o caminho recomendado.");
        status.addView(text("Modo de operação: NATIVO",14,GOLD,true));gap(status,12);
        button(status,"Abrir MusicFX do Android",true,this::openMusicFx);
        button(status,"Abrir Spotify",false,this::openSpotify);
        status.addView(text("Para garantir os ajustes da sessão do Spotify, abra o equalizador pelo próprio Spotify: Configurações e privacidade > Reprodução > Equalizador. O atalho direto pode abrir configurações separadas do aplicativo Port.",12,MUTED,false));

        LinearLayout p=card("Preset de referência","Selecione uma sugestão e configure as cinco frequências manualmente no MusicFX.");
        String selected=NativeMusicFxPresets.selected(prefs);
        int idx=NativeMusicFxPresets.find(selected);
        p.addView(text(selected,21,GOLD,true));gap(p,9);
        p.addView(text(NativeMusicFxPresets.DESCRIPTIONS[idx],13,WHITE,false));gap(p,10);
        drawBands(p,idx);
        button(p,"Ver presets e valores",false,()->{tab=1;render();});
        button(p,"Abrir MusicFX para ajustar",true,this::openMusicFx);

        LinearLayout ab=card("Comparação A/B real","O app não pode ligar ou desligar o MusicFX do Spotify automaticamente.");
        ab.addView(text("A: desligue o botão superior do MusicFX e escute a música.\nB: ligue o MusicFX e escute o mesmo trecho no mesmo volume.\nCompare os graves e a clareza sem alterar o volume.",13,WHITE,false));
        gap(ab,12);button(ab,"Abrir painel para testar A/B",false,this::openMusicFx);
    }
    private void drawBands(LinearLayout parent,int index){
        int[] b=NativeMusicFxPresets.BANDS[index];
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);parent.addView(row);
        for(int i=0;i<5;i++){
            LinearLayout cell=col();cell.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);row.addView(cell,lp);
            TextView number=text(NativeMusicFxPresets.formatDb(b[i]),12,b[i]>=0?GOLD:0xffdd7c8a,true);
            number.setGravity(Gravity.CENTER);cell.addView(number);
            gap(cell,5);
            View bar=new View(this);bar.setBackground(shape(b[i]>=0?GOLD:RED,false));
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(8),dp(8+Math.abs(b[i])*8));
            cell.addView(bar,bp);gap(cell,5);
            TextView freq=text(NativeMusicFxPresets.HZ[i]>=1000
                ?(NativeMusicFxPresets.HZ[i]==4000?"4k":"14k"):NativeMusicFxPresets.HZ[i]+"",11,MUTED,false);
            freq.setGravity(Gravity.CENTER);cell.addView(freq);
        }
        gap(parent,10);
        parent.addView(text("Realçar graves: aproximadamente "+NativeMusicFxPresets.BASS[index]+"%  •  Surround: "+NativeMusicFxPresets.SURROUND[index]+"%",12,GOLD,false));
        gap(parent,12);
    }
    private void presets(){
        LinearLayout intro=card("Presets MusicFX","São referências, não configurações aplicadas remotamente. O Android não oferece uma API pública universal para alterar as bandas de outro aplicativo.");
        intro.addView(text("Frequências: 60 Hz, 230 Hz, 910 Hz, 4 kHz e 14 kHz. Ajuste cada controle no MusicFX seguindo os valores abaixo.",13,WHITE,false));
        for(int i=0;i<NativeMusicFxPresets.NAMES.length;i++){
            int index=i;String name=NativeMusicFxPresets.NAMES[i];
            LinearLayout c=card(name,name.equals(NativeMusicFxPresets.selected(prefs))?"Selecionado como referência atual":NativeMusicFxPresets.DESCRIPTIONS[i]);
            drawBands(c,i);
            button(c,"Usar "+name+" como referência",name.equals(NativeMusicFxPresets.selected(prefs)),()->{
                NativeMusicFxPresets.select(prefs,name);toast("Referência "+name+" salva. Ajuste manualmente no MusicFX.");render();
            });
        }
    }
    private void bluetooth(){
        LinearLayout c=card("Perfis por dispositivo","O app lembra qual preset você prefere. A seleção não movimenta automaticamente os controles físicos do MusicFX.");
        if(Build.VERSION.SDK_INT>=31
            &&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)
            button(c,"Permitir acesso a dispositivos próximos",true,()->requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},REQUEST_BT));
        switchControl(c,"Selecionar referência ao conectar Bluetooth","native_auto");
        c.addView(text("O Android pode impedir a detecção em segundo plano. Quando você abrir este app com o Bluetooth conectado, ele também verificará a saída ativa.",12,MUTED,false));
        LinearLayout def=card("Preset padrão para Bluetooth","Atual: "+prefs.getString("native_bt_default","Paredão"));
        for(String name:NativeMusicFxPresets.NAMES)
            button(def,name,false,()->{prefs.edit().putString("native_bt_default",name).apply();render();});
        LinearLayout current=card("Dispositivo e perfil salvo",prefs.getString("native_bt_status","Nenhum evento Bluetooth registrado ainda."));
        current.addView(text("Último nome: "+prefs.getString("native_bt_name","não identificado"),13,WHITE,false));gap(current,10);
        String id=prefs.getString("native_bt_id","");
        if(!id.isEmpty()){
            String mapped=prefs.getString("native_bt_profile_"+id,"Nenhum");
            current.addView(text("Perfil específico: "+mapped,13,GOLD,false));gap(current,8);
            button(current,"Vincular o preset de referência atual",true,()->{
                prefs.edit().putString("native_bt_profile_"+id,NativeMusicFxPresets.selected(prefs)).apply();
                toast("Referência deste Bluetooth salva.");render();
            });
        }
    }
    private void controls(){
        LinearLayout c=card("Controles rápidos","Atalhos para o MusicFX, Spotify e volume do Android.");
        button(c,"Abrir MusicFX",true,this::openMusicFx);
        button(c,"Abrir Spotify",false,this::openSpotify);
        int max=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int initial=audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        TextView label=text("Volume: "+initial+" / "+max,13,GOLD,true);c.addView(label);
        SeekBar volume=new SeekBar(this);volume.setMax(max);volume.setProgress(initial);c.addView(volume);
        volume.setProgressTintList(ColorStateList.valueOf(RED));
        volume.setThumbTintList(ColorStateList.valueOf(GOLD));
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int v,boolean user){
                label.setText("Volume: "+v+" / "+max);
                if(user)audio.setStreamVolume(AudioManager.STREAM_MUSIC,v,0);
            }
            public void onStartTrackingTouch(SeekBar s){}
            public void onStopTrackingTouch(SeekBar s){}
        });

        LinearLayout f=card("Botão flutuante","Abre o painel MusicFX, alterna a referência do preset e muda o volume. Requer autorização do Android.");
        if(!Settings.canDrawOverlays(this))button(f,"Autorizar botão sobre outros apps",true,this::requestOverlay);
        else switchControl(f,"Mostrar botão flutuante MusicFX","floating");

        LinearLayout diag=card("Diagnóstico de compatibilidade","Verificações de acesso, sem alegar controle direto do Spotify.");
        diag.addView(text("Android API: "+Build.VERSION.SDK_INT+
            "\nMusicFX instalado: "+(hasMusicFx()?"Sim":"Não detectado")+
            "\nSpotify instalado: "+(getPackageManager().getLaunchIntentForPackage("com.spotify.music")!=null?"Sim":"Não detectado")+
            "\nAtalho de painel: "+(canOpenMusicFx()?"Disponível":"Não detectado")+
            "\nEfeitos experimentais: "+(prefs.getBoolean("enabled",false)?"Ativos (desligue no modo experimental)":"Desligados")+
            "\nÚltima saída: "+outputName()+
            "\nReferência: "+NativeMusicFxPresets.selected(prefs),13,WHITE,false));
        gap(diag,12);button(diag,"Atualizar diagnóstico",false,this::render);

        LinearLayout expert=card("Modo experimental (3.0)","Mantido apenas como opção. Pode não alterar o Spotify e não deve ser usado ao mesmo tempo que o MusicFX.");
        button(expert,"Abrir equalização experimental",false,()->startActivity(new Intent(this,StudioActivity.class)));
        expert.addView(text("Sem acesso universal às bandas internas do MusicFX. Para tocar Spotify sem interferências, mantenha os efeitos globais experimentais desligados.",12,MUTED,false));
    }
    private void requestOverlay(){
        try{startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:"+getPackageName())));}
        catch(RuntimeException e){toast("Permissão de sobreposição indisponível.");}
    }
    private void refreshOverlay(){
        try{
            Intent i=new Intent(this,AudioEffectsService.class)
                .setAction(AudioEffectsService.ACTION_APPLY);
            if(prefs.getBoolean("floating",false))startForegroundService(i);
            else if(!prefs.getBoolean("enabled",false))startService(new Intent(this,AudioEffectsService.class).setAction(AudioEffectsService.ACTION_STOP));
            else startService(i);
        }catch(RuntimeException e){toast("Não foi possível iniciar controle flutuante.");}
    }
    private boolean hasMusicFx(){
        try{getPackageManager().getPackageInfo("com.android.musicfx",0);return true;}
        catch(PackageManager.NameNotFoundException e){return false;}
    }
    private boolean canOpenMusicFx(){
        Intent i=new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
            .putExtra(AudioEffect.EXTRA_CONTENT_TYPE,AudioEffect.CONTENT_TYPE_MUSIC);
        return i.resolveActivity(getPackageManager())!=null;
    }
    private void openMusicFx(){
        // Não enviamos IDs de sessão inventados ou tentamos representar o Spotify.
        Intent panel=new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
        panel.putExtra(AudioEffect.EXTRA_CONTENT_TYPE,AudioEffect.CONTENT_TYPE_MUSIC);
        try{
            Intent explicit=new Intent(panel).setPackage("com.android.musicfx");
            if(explicit.resolveActivity(getPackageManager())!=null)startActivityForResult(explicit,19);
            else startActivityForResult(panel,19);
        }catch(RuntimeException e){
            try{startActivityForResult(panel,19);}
            catch(RuntimeException ignored){toast("Abra o equalizador pelo Spotify > Reprodução > Equalizador.");}
        }
    }
    private void openSpotify(){
        Intent launch=getPackageManager().getLaunchIntentForPackage("com.spotify.music");
        if(launch!=null)try{startActivity(launch);return;}catch(RuntimeException ignored){}
        toast("Spotify não encontrado. Abra o aplicativo pela tela inicial.");
    }
    private String outputName(){
        try{
            StringBuilder b=new StringBuilder();
            for(AudioDeviceInfo d:audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)){
                if(b.length()>0)b.append(", ");
                b.append(d.getProductName());
            }
            return b.length()==0?"Indisponível":b.toString();
        }catch(RuntimeException e){return "Indisponível";}
    }
    private void checkAudioRoute(){
        if(!prefs.getBoolean("native_auto",false))return;
        try{
            boolean bt=false;
            for(AudioDeviceInfo d:audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)){
                int t=d.getType();
                if(t==AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||t==AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                    ||(Build.VERSION.SDK_INT>=31&&(t==AudioDeviceInfo.TYPE_BLE_HEADSET || t==AudioDeviceInfo.TYPE_BLE_SPEAKER))){bt=true;break;}
            }
            if(bt&&!prefs.getBoolean("native_bt_active",false)){
                String id=prefs.getString("native_bt_id","");
                String chosen=prefs.getString("native_bt_profile_"+id,prefs.getString("native_bt_default","Paredão"));
                NativeMusicFxPresets.select(prefs,chosen);
                prefs.edit().putBoolean("native_bt_active",true)
                    .putString("native_bt_status","Saída Bluetooth detectada. Referência selecionada, ajuste o MusicFX manualmente.").apply();
            }else if(!bt&&prefs.getBoolean("native_bt_active",false)){
                prefs.edit().putBoolean("native_bt_active",false).apply();
            }
        }catch(RuntimeException ignored){}
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants){
        super.onRequestPermissionsResult(code,permissions,grants);render();
    }
    private void toast(String msg){Toast.makeText(this,msg,Toast.LENGTH_LONG).show();}
}
