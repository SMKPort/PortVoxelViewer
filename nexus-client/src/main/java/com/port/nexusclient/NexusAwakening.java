package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Nexus 4.0: entirely local, grounded situational briefing and opt-in safety.
 * No autonomous network/chat dispatch, no fabricated hostile targets, no
 * hidden AI calls. Existing spells/macros remain server validated.
 */
final class NexusAwakening {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("nexus-awakening.json");
    private static final int MAX_EVENTS=60;
    private static final class Event {
        long at;
        String kind,text;
        Event(long at,String kind,String text){
            this.at=at;this.kind=kind;this.text=text;
        }
    }
    private static final class Settings {
        int schema=1;
        boolean protection=false; // owner must authorize
        boolean warnings=false;   // owner must authorize
        boolean journal=false;    // owner must authorize local storage
        int criticalPercent=35;
        List<Event> events=new ArrayList<>();
    }
    private static Settings settings;
    private static boolean held,criticalSeen;
    private static long lastScan;
    private static UUID previousPlayer;
    private NexusAwakening(){}

    private static synchronized void load(){
        if(settings!=null)return;
        settings=new Settings();
        if(!Files.isRegularFile(FILE))return;
        try{
            Settings from=JSON.fromJson(Files.readString(FILE,StandardCharsets.UTF_8),
                Settings.class);
            if(from==null||from.schema!=1||from.events==null||
               from.events.size()>1000)throw new IllegalArgumentException("schema inválido");
            from.criticalPercent=Math.max(15,Math.min(60,from.criticalPercent));
            from.events.removeIf(e->e==null||e.text==null||e.kind==null||
                e.text.length()>250);
            while(from.events.size()>MAX_EVENTS)from.events.remove(0);
            settings=from;
        }catch(Exception e){
            // Fail closed: no protections, recording or warnings after bad config.
            settings=new Settings();
            System.err.println("Nexus 4.0: config inválida. Original preservado; modos ativos desligados.");
        }
    }
    private static synchronized void save(){
        try{
            Files.createDirectories(FILE.getParent());
            Path tmp=FILE.resolveSibling(FILE.getFileName()+".tmp");
            Files.writeString(tmp,JSON.toJson(settings),StandardCharsets.UTF_8);
            try{Files.move(tmp,FILE,StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException ex){
                Files.move(tmp,FILE,StandardCopyOption.REPLACE_EXISTING);
            }
        }catch(Exception ex){throw new IllegalStateException(
            "Nexus 4.0: não foi possível gravar configuração.",ex);}
    }
    static synchronized String status(){
        load();
        return "Nexus 4.0 | proteção ofensiva "+(settings.protection?"ON":"OFF")+
            " | alertas "+(settings.warnings?"ON":"OFF")+
            " | diário local "+(settings.journal?"ON":"OFF")+
            " | limiar "+settings.criticalPercent+"%"+
            " | proteção em curso "+(held?"SIM":"NÃO");
    }
    static synchronized String protection(boolean enabled){
        load();settings.protection=enabled;held=false;save();
        return enabled?"Proteção autorizada. Macros ofensivas serão suspensas em vida crítica. "+
            "Cura automática continua exigindo sua autorização separada.":
            "Proteção desligada. Macros originais preservadas.";
    }
    static synchronized String warnings(boolean enabled){
        load();settings.warnings=enabled;criticalSeen=false;save();
        return "Alertas contextuais "+(enabled?"ativados.":"desligados.");
    }
    static synchronized String journal(boolean enabled){
        load();settings.journal=enabled;save();
        return "Diário contextual local "+(enabled?"ativado.":"desligado.")+
            " Não captura conversas, nomes nem coordenadas.";
    }
    static synchronized String threshold(int value){
        load();
        if(value<15||value>60)throw new IllegalArgumentException("Use 15 a 60%.");
        settings.criticalPercent=value;held=false;criticalSeen=false;save();
        return "Novo limiar de risco crítico: "+value+"%.";
    }
    private static synchronized void record(String kind,String text){
        load();if(!settings.journal)return;
        String clean=text.replaceAll("[\\p{Cntrl}§]"," ").trim();
        if(clean.length()>190)clean=clean.substring(0,190);
        settings.events.add(new Event(System.currentTimeMillis(),kind,clean));
        while(settings.events.size()>MAX_EVENTS)settings.events.remove(0);
        save();
    }
    static synchronized List<String> journalEntries(){
        load();
        List<String> result=new ArrayList<>();
        for(int i=settings.events.size()-1;i>=0&&result.size()<8;i--){
            Event e=settings.events.get(i);
            result.add("["+java.time.Instant.ofEpochMilli(e.at)+"] "+e.text);
        }
        return result.isEmpty()?List.of("Diário contextual vazio."):List.copyOf(result);
    }
    static synchronized String clearJournal(){
        load();settings.events.clear();save();
        return "Diário contextual apagado. Memórias manuais preservadas.";
    }
    private static NexusAwakeningPolicy.Reading read(Player player){
        if(player==null)return new NexusAwakeningPolicy.Reading(
            Double.NaN,Double.NaN,-1,false);
        double mana=-1;boolean known=false;
        JsonObject arcane=NexusArcaneSensor.snapshot();
        if(arcane.has("available")&&arcane.get("available").getAsBoolean()&&
           arcane.has("manaPercent")&&arcane.has("lastReadAt")){
            long at=arcane.get("lastReadAt").getAsLong();
            long now=System.currentTimeMillis();
            if(at>0&&now>=at&&now-at<=10_000){
                mana=arcane.get("manaPercent").getAsDouble();
                known=Double.isFinite(mana)&&mana>=0;
            }
        }
        return new NexusAwakeningPolicy.Reading(
            player.getHealth(),player.getMaxHealth(),mana,known);
    }
    static synchronized boolean blockOffense(Player player){
        load();
        if(!settings.protection||player==null||!player.isAlive())return false;
        var reading=read(player);
        if(NexusAwakeningPolicy.holdOffense(true,reading,settings.criticalPercent))return true;
        return held&&!NexusAwakeningPolicy.releaseOffense(
            true,reading,settings.criticalPercent);
    }
    static synchronized void tick(){
        load();
        Minecraft mc=Minecraft.getInstance();
        Player player=mc.player;
        if(player==null||mc.level==null||!player.isAlive()){
            held=false;criticalSeen=false;previousPlayer=null;
            return;
        }
        if(!player.getUUID().equals(previousPlayer)){
            held=false;criticalSeen=false;previousPlayer=player.getUUID();
        }
        long now=System.currentTimeMillis();
        if(now-lastScan<900)return;
        lastScan=now;
        var reading=read(player);
        int percent=NexusAwakeningPolicy.healthPercent(reading);
        boolean critical=percent>=0&&percent<=settings.criticalPercent;
        if(settings.protection){
            if(critical&&!held){
                held=true;
                String warning="Vida crítica ("+percent+"%). Macros ofensivas suspensas. "+
                    (NexusAutoHeal.enabled()?"Cura automática autorizada.":
                        "Cura automática está desligada.");
                showLocal(warning,true,false);
                record("protection","Protocolos defensivos ativados em "+percent+"% de vida.");
            }else if(held&&NexusAwakeningPolicy.releaseOffense(
                true,reading,settings.criticalPercent)){
                held=false;
                showLocal("Vida recuperada. Macros ofensivas liberadas.",false,false);
                record("recovery","Proteção encerrada após recuperação da vida.");
            }
        }else held=false;
        if(critical&&!criticalSeen){
            criticalSeen=true;
            if(settings.warnings&&!settings.protection)
                showLocal("Vida crítica: "+percent+"%. Recomendo recuar.",true,false);
            record("risk","Leitura observada: vida crítica em "+percent+"%.");
        }else if(!critical&&percent>settings.criticalPercent+5)criticalSeen=false;
    }
    private static void showLocal(String text,boolean hud,boolean speech){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null)return;
        mc.player.displayClientMessage(Component.literal("§6[Nexus Aterna] §f"+text),false);
        if(hud)NexusCombatHud.show("awakening",text);
        if(speech&&NexusSpeaker.settingEnabled())
            NexusSpeaker.onSpeech(text,0);
    }
    /** Called explicitly from a client command or accepted offline voice intent. */
    static synchronized void report(boolean spoken){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null){
            NexusClientMod.notifyPlayer("Entre em um mundo para receber uma análise situacional.");
            return;
        }
        Player player=mc.player;
        var reading=read(player);
        var risk=NexusAwakeningPolicy.risk(reading,settingsCritical());
        String intro="Port, análise situacional baseada nos sensores disponíveis.";
        showLocal(intro,false,false);
        String health=NexusAwakeningPolicy.healthDescription(reading);
        String mana=NexusAwakeningPolicy.manaDescription(reading);
        showLocal(health,false,false);
        showLocal(mana,false,false);
        showLocal("Macros: "+NexusCombatMacro.enabledCount()+" ativas. "+
            (NexusCombatMacro.running()?NexusCombatMacro.queueStatus():
                "Nenhuma sequência ofensiva em execução."),false,false);
        showLocal("Manutenção arcana: "+(NexusBlessing.enabled()?"ativa.":"desligada.")+
            " Cura automática: "+(NexusAutoHeal.enabled()?"ON.":"OFF."),false,false);
        showLocal("Ameaças na região: não posso confirmar a segurança da área "+
            "apenas com os dados do cliente.",false,false);
        String advice=NexusAwakeningPolicy.suggestion(risk,NexusAutoHeal.enabled());
        showLocal(advice,true,spoken);
        record("report","Consulta situacional manual. "+health+" "+mana);
    }
    private static synchronized int settingsCritical(){load();return settings.criticalPercent;}
    static synchronized void recall(String query,boolean spoken){
        if(Minecraft.getInstance().player==null)return;
        String topic=query==null?"":query.trim();
        List<NexusOwnerMemories.Memory> notes=topic.isBlank()?
            NexusOwnerMemories.list().stream().limit(3).toList():
            NexusOwnerMemories.relevant(topic,3);
        if(notes.isEmpty()){
            showLocal("Não tenho uma memória manual confirmada sobre esse assunto.",true,
                spoken);
            return;
        }
        showLocal("Port, estes são registros que você me confiou:",false,false);
        for(var note:notes){
            String value="["+note.topic+"] "+note.text;
            if(value.length()>220)value=value.substring(0,217)+"...";
            showLocal(value,false,false);
        }
        if(spoken)showLocal("Encontrei "+notes.size()+" memória(s) registradas.",true,true);
    }
}
