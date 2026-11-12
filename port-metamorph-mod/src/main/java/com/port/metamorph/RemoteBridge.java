package com.port.metamorph;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.security.SecureRandom;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/** Outbound HTTPS only. Browser and game pair with a short-lived code. */
public final class RemoteBridge {
    public static final String WEB_URL="https://port-metamorph.onrender.com";
    private static final String DEVICE_API=WEB_URL+"/api/device/";
    private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("port-metamorph-device.json");
    private static final Gson JSON=new Gson();
    private static final SecureRandom RNG=new SecureRandom();
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
        .followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final ScheduledExecutorService EXEC=Executors.newSingleThreadScheduledExecutor(r->{
        Thread t=new Thread(r,"port-metamorph-web");t.setDaemon(true);return t;
    });
    private static String deviceId=UUID.randomUUID().toString(),deviceToken="";
    private static volatile String status="Não vinculado",currentCode="";
    private static volatile boolean paired=false;
    private static volatile boolean started=false;
    private static final class LocalDevice {String deviceId,deviceToken;}
    private static void load(){
        try{
            if(!Files.isRegularFile(FILE))return;
            LocalDevice d=JSON.fromJson(Files.readString(FILE,StandardCharsets.UTF_8),LocalDevice.class);
            if(d!=null&&d.deviceId!=null&&d.deviceId.matches("[0-9a-f-]{36}")&&
                d.deviceToken!=null&&d.deviceToken.matches("[0-9a-f]{64}")){
                deviceId=d.deviceId;deviceToken=d.deviceToken;
                status="Dispositivo anterior carregado. Use /metamorph pair para vincular.";
            }
        }catch(Exception ignored){}
    }
    private static void save(){
        try{
            Files.createDirectories(FILE.getParent());
            LocalDevice d=new LocalDevice();d.deviceId=deviceId;d.deviceToken=deviceToken;
            Files.writeString(FILE,JSON.toJson(d),StandardCharsets.UTF_8);
        }catch(Exception ignored){}
    }
    public static void start(){
        if(started)return;
        started=true;load();
        EXEC.scheduleWithFixedDelay(RemoteBridge::poll,4,4,TimeUnit.SECONDS);
    }
    public static String status(){return status;}
    public static String code(){return currentCode;}
    public static boolean paired(){return paired;}

    public static void pair(){
        byte[] random=new byte[32];RNG.nextBytes(random);
        deviceToken=java.util.HexFormat.of().formatHex(random);
        String code=String.format("%08X",RNG.nextInt());
        currentCode=code;paired=false;
        status="Registrando código no site...";
        save();
        JsonObject body=new JsonObject();
        body.addProperty("deviceId",deviceId);
        body.addProperty("deviceToken",deviceToken);
        body.addProperty("code",code);
        EXEC.execute(()->{
            try{
                JsonObject response=request("register",body,false,55);
                if(response.has("ok")&&response.get("ok").getAsBoolean()){
                    status="Código "+code+" pronto. Digite no site em até 5 min.";
                    say("Código de pareamento: §e"+code+"§f. Acesse "+WEB_URL);
                }else{
                    status="Pareamento recusado. Tente novamente.";say(status);
                }
            }catch(Exception ex){
                status="Falha de conexão com site. Use /metamorph pair novamente.";
                say(status);
            }
        });
    }
    public static void disconnect(){
        paired=false;currentCode="";deviceToken="";status="Desvinculado neste Minecraft.";
        try{Files.deleteIfExists(FILE);}catch(Exception ignored){}
        say(status);
    }
    private static void poll(){
        if(deviceToken.isBlank())return;
        JsonObject body=new JsonObject();body.addProperty("deviceId",deviceId);
        body.add("config",JsonParser.parseString(MorphState.json()));
        try{
            JsonObject response=request("poll",body,true,15);
            paired=response.has("paired")&&response.get("paired").getAsBoolean();
            status=paired?"Painel web vinculado.":"Aguardando código no painel web.";
            if(response.has("commands")&&response.get("commands").isJsonArray()){
                for(var element:response.getAsJsonArray("commands")){
                    if(!element.isJsonObject())continue;
                    JsonObject cmd=element.getAsJsonObject();
                    if(!cmd.has("type")||!"configure".equals(cmd.get("type").getAsString()))continue;
                    if(!cmd.has("config")||!cmd.get("config").isJsonObject())continue;
                    MorphState.Data next=JSON.fromJson(cmd.get("config"),MorphState.Data.class);
                    Minecraft.getInstance().execute(()->{
                        MorphState.applyFromWeb(next);
                        say("Forma atualizada pelo painel web: "+MorphState.current().name);
                    });
                }
            }
        }catch(Exception ex){
            status="Painel web indisponível. O mod local continua funcionando.";
            paired=false;
        }
    }
    private static JsonObject request(String endpoint,JsonObject body,boolean authenticate,int timeout) throws Exception{
        HttpRequest.Builder builder=HttpRequest.newBuilder(URI.create(DEVICE_API+endpoint))
            .timeout(Duration.ofSeconds(timeout))
            .header("Content-Type","application/json")
            .header("Accept","application/json")
            .POST(HttpRequest.BodyPublishers.ofString(JSON.toJson(body)));
        if(authenticate)builder.header("Authorization","Bearer "+deviceToken);
        HttpResponse<String> response=HTTP.send(builder.build(),HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()!=200||response.body().length()>150000)
            throw new IllegalStateException("HTTP "+response.statusCode());
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
    private static void say(String message){
        Minecraft.getInstance().execute(()->{
            Minecraft mc=Minecraft.getInstance();
            if(mc.player!=null)mc.player.displayClientMessage(
                Component.literal("§6[PORT METAMORPH] §f"+message),false);
        });
    }
    private RemoteBridge(){}
}
