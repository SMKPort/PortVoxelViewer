package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.neoforged.fml.loading.FMLPaths;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Base64;

/**
 * Hearing and optional memory. Defaults entirely OFF; explicit peer consent
 * must be obtained before adding each speaker to an allowlist. No WAV is stored.
 */
final class NexusHearing {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path SETTINGS=FMLPaths.CONFIGDIR.get().resolve("nexus-hearing.json");
    private static final Path MEMORIES=FMLPaths.CONFIGDIR.get().resolve("nexus-hearing-memories.json");
    private static final int SAMPLE_RATE=16000, MAX_SECONDS=5;
    private static final long GAP_MS=800, SERVER_GAP_MS=8500;
    private static Settings settings;
    private static List<Memory> memory;
    private static final Map<String,Clip> pending=new LinkedHashMap<>();
    private static volatile boolean plasmoReady;
    private static long lastUpload;
    private static boolean busy;

    private static class Settings {
        boolean listening=false,memory=false,answer=false;
        List<String> allowedPlayers=new ArrayList<>();
    }
    private static class Memory {
        String speaker,text,at;
        Memory(String speaker,String text){
            this.speaker=speaker;this.text=text;
            this.at=java.time.Instant.now().toString();
        }
    }
    private static class Clip {
        final ByteArrayOutputStream pcm=new ByteArrayOutputStream();
        final long started=System.currentTimeMillis();
        long lastFrame=started;
        int samples;
    }
    private NexusHearing(){}
    private static void load(){
        if(settings!=null)return;
        settings=new Settings();
        if(!Files.isRegularFile(SETTINGS))return;
        try{
            Settings stored=JSON.fromJson(Files.readString(SETTINGS,StandardCharsets.UTF_8),Settings.class);
            if(stored!=null){
                settings=stored;
                if(settings.allowedPlayers==null)settings.allowedPlayers=new ArrayList<>();
                settings.allowedPlayers.removeIf(s->s==null||!s.matches("[A-Za-z0-9_]{3,16}"));
                if(settings.allowedPlayers.size()>20)
                    settings.allowedPlayers=new ArrayList<>(settings.allowedPlayers.subList(0,20));
                // Hearing requires explicit reauthorization on each game launch.
                settings.listening=false;
            }
        }catch(Exception error){System.err.println("Nexus Hearing: preferências inválidas.");}
    }
    static synchronized void setPlasmoReady(boolean ready){plasmoReady=ready;if(!ready)pending.clear();}
    static synchronized boolean isListening(){load();return settings.listening&&plasmoReady;}
    static synchronized boolean isAllowed(String nick){
        load();
        return settings.allowedPlayers.stream().anyMatch(s->s.equalsIgnoreCase(nick));
    }
    static synchronized String status(){
        load();
        return "Audição "+(isListening()?"ON":settings.listening?"AGUARDANDO PLASMO":"OFF")+
            " | Plasmo "+(plasmoReady?"OK":"não detectado")+
            " | memória "+(settings.memory?"ON":"OFF")+
            " | resposta "+(settings.answer?"ON":"OFF")+
            " | jogadores: "+(settings.allowedPlayers.isEmpty()?"nenhum":String.join(", ",settings.allowedPlayers));
    }
    static synchronized void listen(boolean enabled){
        load();
        if(enabled&&settings.allowedPlayers.isEmpty())
            throw new IllegalStateException("Adicione primeiro um jogador que consentiu, via /nexusclient hearing allow NICK.");
        if(enabled&&!plasmoReady)
            throw new IllegalStateException("Plasmo Voice não está disponível no cliente.");
        settings.listening=enabled;
        pending.clear();saveSettings();
    }
    static synchronized void remember(boolean enabled){
        load();settings.memory=enabled;
        saveSettings();
    }
    static synchronized void answer(boolean enabled){
        load();settings.answer=enabled;saveSettings();
    }
    static synchronized void allow(String name){
        load();
        if(!name.matches("[A-Za-z0-9_]{3,16}"))throw new IllegalArgumentException("Nick inválido.");
        if(!isAllowed(name)){
            if(settings.allowedPlayers.size()>=20)throw new IllegalStateException("Lista cheia (máximo 20).");
            settings.allowedPlayers.add(name);
            saveSettings();
        }
    }
    static synchronized void deny(String name){
        load();
        settings.allowedPlayers.removeIf(s->s.equalsIgnoreCase(name));
        pending.keySet().removeIf(s->s.equalsIgnoreCase(name));
        if(settings.allowedPlayers.isEmpty())settings.listening=false;
        saveSettings();
    }
    static synchronized void clearMemory(){
        memory=new ArrayList<>();
        saveMemory();
    }
    static synchronized List<String> recentMemories(){
        loadMemory();
        List<String> out=new ArrayList<>();
        for(int i=memory.size()-1;i>=Math.max(0,memory.size()-6);i--)
            out.add(memory.get(i).speaker+": "+memory.get(i).text);
        return out.isEmpty()?List.of("Nenhuma memória de voz salva."):out;
    }
    private static void saveSettings(){
        try{
            Files.createDirectories(SETTINGS.getParent());
            atomic(SETTINGS,JSON.toJson(settings));
        }catch(IOException e){throw new IllegalStateException("Falha ao salvar preferências de audição.",e);}
    }
    private static void loadMemory(){
        if(memory!=null)return;
        memory=new ArrayList<>();
        if(!Files.isRegularFile(MEMORIES))return;
        try{
            Memory[] read=JSON.fromJson(Files.readString(MEMORIES,StandardCharsets.UTF_8),Memory[].class);
            if(read!=null)for(Memory item:read)
                if(item!=null&&item.speaker!=null&&item.text!=null&&item.at!=null)
                    memory.add(item);
            while(memory.size()>200)memory.remove(0);
        }catch(Exception e){System.err.println("Nexus Hearing: falha ao ler memórias.");}
    }
    private static void saveMemory(){
        try{Files.createDirectories(MEMORIES.getParent());atomic(MEMORIES,JSON.toJson(memory));}
        catch(IOException e){System.err.println("Nexus Hearing: falha ao salvar memórias.");}
    }
    private static void atomic(Path target,String body)throws IOException{
        Path tmp=target.resolveSibling(target.getFileName()+".tmp");
        Files.writeString(tmp,body,StandardCharsets.UTF_8);
        try{Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
        catch(AtomicMoveNotSupportedException e){Files.move(tmp,target,StandardCopyOption.REPLACE_EXISTING);}
    }
    /** Audio thread; only allowed speakers, never disk. Downmix and decimate 48k -> 16k. */
    static synchronized void capture(String speaker,short[] samples,int rate,int channels){
        if(!isListening()||!isAllowed(speaker)||samples==null||
            samples.length==0||rate<8000||rate>48000||channels<1||channels>2)return;
        if(busy||pending.size()>3)return;
        Clip clip=pending.computeIfAbsent(speaker,key->new Clip());
        if(clip.samples>=SAMPLE_RATE*MAX_SECONDS)return;
        int frames=samples.length/channels;
        // Average source frames into the nearest 16kHz buckets to avoid aliasing.
        for(int outFrame=0;outFrame<(int)Math.ceil(frames*16000.0/rate);outFrame++){
            int from=(int)Math.floor(outFrame*rate/16000.0);
            int to=Math.min(frames,Math.max(from+1,(int)Math.floor((outFrame+1)*rate/16000.0)));
            if(from>=frames||clip.samples>=SAMPLE_RATE*MAX_SECONDS)break;
            long sum=0;int count=0;
            for(int i=from;i<to;i++)for(int c=0;c<channels;c++){sum+=samples[i*channels+c];count++;}
            int value=(int)(sum/Math.max(count,1));
            clip.pcm.write(value&255);clip.pcm.write((value>>>8)&255);clip.samples++;
        }
        clip.lastFrame=System.currentTimeMillis();
    }
    /** Called from client tick, never from audio thread. */
    static synchronized void tick(){
        if(!isListening()){pending.clear();return;}
        if(busy||lastUpload>0&&System.currentTimeMillis()-lastUpload<SERVER_GAP_MS)return;
        long now=System.currentTimeMillis();
        String who=null;Clip clip=null;
        for(var entry:pending.entrySet()){
            Clip c=entry.getValue();
            if(now-c.lastFrame>=GAP_MS||c.samples>=SAMPLE_RATE*MAX_SECONDS){
                who=entry.getKey();clip=c;break;
            }
        }
        if(who==null)return;
        pending.remove(who);
        if(clip.samples<SAMPLE_RATE/2)return;
        byte[] wave=wave(clip.pcm.toByteArray());
        JsonArray history=new JsonArray();
        if(settings.memory){
            loadMemory();
            for(int i=Math.max(0,memory.size()-7);i<memory.size();i++){
                Memory item=memory.get(i);
                if(!item.speaker.equalsIgnoreCase(who))continue;
                JsonObject row=new JsonObject();
                row.addProperty("role","user");row.addProperty("text",item.text.substring(0,Math.min(item.text.length(),250)));
                history.add(row);
            }
        }
        busy=true;lastUpload=now;
        ClientLink.transcribeHearing(who,wave,settings.answer,history,new ArrayList<>(settings.allowedPlayers));
    }
    private static byte[] wave(byte[] pcm){
        int length=pcm.length;
        var buffer=java.nio.ByteBuffer.allocate(44+length).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        buffer.put(new byte[]{'R','I','F','F'}).putInt(length+36)
            .put(new byte[]{'W','A','V','E','f','m','t',' '}).putInt(16).putShort((short)1)
            .putShort((short)1).putInt(SAMPLE_RATE).putInt(SAMPLE_RATE*2)
            .putShort((short)2).putShort((short)16)
            .put(new byte[]{'d','a','t','a'}).putInt(length).put(pcm);
        return buffer.array();
    }
    static synchronized void onTranscription(String speaker,String transcript,String reply){
        busy=false;
        if(!isAllowed(speaker)||!settings.listening)return;
        String text=transcript.replaceAll("[\\p{Cntrl}]"," ").trim();
        if(text.length()>400)text=text.substring(0,400);
        if(!text.isEmpty()&&settings.memory){
            loadMemory();memory.add(new Memory(speaker,text));
            while(memory.size()>200)memory.remove(0);
            saveMemory();
        }
        if(!text.isEmpty()&&settings.answer&&!reply.isBlank())ClientLink.speak(reply,"mind");
    }
    static synchronized void onTranscriptionError(String error){
        busy=false;
        if(settings.listening)NexusClientMod.notifyPlayer("Audição: "+error);
    }
}
