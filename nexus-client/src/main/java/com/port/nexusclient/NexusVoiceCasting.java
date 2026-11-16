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
        // Empty string = OS default. A missing named device must NOT silently
        // select another microphone.
        String microphone="";
    }
    private static State state;
    private static volatile boolean captureAllowed,closed;
    private static volatile Thread worker;
    private static volatile String status="Desativado",lastCommand="",lastOutcome="";
    private static volatile long lastAt;
    private static volatile String lastTranscript="",lastDecision="Nenhuma frase analisada.",spellError="";
    private static volatile float lastConfidence;
    private static volatile int spellCount,inputLevel;
    private static volatile long audioFrames,finalPhrases,acceptedPhrases;
    private static long lastDispatch,scanAt;
    private static volatile long revision;
    private static volatile long microphoneRevision;
    private static volatile String capturingMicrophone="",lastOpenedMicrophone="";
    private static volatile List<NexusVoiceGrammar.Spell> owned=List.of();
    private static volatile Map<String,NexusVoiceGrammar.Spell> grammar=Map.of();
    // Updated on the client thread; read-only snapshot for the Vosk worker.
    private static volatile List<String> travelVoicePhrases=List.of();
    private NexusVoiceCasting(){}

    private static synchronized void load(){
        if(state!=null)return;
        state=new State();
        if(!Files.isRegularFile(SETTINGS))return;
        try{
            State saved=JSON.fromJson(Files.readString(SETTINGS,StandardCharsets.UTF_8),State.class);
            if(saved!=null&&saved.schema==1){
                saved.language="en".equals(saved.language)?"en":"pt";
                saved.microphone=NexusMicrophoneChoices.normalize(saved.microphone);
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
    static synchronized String selectedMicrophone(){
        load();
        return NexusMicrophoneChoices.normalize(state.microphone);
    }
    static String capturingMicrophone(){return capturingMicrophone;}
    static synchronized void setMicrophone(String value){
        load();
        String selected=NexusMicrophoneChoices.normalize(value);
        if(!NexusMicrophoneChoices.available(selected,NexusMicrophones.list()))
            throw new IllegalArgumentException("Microfone desconectado. Atualize a lista.");
        if(selected.equals(state.microphone))return;
        state.microphone=selected;
        save();
        microphoneRevision++;
        status="Microfone selecionado: "+NexusMicrophones.displayName(selected)+
            (state.enabled?". Troca em andamento.":" (VoiceCast desligado).");
    }
    static String status(){return status;}
    static String lastCommand(){return lastCommand;}
    static String lastOutcome(){return lastOutcome;}
    static long lastAt(){return lastAt;}
    static String lastTranscript(){return lastTranscript;}
    static String lastDecision(){return lastDecision;}
    static String spellError(){return spellError;}
    static int spellCount(){return spellCount;}
    static String diagnostics(){
        return "MIC "+(captureAllowed?"capturando":"suspenso/fechado")+
            " | Último dispositivo "+(lastOpenedMicrophone.isBlank()?"nenhum":lastOpenedMicrophone)+
            " | Modelo "+(NexusVoiceModels.installed(language())?"OK":"ausente")+
            " | Magias "+spellCount+" | Nível de áudio "+inputLevel+
            " | Áudio "+audioFrames+" blocos"+
            " | Frases "+finalPhrases+" | Comandos "+acceptedPhrases;
    }
    static String recognitionReport(){
        return "Ouvido: "+(lastTranscript.isBlank()?"(nada)":lastTranscript)+
            " | Confiança "+Math.round(lastConfidence*100)+"% | "+lastDecision;
    }
    static String checkPhrase(String phrase){
        var index=grammar;
        var command=NexusVoiceGrammar.recognize(phrase,index);
        if(command==null)return "Não reconhecido. Magias equipadas: "+spellCount+
            ". Use 'Nexus, execute [feitiço]' ou 'Nexus, cancelar'.";
        return command.cancel()?"Comando de cancelamento válido (nenhuma magia executada).":
            "Feitiço encontrado: "+command.spellId()+" no slot "+
            command.slot()+" (nenhuma magia executada).";
    }

    static synchronized void setEnabled(boolean on){
        load();
        if(closed)return;
        if(on){
            if(!ModList.get().isLoaded("irons_spellbooks") &&
               !ModList.get().isLoaded("waystones")){
                status="Ative Iron's Spells ou Waystones para usar comandos de voz.";return;
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
            (ModList.get().isLoaded("irons_spellbooks") ||
                ModList.get().isLoaded("waystones"))&&
            NexusVoiceModels.installed(state.language);
        captureAllowed=active;
        if(!active)return;
        long now=System.currentTimeMillis();
        if(now-scanAt>=900){
            scanAt=now;
            List<String> destinations=NexusArcaneTravel.voiceVocabulary();
            if(!destinations.equals(travelVoicePhrases)){
                travelVoicePhrases=destinations;
                revision++;
            }
            List<NexusVoiceGrammar.Spell> recent=ModList.get().isLoaded("irons_spellbooks")
                ? equippedSpells(mc.player):List.of();
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
    static List<NexusVoiceGrammar.Spell> equippedSpells(Player player){
        List<NexusVoiceGrammar.Spell> result=new ArrayList<>();
        try{
            Class<?> manager=Class.forName("io.redspace.ironsspellbooks.api.magic.SpellSelectionManager");
            Class<?> spellData=Class.forName("io.redspace.ironsspellbooks.api.spells.SpellData");
            Class<?> spellClass=Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell");
            Object instance=manager.getConstructor(Player.class).newInstance(player);
            result.addAll(NexusSpellAccess.listEquipped(instance,spellData,spellClass));
            spellError="";
        }catch(Throwable exception){
            // This is a separate diagnostic: never overwrite an audio failure.
            spellError=exception.getClass().getSimpleName()+": "+exception.getMessage();
        }
        spellCount=result.size();
        return List.copyOf(result);
    }

    private static void captureLoop(){
        Model model=null;Recognizer recognizer=null;
        long device=0L,loadedRevision=-1,openedMicrophoneRevision=-1;
        ByteBuffer pcm=BufferUtils.createByteBuffer(SAMPLES_PER_CHUNK*2).order(ByteOrder.LITTLE_ENDIAN);
        byte[] samples=new byte[SAMPLES_PER_CHUNK*2];
        int spokenFrames=0;
        try{
            model=new Model(NexusVoiceModels.model(language()).toString());
            while(!closed&&state!=null&&state.enabled&&!Thread.currentThread().isInterrupted()){
                if(!captureAllowed||openedMicrophoneRevision!=microphoneRevision){
                    // Close the old capture device before opening the new one.
                    // Both operations stay on this same microphone thread.
                    if(device!=0L){
                        ALC11.alcCaptureStop(device);
                        ALC11.alcCaptureCloseDevice(device);
                        device=0L;
                    }
                    capturingMicrophone="";
                    if(recognizer!=null){recognizer.close();recognizer=null;loadedRevision=-1;}
                    spokenFrames=0;
                    openedMicrophoneRevision=microphoneRevision;
                    if(!captureAllowed){Thread.sleep(120);continue;}
                }
                long currentRevision=revision;
                if(recognizer==null||loadedRevision!=currentRevision){
                    if(recognizer!=null)recognizer.close();
                    Map<String,NexusVoiceGrammar.Spell> snapshot=grammar;
                    JsonArray phrases=new JsonArray();
                    List<String> terms=new ArrayList<>(NexusVoiceGrammar.vocabulary(snapshot));
                    terms.remove("[unk]");
                    terms.addAll(travelVoicePhrases);
                    terms.add("[unk]");
                    for(String phrase:terms)phrases.add(phrase);
                    recognizer=new Recognizer(model,SAMPLE_RATE,phrases.toString());
                    recognizer.setWords(true);
                    loadedRevision=currentRevision;
                    spokenFrames=0;
                }
                if(device==0L){
                    String requested=selectedMicrophone();
                    // Never fall back to default if the selected device vanishes.
                    if(!NexusMicrophones.isPresent(requested)){
                        status="Microfone selecionado desconectado: "+
                            requested+". Escolha outro em F8 > Combate > Microfone.";
                        Thread.sleep(1200);
                        continue;
                    }
                    device=ALC11.alcCaptureOpenDevice(requested.isEmpty()?null:requested,
                        SAMPLE_RATE,AL10.AL_FORMAT_MONO16,SAMPLE_RATE);
                    if(device==0L){
                        status="Não consegui abrir o microfone: "+
                            NexusMicrophones.displayName(requested)+
                            ". Verifique permissões do Windows ou se ele está em uso.";
                        Thread.sleep(1600);
                        continue;
                    }
                    capturingMicrophone=NexusMicrophones.displayName(requested);
                    lastOpenedMicrophone=capturingMicrophone;
                    ALC11.alcCaptureStart(device);
                    status=grammar.isEmpty() && travelVoicePhrases.isEmpty()?
                        "Microfone: "+capturingMicrophone+". Equipe feitiços ou ative destinos.":
                        "Ouvindo em "+capturingMicrophone+". Diga 'Nexus, execute [feitiço]'.";
                }
                if(ALC10.alcGetInteger(device,ALC11.ALC_CAPTURE_SAMPLES)<SAMPLES_PER_CHUNK){
                    Thread.sleep(20);continue;
                }
                pcm.clear();
                ALC11.alcCaptureSamples(device,pcm,SAMPLES_PER_CHUNK);
                pcm.position(0);pcm.get(samples);
                audioFrames++;
                double meanSquares=0;
                for(int i=0;i<samples.length;i+=2){
                    int value=(short)((samples[i]&0xff)|((samples[i+1]&0xff)<<8));
                    meanSquares+=(double)value*value;
                }
                inputLevel=(int)Math.min(32767,Math.sqrt(meanSquares/SAMPLES_PER_CHUNK));
                if(inputLevel>240)spokenFrames++;
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
            capturingMicrophone="";
            if(recognizer!=null)try{recognizer.close();}catch(Throwable ignored){}
            if(model!=null)try{model.close();}catch(Throwable ignored){}
        }
    }

    private static void onFinal(String json,int spokenFrames){
        if(!captureAllowed)return;
        finalPhrases++;
        try{
            JsonObject result=JsonParser.parseString(json).getAsJsonObject();
            String phrase=result.has("text")?result.get("text").getAsString():"";
            if(!phrase.isBlank())lastTranscript=phrase;
            float confidence=confidence(result);
            lastConfidence=confidence;
            if(spokenFrames<MIN_VOICE_FRAMES){
                lastDecision="Frase muito curta ou microfone com volume baixo.";
                return;
            }
            if(phrase.isBlank()){
                lastDecision="O microfone capturou áudio, mas o Vosk não encontrou palavras.";
                return;
            }
            Map<String,NexusVoiceGrammar.Spell> lookup=grammar;
            NexusVoiceGrammar.Command command=NexusVoiceGrammar.recognize(phrase,lookup);
            NexusTravelPolicy.VoiceIntent travel=NexusTravelPolicy.interpretVoice(
                phrase,travelVoicePhrases);
            if(command==null&&travel==null){
                lastDecision="Frase ouvida, porém fora dos comandos equipados do Nexus.";
                return;
            }
            if(confidence<MIN_CONFIDENCE){
                lastDecision="Comando identificado, porém confiança insuficiente ("+
                    Math.round(confidence*100)+"%).";
                return;
            }
            acceptedPhrases++;
            if(travel!=null){
                lastDecision="Comando de viagem reconhecido. Verificando confirmação local...";
                Minecraft.getInstance().execute(()->{
                    if(!enabled()||!captureAllowed||Minecraft.getInstance().screen!=null)return;
                    long now=System.currentTimeMillis();
                    if(now-lastDispatch<COMMAND_DELAY_MS)return;
                    lastDispatch=now;lastAt=now;lastCommand=phrase;
                    NexusArcaneTravel.handleVoice(travel);
                    lastOutcome=NexusArcaneTravel.status();
                    lastDecision=lastOutcome;
                });
                return;
            }
            lastDecision="Comando aceito. Solicitando conjuração ao jogo...";
            Minecraft.getInstance().execute(()->dispatch(command));
        }catch(Exception error){
            lastDecision="Erro ao interpretar resultado local: "+error.getClass().getSimpleName();
        }
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
        if(now-lastDispatch<COMMAND_DELAY_MS){
            lastDecision="Comando descartado pelo intervalo mínimo de 1,5s.";
            return;
        }
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
                lastDecision=lastOutcome;
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
            lastDecision=lastOutcome;
        }catch(Throwable error){
            lastOutcome="Integração com Iron's Spells indisponível: "+
                error.getClass().getSimpleName();
            lastDecision=lastOutcome;
        }
    }
    static boolean clientIsCasting(){
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
