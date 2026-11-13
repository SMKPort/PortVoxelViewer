package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.neoforged.fml.loading.FMLPaths;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Streaming Plasmo hearing, consent-only, no audio saved to disk. */
final class NexusHearing {
    private static final Gson JSON=new Gson();
    private static final Path SETTINGS=FMLPaths.CONFIGDIR.get().resolve("nexus-hearing.json");
    private static final NexusHearingMemory MEMORIES=new NexusHearingMemory();
    private static final int PARALLEL=3,MAX_QUEUED=36;
    private static final long MAX_QUEUE_BYTES=12L*1024*1024;
    private static final Map<String,NexusVoiceSegmenter> streams=new LinkedHashMap<>();
    private static final ArrayDeque<Clip> queued=new ArrayDeque<>();
    private static final Map<Long,Clip> inFlight=new LinkedHashMap<>();
    private static Settings settings;
    private static volatile boolean plasmoReady;
    private static long nextId=1,queueBytes,processed,dropped,retries,noticeAt,dispatchAfter;
    private static String lastError="",lastSpeaker="";

    private static class Settings {
        boolean listening=false,memory=false,answer=false;
        int sensitivity=55;
        List<String> allowedPlayers=new ArrayList<>();
    }
    private static final class Clip {
        final long id,capturedAt;
        final String speaker;
        final byte[] pcm;
        int attempts;
        Clip(long id,String speaker,NexusVoiceSegmenter.Segment segment){
            this.id=id;this.speaker=speaker;pcm=segment.pcm;capturedAt=segment.startedAtMs;
        }
    }
    private NexusHearing(){}
    private static void load(){
        if(settings!=null)return;
        settings=new Settings();
        if(!Files.isRegularFile(SETTINGS))return;
        try{
            Settings saved=JSON.fromJson(Files.readString(SETTINGS,StandardCharsets.UTF_8),Settings.class);
            if(saved!=null){
                settings=saved;
                if(settings.allowedPlayers==null)settings.allowedPlayers=new ArrayList<>();
                settings.allowedPlayers.removeIf(s->s==null||!s.matches("[A-Za-z0-9_]{3,16}"));
                if(settings.allowedPlayers.size()>20)
                    settings.allowedPlayers=new ArrayList<>(settings.allowedPlayers.subList(0,20));
                settings.sensitivity=Math.max(20,Math.min(100,settings.sensitivity));
                settings.listening=false; // Explicit approval on every game launch.
            }
        }catch(Exception e){System.err.println("Nexus Hearing: configuração inválida.");}
    }
    static synchronized void setPlasmoReady(boolean ready){
        plasmoReady=ready;
        if(!ready)dropPending();
    }
    static synchronized boolean isListening(){load();return settings.listening&&plasmoReady;}
    static synchronized boolean isAllowed(String nick){
        load();
        return nick!=null&&settings.allowedPlayers.stream().anyMatch(s->s.equalsIgnoreCase(nick));
    }
    static synchronized JsonObject snapshot(){
        load();
        JsonObject out=new JsonObject();
        out.addProperty("ready",plasmoReady);
        out.addProperty("listening",isListening());
        out.addProperty("memory",settings.memory);
        out.addProperty("answer",settings.answer);
        out.addProperty("sensitivity",settings.sensitivity);
        out.add("allowedPlayers",JSON.toJsonTree(settings.allowedPlayers));
        out.addProperty("memoryCount",MEMORIES.count());
        out.addProperty("memoryFull",MEMORIES.full());
        out.addProperty("queued",queued.size());
        out.addProperty("inFlight",inFlight.size());
        out.addProperty("processed",processed);
        out.addProperty("dropped",dropped);
        out.addProperty("retries",retries);
        out.addProperty("lastSpeaker",lastSpeaker);
        out.addProperty("lastError",lastError);
        out.add("social",NexusSocial.snapshot());
        out.add("speaker",NexusSpeaker.snapshot());
        return out;
    }
    static synchronized String status(){
        load();
        return "Audição "+(isListening()?"CONTÍNUA":settings.listening?"AGUARDANDO PLASMO":"OFF")+
            " | Plasmo "+(plasmoReady?"OK":"não detectado")+
            " | Memórias "+MEMORIES.count()+(MEMORIES.full()?" (LIMITE DE DISCO)":"")+
            " | Fila "+queued.size()+" / "+inFlight.size()+" enviando"+
            " | Transcrições "+processed+" | Perdidas "+dropped;
    }
    static synchronized void configure(JsonObject data){
        load();
        for(String name:new String[]{"listening","memory","answer"})
            if(!data.has(name)||!data.get(name).isJsonPrimitive()||
                !data.get(name).getAsJsonPrimitive().isBoolean())
                throw new IllegalArgumentException("Configuração inválida: "+name);
        if(!data.has("allowedPlayers")||!data.get("allowedPlayers").isJsonArray())
            throw new IllegalArgumentException("Informe jogadores consentidos.");
        var allowed=new ArrayList<String>();
        if(data.getAsJsonArray("allowedPlayers").size()>20)
            throw new IllegalArgumentException("Máximo 20 jogadores.");
        for(var entry:data.getAsJsonArray("allowedPlayers")){
            String nick=entry.getAsString();
            if(!nick.matches("[A-Za-z0-9_]{3,16}"))throw new IllegalArgumentException("Nick inválido.");
            if(allowed.stream().noneMatch(s->s.equalsIgnoreCase(nick)))allowed.add(nick);
        }
        int sensitivity=data.has("sensitivity")?data.get("sensitivity").getAsInt():55;
        if(sensitivity<20||sensitivity>100)
            throw new IllegalArgumentException("Sensibilidade de 20 a 100.");
        boolean enable=data.get("listening").getAsBoolean();
        if(enable&&allowed.isEmpty())throw new IllegalArgumentException("Adicione um jogador consentido.");
        if(enable&&!plasmoReady)throw new IllegalStateException("Plasmo Voice indisponível.");
        boolean reset=!enable||sensitivity!=settings.sensitivity||
            !allowed.equals(settings.allowedPlayers);
        // Revocation removes raw transcripts and derived narrative memories.
        for(String previous:settings.allowedPlayers)
            if(allowed.stream().noneMatch(s->s.equalsIgnoreCase(previous))){
                MEMORIES.forget(previous);
                NexusSocial.forgetSpeaker(previous);
            }
        settings.listening=enable;settings.memory=data.get("memory").getAsBoolean();
        settings.answer=data.get("answer").getAsBoolean();
        settings.sensitivity=sensitivity;settings.allowedPlayers=allowed;
        if(data.has("social")&&data.get("social").isJsonObject())
            NexusSocial.configure(data.getAsJsonObject("social"));
        if(data.has("speaker")&&data.get("speaker").isJsonObject())
            NexusSpeaker.configure(data.getAsJsonObject("speaker"));
        if(reset)dropPending();
        saveSettings();
    }
    static synchronized void listen(boolean enabled){
        load();
        if(enabled&&settings.allowedPlayers.isEmpty())
            throw new IllegalStateException("Autorize um participante consentido com /nexusclient hearing allow NICK.");
        if(enabled&&!plasmoReady)throw new IllegalStateException("Plasmo Voice não detectado.");
        settings.listening=enabled;
        if(!enabled)dropPending();
        saveSettings();
    }
    static synchronized void remember(boolean enabled){load();settings.memory=enabled;saveSettings();}
    static synchronized void answer(boolean enabled){load();settings.answer=enabled;saveSettings();}
    static synchronized void allow(String name){
        load();
        if(name==null||!name.matches("[A-Za-z0-9_]{3,16}"))
            throw new IllegalArgumentException("Nick inválido.");
        if(!isAllowed(name)){
            if(settings.allowedPlayers.size()>=20)throw new IllegalStateException("Lista cheia (20 jogadores).");
            settings.allowedPlayers.add(name);saveSettings();
        }
    }
    static synchronized void deny(String name){
        load();
        settings.allowedPlayers.removeIf(s->s.equalsIgnoreCase(name));
        removeSpeaker(name);
        if(settings.allowedPlayers.isEmpty())settings.listening=false;
        saveSettings();
        MEMORIES.forget(name); // Withdrawing consent also purges stored transcripts.
        NexusSocial.forgetSpeaker(name);
    }
    static synchronized long forgetSpeaker(String name){
        if(name==null||!name.matches("[A-Za-z0-9_]{3,16}"))
            throw new IllegalArgumentException("Nick inválido.");
        removeSpeaker(name);
        long removed=MEMORIES.forget(name);
        NexusSocial.forgetSpeaker(name);
        return removed;
    }
    static synchronized void clearMemory(){MEMORIES.clear();NexusSocial.clear();}
    static synchronized List<String> recentMemories(){return MEMORIES.recent(6);}
    static synchronized void resetSession(){dropPending();}
    private static void dropPending(){
        queued.clear();inFlight.clear();streams.clear();queueBytes=0;dispatchAfter=0;
    }
    private static void removeSpeaker(String nick){
        if(nick==null)return;
        streams.remove(nick.toLowerCase(Locale.ROOT));
        for(Iterator<Clip> it=queued.iterator();it.hasNext();){
            Clip clip=it.next();
            if(clip.speaker.equalsIgnoreCase(nick)){queueBytes-=clip.pcm.length;it.remove();}
        }
        inFlight.values().removeIf(c->c.speaker.equalsIgnoreCase(nick));
    }
    private static void saveSettings(){
        try{
            Files.createDirectories(SETTINGS.getParent());
            Path temp=SETTINGS.resolveSibling(SETTINGS.getFileName()+".tmp");
            Files.writeString(temp,JSON.toJson(settings),StandardCharsets.UTF_8);
            try{Files.move(temp,SETTINGS,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException ex){Files.move(temp,SETTINGS,StandardCopyOption.REPLACE_EXISTING);}
        }catch(IOException ex){throw new IllegalStateException("Falha ao salvar audição.",ex);}
    }
    /** Called from Plasmo audio thread. Bounded RAM only; no network or disk I/O. */
    static synchronized void capture(String nick,short[] samples,int sampleRate,int channels){
        if(!isListening()||!isAllowed(nick)||samples==null)return;
        String key=nick.toLowerCase(Locale.ROOT);
        if(!streams.containsKey(key)&&streams.size()>=20)return;
        var segmenter=streams.computeIfAbsent(key,ignored->new NexusVoiceSegmenter(settings.sensitivity));
        for(var clip:segmenter.accept(samples,sampleRate,channels,System.currentTimeMillis()))
            enqueue(nick,clip);
    }
    private static void enqueue(String nick,NexusVoiceSegmenter.Segment segment){
        if(segment==null||!isListening()||!isAllowed(nick))return;
        Clip clip=new Clip(nextId++,nick,segment);
        while(!queued.isEmpty()&&(queued.size()>=MAX_QUEUED||
            queueBytes+clip.pcm.length>MAX_QUEUE_BYTES)){
            queueBytes-=queued.removeFirst().pcm.length;
            dropped++;
        }
        if(clip.pcm.length>MAX_QUEUE_BYTES){dropped++;return;}
        queued.addLast(clip);queueBytes+=clip.pcm.length;
    }
    /** Minecraft tick: flush utterances on silence and dispatch up to 3 parallel HTTP calls. */
    static synchronized void tick(){
        if(!isListening()){if(!streams.isEmpty()||!queued.isEmpty())dropPending();return;}
        long now=System.currentTimeMillis();
        for(var stream:streams.entrySet()){
            var tail=stream.getValue().flushIdle(now);
            if(tail!=null)enqueue(stream.getKey(),tail);
        }
        if(ClientConfig.token.isBlank()||now<dispatchAfter)return;
        while(inFlight.size()<PARALLEL&&!queued.isEmpty()){
            Clip clip=queued.removeFirst();queueBytes-=clip.pcm.length;
            if(!isAllowed(clip.speaker))continue;
            inFlight.put(clip.id,clip);
            ClientLink.transcribeHearing(clip.id,clip.speaker,wave(clip.pcm),
                new ArrayList<>(settings.allowedPlayers));
        }
    }
    private static byte[] wave(byte[] samples){
        ByteBuffer out=ByteBuffer.allocate(44+samples.length).order(ByteOrder.LITTLE_ENDIAN);
        out.put(new byte[]{'R','I','F','F'}).putInt(samples.length+36)
            .put(new byte[]{'W','A','V','E','f','m','t',' '}).putInt(16).putShort((short)1)
            .putShort((short)1).putInt(NexusVoiceSegmenter.RATE)
            .putInt(NexusVoiceSegmenter.RATE*2).putShort((short)2).putShort((short)16)
            .put(new byte[]{'d','a','t','a'}).putInt(samples.length).put(samples);
        return out.array();
    }
    /** Async result arrives on game thread. Matching by ID tolerates out-of-order responses. */
    static synchronized void onTranscription(long clipId,String transcript){
        Clip clip=inFlight.remove(clipId);
        if(clip==null||!isListening()||!isAllowed(clip.speaker))return;
        processed++;lastSpeaker=clip.speaker;lastError="";
        String text=String.valueOf(transcript).replaceAll("[\\p{Cntrl}]"," ").trim();
        if(text.isEmpty())return;
        text=text.substring(0,Math.min(400,text.length()));
        if(settings.memory){
            if(MEMORIES.record(clip.speaker,text,clip.capturedAt)){
                NexusSocial.heard(clip.speaker,text,clip.capturedAt);
            }else lastError=MEMORIES.full()?"Memórias atingiram 24 MiB.":"Falha ao salvar memória.";
        }
        if(settings.answer&&wake(text)){
            List<String> context=new ArrayList<>();
            if(settings.memory){
                for(String raw:MEMORIES.search(clip.speaker,text,8))
                    if(!NexusSocial.isObsolete(clip.speaker,raw))context.add(raw);
                context.addAll(NexusSocial.relevant(clip.speaker,text,4));
                context.addAll(NexusSocial.recentConversation(clip.capturedAt,5));
            }
            ClientLink.replyHearing(clip.speaker,text,context);
        }
    }
    private static boolean wake(String text){
        String normalized=java.text.Normalizer.normalize(text,java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).trim();
        return normalized.matches("^(?:(?:ei|ola)\\s+)?(?:nexus|nexo|nexos)(?:\\s+aeterna|\\s+aterna)?(?:$|[\\s,.!?:;].*)");
    }
    static synchronized void onReply(String speaker,String text){
        if(isListening()&&settings.answer&&isAllowed(speaker)&&text!=null&&!text.isBlank())
            ClientLink.speak(text,"mind");
    }
    static synchronized void onTranscriptionError(long clipId,String reason,boolean transientFailure){
        Clip clip=inFlight.remove(clipId);
        if(clip==null)return;
        lastError=String.valueOf(reason).substring(0,Math.min(120,String.valueOf(reason).length()));
        if(transientFailure&&isListening()&&isAllowed(clip.speaker)&&clip.attempts<2){
            clip.attempts++;retries++;dispatchAfter=System.currentTimeMillis()+2000L*clip.attempts;
            if(queued.size()<MAX_QUEUED&&queueBytes+clip.pcm.length<=MAX_QUEUE_BYTES){
                queued.addFirst(clip);queueBytes+=clip.pcm.length;
            }else dropped++;
        }else dropped++;
        if(isListening()&&System.currentTimeMillis()-noticeAt>30000){
            noticeAt=System.currentTimeMillis();
            NexusClientMod.notifyPlayer("Audição: "+lastError+" (fila "+queued.size()+").");
        }
    }
}
