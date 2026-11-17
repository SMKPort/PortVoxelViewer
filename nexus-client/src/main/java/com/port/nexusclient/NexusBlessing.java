package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Client-only, explicitly enabled self-buff maintenance for a spell the owner
 * selects and learns from a REAL observed effect refresh.
 *
 * No invented effect ids, direct player edits, custom packets or background AI.
 * Spell cost and cooldowns are checked locally and again by the server.
 */
final class NexusBlessing {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("nexus-blessing.json");
    private static final class Config {
        int schema=1;
        boolean enabled=false;
        String spellId="";
        String realm="";
        int thresholdSeconds=NexusBlessingPolicy.DEFAULT_SECONDS;
        List<String> effects=new ArrayList<>();
    }
    private record Learning(Map<String,Integer> before,Set<String> found,
                            String realm,Object level,long endAt,long firstObserved) {
        Learning withObserved(Set<String> updated,long first) {
            return new Learning(before,updated,realm,level,endAt,first);
        }
    }
    private enum Preflight { READY, MANA, COOLDOWN, UNKNOWN }
    private static Config config;
    private static Learning learning;
    private static boolean readOnly;
    private static long lastCheckAt,lastAttemptAt;
    private static String status="Blessing: aguardando configuração e autorização.";
    private NexusBlessing(){}

    private static synchronized void load(){
        if(config!=null)return;
        config=new Config();
        if(!Files.isRegularFile(FILE))return;
        try {
            Config saved=JSON.fromJson(Files.readString(FILE,StandardCharsets.UTF_8),Config.class);
            if(saved==null||saved.schema!=1||saved.effects==null||saved.realm==null)
                throw new IllegalArgumentException("Schema inválido.");
            if(!saved.spellId.isBlank()&&!NexusBlessingPolicy.plausibleSpellId(saved.spellId))
                throw new IllegalArgumentException("ID de magia inválido.");
            if(saved.effects.size()>8||saved.effects.stream().anyMatch(e->
                !NexusBlessingPolicy.plausibleSpellId(e)))
                throw new IllegalArgumentException("Lista de efeitos inválida.");
            saved.thresholdSeconds=NexusBlessingPolicy.seconds(saved.thresholdSeconds);
            saved.effects=new ArrayList<>(new LinkedHashSet<>(saved.effects));
            config=saved;
        }catch(Exception error){
            config=new Config();readOnly=true;
            status="Arquivo de Blessing inválido. Original preservado e automação desligada.";
        }
    }

    private static synchronized void save(){
        if(readOnly)throw new IllegalStateException("Arquivo inválido; corrigi-lo antes de salvar.");
        try {
            Files.createDirectories(FILE.getParent());
            Path tmp=FILE.resolveSibling(FILE.getFileName()+".tmp");
            Files.writeString(tmp,JSON.toJson(config),StandardCharsets.UTF_8);
            try {
                Files.move(tmp,FILE,StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            }catch(AtomicMoveNotSupportedException error){
                Files.move(tmp,FILE,StandardCopyOption.REPLACE_EXISTING);
            }
        }catch(Exception error){
            throw new IllegalStateException("Não foi possível salvar nexus-blessing.json",error);
        }
    }

    private static String realm(){
        Minecraft mc=Minecraft.getInstance();
        String source;
        if(mc.getCurrentServer()!=null)
            source="multi:"+mc.getCurrentServer().ip.toLowerCase(Locale.ROOT);
        else if(mc.getSingleplayerServer()!=null)
            source="single:"+mc.getSingleplayerServer().getWorldData().getLevelName();
        else return "";
        try {
            byte[] hash=MessageDigest.getInstance("SHA-256")
                .digest(source.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        }catch(Exception error){return "";}
    }

    static synchronized boolean enabled(){load();return config.enabled;}
    static synchronized int thresholdSeconds(){load();return config.thresholdSeconds;}
    static synchronized String spellId(){load();return config.spellId;}
    static synchronized List<String> trackedEffects(){load();return List.copyOf(config.effects);}
    static synchronized String status(){load();return status;}
    static synchronized String summary(){
        load();
        String prefix="Blessing "+(config.enabled?"ON":"OFF")+" | "+
            (config.spellId.isBlank()?"magia não definida":config.spellId)+
            " | renovar em "+config.thresholdSeconds+"s | "+
            config.effects.size()+" efeito(s)";
        if(learning!=null)prefix+=" | APRENDENDO";
        return prefix+" | "+status;
    }

    private static String exactEquippedBlessing(Player player) {
        List<NexusVoiceGrammar.Spell> matches=NexusVoiceCasting.equippedSpells(player).stream()
            .filter(spell->spell.id().endsWith(":blessing")).toList();
        return matches.size()==1?matches.getFirst().id():"";
    }

    static synchronized List<String> candidates(){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null)return List.of();
        return NexusVoiceCasting.equippedSpells(mc.player).stream()
            .map(NexusVoiceGrammar.Spell::id)
            .filter(id->id.contains("bless"))
            .distinct().toList();
    }

    private static boolean equipped(Player player,String id) {
        return NexusVoiceCasting.equippedSpells(player).stream()
            .anyMatch(entry->entry.id().equals(id));
    }

    static synchronized String setSpell(String id){
        load();
        if(!NexusBlessingPolicy.plausibleSpellId(id))
            return status="Use o ID namespace:magia (veja /nexusclient voicecast spells).";
        if(readOnly)return status;
        config.enabled=false;
        config.spellId=id;
        config.effects.clear();
        config.realm="";
        learning=null;
        lastAttemptAt=0;
        save();
        return status="Blessing apontado para "+id+". Execute /nexusclient blessing learn e lance a magia manualmente.";
    }

    static synchronized String setThreshold(int seconds){
        load();
        if(seconds<NexusBlessingPolicy.MIN_SECONDS||seconds>NexusBlessingPolicy.MAX_SECONDS)
            return status="Antecedência deve ser de 3 a 60 segundos.";
        config.thresholdSeconds=seconds;
        save();
        return status="Nexus renovará quando o efeito tiver "+seconds+"s ou menos.";
    }

    static synchronized String setEnabled(boolean enable){
        load();
        if(readOnly)return status;
        if(!enable){
            config.enabled=false;
            learning=null;
            save();
            return status="Renovação automática do Blessing desativada.";
        }
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||!ModList.get().isLoaded("irons_spellbooks"))
            return status="Entre no mundo com Iron's Spells disponível.";
        if(config.spellId.isBlank()){
            String detected=exactEquippedBlessing(mc.player);
            if(!detected.isBlank())config.spellId=detected;
        }
        if(config.spellId.isBlank())return status="Defina a magia exata usando /nexusclient blessing spell <id>.";
        if(!equipped(mc.player,config.spellId))
            return status="A magia "+config.spellId+" precisa estar equipada.";
        if(config.effects.isEmpty())
            return status="Primeiro aprenda o efeito: /nexusclient blessing learn.";
        if(realm().isBlank()||!realm().equals(config.realm))
            return status="Blessing aprendido em outro servidor/mundo. Aprenda aqui antes de ligar.";
        config.enabled=true;
        lastAttemptAt=0;
        save();
        return status="Manutenção automática autorizada. Renovação a "+config.thresholdSeconds+
            "s do fim, respeitando mana e cooldown.";
    }

    /** Learn only positive visible potion effects that appear or are renewed. */
    static synchronized String learn(){
        load();
        Minecraft mc=Minecraft.getInstance();
        if(readOnly)return status;
        if(mc.player==null||mc.level==null||!ModList.get().isLoaded("irons_spellbooks"))
            return status="Entre no mundo com Iron's Spells.";
        if(config.spellId.isBlank()){
            String detected=exactEquippedBlessing(mc.player);
            if(!detected.isBlank()){config.spellId=detected;save();}
        }
        if(config.spellId.isBlank())
            return status="Não identifiquei uma magia :blessing única. Use /nexusclient blessing spell <id>.";
        if(!equipped(mc.player,config.spellId))
            return status="Equipe "+config.spellId+" antes de aprender.";
        config.enabled=false;
        save();
        long now=System.currentTimeMillis();
        learning=new Learning(effects(mc.player),new LinkedHashSet<>(),realm(),mc.level,
            now+NexusBlessingPolicy.LEARN_WINDOW_MS,0);
        return status="Aprendendo "+config.spellId+": lance Blessing MANUALMENTE agora. "
            +"O Nexus observará quais efeitos positivos aparecem ou são renovados (30s).";
    }

    private static Map<String,Integer> effects(Player player){
        Map<String,Integer> visible=new LinkedHashMap<>();
        for(MobEffectInstance entry:player.getActiveEffects()){
            if(!entry.getEffect().value().isBeneficial())continue;
            var id=BuiltInRegistries.MOB_EFFECT.getKey(entry.getEffect().value());
            if(id!=null)visible.put(id.toString(),entry.getDuration());
        }
        return visible;
    }

    static synchronized List<String> effectsStatus(){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null)return List.of("Entre no mundo para ver seus efeitos.");
        Map<String,Integer> current=effects(mc.player);
        List<String> lines=new ArrayList<>();
        for(var entry:current.entrySet())
            lines.add(entry.getKey()+" : "+Math.max(0,entry.getValue()/20)+"s");
        return lines.isEmpty()?List.of("Nenhum efeito positivo ativo no jogador."):List.copyOf(lines);
    }

    static synchronized String trackEffect(String id) {
        load();
        if(!NexusBlessingPolicy.plausibleSpellId(id))return status="ID inválido.";
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null)return status="Entre no mundo.";
        if(config.spellId.isBlank()||!equipped(mc.player,config.spellId))
            return status="Configure e equipe a magia Blessing primeiro.";
        if(!effects(mc.player).containsKey(id))
            return status="O efeito "+id+" precisa estar ativo em você para ser vinculado.";
        if(readOnly)return status;
        config.enabled=false;
        config.realm=realm();
        config.effects=new ArrayList<>(List.of(id));
        learning=null;
        save();
        return status="Efeito "+id+" associado a "+config.spellId+
            ". Agora ative a renovação, se esse for o efeito produzido pela magia.";
    }

    static synchronized void tick(){
        load();
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null){
            learning=null;lastCheckAt=0;return;
        }
        long now=System.currentTimeMillis();
        if(now-lastCheckAt<NexusBlessingPolicy.CHECK_INTERVAL_MS)return;
        lastCheckAt=now;
        Player player=mc.player;
        Map<String,Integer> current=effects(player);
        if(learning!=null){
            Learning record=learning;
            if(!record.realm().equals(realm())||mc.level!=record.level()){
                learning=null;status="Aprendizado cancelado: mundo alterado.";return;
            }
            Set<String> observed=new LinkedHashSet<>(record.found());
            for(var entry:current.entrySet()) {
                int before=record.before().getOrDefault(entry.getKey(),-1);
                if(NexusBlessingPolicy.learned(before,entry.getValue()))
                    observed.add(entry.getKey());
            }
            long first=record.firstObserved();
            if(!observed.isEmpty()&&first==0)first=now;
            learning=record.withObserved(observed,first);
            if(now>=record.endAt()||(first>0&&now-first>=3_000L)) {
                learning=null;
                if(observed.isEmpty()){
                    status="Nenhum efeito positivo novo/renovado foi detectado. "
                        +"Confira se Blessing foi conjurado em você.";
                }else if(observed.size()>8){
                    status="Muitos efeitos simultâneos. Aprendizado descartado por segurança.";
                }else{
                    config.effects=new ArrayList<>(observed);
                    config.realm=realm();
                    config.enabled=false;
                    save();
                    status="Aprendi os efeitos do Blessing: "+String.join(", ",observed)+
                        ". Confirme com /nexusclient blessing on.";
                    NexusClientMod.notifyPlayer("[Nexus / Blessing] "+status);
                }
            }
            return; // Never auto-cast while learning.
        }
        if(!config.enabled||config.effects.isEmpty()||config.spellId.isBlank())return;
        if(mc.screen!=null||!mc.isWindowActive()||player.isSpectator()||
            !player.isAlive()||!realm().equals(config.realm)||realm().isBlank()||
            !ModList.get().isLoaded("irons_spellbooks")||NexusBuffManager.busy()||
            player.isUsingItem())return;
        List<Integer> durations=new ArrayList<>();
        for(String effect:config.effects)durations.add(current.getOrDefault(effect,-1));
        boolean expiring=NexusBlessingPolicy.needsRefresh(durations,config.thresholdSeconds);
        if(!expiring)return;
        if(!NexusBlessingPolicy.allowed(config.enabled,true,NexusVoiceCasting.clientIsCasting(),
            true,true,expiring,now,lastAttemptAt))return;

        NexusVoiceGrammar.Spell selected=null;
        for(var spell:NexusVoiceCasting.equippedSpells(player))
            if(config.spellId.equals(spell.id())){selected=spell;break;}
        if(selected==null){
            status="Blessing não está equipado. Renovação suspensa.";return;
        }
        Preflight preflight=checkCast(player,selected.slot(),config.spellId);
        if(preflight!=Preflight.READY) {
            status=switch(preflight){
                case COOLDOWN -> "Blessing em cooldown, aguardando.";
                case MANA -> "Mana insuficiente para renovar Blessing.";
                default -> "Não foi possível verificar Blessing; conjuração evitada.";
            };
            return;
        }
        lastAttemptAt=now; // Prevent repeated packets even if the server declines.
        try {
            Class<?> type=Class.forName(
                "io.redspace.ironsspellbooks.network.casting.QuickCastPacket");
            Object packet=type.getConstructor(int.class).newInstance(selected.slot());
            PacketDistributor.sendToServer((CustomPacketPayload)packet);
            status="Efeito próximo do fim. Renovação do Blessing solicitada (slot "+
                selected.slot()+").";
            NexusCombatHud.show("blessing",status);
            NexusClientMod.notifyPlayer("[Nexus / Blessing] "+status);
        }catch(Throwable error){
            status="Falha ao solicitar Blessing; verifique o Iron's Spells.";
            NexusCombatHud.show("blessing",status);
        }
    }

    private static Preflight checkCast(Player player,int slot,String expected){
        try{
            Class<?> managerType=Class.forName(
                "io.redspace.ironsspellbooks.api.magic.SpellSelectionManager");
            Class<?> dataType=Class.forName(
                "io.redspace.ironsspellbooks.api.spells.SpellData");
            Class<?> spellType=Class.forName(
                "io.redspace.ironsspellbooks.api.spells.AbstractSpell");
            Object manager=managerType.getConstructor(Player.class).newInstance(player);
            Object data=managerType.getMethod("getSpellData",int.class).invoke(manager,slot);
            if(data==null)return Preflight.UNKNOWN;
            Object spell=dataType.getMethod("getSpell").invoke(data);
            if(spell==null||!expected.equals(
                String.valueOf(spellType.getMethod("getSpellId").invoke(spell))))
                return Preflight.UNKNOWN;
            Class<?> magicData=Class.forName(
                "io.redspace.ironsspellbooks.player.ClientMagicData");
            Object cooldowns=magicData.getMethod("getCooldowns").invoke(null);
            if(cooldowns==null)return Preflight.UNKNOWN;
            if(Boolean.TRUE.equals(cooldowns.getClass()
                .getMethod("isOnCooldown",spellType).invoke(cooldowns,spell)))
                return Preflight.COOLDOWN;
            int baseLevel=((Number)dataType.getMethod("getLevel").invoke(data)).intValue();
            Method normalLevel=spellType.getMethod("getLevelFor",int.class,LivingEntity.class);
            int effective=((Number)normalLevel.invoke(spell,baseLevel,player)).intValue();
            int cost=((Number)spellType.getMethod("getManaCost",int.class)
                .invoke(spell,effective)).intValue();
            int mana=((Number)magicData.getMethod("getPlayerMana").invoke(null)).intValue();
            return mana>=cost?Preflight.READY:Preflight.MANA;
        }catch(Throwable error){return Preflight.UNKNOWN;}
    }
}
