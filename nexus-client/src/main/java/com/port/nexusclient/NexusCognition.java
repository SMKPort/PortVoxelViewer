package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Nexus 4.0 context layer: limited to facts visible to the local player.
 * No LLM guesses, no automatic spell casting, no outgoing player chat.
 */
final class NexusCognition {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("nexus-cognition.json");
    private static class Settings {
        int schema=1;
        boolean monitor=false;
        boolean pauseOffensiveMacros=false;
        int criticalPercent=25;
    }
    private static Settings config;
    private static boolean lastCritical;
    private static long lastScanAt;
    private static String lastAlert="";
    private NexusCognition(){}

    private static void load(){
        if(config!=null)return;
        config=new Settings();
        if(!Files.isRegularFile(FILE))return;
        try{
            Settings saved=JSON.fromJson(Files.readString(FILE,StandardCharsets.UTF_8),Settings.class);
            if(saved!=null&&saved.schema==1){
                saved.criticalPercent=Math.max(10,Math.min(60,saved.criticalPercent));
                config=saved;
            }
        }catch(Exception error){
            System.err.println("Nexus Cognition: preferências inválidas, usando modo manual.");
        }
    }
    private static void save(){
        try{
            Files.createDirectories(FILE.getParent());
            Path temp=FILE.resolveSibling(FILE.getFileName()+".tmp");
            Files.writeString(temp,JSON.toJson(config),StandardCharsets.UTF_8);
            try{Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException error){
                Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING);}
        }catch(Exception error){throw new IllegalStateException("Falha ao salvar Nexus 4.0.",error);}
    }
    static synchronized boolean monitor(){load();return config.monitor;}
    static synchronized boolean emergency(){load();return config.pauseOffensiveMacros;}
    static synchronized int threshold(){load();return config.criticalPercent;}
    static synchronized void monitor(boolean yes){
        load();config.monitor=yes;lastCritical=false;save();
    }
    static synchronized void emergency(boolean yes){
        load();config.pauseOffensiveMacros=yes;save();
    }
    static synchronized void threshold(int value){
        load();
        if(value<10||value>60)throw new IllegalArgumentException("Use 10 a 60%.");
        config.criticalPercent=value;lastCritical=false;save();
    }
    static synchronized void off(){
        load();config.monitor=false;config.pauseOffensiveMacros=false;lastCritical=false;save();
    }
    private static String dimension(Player p){
        try{return p.level().dimension().location().toString();}
        catch(Exception error){return "desconhecida";}
    }
    private static NexusCognitionPolicy.Facts facts(Player p){
        JsonObject arcane=NexusArcaneSensor.snapshot();
        JsonObject arm=NexusVitals.snapshot();
        double mana=-1,maxMana=-1,integrity=-1;
        boolean armActive=arm.has("managed")&&arm.get("managed").getAsBoolean()&&
            arm.has("enabled")&&arm.get("enabled").getAsBoolean();
        if(arcane.has("mana")&&arcane.get("mana").isJsonPrimitive())
            mana=arcane.get("mana").getAsDouble();
        if(arcane.has("maxMana")&&arcane.get("maxMana").isJsonPrimitive())
            maxMana=arcane.get("maxMana").getAsDouble();
        if(armActive&&arm.has("integrity"))integrity=arm.get("integrity").getAsDouble();
        boolean healEquipped=false;
        if(NexusAutoHeal.enabled()){
            healEquipped=NexusVoiceCasting.equippedSpells(p).stream()
                .anyMatch(spell->"irons_spellbooks:heal".equals(spell.id()));
        }
        return new NexusCognitionPolicy.Facts(
            p.getHealth(),p.getMaxHealth(),p.getFoodData().getFoodLevel(),
            mana,maxMana,integrity,armActive,NexusCombatMacro.running(),
            NexusAutoHeal.enabled(),healEquipped,dimension(p));
    }
    static synchronized List<String> report(){
        load();
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null)return List.of(
            "Nexus 4.0: aguardando entrada no mundo. Nenhuma análise de situação disponível.");
        Player p=mc.player;
        var input=facts(p);
        var a=NexusCognitionPolicy.assess(input,config.criticalPercent);
        List<String> lines=new ArrayList<>();
        lines.add("NEXUS 4.0 | Risco "+a.risk()+" | vida "+a.healthPercent()+
            "% | mana "+(a.manaPercent()<0?"sem leitura":a.manaPercent()+"%")+
            " | fome "+input.hunger()+"/20");
        lines.add("Local: "+input.dimension()+" | posição "+
            p.blockPosition().getX()+", "+p.blockPosition().getY()+", "+
            p.blockPosition().getZ()+" (cliente).");
        lines.add("Combate: "+NexusCombatMacro.enabledCount()+" macros habilitadas"+
            (NexusCombatMacro.emergencySuspended()?" | SUSPENSAS por emergência":
            NexusCombatMacro.running()?" | sequência em execução":"")+
            " | Auto Heal "+(input.healAuthorized()?"autorizado":"desligado")+".");
        lines.addAll(a.notes());
        return List.copyOf(lines);
    }
    static synchronized String status(){
        load();
        return "Nexus 4.0 | monitor "+(config.monitor?"ON":"OFF")+
            " | suspender macros no risco "+(config.pauseOffensiveMacros?"ON":"OFF")+
            " | vida crítica <= "+config.criticalPercent+"%"+
            (lastAlert.isBlank()?"":" | último alerta: "+lastAlert);
    }
    static synchronized String recall(String query){
        List<String> matches=new ArrayList<>();
        for(var memory:NexusOwnerMemories.relevant(query,2))
            matches.add("Memória #"+memory.id+" ["+memory.topic+"]: "+memory.text);
        matches.addAll(NexusNarrativeJournal.recall(query,2).stream()
            .filter(line->!line.equals("Nenhum acontecimento correspondente no diário."))
            .toList());
        return matches.isEmpty()?"Não encontrei registros locais relacionados. Não vou inventar lembranças.":
            String.join(" | ",matches);
    }
    static synchronized void tick(){
        load();
        Minecraft mc=Minecraft.getInstance();
        if(!config.monitor||mc.player==null||mc.level==null||mc.player.isSpectator()){
            lastCritical=false;return;
        }
        long now=System.currentTimeMillis();
        if(now-lastScanAt<1000)return;
        lastScanAt=now;
        var assessment=NexusCognitionPolicy.assess(facts(mc.player),config.criticalPercent);
        if(!assessment.critical()){
            if(assessment.healthPercent()>config.criticalPercent+10)lastCritical=false;
            return;
        }
        if(lastCritical)return;
        lastCritical=true;
        lastAlert="Vida "+assessment.healthPercent()+"%. Recomendo recuar.";
        NexusCombatHud.show("nexus4",lastAlert);
        NexusClientMod.notifyPlayer("§c[Nexus Aterna] "+lastAlert);
        if(config.pauseOffensiveMacros){
            NexusCombatMacro.suspendForEmergency();
            NexusClientMod.notifyPlayer("§6[Nexus Aterna] Macros ofensivas suspensas até autorização manual.");
        }
        NexusNarrativeJournal.recordCritical(assessment.healthPercent(),dimension(mc.player));
    }
}
