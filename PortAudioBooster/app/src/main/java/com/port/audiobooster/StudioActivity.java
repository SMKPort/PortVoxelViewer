package com.port.audiobooster;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Interface principal do Port Audio Booster 3.0. */
public final class StudioActivity extends Activity {
    private static final int BG=0xff0a090d, PANEL=0xff1d171e, GOLD=0xffe7b74b, RED=0xffcc2639, LIGHT=0xfff4efe8;
    private static final String[] TABS={"SPOTIFY","EQ","BOOST","ESPECTRO","BLUETOOTH","AJUSTES"};
    private SharedPreferences p;
    private AudioManager audio;
    private LinearLayout body;
    private int tab=0;
    private SpectrumView spectrum;
    private static final int MIC=50,BT=51,EXPORT=52,IMPORT=53;

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        p=getSharedPreferences(GlobalAudioEngine.PREFS,MODE_PRIVATE);
        p.edit().putBoolean("native_mode",false).apply();
        audio=(AudioManager)getSystemService(AUDIO_SERVICE);
        if(!p.contains("eq_mode")){
            AudioProfiles.setPreset(p,p.getString("preset","Paredão"));
            p.edit().putInt("eq_mode",10).putBoolean("safe_gain",true).apply();
        }
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        if(p.getBoolean("enabled",false)||p.getBoolean("floating",false))service(AudioEffectsService.ACTION_START);
        render();
    }
    @Override protected void onResume(){super.onResume();if(body!=null)render();checkBluetoothRoute();}
    @Override protected void onPause(){if(spectrum!=null)spectrum.stop();super.onPause();}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int c){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(12));return d;}
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private TextView text(String s,int size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);return v;}
    private void space(LinearLayout parent,int h){parent.addView(new View(this),new LinearLayout.LayoutParams(1,dp(h)));}
    private LinearLayout section(String heading,String info){
        LinearLayout c=vertical();c.setPadding(dp(14),dp(14),dp(14),dp(14));c.setBackground(bg(PANEL));
        body.addView(c,new LinearLayout.LayoutParams(-1,-2));space(body,12);
        TextView title=text(heading,19,LIGHT);title.setTypeface(null,Typeface.BOLD);c.addView(title);
        space(c,5);c.addView(text(info,12,0xffc0abb3));space(c,14);return c;
    }
    private Button button(LinearLayout parent,String label,Runnable run){
        Button b=new Button(this);b.setAllCaps(false);b.setText(label);b.setTextColor(LIGHT);b.setTextSize(13);
        b.setBackground(bg(0xff553340));parent.addView(b,new LinearLayout.LayoutParams(-1,dp(44)));
        space(parent,6);b.setOnClickListener(v->run.run());return b;
    }
    private interface Update{void changed(int n);}
    private void slider(LinearLayout parent,String label,int min,int max,int value,String unit,Update callback){
        LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);parent.addView(r);
        r.addView(text(label,12,LIGHT),new LinearLayout.LayoutParams(0,-2,1));
        TextView v=text(value+unit,12,GOLD);r.addView(v);
        SeekBar bar=new SeekBar(this);bar.setMax(max-min);bar.setProgress(Math.max(0,Math.min(max-min,value-min)));
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(RED));
        bar.setThumbTintList(android.content.res.ColorStateList.valueOf(GOLD));
        parent.addView(bar);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int progress,boolean user){int n=progress+min;v.setText(n+unit);if(user)callback.changed(n);}
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });
        space(parent,8);
    }
    private void toggle(LinearLayout parent,String title,String key,boolean def){
        LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);parent.addView(r);
        r.addView(text(title,13,LIGHT),new LinearLayout.LayoutParams(0,-2,1));
        Switch s=new Switch(this);s.setChecked(p.getBoolean(key,def));r.addView(s);space(parent,8);
        s.setOnCheckedChangeListener((btn,on)->{
            if("auto_bt".equals(key)&&on&&Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){
                s.setChecked(false);requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},BT);return;
            }
            p.edit().putBoolean(key,on).apply();
            if("enabled".equals(key)){
                service(on?AudioEffectsService.ACTION_START:AudioEffectsService.ACTION_STOP);
            }else if("floating".equals(key)){
                if(on||p.getBoolean("enabled",false))service(AudioEffectsService.ACTION_START);
                else service(AudioEffectsService.ACTION_STOP);
            }else if("safe_gain".equals(key))apply();
            if("auto_bt".equals(key)&&on)checkBluetoothRoute();
        });
    }
    private void service(String action){
        try{
            Intent i=new Intent(this,AudioEffectsService.class).setAction(action);
            if(AudioEffectsService.ACTION_START.equals(action)||AudioEffectsService.ACTION_APPLY.equals(action))
                startForegroundService(i);else startService(i);
        }catch(RuntimeException e){toast("Android bloqueou o serviço. Abra o app e tente novamente.");}
    }
    private void apply(){if(p.getBoolean("enabled",false))service(AudioEffectsService.ACTION_APPLY);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private void render(){
        if(spectrum!=null){spectrum.stop();spectrum=null;}
        LinearLayout root=vertical();root.setPadding(dp(14),dp(14),dp(14),dp(12));root.setBackgroundColor(BG);setContentView(root);
        TextView tag=text("P O R T  A U D I O  /  V 3.0",11,GOLD);tag.setTypeface(null,Typeface.BOLD);root.addView(tag);
        root.addView(text("AUDIO BOOSTER",27,LIGHT));space(root,10);
        HorizontalScrollView scrollTabs=new HorizontalScrollView(this);scrollTabs.setHorizontalScrollBarEnabled(false);root.addView(scrollTabs);
        LinearLayout tabRow=new LinearLayout(this);scrollTabs.addView(tabRow);
        for(int i=0;i<TABS.length;i++){
            final int selected=i;
            TextView t=text(TABS[i],11,i==tab?GOLD:0xffcabfc4);t.setGravity(Gravity.CENTER);
            t.setBackground(bg(i==tab?0xff3b2631:BG));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(109),dp(43));lp.rightMargin=dp(4);tabRow.addView(t,lp);
            t.setOnClickListener(v->{tab=selected;render();});
        }
        space(root,12);
        ScrollView sv=new ScrollView(this);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        body=vertical();body.setPadding(0,0,0,dp(30));sv.addView(body);
        if(tab==0)spotify();else if(tab==1)eq();else if(tab==2)boost();
        else if(tab==3)spectro();else if(tab==4)bluetooth();else settings();
    }
    private void spotify(){
        LinearLayout c=section("🎵 Spotify","Controle o áudio do celular enquanto escuta o Spotify. A equalização externa é experimental.");
        toggle(c,"Ativar efeitos globais (experimental)","enabled",false);
        button(c,"Abrir Spotify",()->{
            Intent launch=getPackageManager().getLaunchIntentForPackage("com.spotify.music");
            if(launch!=null)startActivity(launch);else toast("Spotify não encontrado");
        });
        button(c,"Painel de EQ do Android",()->{
            try{startActivity(new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                .putExtra(AudioEffect.EXTRA_CONTENT_TYPE,AudioEffect.CONTENT_TYPE_MUSIC));}
            catch(RuntimeException e){toast("Sem equalizador nativo compatível");}
        });
        button(c,"Reprodução do Spotify / permissões",()->startActivity(new Intent(this,MainActivity.class)));
        LinearLayout ab=section("Comparação A/B","A = original (sem efeitos). B = processamento configurado, se compatível.");
        button(ab,p.getBoolean("compare_original",false)?"A: original. Ativar B":"B: efeitos. Ouvir A",()->{
            p.edit().putBoolean("compare_original",!p.getBoolean("compare_original",false)).apply();apply();render();
        });
        section("Estado de processamento",p.getString("status","Desligado"))
            .addView(text("Status da API não comprova que o Spotify esteja sendo modificado.",12,GOLD));
    }
    private void eq(){
        int mode=p.getInt("eq_mode",10);
        LinearLayout c=section("Equalizador "+mode+" bandas","Bandas virtuais interpoladas para o hardware. O aparelho pode ter menos bandas físicas.");
        for(int m:new int[]{5,10,31})button(c,m+" bandas"+(m==mode?" ✓":""),()->{
            p.edit().putInt("eq_mode",m).apply();apply();render();
        });
        int[] f=AudioProfiles.freqs(mode);
        for(int i=0;i<f.length;i++){
            String key=mode==5?"band_"+i:"eq_"+mode+"_"+i;
            String label=f[i]>=1000?(f[i]/1000d)+" kHz":f[i]+" Hz";
            slider(c,label,-12,12,p.getInt(key,0)," dB",n->{
                p.edit().putInt(key,n).putString("preset","Personalizado").apply();apply();
            });
        }
        LinearLayout preset=section("Presets","Paredão, Grave Extremo, Voz Limpa, Funk e outros perfis.");
        for(int i=0;i<AudioProfiles.NAMES.length-1;i++){
            String name=AudioProfiles.NAMES[i];
            button(preset,name+(name.equals(p.getString("preset",""))?" ✓":""),()->{
                AudioProfiles.setPreset(p,name);apply();render();
            });
        }
    }
    private void boost(){
        LinearLayout c=section("Bass Boost PRO 🔊","Ajustes de foco no subgrave, grave e impacto mapeados para o EQ do sistema.");
        int[] max={100,9,9,9,100,6};
        String[] keys={"bass","bass_sub","bass_punch","bass_impact","space","gain"};
        String[] titles={"Bass Boost","Subgrave 20–60 Hz","Grave 60–120 Hz","Impacto 120–200 Hz","Espacial 3D","Ganho (quando possível)"};
        String[] units={"%"," dB"," dB"," dB","%"," dB"};
        int[] defaults={35,0,0,0,0,0};
        for(int i=0;i<keys.length;i++){
            String key=keys[i];slider(c,titles[i],0,max[i],p.getInt(key,defaults[i]),units[i],
                n->{p.edit().putInt(key,n).apply();apply();});
        }
        toggle(c,"Limitar ganho / compressor quando suportado","safe_gain",true);
        c.addView(text("O limiter DynamicsProcessing e o ganho externo podem ser ignorados pelo Android. Cuidado com audição e alto-falantes.",12,GOLD));
        LinearLayout volume=section("Volume do Android","Volume multimídia padrão, inclusive Spotify.");
        int mx=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        slider(volume,"Volume",0,mx,audio.getStreamVolume(AudioManager.STREAM_MUSIC)," / "+mx,
            n->audio.setStreamVolume(AudioManager.STREAM_MUSIC,n,0));
    }
    private void spectro(){
        LinearLayout c=section("Analisador de espectro","Dados reais via Visualizer(0), sem simulação. O Android pode ocultar áudio do Spotify.");
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            button(c,"Autorizar API Visualizer",()->requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},MIC));return;
        }
        spectrum=new SpectrumView(this);c.addView(spectrum,new LinearLayout.LayoutParams(-1,dp(200)));
        button(c,"Atualizar captura",()->{spectrum.start();toast(spectrum.getStatus());});
        spectrum.start();
        c.addView(text(spectrum.getStatus(),12,GOLD));
    }
    private void bluetooth(){
        LinearLayout c=section("Perfis por Bluetooth","Ativação automática depende das restrições de segundo plano do Android.");
        if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)
            button(c,"Permitir dispositivos próximos",()->requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},BT));
        toggle(c,"Iniciar efeito ao conectar dispositivo Bluetooth","auto_bt",false);
        c.addView(text("Ao conectar um dispositivo de áudio, será aplicado o preset padrão ou o perfil específico vinculado.",12,GOLD));
        LinearLayout preset=section("Preset Bluetooth padrão",p.getString("bt_default","Paredão"));
        for(int i=0;i<AudioProfiles.NAMES.length-1;i++){
            String name=AudioProfiles.NAMES[i];
            button(preset,name,()->{p.edit().putString("bt_default",name).apply();render();});
        }
        LinearLayout device=section("Dispositivo atual","Última conexão: "+p.getString("bt_name","nenhum registrado"));
        String addr=p.getString("bt_current","");
        if(!addr.isEmpty())button(device,"Vincular preset atual a este dispositivo",()->{
            p.edit().putString("bt_profile_"+addr,p.getString("preset","Paredão")).apply();toast("Perfil salvo");
        });
    }
    private void settings(){
        LinearLayout overlay=section("Botão flutuante","Controle rápido sobre o Spotify: A/B, presets, graves e volume.");
        if(!Settings.canDrawOverlays(this))button(overlay,"Autorizar sobreposição",()->{
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));
        });
        else toggle(overlay,"Exibir botão flutuante","floating",false);
        LinearLayout backup=section("Presets portáteis","Exportar e importar ajustes completos em JSON.");
        button(backup,"Exportar preset JSON",()->{
            Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"PortAudio-Preset-v3.json");
            startActivityForResult(i,EXPORT);
        });
        button(backup,"Importar preset JSON",()->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/json");startActivityForResult(i,IMPORT);
        });
        LinearLayout diag=section("Diagnóstico","Regras de compatibilidade do Android.");
        diag.addView(text("Android API: "+Build.VERSION.SDK_INT
            +"\nSpotify visível: "+(getPackageManager().getLaunchIntentForPackage("com.spotify.music")!=null?"Sim":"Não")
            +"\nEfeito global: "+p.getString("status","Não testado")
            +"\nSobreposição: "+(Settings.canDrawOverlays(this)?"Sim":"Não")
            +"\nVisualizer: "+(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED?"Permissão OK":"Sem permissão")
            +"\nBluetooth: "+(Build.VERSION.SDK_INT<31||checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED?"Permissão OK":"Sem permissão")
            +"\nSaída de áudio: "+outputRoute(),13,LIGHT));
        button(diag,"Reavaliar diagnóstico",this::render);
        diag.addView(text("Spotify Connect em outro equipamento não pode ser equalizado por este celular. A/B e o analisador não comprovam processamento do Spotify.",12,GOLD));
    }
    private String outputRoute(){
        StringBuilder b=new StringBuilder();
        for(AudioDeviceInfo d:audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)){
            if(b.length()>0)b.append(", ");
            b.append(d.getProductName());
        }
        return b.toString();
    }
    private void checkBluetoothRoute(){
        if(!p.getBoolean("auto_bt",false)||p.getBoolean("enabled",false))return;
        for(AudioDeviceInfo d:audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)){
            int type=d.getType();
            if(type==AudioDeviceInfo.TYPE_BLUETOOTH_A2DP||type==AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                ||(Build.VERSION.SDK_INT>=31&&type==AudioDeviceInfo.TYPE_BLE_HEADSET)){
                AudioProfiles.setPreset(p,p.getString("bt_default","Paredão"));
                p.edit().putBoolean("enabled",true).apply();service(AudioEffectsService.ACTION_START);render();break;
            }
        }
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){
        super.onRequestPermissionsResult(request,permissions,grants);render();
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        try{
            if(request==EXPORT){
                try(OutputStream os=getContentResolver().openOutputStream(data.getData())){
                    os.write(ProfileIO.exportJson(p).getBytes(StandardCharsets.UTF_8));
                }
                toast("Preset exportado");
            }else if(request==IMPORT){
                ByteArrayOutputStream buffer=new ByteArrayOutputStream();
                try(InputStream in=getContentResolver().openInputStream(data.getData())){
                    byte[] buf=new byte[2048];int n;
                    while((n=in.read(buf))!=-1){if(buffer.size()+n>65536)throw new IllegalArgumentException("Arquivo maior que 64 KB");buffer.write(buf,0,n);}
                }
                ProfileIO.importJson(p,new String(buffer.toByteArray(),StandardCharsets.UTF_8));
                apply();render();toast("Preset importado");
            }
        }catch(Exception e){toast("Erro no preset: "+e.getClass().getSimpleName());}
    }
}
