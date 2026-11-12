package com.port.performance;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

final class PerformanceStats {
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final AtomicBoolean SENDING=new AtomicBoolean(false);
    private static int ticks=0;
    private static volatile int fps=0;
    private static volatile int ping=-1;
    private static volatile long used=0,allocated=0,max=0;
    private static volatile String lastResult="Aguardando ativação da transmissão.";

    private PerformanceStats(){}

    static void tick(){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null){BoosterEngine.sample();return;}
        if(++ticks%20!=0)return;  // read once per second, from the Minecraft client thread
        fps=Math.max(0,mc.getFps());
        PlayerInfo info=mc.getConnection()==null?null:mc.getConnection().getPlayerInfo(mc.player.getUUID());
        ping=info==null?-1:Math.max(0,info.getLatency());
        Runtime runtime=Runtime.getRuntime();
        allocated=runtime.totalMemory()/1048576L;
        used=(runtime.totalMemory()-runtime.freeMemory())/1048576L;
        max=runtime.maxMemory()/1048576L;
        BoosterEngine.sample();
        if(PerformanceConfig.streamEnabled&&ticks%100==0)send(mc.player.getGameProfile().getName());
    }

    static int fps(){return fps;}
    static int ping(){return ping;}
    static long used(){return used;}
    static long allocated(){return allocated;}
    static long max(){return max;}
    static String lastResult(){return lastResult;}

    private static void send(String player){
        if(!SENDING.compareAndSet(false,true))return;
        JsonObject json=new JsonObject();
        json.addProperty("player",player);
        json.addProperty("fps",fps);
        json.addProperty("ping",ping);
        json.addProperty("memoryUsedMb",used);
        json.addProperty("memoryAllocatedMb",allocated);
        json.addProperty("memoryMaxMb",max);
        BoosterEngine.snapshot(json);
        final String key=PerformanceConfig.token;
        try{
            HttpRequest request=HttpRequest.newBuilder(URI.create(PerformanceConfig.url+"/api/metrics"))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type","application/json")
                .header("X-Port-Perf-Key",key)
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
            HTTP.sendAsync(request,HttpResponse.BodyHandlers.ofString())
                .whenComplete((response,error)->{
                    if(error!=null)lastResult="Falha HTTPS: "+error.getClass().getSimpleName();
                    else {
                        lastResult=response.statusCode()==200?"Dados enviados para o site.":"HTTP "+response.statusCode();
                        if(response.statusCode()==200&&PerformanceConfig.webControlEnabled){
                            try{
                                JsonObject answer=JsonParser.parseString(response.body()).getAsJsonObject();
                                if(answer.has("command")&&answer.get("command").isJsonObject()){
                                    JsonObject command=answer.getAsJsonObject("command");
                                    String action=command.has("action")?command.get("action").getAsString():"";
                                    Integer target=command.has("targetFps")?command.get("targetFps").getAsInt():null;
                                    Minecraft.getInstance().execute(()->{
                                        if(PerformanceConfig.webControlEnabled)BoosterEngine.webAction(action,target);
                                    });
                                }
                            }catch(Exception error){lastResult="Resposta de controle inválida. Métricas enviadas.";}
                        }
                    }
                    SENDING.set(false);
                });
        }catch(Exception error){lastResult="Erro de configuração: "+error.getClass().getSimpleName();SENDING.set(false);}
    }
}
