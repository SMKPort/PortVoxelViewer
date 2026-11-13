package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.neoforged.fml.loading.FMLPaths;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Local-only voice playback. Never sends synthesized sound to Plasmo Voice.
 * Off by default. No PCM/audio file is written to disk.
 */
final class NexusSpeaker {
    private static final Gson JSON=new Gson();
    private static final Path SETTINGS=FMLPaths.CONFIGDIR.get().resolve("nexus-speaker.json");
    private static final Set<String> VOICES=Set.of("auto","Charon","Orus","Kore","Puck","Algenib","Fenrir");
    private static Settings settings;
    private static long lastAt;
    private static volatile boolean busy;
    private static volatile String lastError="";
    private static volatile SourceDataLine output;
    private static class Settings {
        boolean enabled=false;
        String voice="auto";
    }
    private NexusSpeaker(){}
    private static synchronized void load(){
        if(settings!=null)return;
        settings=new Settings();
        if(!Files.isRegularFile(SETTINGS))return;
        try{
            Settings stored=JSON.fromJson(Files.readString(SETTINGS,StandardCharsets.UTF_8),Settings.class);
            if(stored!=null&&VOICES.contains(stored.voice))settings=stored;
        }catch(Exception ex){System.err.println("Nexus TTS: preferências inválidas.");}
    }
    static synchronized JsonObject snapshot(){
        load();
        JsonObject result=new JsonObject();
        result.addProperty("enabled",settings.enabled);
        result.addProperty("voice",settings.voice);
        result.addProperty("playing",busy);
        result.addProperty("lastError",lastError);
        return result;
    }
    static synchronized void configure(JsonObject input){
        load();
        if(input==null||!input.has("enabled")||!input.get("enabled").isJsonPrimitive()||
           !input.get("enabled").getAsJsonPrimitive().isBoolean()||
           !input.has("voice")||!input.get("voice").isJsonPrimitive())
            throw new IllegalArgumentException("Voz: preferências inválidas.");
        String newVoice=input.get("voice").getAsString();
        if(!VOICES.contains(newVoice))throw new IllegalArgumentException("Voz não suportada.");
        boolean enabled=input.get("enabled").getAsBoolean();
        settings.enabled=enabled;settings.voice=newVoice;
        if(!enabled)stop();
        persist();
    }
    static synchronized void enable(boolean enabled){
        load();settings.enabled=enabled;
        if(!enabled)stop();
        persist();
    }
    static synchronized void voice(String selected){
        load();if(!VOICES.contains(selected))throw new IllegalArgumentException("Voz inválida.");
        settings.voice=selected;persist();
    }
    private static String selected(int stage){
        if(!"auto".equals(settings.voice))return settings.voice;
        return switch(stage){
            case 0 -> "Charon";
            case 1 -> "Orus";
            case 2 -> "Puck";
            case 3 -> "Kore";
            default -> "Algenib";
        };
    }
    private static void persist(){
        try{
            Files.createDirectories(SETTINGS.getParent());
            Files.writeString(SETTINGS,JSON.toJson(settings),StandardCharsets.UTF_8);
        }catch(Exception e){System.err.println("Nexus TTS: não foi possível salvar preferências.");}
    }
    static synchronized String status(){
        load();
        return "Voz do Nexus "+(settings.enabled?"ON":"OFF")+
            " | "+settings.voice+" | Som "+(busy?"reproduzindo":"silencioso")+
            (lastError.isBlank()?"":" | "+lastError);
    }
    static synchronized boolean settingEnabled(){load();return settings.enabled;}

    static synchronized void onSpeech(String message,int stage){
        load();
        if(!settings.enabled||busy||ClientConfig.token.isBlank()||
           message==null||message.isBlank()||
           System.currentTimeMillis()-lastAt<11000)return;
        String safe=message.replaceAll("[\\p{Cntrl}]"," ").trim();
        if(safe.length()>300)safe=safe.substring(0,300);
        if(safe.isEmpty())return;
        busy=true;lastAt=System.currentTimeMillis();lastError="";
        ClientLink.synthesizeNexus(safe,Math.max(0,Math.min(4,stage)),selected(stage));
    }
    static void accept(JsonObject reply){
        if(reply==null||!reply.has("ok")||!reply.get("ok").getAsBoolean()||!reply.has("pcm")){
            failed(reply!=null&&reply.has("error")?reply.get("error").getAsString():
                "Gemini não devolveu áudio.");
            return;
        }
        try{
            String encoded=reply.get("pcm").getAsString();
            if(encoded.length()>3_300_000)throw new IllegalArgumentException("Áudio muito longo.");
            byte[] data=Base64.getDecoder().decode(encoded);
            if(data.length<100||data.length>2_400_000||data.length%2!=0)
                throw new IllegalArgumentException("Formato de áudio inválido.");
            CompletableFuture.runAsync(()->play(data));
        }catch(Exception e){failed("Áudio inválido: "+e.getMessage());}
    }
    private static void play(byte[] pcm){
        SourceDataLine line=null;
        try{
            AudioFormat format=new AudioFormat(24000,16,1,true,false);
            var info=new DataLine.Info(SourceDataLine.class,format);
            line=(SourceDataLine)AudioSystem.getLine(info);
            output=line;
            line.open(format,8192);
            line.start();
            int offset=0;
            while(offset<pcm.length&&isEnabled()&&line.isOpen()){
                int n=line.write(pcm,offset,Math.min(4096,pcm.length-offset));
                if(n<=0)break;
                offset+=n;
            }
            if(isEnabled()&&line.isOpen())line.drain();
        }catch(Exception error){failed("Saída de áudio indisponível: "+error.getMessage());}
        finally{
            if(line!=null){try{line.stop();line.close();}catch(Exception ignored){}}
            output=null;busy=false;
        }
    }
    private static synchronized boolean isEnabled(){load();return settings.enabled;}
    static synchronized void stop(){
        SourceDataLine line=output;
        if(line!=null){try{line.stop();line.close();}catch(Exception ignored){}}
        busy=false;
    }
    static synchronized void failed(String error){
        lastError=String.valueOf(error).substring(0,Math.min(150,String.valueOf(error).length()));
        busy=false;
    }
}
