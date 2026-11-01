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
  private final ScrollView[] pageViews=new ScrollView[5];
  private final Button[] navButtons=new Button[5];
  private TextView pageSubtitle;


  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    LinearLayout shell=new LinearLayout(this);
    shell.setOrientation(LinearLayout.VERTICAL);
    shell.setBackgroundColor(BG);

    LinearLayout header=new LinearLayout(this);
    header.setOrientation(LinearLayout.VERTICAL);
    header.setPadding(dp(18),dp(20),dp(18),dp(11));
    header.addView(text("PORT  /  SMART HOME",11,GOLD,true));
    header.addView(text("LumiPort ✦",30,GOLD,true));
    pageSubtitle=text("Tudo em um toque",13,SUB,false);
    header.addView(pageSubtitle);
    shell.addView(header);

    android.widget.FrameLayout contentFrame=new android.widget.FrameLayout(this);
    shell.addView(contentFrame,new LinearLayout.LayoutParams(-1,0,1));
    String[] titles={"Início","Dispositivos","Cores","Cenas","Ajustes"};
    String[] symbols={"⌂","◈","◉","✦","⚙"};
    LinearLayout[] pageRoots=new LinearLayout[5];
    for(int i=0;i<5;i++){
      ScrollView scroll=new ScrollView(this);
      scroll.setFillViewport(true);
      scroll.setVerticalScrollBarEnabled(false);
      LinearLayout page=new LinearLayout(this);
      page.setOrientation(LinearLayout.VERTICAL);
      page.setPadding(dp(18),dp(12),dp(18),dp(23));
      scroll.addView(page);
      contentFrame.addView(scroll,new android.widget.FrameLayout.LayoutParams(-1,-1));
      pageViews[i]=scroll;
      pageRoots[i]=page;
      scroll.setVisibility(View.GONE);
    }

    // INÍCIO: somente os comandos essenciais.
    LinearLayout home=pageRoots[0];
    home.addView(text("CONTROLE CENTRAL",12,GOLD,true));
    LinearLayout master=card(home);
    master.addView(text("Iluminação conjunta",20,TEXT,true));
    master.addView(text("Controle a lâmpada e a fita com um toque.",13,SUB,false));
    gap(master,13);
    LinearLayout masterRow=new LinearLayout(this);
    Button allOn=button("●  Ligar tudo",GOLD,Color.BLACK);
    Button allOff=button("○  Desligar tudo",RED,TEXT);
    masterRow.addView(allOn,new LinearLayout.LayoutParams(0,dp(52),1));
    masterRow.addView(allOff,new LinearLayout.LayoutParams(0,dp(52),1));
    master.addView(masterRow);
    allOn.setOnClickListener(v->dispatch(true,false));
    allOff.setOnClickListener(v->dispatch(false,false));
    gap(home,16);
    LinearLayout summary=card(home);
    summary.addView(text("SEUS DISPOSITIVOS",12,GOLD,true));
    summary.addView(text("💡  Lâmpada Wi-Fi",16,TEXT,true));
    summary.addView(text("Smart Life / Tuya • Home Assistant",12,SUB,false));
    gap(summary,10);
    summary.addView(text("🌈  Fita LED",16,TEXT,true));
    summary.addView(text("HappyLighting • Bluetooth",12,SUB,false));
    gap(home,15);
    Button goColors=button("◉  Cores e brilho",CARD,GOLD);
    home.addView(goColors);
    goColors.setOnClickListener(v->showTab(2));
    Button goScenes=button("✦  Cenas prontas",CARD,GOLD);
    home.addView(goScenes);
    goScenes.setOnClickListener(v->showTab(3));
    gap(home,12);
    home.addView(text("As duas luzes recebem os comandos do app. Mudanças feitas em outros aplicativos não são sincronizadas automaticamente.",12,SUB,false));

    // DISPOSITIVOS: comandos independentes.
    LinearLayout devicesPage=pageRoots[1];
    devicesPage.addView(text("CONTROLE INDIVIDUAL",12,GOLD,true));
    LinearLayout bulbCard=card(devicesPage);
    bulbCard.addView(text("💡  Lâmpada Wi-Fi",19,TEXT,true));
    bulbCard.addView(text("Smart Life / Tuya via Home Assistant",12,SUB,false));
    gap(bulbCard,12);
    LinearLayout bulbRow=new LinearLayout(this);
    Button bulbOn=button("Ligar lâmpada",GOLD,Color.BLACK);
    Button bulbOff=button("Desligar",RED,TEXT);
    bulbRow.addView(bulbOn,new LinearLayout.LayoutParams(0,dp(52),1));
    bulbRow.addView(bulbOff,new LinearLayout.LayoutParams(0,dp(52),1));
    bulbCard.addView(bulbRow);
    bulbOn.setOnClickListener(v->powerHa(true,false));
    bulbOff.setOnClickListener(v->powerHa(false,false));
    gap(devicesPage,14);
    LinearLayout stripCard=card(devicesPage);
    stripCard.addView(text("🌈  Fita LED",19,TEXT,true));
    stripCard.addView(text("HappyLighting • conexão Bluetooth",12,SUB,false));
    bleStatus=text("Bluetooth ainda não conectado",12,SUB,false);
    stripCard.addView(bleStatus);
    gap(stripCard,12);
    LinearLayout stripRow=new LinearLayout(this);
    Button stripOn=button("Ligar fita",GOLD,Color.BLACK);
    Button stripOff=button("Desligar",RED,TEXT);
    stripRow.addView(stripOn,new LinearLayout.LayoutParams(0,dp(52),1));
    stripRow.addView(stripOff,new LinearLayout.LayoutParams(0,dp(52),1));
    stripCard.addView(stripRow);
    stripOn.setOnClickListener(v->powerBle(true,false));
    stripOff.setOnClickListener(v->powerBle(false,false));
    gap(devicesPage,14);
    Button deviceSetup=button("⚙  Configurar as conexões",CARD,GOLD);
    devicesPage.addView(deviceSetup);
    deviceSetup.setOnClickListener(v->showTab(4));

    // CORES: escolha RGB e brilho sem misturar ajustes técnicos.
    LinearLayout colorsPage=pageRoots[2];
    colorsPage.addView(text("CORES E BRILHO",12,GOLD,true));
    LinearLayout colorCard=card(colorsPage);
    colorCard.addView(text("Aplicar nas duas luzes",18,TEXT,true));
    colorCard.addView(text("Toque na cor desejada para aplicar.",12,SUB,false));
    gap(colorCard,10);
    preview=text("RGB  #C42235",16,TEXT,true);
    colorCard.addView(preview);
    gap(colorCard,10);
    String[] colorLabels={"Rubro","Dourado","Branco","Azul","Roxo","Verde","Rosa","Ciano"};
    int[] palette={RED,GOLD,0xFFFFFFFF,0xFF2353FF,0xFF8735EF,0xFF28A75C,0xFFFE65BC,0xFF00D4E4};
    for(int i=0;i<2;i++){
      LinearLayout paletteRow=new LinearLayout(this);
      colorCard.addView(paletteRow);
      for(int j=0;j<4;j++){
        int at=i*4+j;
        final int color=palette[at];
        Button swatch=button(colorLabels[at],color,contrast(color));
        swatch.setTextSize(11);
        paletteRow.addView(swatch,new LinearLayout.LayoutParams(0,dp(52),1));
        swatch.setOnClickListener(v->{selectedColor=color;updatePreview();dispatch(true,true);});
      }
    }
    gap(colorCard,16);
    colorCard.addView(text("BRILHO GERAL",12,GOLD,true));
    brightness=new SeekBar(this);
    brightness.setMax(100);
    brightness.setProgress(80);
    colorCard.addView(brightness);
    Button applyColor=button("Aplicar cor e brilho",RED,TEXT);
    colorCard.addView(applyColor);
    applyColor.setOnClickListener(v->dispatch(true,true));
    gap(colorsPage,10);
    colorsPage.addView(text("A cor selecionada será enviada à lâmpada e à fita quando você aplicar.",12,SUB,false));

    // CENAS: presets acessíveis com um toque.
    LinearLayout scenesPage=pageRoots[3];
    scenesPage.addView(text("AMBIENTES PRONTOS",12,GOLD,true));
    scenesPage.addView(text("Cada cena ajusta cor e brilho das duas luzes.",13,SUB,false));
    gap(scenesPage,12);
    String[] sceneNames={"🎮  Gaming","🌙  Relaxar","🔥  Rubro","☀  Branco"};
    String[] descriptions={"Vermelho • 90%","Dourado suave • 30%","Vermelho intenso • 95%","Branco • 100%"};
    int[] sceneColors={RED,GOLD,0xFFFF163B,Color.WHITE};
    int[] sceneLevels={90,30,95,100};
    for(int i=0;i<sceneNames.length;i++){
      final int color=sceneColors[i], level=sceneLevels[i];
      LinearLayout sceneCard=card(scenesPage);
      sceneCard.addView(text(sceneNames[i],19,TEXT,true));
      sceneCard.addView(text(descriptions[i],13,SUB,false));
      gap(sceneCard,9);
      Button run=button("Aplicar cena",CARD,GOLD);
      sceneCard.addView(run);
      run.setOnClickListener(v->{selectedColor=color;brightness.setProgress(level);updatePreview();dispatch(true,true);});
      gap(scenesPage,10);
    }

    // AJUSTES: credenciais, configurações e busca Bluetooth.
    LinearLayout settingsPage=pageRoots[4];
    settingsPage.addView(text("CONEXÕES",12,GOLD,true));
    LinearLayout settings=card(settingsPage);
    settings.addView(text("Home Assistant / Tuya",18,TEXT,true));
    settings.addView(text("Configure sua lâmpada Smart Life.",12,SUB,false));
    haUrl=input(settings,"URL do Home Assistant (http://192.168.1.10:8123)",false);
    haToken=input(settings,"Token de acesso (não é salvo)",true);
    entity=input(settings,"Entidade da lâmpada: light.quarto",false);
    haUrl.setText(getPreferences(MODE_PRIVATE).getString("url",""));
    entity.setText(getPreferences(MODE_PRIVATE).getString("entity",""));
    Button save=button("Salvar endereço e entidade",GOLD,Color.BLACK);
    settings.addView(save);
    save.setOnClickListener(v->{getPreferences(MODE_PRIVATE).edit().putString("url",haUrl.getText().toString().trim()).putString("entity",entity.getText().toString().trim()).apply();toast("Endereço e entidade salvos. O token não é armazenado.");});
    gap(settingsPage,14);
    LinearLayout bleSettings=card(settingsPage);
    bleSettings.addView(text("Fita HappyLighting",18,TEXT,true));
    bleSettings.addView(text("Busque seu controlador Bluetooth próximo.",12,SUB,false));
    Button scan=button("⌁  Procurar fita Bluetooth",RED,TEXT);
    bleSettings.addView(scan);
    scan.setOnClickListener(v->ensureScanPermission());
    bleSettings.addView(text("Compatível com controladores LEDBLE / LEDCAR e característica FFE1. Outros modelos podem exigir integração adicional.",12,SUB,false));

    status=text("Pronto para configurar. Nenhum dispositivo foi acionado.",12,GOLD,false);
    status.setMaxLines(2);
    status.setPadding(dp(15),dp(9),dp(15),dp(9));
    status.setBackgroundColor(0xFF1A1113);
    shell.addView(status);
    LinearLayout nav=new LinearLayout(this);
    nav.setOrientation(LinearLayout.HORIZONTAL);
    nav.setPadding(dp(3),dp(5),dp(3),dp(6));
    nav.setBackgroundColor(CARD);
    for(int i=0;i<5;i++){
      final int index=i;
      Button button=new Button(this);
      button.setText(symbols[i]+"\n"+titles[i]);
      button.setAllCaps(false);
      button.setTextSize(10);
      button.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
      button.setTextColor(SUB);
      button.setMinHeight(dp(58));
      button.setPadding(0,0,0,0);
      button.setBackground(shape(CARD,12));
      button.setContentDescription("Aba "+titles[i]);
      nav.addView(button,new LinearLayout.LayoutParams(0,dp(62),1));
      navButtons[i]=button;
      button.setOnClickListener(v->showTab(index));
    }
    shell.addView(nav);
    setContentView(shell);
    updatePreview();
    showTab(getPreferences(MODE_PRIVATE).getInt("selected_tab",0));
  }

  private void showTab(int tab){
    if(tab<0||tab>=pageViews.length)tab=0;
    final String[] captions={"Tudo em um toque","Controle cada luz separadamente","Escolha a cor e o brilho","Cenas para cada momento","Conexões e preferências"};
    for(int i=0;i<pageViews.length;i++){
      boolean chosen=(i==tab);
      pageViews[i].setVisibility(chosen?View.VISIBLE:View.GONE);
      navButtons[i].setTextColor(chosen?GOLD:SUB);
      navButtons[i].setBackground(shape(chosen?0xFF452129:CARD,12));
    }
    pageSubtitle.setText(captions[tab]);
    getPreferences(MODE_PRIVATE).edit().putInt("selected_tab",tab).apply();
  }
  private void updatePreview(){if(preview!=null){preview.setText(String.format(java.util.Locale.ROOT,"RGB  #%06X",selectedColor&0xFFFFFF));preview.setTextColor(selectedColor==0xFFFFFFFF?TEXT:selectedColor);}}
  private void dispatch(boolean on,boolean withColor){
    powerBle(on,withColor);
    powerHa(on,withColor);
  }
  private void powerHa(boolean on,boolean withColor){
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
    if(gatt==null||writeChar==null){inform("Conecte a fita Bluetooth em Ajustes.");return;}
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
