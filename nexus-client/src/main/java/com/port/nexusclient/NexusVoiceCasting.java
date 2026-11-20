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
        String language="auto";
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
    // Only macros explicitly enabled with a registered activation phrase.
    private static volatile Map<String,String> macroVoicePhrases=Map.of();
    private NexusVoiceCasting(){}

    private static synchronized void load(){
        if(state!=null)return;
        state=new State();
        if(!Files.isRegularFile(SETTINGS))return;
        try{
            State saved=JSON.fromJson(Files.readString(SETTINGS,StandardCharsets.UTF_8),State.class);
            if(saved!=null&&saved.schema==1){
                saved.language="en".equals(saved.language)?"en":
                    "pt".equals(saved.language)?"pt":"auto";
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
            " | Modelo "+(NexusVoiceModels.ready(language())
                ?String.join("+",NexusVoiceModels.activeLanguages(language())).toUpperCase()
                :"ausente")+
            " | Magias "+spellCount+" | Nível de áudio "+inputLevel+
            " | Áudio "+audioFrames+" blocos"+
            " | Frases "+finalPhrases+" | Comandos "+acceptedPhrases;
    }
    static String recognitionReport(){
        return "Ouvido: "+(lastTranscript.isBlank()?"(nada)":lastTranscript)+
            " | Confiança "+Math.round(lastConfidence*100)+"% | "+lastDecision;
    }
    static String checkPhrase(String phrase){
        var command=NexusVoiceGrammar.recognize(phrase,grammar);
        if(command!=null)return command.cancel()?
            "Comando de cancelamento válido (sem executar).":
            "Feitiço encontrado: "+command.spellId()+" no slot "+
                command.slot()+" (sem executar).";
        var travel=NexusTravelPolicy.interpretVoice(phrase,travelVoicePhrases);
        if(travel!=null)return "Comando de Waystones identificado (sem executar).";
        String macro=NexusMacroPhrasePolicy.interpret(phrase,macroVoicePhrases);
        if(macro!=null)return "Frase reconhecida para macro "+macro+
            " (simulação: nenhuma magia executada).";
        String awakening=NexusAwakeningPolicy.voiceCommand(phrase);
        if(awakening!=null)return "Nexus 4.0: comando contextual "+awakening+
            " reconhecido (sem executar).";
        return "Frase não reconhecida. Diga Nexus seguido da frase cadastrada.";
    }

    static synchronized void setEnabled(boolean on){
        load();
        if(closed)return;
        if(on){
            if(!ModList.get().isLoaded("irons_spellbooks") &&
               !ModList.get().isLoaded("waystones")){
                status="Ative Iron's Spells ou Waystones para usar comandos de voz.";return;
            }
            if(!NexusVoiceModels.ready(state.language)){
                status="Nenhum modelo Vosk compatível instalado. Use "
                    +"/nexusclient voicecast model install.";
                return;
            }
        }
        state.enabled=on;save();
        status=on?"Voz ativada. Diga 'Nexus, Divine Smite' ou o nome de outra magia equipada."
            :"Conjuração por voz desativada.";
        if(!on){
            captureAllowed=false;
            Thread old=worker;
            if(old!=null)old.interrupt();
        }
    }
    static synchronized void setLanguage(String lang){
        load();
        if(!"pt".equals(lang)&&!"en".equals(lang)&&!"auto".equals(lang))
            throw new IllegalArgumentException("Escolha pt, en ou auto.");
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
            NexusVoiceModels.ready(state.language);
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
            Map<String,String> currentMacros=NexusCombatMacro.voicePhrases();
            if(!currentMacros.equals(macroVoicePhrases)){
                macroVoicePhrases=currentMacros;
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

    private record AudioResult(String language,String json) {}

    /**
     * One microphone, one or two completely local Vosk recognizers.
     * AUTO uses PT and EN when installed; neither model sends audio over the network.
     */
    private static void captureLoop(){
        Map<String,Model> models=new LinkedHashMap<>();
        Map<String,Recognizer> recognizers=new LinkedHashMap<>();
        long device=0L,loadedRevision=-1,openedMicrophoneRevision=-1;
        ByteBuffer pcm=BufferUtils.createByteBuffer(SAMPLES_PER_CHUNK*2).order(ByteOrder.LITTLE_ENDIAN);
        byte[] samples=new byte[SAMPLES_PER_CHUNK*2];
        int spokenFrames=0;
        try{
            List<String> available=NexusVoiceModels.activeLanguages(language());
            if(available.isEmpty())throw new IllegalStateException("Nenhum modelo Vosk instalado.");
            for(String lang:available)models.put(lang,
                new Model(NexusVoiceModels.model(lang).toString()));

            while(!closed&&state!=null&&state.enabled&&!Thread.currentThread().isInterrupted()){
                if(!captureAllowed||openedMicrophoneRevision!=microphoneRevision){
                    if(device!=0L){
                        ALC11.alcCaptureStop(device);
                        ALC11.alcCaptureCloseDevice(device);
                        device=0L;
                    }
                    capturingMicrophone="";
                    for(Recognizer existing:recognizers.values())existing.close();
                    recognizers.clear();loadedRevision=-1;
                    spokenFrames=0;
                    openedMicrophoneRevision=microphoneRevision;
                    if(!captureAllowed){Thread.sleep(120);continue;}
                }
                long currentRevision=revision;
                if(recognizers.isEmpty()||loadedRevision!=currentRevision){
                    for(Recognizer existing:recognizers.values())existing.close();
                    recognizers.clear();
                    JsonArray phrases=new JsonArray();
                    List<String> terms=new ArrayList<>(NexusVoiceGrammar.vocabulary(grammar));
                    terms.remove("[unk]");
                    terms.addAll(travelVoicePhrases);
                    terms.addAll(macroVoicePhrases.keySet());
                    terms.addAll(NexusAwakeningPolicy.voiceVocabulary());
                    terms.add("[unk]");
                    for(String phrase:new java.util.LinkedHashSet<>(terms))phrases.add(phrase);
                    for(var entry:models.entrySet()){
                        Recognizer recognizer=new Recognizer(entry.getValue(),SAMPLE_RATE,
                            phrases.toString());
                        recognizer.setWords(true);
                        recognizers.put(entry.getKey(),recognizer);
                    }
                    loadedRevision=currentRevision;spokenFrames=0;
                }
                if(device==0L){
                    String requested=selectedMicrophone();
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
                    status=grammar.isEmpty()&&travelVoicePhrases.isEmpty()
                        ?"Microfone: "+capturingMicrophone+". Equipe magias ou cadastre destinos."
                        :"Ouvindo em "+capturingMicrophone+" ["
                            +String.join("+",models.keySet()).toUpperCase()
                            +"]. Diga 'Nexus, Divine Smite'.";
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

                // If either language detects an utterance boundary, flush both.
                // Select one intent once, instead of casting twice.
                Map<String,String> completed=new LinkedHashMap<>();
                for(var entry:recognizers.entrySet()){
                    if(entry.getValue().acceptWaveForm(samples,samples.length))
                        completed.put(entry.getKey(),entry.getValue().getResult());
                }
                if(!completed.isEmpty()){
                    for(var entry:recognizers.entrySet()){
                        if(!completed.containsKey(entry.getKey()))
                            completed.put(entry.getKey(),entry.getValue().getFinalResult());
                    }
                    List<AudioResult> results=new ArrayList<>();
                    for(var entry:completed.entrySet())
                        results.add(new AudioResult(entry.getKey(),entry.getValue()));
                    onFinalCandidates(results,spokenFrames);
                    spokenFrames=0;
                    // getFinalResult ends that segment; recreate recognizers in dual mode.
                    if(models.size()>1){
                        for(Recognizer existing:recognizers.values())existing.close();
                        recognizers.clear();
                        loadedRevision=-1;
                    }
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
            for(Recognizer existing:recognizers.values())
                try{existing.close();}catch(Throwable ignored){}
            for(Model model:models.values())
                try{model.close();}catch(Throwable ignored){}
        }
    }

    /**
     * Deterministic arbitration: accept only equipped spells or registered
     * travel intents, confidence checked, with cross-language conflicts rejected.
     */
    private static void onFinalCandidates(List<AudioResult> recognized,int spokenFrames){
        if(!captureAllowed)return;
        finalPhrases++;
        try{
            if(spokenFrames<MIN_VOICE_FRAMES){
                lastDecision="Frase muito curta ou microfone com volume baixo.";
                return;
            }
            List<NexusVoiceCandidatePolicy.Candidate> eligible=new ArrayList<>();
            String bestTranscript="";
            float bestScore=-1;
            for(AudioResult audio:recognized){
                if(audio.json()==null)continue;
                JsonObject result=JsonParser.parseString(audio.json()).getAsJsonObject();
                String phrase=result.has("text")?result.get("text").getAsString():"";
                float confidence=confidence(result);
                if(!phrase.isBlank()&&confidence>bestScore){
                    bestTranscript=phrase;bestScore=confidence;
                }
                NexusVoiceGrammar.Command spell=NexusVoiceGrammar.recognize(phrase,grammar);
                NexusTravelPolicy.VoiceIntent travel=NexusTravelPolicy.interpretVoice(
                    phrase,travelVoicePhrases);
                String macro=NexusMacroPhrasePolicy.interpret(phrase,macroVoicePhrases);
                String awakening=NexusAwakeningPolicy.voiceCommand(phrase);
                if(spell!=null){
                    String action=spell.cancel()?"cancel":
                        "spell:"+spell.spellId()+":"+spell.slot();
                    eligible.add(new NexusVoiceCandidatePolicy.Candidate(
                        audio.language(),phrase,action,confidence));
                }else if(travel!=null){
                    eligible.add(new NexusVoiceCandidatePolicy.Candidate(
                        audio.language(),phrase,
                        "travel:"+travel.action()+":"+travel.destination(),confidence));
                }else if(macro!=null){
                    eligible.add(new NexusVoiceCandidatePolicy.Candidate(
                        audio.language(),phrase,"macro:"+macro,confidence));
                }else if(awakening!=null){
                    eligible.add(new NexusVoiceCandidatePolicy.Candidate(
                        audio.language(),phrase,"awakening:"+awakening,confidence));
                }
            }
            var selected=NexusVoiceCandidatePolicy.choose(eligible,MIN_CONFIDENCE);
            if(!selected.accepted()){
                lastTranscript=bestTranscript;
                lastConfidence=Math.max(0,bestScore);
                lastDecision=selected.reason();
                return;
            }
            String phrase=selected.winner().spoken();
            lastTranscript=phrase;
            lastConfidence=selected.winner().confidence();
            NexusVoiceGrammar.Command spell=NexusVoiceGrammar.recognize(phrase,grammar);
            NexusTravelPolicy.VoiceIntent travel=NexusTravelPolicy.interpretVoice(
                phrase,travelVoicePhrases);
            String macro=NexusMacroPhrasePolicy.interpret(phrase,macroVoicePhrases);
            String awakening=NexusAwakeningPolicy.voiceCommand(phrase);
            acceptedPhrases++;
            if(spell==null&&travel==null&&macro==null&&awakening!=null){
                lastDecision=selected.reason()+" Consultando sensores locais...";
                Minecraft.getInstance().execute(()->{
                    if(!enabled()||!captureAllowed||Minecraft.getInstance().screen!=null)return;
                    long now=System.currentTimeMillis();
                    if(now-lastDispatch<COMMAND_DELAY_MS)return;
                    lastDispatch=now;lastAt=now;lastCommand=phrase;
                    if("report".equals(awakening))NexusAwakening.report(true);
                    else if("memories".equals(awakening))NexusAwakening.recall("",true);
                    lastOutcome="Nexus 4.0: consulta contextual "+awakening+" concluída.";
                    lastDecision=lastOutcome;
                });
            }else if(spell==null&&travel==null&&macro!=null){
                lastDecision=selected.reason()+" Verificando macro personalizada...";
                Minecraft.getInstance().execute(()->{
                    if(!enabled()||!captureAllowed||Minecraft.getInstance().screen!=null)return;
                    long now=System.currentTimeMillis();
                    if(now-lastDispatch<COMMAND_DELAY_MS)return;
                    lastDispatch=now;lastAt=now;lastCommand=phrase;
                    lastOutcome=NexusCombatMacro.activateByVoice(macro,phrase);
                    lastDecision=lastOutcome;
                });
            }else if(travel!=null){
                lastDecision=selected.reason()+" Verificando viagem...";
                Minecraft.getInstance().execute(()->{
                    if(!enabled()||!captureAllowed||Minecraft.getInstance().screen!=null)return;
                    long now=System.currentTimeMillis();
                    if(now-lastDispatch<COMMAND_DELAY_MS)return;
                    lastDispatch=now;lastAt=now;lastCommand=phrase;
                    NexusArcaneTravel.handleVoice(travel);
                    lastOutcome=NexusArcaneTravel.status();
                    lastDecision=lastOutcome;
                });
            }else if(spell!=null){
                lastDecision=selected.reason()+" Solicitando conjuração...";
                Minecraft.getInstance().execute(()->dispatch(spell));
            }
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
