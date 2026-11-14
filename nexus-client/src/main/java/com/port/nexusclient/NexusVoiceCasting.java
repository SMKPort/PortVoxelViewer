package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC11;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Offline, opt-in local microphone recognition. The only network packets
 * generated are the regular authenticated Iron's Spells QuickCast/CancelCast
 * packets. Audio is neither saved nor sent to Render, Gemini or Plasmo.
 */
final class NexusVoiceCasting {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path SETTINGS=FMLPaths.CONFIGDIR.get().resolve("nexus-voice-settings.json");
    private static final int SAMPLE_RATE=16000,SAMPLES_PER_CHUNK=1600;
    private static final int MIN_VOICE_FRAMES=4; // >=400 ms of actual speech
    private static final float MIN_CONFIDENCE=.58f;
    private static final long COMMAND_DELAY_MS=1500;
    private static final class State {
        int schema=1;
        boolean enabled=false;
        String language="pt";
    }
    private static State state;
    private static volatile boolean captureAllowed,closed;
    private static volatile Thread worker;
    private static volatile String status="Desativado",lastCommand="",lastOutcome="";
    private static volatile long lastAt;
    private static long lastDispatch,scanAt,revision;
    private static volatile List<NexusVoiceGrammar.Spell> owned=List.of();
    private static volatile Map<String,NexusVoiceGrammar.Spell> grammar=Map.of();
    private NexusVoiceCasting(){}

    private static synchronized void load(){
        if(state!=null)return;
        state=new State();
        if(!Files.isRegularFile(SETTINGS))return;
        try{
            State saved=JSON.fromJson(Files.readString(SETTINGS,StandardCharsets.UTF_8),State.class);
            if(saved!=null&&saved.schema==1){
                saved.language="en".equals(saved.language)?"en":"pt";
                // Local microphone is session-scoped: never reopen it silently
                // after restarting the game, regardless of the last saved flag.
                saved.enabled=false;
                state=saved;
            }
        }catch(Exception ex){status="Configuração de voz inválida; usando padrão desligado.";}
    }
    private static synchronized void save(){
        try{
            Files.createDirectories(SETTINGS.getParent());
            Files.writeString(SETTINGS,JSON.toJson(state),StandardCharsets.UTF_8);
        }catch(Exception ex){status="Não foi possível persistir as permissões do microfone.";}
    }

    static synchronized boolean enabled(){load();return state.enabled;}
    static synchronized String language(){load();return state.language;}
    static String status(){return status;}
    static String lastCommand(){return lastCommand;}
    static String lastOutcome(){return lastOutcome;}
    static long lastAt(){return lastAt;}

    static synchronized void setEnabled(boolean on){
        load();
        if(closed)return;
        if(on){
            if(!ModList.get().isLoaded("irons_spellbooks")){
                status="Iron's Spells não instalado.";return;
            }
            if(!NexusVoiceModels.installed(state.language)){
                status="Instale o modelo "+state.language.toUpperCase()+
                    " com /nexusclient voicecast model install.";
                return;
            }
        }
        state.enabled=on;save();
        status=on?"Voz ativada. Fale 'Nexus, execute Sunbeam'.":"Conjuração por voz desativada.";
        if(!on){
            captureAllowed=false;
            Thread old=worker;
            if(old!=null)old.interrupt();
        }
    }
    static synchronized void setLanguage(String lang){
        load();
        if(!"pt".equals(lang)&&!"en".equals(lang))
            throw new IllegalArgumentException("Escolha pt ou en.");
        if(state.language.equals(lang))return;
        state.enabled=false;
        captureAllowed=false;
        Thread old=worker;
        if(old!=null)old.interrupt();
        state.language=lang;save();
        status="Idioma "+lang.toUpperCase()+" selecionado. Reative a conjuração.";
    }

    /** Called from the client thread. Never resolve player data on the microphone thread. */
    static void tick(){
        if(closed)return;
        load();
        Minecraft mc=Minecraft.getInstance();
        boolean active=state.enabled&&mc.player!=null&&mc.level!=null&&
            mc.screen==null&&mc.isWindowActive()&&
            ModList.get().isLoaded("irons_spellbooks")&&
            NexusVoiceModels.installed(state.language);
        captureAllowed=active;
        if(!active)return;
        long now=System.currentTimeMillis();
        if(now-scanAt>=900){
            scanAt=now;
            List<NexusVoiceGrammar.Spell> recent=equippedSpells(mc.player);
            if(!recent.equals(owned)){
                owned=recent;
                grammar=NexusVoiceGrammar.phrases(recent);
                revision++;
            }
        }
        Thread old=worker;
        if(old==null||!old.isAlive()){
            Thread fresh=new Thread(NexusVoiceCasting::captureLoop,"Nexus-Offline-Voice");
            fresh.setDaemon(true);
            worker=fresh;
            fresh.start();
        }
    }

    /** Spell indices are the exact list positions used by Iron's QuickCastPacket. */
    private static List<NexusVoiceGrammar.Spell> equippedSpells(Player player){
        List<NexusVoiceGrammar.Spell> result=new ArrayList<>();
        try{
            Class<?> manager=Class.forName("io.redspace.ironsspellbooks.api.magic.SpellSelectionManager");
            Class<?> spellData=Class.forName("io.redspace.ironsspellbooks.api.spells.SpellData");
            Class<?> spellClass=Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell");
            Object instance=manager.getConstructor(Player.class).newInstance(player);
            int count=Math.max(0,Math.min(128,(int)manager.getMethod("getSpellCount").invoke(instance)));
            Method dataAt=manager.getMethod("getSpellData",int.class);
            Method getSpell=spellData.getMethod("getSpell");
            Method spellId=spellClass.getMethod("getSpellId");
            for(int i=0;i<count;i++){
                Object data=dataAt.invoke(instance,i);
                if(data==null)continue;
                Object spell=getSpell.invoke(data);
                if(spell==null)continue;
                String id=String.valueOf(spellId.invoke(spell));
                if(id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
                    result.add(new NexusVoiceGrammar.Spell(id,i));
            }
        }catch(Throwable exception){
            // Fails closed. Cannot voice-cast with an unknown spell selection API.
            status="Não consegui ler os feitiços equipados do Iron's Spells.";
        }
        return List.copyOf(result);
    }

    private static void captureLoop(){
        Model model=null;Recognizer recognizer=null;
        long device=0L,loadedRevision=-1;
        ByteBuffer pcm=BufferUtils.createByteBuffer(SAMPLES_PER_CHUNK*2).order(ByteOrder.LITTLE_ENDIAN);
        byte[] samples=new byte[SAMPLES_PER_CHUNK*2];
        int spokenFrames=0;
        try{
            model=new Model(NexusVoiceModels.model(language()).toString());
            while(!closed&&state!=null&&state.enabled&&!Thread.currentThread().isInterrupted()){
                if(!captureAllowed){
                    if(device!=0L){ALC11.alcCaptureStop(device);ALC11.alcCaptureCloseDevice(device);device=0L;}
                    if(recognizer!=null){recognizer.close();recognizer=null;loadedRevision=-1;}
                    spokenFrames=0;
                    Thread.sleep(120);continue;
                }
                long currentRevision=revision;
                if(recognizer==null||loadedRevision!=currentRevision){
                    if(recognizer!=null)recognizer.close();
                    Map<String,NexusVoiceGrammar.Spell> snapshot=grammar;
                    JsonArray phrases=new JsonArray();
                    for(String phrase:NexusVoiceGrammar.vocabulary(snapshot))phrases.add(phrase);
                    recognizer=new Recognizer(model,SAMPLE_RATE,phrases.toString());
                    recognizer.setWords(true);
                    loadedRevision=currentRevision;
                    spokenFrames=0;
                }
                if(device==0L){
                    device=ALC11.alcCaptureOpenDevice((String)null,SAMPLE_RATE,
                        AL10.AL_FORMAT_MONO16,SAMPLE_RATE);
                    if(device==0L){
                        status="Microfone indisponível. Verifique permissões ou outros aplicativos.";
                        Thread.sleep(1600);continue;
                    }
                    ALC11.alcCaptureStart(device);
                    status=grammar.isEmpty()?
                        "Microfone local pronto. Equipe feitiços no Iron's Spells.":
                        "Ouvindo localmente: diga 'Nexus, execute [feitiço]'.";
                }
                if(ALC10.alcGetInteger(device,ALC11.ALC_CAPTURE_SAMPLES)<SAMPLES_PER_CHUNK){
                    Thread.sleep(20);continue;
                }
                pcm.clear();
                ALC11.alcCaptureSamples(device,pcm,SAMPLES_PER_CHUNK);
                pcm.position(0);pcm.get(samples);
                double meanSquares=0;
                for(int i=0;i<samples.length;i+=2){
                    int value=(short)((samples[i]&0xff)|((samples[i+1]&0xff)<<8));
                    meanSquares+=(double)value*value;
                }
                if(Math.sqrt(meanSquares/SAMPLES_PER_CHUNK)>240)spokenFrames++;
                if(recognizer.acceptWaveForm(samples,samples.length)){
                    String result=recognizer.getResult();
                    int frames=spokenFrames;spokenFrames=0;
                    onFinal(result,frames);
                }
            }
        }catch(InterruptedException ignored){
            Thread.currentThread().interrupt();
        }catch(Throwable error){
            String message=String.valueOf(error.getMessage());
            status="Falha na voz offline: "+message.substring(0,Math.min(message.length(),85));
        }finally{
            if(device!=0L){
                try{ALC11.alcCaptureStop(device);}catch(Throwable ignored){}
                try{ALC11.alcCaptureCloseDevice(device);}catch(Throwable ignored){}
            }
            if(recognizer!=null)try{recognizer.close();}catch(Throwable ignored){}
            if(model!=null)try{model.close();}catch(Throwable ignored){}
        }
    }

    private static void onFinal(String json,int spokenFrames){
        if(spokenFrames<MIN_VOICE_FRAMES||!captureAllowed)return;
        try{
            JsonObject result=JsonParser.parseString(json).getAsJsonObject();
            String phrase=result.has("text")?result.get("text").getAsString():"";
            Map<String,NexusVoiceGrammar.Spell> lookup=grammar;
            NexusVoiceGrammar.Command command=NexusVoiceGrammar.recognize(phrase,lookup);
            if(command==null)return;
            float confidence=confidence(result);
            if(confidence<MIN_CONFIDENCE)return;
            Minecraft.getInstance().execute(()->dispatch(command));
        }catch(Exception ignored){}
    }
    private static float confidence(JsonObject json){
        if(!json.has("result")||!json.get("result").isJsonArray())return 0f;
        JsonArray words=json.getAsJsonArray("result");
        if(words.size()==0)return 0f;
        float sum=0;int count=0;
        for(var el:words){
            if(!el.isJsonObject())continue;
            JsonObject word=el.getAsJsonObject();
            if(!word.has("conf"))continue;
            sum+=word.get("conf").getAsFloat();count++;
        }
        return count==0?0:sum/count;
    }

    /** Runs only on the Minecraft client thread. All authority stays server-side. */
    private static void dispatch(NexusVoiceGrammar.Command command){
        Minecraft mc=Minecraft.getInstance();
        if(!enabled()||mc.player==null||mc.level==null||mc.screen!=null)return;
        long now=System.currentTimeMillis();
        if(now-lastDispatch<COMMAND_DELAY_MS)return;
        lastDispatch=now;
        lastAt=now;lastCommand=command.phrase();
        try{
            if(command.cancel()){
                if(!clientIsCasting()){
                    lastOutcome="Nenhuma conjuração ativa para cancelar.";
                    return;
                }
                Class<?> type=Class.forName(
                    "io.redspace.ironsspellbooks.network.casting.CancelCastPacket");
                Object packet=type.getConstructor(boolean.class).newInstance(true);
                PacketDistributor.sendToServer((CustomPacketPayload)packet);
                lastOutcome="Cancelamento solicitado ao Iron's Spells.";
                return;
            }
            if(clientIsCasting()){
                lastOutcome="Conjuração em andamento. Diga 'Nexus, cancelar'.";
                return;
            }
            // Rebuild once immediately to defeat a stale voice snapshot after swapping books.
            List<NexusVoiceGrammar.Spell> actual=equippedSpells(mc.player);
            boolean ownedNow=actual.stream().anyMatch(s->s.slot()==command.slot()&&
                s.id().equals(command.spellId()));
            if(!ownedNow){
                lastOutcome="Feitiço não está mais equipado. Comando descartado.";
                return;
            }
            Class<?> type=Class.forName(
                "io.redspace.ironsspellbooks.network.casting.QuickCastPacket");
            Object packet=type.getConstructor(int.class).newInstance(command.slot());
            PacketDistributor.sendToServer((CustomPacketPayload)packet);
            lastOutcome="Conjuração solicitada: "+command.spellId().split(":")[1]+
                " (mana e cooldown validados pelo servidor).";
        }catch(Throwable error){
            lastOutcome="Integração com Iron's Spells indisponível nesta versão.";
        }
    }
    private static boolean clientIsCasting(){
        try{
            Class<?> data=Class.forName("io.redspace.ironsspellbooks.player.ClientMagicData");
            return Boolean.TRUE.equals(data.getMethod("isCasting").invoke(null));
        }catch(Throwable ignored){return false;}
    }

    static void shutdown(){
        closed=true;captureAllowed=false;
        Thread active=worker;
        if(active!=null){
            active.interrupt();
            try{active.join(1600);}catch(InterruptedException e){Thread.currentThread().interrupt();}
        }
    }
}
