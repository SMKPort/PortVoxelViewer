package com.port.lumiport;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.text.InputType;
import org.json.JSONObject;
import org.json.JSONArray;
import java.net.URL;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends Activity {
  private static final int BG=0xFF0B0809, CARD=0xFF1A1113, RED=0xFFC42235, GOLD=0xFFE0AD4C, TEXT=0xFFFFF4E6, SUB=0xFFC6B6AC;
  private static final UUID FFE1=UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
  private final Handler handler=new Handler(Looper.getMainLooper());
  private final ArrayList<BluetoothDevice> devices=new ArrayList<>();
  private final ArrayList<String> names=new ArrayList<>();
  private EditText haUrl, haToken, entity;
  private TextView status, bleStatus, preview;
  private SeekBar brightness;
  private BluetoothLeScanner scanner;
  private BluetoothGatt gatt;
  private BluetoothGattCharacteristic writeChar;
  private int selectedColor=RED;
  private ScanCallback scanCallback;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    ScrollView sc=new ScrollView(this); sc.setBackgroundColor(BG);
    LinearLayout root=new LinearLayout(this); root.setOrientation(1); root.setPadding(dp(20),dp(25),dp(20),dp(28)); sc.addView(root);
    root.addView(text("PORT  /  SMART HOME",12,GOLD,true));
    root.addView(text("LumiPort ✦",36,GOLD,true));
    root.addView(text("Sua luz. Seu domínio.",15,SUB,false)); gap(root,18);
    LinearLayout devicesCard=card(root);
    devicesCard.addView(text("SEUS DISPOSITIVOS",12,GOLD,true));
    devicesCard.addView(text("💡 Lâmpada Wi-Fi  •  Smart Life / Tuya",16,TEXT,true));
    devicesCard.addView(text("Controle via Home Assistant",12,SUB,false)); gap(devicesCard,8);
    devicesCard.addView(text("🌈 Fita LED  •  HappyLighting",16,TEXT,true));
    bleStatus=text("Bluetooth ainda não conectado",12,SUB,false); devicesCard.addView(bleStatus); gap(root,14);
    root.addView(text("CONTROLE GERAL",13,GOLD,true));
    LinearLayout row=new LinearLayout(this); root.addView(row);
    Button on=button("LIGAR AS DUAS",GOLD,Color.BLACK); Button off=button("DESLIGAR",RED,TEXT);
    row.addView(on,new LinearLayout.LayoutParams(0,dp(52),1)); row.addView(off,new LinearLayout.LayoutParams(0,dp(52),1));
    on.setOnClickListener(v->dispatch(true,false)); off.setOnClickListener(v->dispatch(false,false)); gap(root,18);
    LinearLayout colors=card(root); colors.addView(text("CORES E BRILHO",16,GOLD,true));
    preview=text("RGB  #C42235",15,TEXT,true); colors.addView(preview);
    String[] labels={"Rubro","Dourado","Branco","Azul","Roxo","Verde","Rosa","Ciano"};
    int[] palette={RED,GOLD,0xFFFFFFFF,0xFF2353FF,0xFF8735EF,0xFF28A75C,0xFFFE65BC,0xFF00D4E4};
    for(int i=0;i<2;i++){
      LinearLayout line=new LinearLayout(this); colors.addView(line);
      for(int j=0;j<4;j++){final int c=palette[4*i+j];Button b=button(labels[4*i+j],c,contrast(c)); b.setTextSize(11);
        line.addView(b,new LinearLayout.LayoutParams(0,dp(50),1)); b.setOnClickListener(v->{selectedColor=c;updatePreview();dispatch(true,true);});}
    }
    colors.addView(text("BRILHO",12,SUB,true));
    brightness=new SeekBar(this); brightness.setMax(100);brightness.setProgress(80);colors.addView(brightness);
    Button apply=button("APLICAR COR E BRILHO",RED,TEXT);colors.addView(apply);
    apply.setOnClickListener(v->dispatch(true,true)); gap(root,15);
    root.addView(text("CENAS",13,GOLD,true));
    LinearLayout scenes=new LinearLayout(this);root.addView(scenes);
    Button gaming=button("🎮 Gaming",CARD,TEXT); Button relax=button("🌙 Relaxar",CARD,TEXT);
    scenes.addView(gaming,new LinearLayout.LayoutParams(0,dp(55),1));
    scenes.addView(relax,new LinearLayout.LayoutParams(0,dp(55),1));
    gaming.setOnClickListener(v->{selectedColor=RED;brightness.setProgress(90);updatePreview();dispatch(true,true);});
    relax.setOnClickListener(v->{selectedColor=GOLD;brightness.setProgress(30);updatePreview();dispatch(true,true);});
    gap(root,15);
    LinearLayout settings=card(root);
    settings.addView(text("CONEXÕES",16,GOLD,true));
    settings.addView(text("Home Assistant com integração Tuya",12,SUB,false));
    haUrl=input(settings,"URL Home Assistant (http://192.168.1.10:8123)",false);
    haToken=input(settings,"Token de acesso (não é salvo)",true);
    entity=input(settings,"Entidade da lâmpada: light.quarto",false);
    haUrl.setText(getPreferences(MODE_PRIVATE).getString("url",""));
    entity.setText(getPreferences(MODE_PRIVATE).getString("entity",""));
    Button save=button("SALVAR ENDEREÇO",GOLD,Color.BLACK);settings.addView(save);
    save.setOnClickListener(v->{getPreferences(MODE_PRIVATE).edit().putString("url",haUrl.getText().toString().trim()).putString("entity",entity.getText().toString().trim()).apply();toast("Endereço e entidade salvos. O token não é armazenado.");});
    Button scan=button("⌁  CONECTAR FITA BLUETOOTH",RED,TEXT);settings.addView(scan);scan.setOnClickListener(v->ensureScanPermission());
    settings.addView(text("Compatível no modo direto apenas com LEDBLE / LEDCAR e característica FFE1. Conexão Bluetooth precisa de proximidade.",12,SUB,false));
    gap(root,14);status=text("Aguardando configuração",13,GOLD,false);root.addView(status);
    setContentView(sc);updatePreview();
  }
  private void updatePreview(){if(preview!=null){preview.setText(String.format(java.util.Locale.ROOT,"RGB  #%06X",selectedColor&0xFFFFFF));preview.setTextColor(selectedColor==0xFFFFFFFF?TEXT:selectedColor);}}
  private void dispatch(boolean on,boolean withColor){
    powerBle(on,withColor);
    String url=haUrl.getText().toString().trim(),token=haToken.getText().toString().trim(),id=entity.getText().toString().trim();
    if(!id.matches("light\\.[a-z0-9_]+")||url.isEmpty()||token.isEmpty()){
      inform("Informe URL, token e entidade do Home Assistant para controlar a lâmpada");return;
    }
    // Security: plain HTTP with token only to private local IPs / homeassistant.local.
    if(url.startsWith("http://")&&!url.matches("http://(localhost|homeassistant\\.local|127\\.[0-9.]+|10\\.[0-9.]+|192\\.168\\.[0-9.]+|172\\.(1[6-9]|2[0-9]|3[01])\\.[0-9.]+)(:[0-9]+)?/?")) {
      inform("Em HTTP use endereço privado da rede. Para internet use HTTPS.");return;
    }
    if(!url.startsWith("http://")&&!url.startsWith("https://")){inform("URL deve começar com http:// ou https://");return;}
    int rgb=selectedColor;int br=Math.max(1,brightness.getProgress());
    inform("Enviando comandos...");
    new Thread(()->{
      HttpURLConnection connection=null;
      try{
        String endpoint=url.replaceAll("/+$","")+"/api/services/light/"+(on?"turn_on":"turn_off");
        connection=(HttpURLConnection)new URL(endpoint).openConnection();
        connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(6000);connection.setReadTimeout(8000);
        connection.setRequestMethod("POST");connection.setRequestProperty("Authorization","Bearer "+token);
        connection.setRequestProperty("Content-Type","application/json");connection.setDoOutput(true);
        JSONObject data=new JSONObject();data.put("entity_id",id);
        if(on&&withColor){JSONArray colors=new JSONArray();colors.put((rgb>>16)&255);colors.put((rgb>>8)&255);colors.put(rgb&255);data.put("rgb_color",colors);data.put("brightness_pct",br);}
        try(OutputStream os=connection.getOutputStream()){os.write(data.toString().getBytes(StandardCharsets.UTF_8));}
        int response=connection.getResponseCode();inform(response<300?"Comando recebido pelo Home Assistant":"Falha HTTP "+response);
      }catch(Exception ex){inform("Home Assistant: "+ex.getMessage());}
      finally{if(connection!=null)connection.disconnect();}
    }).start();
  }
  @SuppressWarnings("deprecation")
  private void powerBle(boolean on,boolean withColor){
    if(gatt==null||writeChar==null)return;
    sendBle(new byte[]{0x7E,0x04,0x04,(byte)(on?1:0),(byte)0xFF,(byte)0xFF,(byte)0xFF,0x00,(byte)0xEF});
    if(on&&withColor){final int c=selectedColor; final int b=Math.max(1,brightness.getProgress());
      handler.postDelayed(()->sendBle(new byte[]{0x7E,0x07,0x05,0x03,(byte)(c>>16),(byte)(c>>8),(byte)c,0x00,(byte)0xEF}),160);
      handler.postDelayed(()->sendBle(new byte[]{0x7E,0x04,0x01,(byte)b,(byte)0xFF,(byte)0xFF,(byte)0xFF,0x00,(byte)0xEF}),330);
    }
  }
  @SuppressWarnings("deprecation")
  @android.annotation.SuppressLint("MissingPermission")
  private void sendBle(byte[] bytes){
    if(gatt==null||writeChar==null)return;
    try{
      if(Build.VERSION.SDK_INT>=33)gatt.writeCharacteristic(writeChar,bytes,BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
      else{writeChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);writeChar.setValue(bytes);gatt.writeCharacteristic(writeChar);}
    }catch(Exception ex){inform("Bluetooth: "+ex.getMessage());}
  }
  private void ensureScanPermission(){
    ArrayList<String> permissions=new ArrayList<>();
    if(Build.VERSION.SDK_INT>=31){
      if(checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)!=PackageManager.PERMISSION_GRANTED)permissions.add(Manifest.permission.BLUETOOTH_SCAN);
      if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
    }else if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
    if(permissions.isEmpty())scan();else requestPermissions(permissions.toArray(new String[0]),42);
  }
  @Override public void onRequestPermissionsResult(int requestCode,String[] perms,int[] grants){
    super.onRequestPermissionsResult(requestCode,perms,grants);
    if(requestCode!=42)return;
    for(int g:grants)if(g!=PackageManager.PERMISSION_GRANTED){inform("Permissão Bluetooth necessária");return;}
    scan();
  }
  @android.annotation.SuppressLint("MissingPermission")
  private void scan(){
    BluetoothManager manager=(BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
    BluetoothAdapter adapter=manager==null?null:manager.getAdapter();
    if(adapter==null||!adapter.isEnabled()){inform("Ative o Bluetooth do celular");return;}
    scanner=adapter.getBluetoothLeScanner();if(scanner==null){inform("Scanner Bluetooth indisponível");return;}
    devices.clear();names.clear();
    scanCallback=new ScanCallback(){
      @Override public void onScanResult(int callbackType,ScanResult r){
        BluetoothDevice d=r.getDevice();
        String name=r.getScanRecord()==null?null:r.getScanRecord().getDeviceName();
        if(name==null)return;
        String test=name.toLowerCase(java.util.Locale.ROOT);
        if(!test.startsWith("ledble")&&!test.startsWith("ledcar-00"))return;
        if(!devices.contains(d)){devices.add(d);names.add(name+"  ("+d.getAddress()+")");}
      }
      @Override public void onScanFailed(int code){inform("Falha no scan Bluetooth: "+code);}
    };
    try{scanner.startScan(scanCallback);inform("Buscando fitas LEDBLE/LEDCAR...");
      handler.postDelayed(()->{try{scanner.stopScan(scanCallback);}catch(Exception ignored){}
        if(devices.isEmpty()){inform("Nenhuma fita compatível detectada");return;}
        new AlertDialog.Builder(this).setTitle("Escolha a fita").setItems(names.toArray(new String[0]),(dialog,index)->connect(devices.get(index))).setNegativeButton("Cancelar",null).show();
      },8000);
    }catch(Exception ex){inform("Falha ao iniciar Bluetooth: "+ex.getMessage());}
  }
  @android.annotation.SuppressLint("MissingPermission")
  private void connect(BluetoothDevice device){
    if(gatt!=null){gatt.close();gatt=null;writeChar=null;}
    inform("Conectando à fita...");
    gatt=device.connectGatt(this,false,new BluetoothGattCallback(){
      @Override public void onConnectionStateChange(BluetoothGatt g,int status,int state){
        if(status==BluetoothGatt.GATT_SUCCESS&&state==BluetoothProfile.STATE_CONNECTED)g.discoverServices();
        else if(state==BluetoothProfile.STATE_DISCONNECTED){writeChar=null;inform("Fita desconectada");}
      }
      @Override public void onServicesDiscovered(BluetoothGatt g,int code){
        if(code!=BluetoothGatt.GATT_SUCCESS){inform("Falha nos serviços BLE");return;}
        for(BluetoothGattService service:g.getServices()){
          BluetoothGattCharacteristic c=service.getCharacteristic(FFE1);
          if(c!=null&&((c.getProperties()&(BluetoothGattCharacteristic.PROPERTY_WRITE|BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE))!=0)){
            writeChar=c;inform("Fita conectada via Bluetooth");runOnUiThread(()->bleStatus.setText("● Fita LED conectada"));return;
          }
        }
        inform("Controlador não possui FFE1. Necessária outra integração.");
      }
    },BluetoothDevice.TRANSPORT_LE);
  }
  private void inform(String message){runOnUiThread(()->{if(status!=null)status.setText(message);});}
  private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
  private LinearLayout card(LinearLayout root){LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(dp(14),dp(16),dp(14),dp(16));
    GradientDrawable d=shape(CARD,16);d.setStroke(dp(1),0xFF60402F);l.setBackground(d);root.addView(l);
    return l;}
  private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
    if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(4),0,dp(4));return t;}
  private EditText input(LinearLayout parent,String hint,boolean secret){
    EditText e=new EditText(this);e.setTextColor(TEXT);e.setHintTextColor(SUB);e.setHint(hint);e.setSingleLine(true);e.setTextSize(13);
    if(secret)e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
    e.setBackground(shape(0xFF302023,9));e.setPadding(dp(12),dp(13),dp(12),dp(13));
    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(8);parent.addView(e,p);return e;}
  private Button button(String label,int background,int foreground){Button b=new Button(this);b.setText(label);b.setTextSize(12);b.setAllCaps(false);b.setTextColor(foreground);
    b.setBackground(shape(background,12));b.setPadding(dp(5),dp(5),dp(5),dp(5));b.setMinHeight(dp(50));return b;}
  private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
  private int contrast(int c){int y=((c>>16&255)*299+(c>>8&255)*587+(c&255)*114)/1000;return y>165?Color.BLACK:Color.WHITE;}
  private void gap(LinearLayout root,int height){root.addView(new View(this),new LinearLayout.LayoutParams(1,dp(height)));}
  private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
  @android.annotation.SuppressLint("MissingPermission")
  @Override public void onDestroy(){if(scanner!=null&&scanCallback!=null)try{scanner.stopScan(scanCallback);}catch(Exception ignored){}if(gatt!=null)gatt.close();super.onDestroy();}
}
