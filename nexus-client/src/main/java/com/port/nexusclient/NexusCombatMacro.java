package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Multiple separately authorized hit-combo macros for the SAME living target.
 * A hit is counted only when its hurt animation becomes visible to this client.
 * Spell casts always use Iron's Spells normal, server-validated QuickCastPacket.
 */
final class NexusCombatMacro {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("nexus-combat-macros.json");
    private static final String DEFAULT_SPELL="irons_spellbooks:divine_smite";
    private static final int DEFAULT_HITS=3,DEFAULT_WINDOW=5;
    private static final long SHARED_GAP_MS=6_000L;

    private static final class Macro {
        String spell=DEFAULT_SPELL;
        int hits=DEFAULT_HITS;
        int windowSeconds=DEFAULT_WINDOW;
        boolean enabled=false;
    }
    private static final class Settings {
        int schema=2;
        String selectedSpell="";
        List<Macro> macros=new ArrayList<>();
    }
    /** Legacy single-combo settings from alpha.5/alpha.6. */
    private static final class Legacy {
        int schema=1;
        boolean enabled=false;
        int hits=DEFAULT_HITS;
        int windowSeconds=DEFAULT_WINDOW;
        String spell=DEFAULT_SPELL;
    }
    record View(String spell,boolean enabled,int hits,int windowSeconds,int progress){}
    private static Settings config;
    private static boolean readOnly;
    private static final Map<String,NexusMacroPolicy.State> STATES=new HashMap<>();
    private record Pending(LivingEntity target,int hurtAtAttack,long expiresAt,UUID playerId){}
    private static Pending pending;
    private static long lastAnyCastAt=-1,lastInfoAt;
    private static String lastOutcome="Macros desligadas. Use F8 > Combate > Configurar Macros.";

    private NexusCombatMacro(){}

    private static synchronized void load(){
        if(config!=null)return;
        config=new Settings();
        if(!Files.isRegularFile(FILE))return;
        try{
            String source=Files.readString(FILE,StandardCharsets.UTF_8);
            JsonObject object=JsonParser.parseString(source).getAsJsonObject();
            int schema=object.get("schema").getAsInt();
            Settings incoming;
            if(schema==1){
                Legacy saved=JSON.fromJson(source,Legacy.class);
                if(saved==null)throw new IllegalStateException("Macro antiga inválida.");
                incoming=new Settings();
                Macro migrated=new Macro();
                migrated.spell=saved.spell;
                migrated.hits=saved.hits;
                migrated.windowSeconds=saved.windowSeconds;
                migrated.enabled=saved.enabled;
                incoming.macros.add(migrated);
                incoming.selectedSpell=saved.spell;
                Path backup=FILE.resolveSibling("nexus-combat-macros-v1-backup.json");
                if(!Files.exists(backup))
                    Files.copy(FILE,backup,StandardCopyOption.COPY_ATTRIBUTES);
                lastOutcome="Macro anterior migrada. Configurações preservadas.";
            }else if(schema==2){
                incoming=JSON.fromJson(source,Settings.class);
            }else throw new IllegalStateException("Versão de configuração incompatível.");
            validate(incoming);
            config=incoming;
        }catch(Exception error){
            config=new Settings();readOnly=true;
            lastOutcome="Arquivo de macros inválido. Original preservado; automação bloqueada.";
        }
    }
    private static void validate(Settings value){
        if(value==null||value.selectedSpell==null||value.macros==null||
            value.macros.size()>NexusMacroRosterPolicy.MAX_MACROS)
            throw new IllegalArgumentException("Configuração inválida.");
        Set<String> ids=new LinkedHashSet<>();
        for(Macro macro:value.macros){
            if(macro==null||!NexusMacroRosterPolicy.validSpell(macro.spell)||
                !ids.add(macro.spell))
                throw new IllegalArgumentException("Magias duplicadas ou inválidas.");
            macro.hits=Math.max(2,Math.min(6,macro.hits));
            macro.windowSeconds=Math.max(2,Math.min(10,macro.windowSeconds));
        }
        if(!value.selectedSpell.isBlank()&&!ids.contains(value.selectedSpell))
            value.selectedSpell="";
        if(value.selectedSpell.isBlank()&&!value.macros.isEmpty())
            value.selectedSpell=value.macros.getFirst().spell;
    }
    private static synchronized void save(){
        if(readOnly)throw new IllegalStateException(
            "Configuração inválida. Verifique nexus-combat-macros.json.");
        try{
            Files.createDirectories(FILE.getParent());
            Path tmp=FILE.resolveSibling(FILE.getFileName()+".tmp");
            Files.writeString(tmp,JSON.toJson(config),StandardCharsets.UTF_8);
            try{Files.move(tmp,FILE,StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException error){
                Files.move(tmp,FILE,StandardCopyOption.REPLACE_EXISTING);
            }
        }catch(Exception error){throw new IllegalStateException("Erro ao salvar macros.",error);}
    }
    private static Macro selected(){
        if(config==null)return null;
        for(Macro macro:config.macros)
            if(macro.spell.equals(config.selectedSpell))return macro;
        return null;
    }
    private static Macro find(String id){
        if(config==null)return null;
        for(Macro macro:config.macros)
            if(macro.spell.equals(id))return macro;
        return null;
    }
    private static NexusMacroPolicy.State state(Macro macro){
        return STATES.computeIfAbsent(macro.spell,k->new NexusMacroPolicy.State());
    }

    static synchronized boolean enabled(){
        load();
        return config.macros.stream().anyMatch(macro->macro.enabled);
    }
    static synchronized boolean selectedEnabled(){
        load();Macro m=selected();return m!=null&&m.enabled;
    }
    static synchronized int enabledCount(){
        load();return (int)config.macros.stream().filter(m->m.enabled).count();
    }
    static synchronized List<View> entries(){
        load();
        return config.macros.stream()
            .map(m->new View(m.spell,m.enabled,m.hits,m.windowSeconds,state(m).hits))
            .toList();
    }
    static synchronized List<String> candidates(){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null)return List.of();
        return NexusVoiceCasting.equippedSpells(mc.player).stream()
            .map(NexusVoiceGrammar.Spell::id).distinct().toList();
    }
    static synchronized String addSpell(String id){
        load();
        if(readOnly)return lastOutcome;
        String normalized=id==null?"":id.toLowerCase(Locale.ROOT).trim();
        if(!NexusMacroRosterPolicy.validSpell(normalized))
            return lastOutcome="Identificador inválido. Use namespace:feitiço.";
        if(!candidates().contains(normalized))
            return lastOutcome="Equipe a magia no Iron's Spells antes de cadastrá-la.";
        if(find(normalized)!=null){
            config.selectedSpell=normalized;save();
            return lastOutcome="Macro existente selecionada: "+normalized+".";
        }
        if(config.macros.size()>=NexusMacroRosterPolicy.MAX_MACROS)
            return lastOutcome="Limite de "+NexusMacroRosterPolicy.MAX_MACROS+" macros.";
        Macro added=new Macro();
        added.spell=normalized;
        config.macros.add(added);
        config.selectedSpell=normalized;
        reset();
        save();
        return lastOutcome="Macro adicionada: "+normalized+". Ative separadamente.";
    }
    static synchronized String selectSpell(String id){
        load();
        if(find(id)==null)return lastOutcome="Macro não cadastrada: "+id;
        config.selectedSpell=id;save();
        return lastOutcome="Macro selecionada: "+id;
    }
    static synchronized String removeSpell(String id){
        load();
        if(readOnly)return lastOutcome;
        Macro existing=find(id);
        if(existing==null)return lastOutcome="Macro não encontrada.";
        config.macros.remove(existing);
        STATES.remove(id);
        if(id.equals(config.selectedSpell))
            config.selectedSpell=config.macros.isEmpty()?"":config.macros.getFirst().spell;
        reset();save();
        return lastOutcome="Macro removida: "+id+".";
    }
    static synchronized int hits(){
        load();Macro m=selected();return m==null?DEFAULT_HITS:m.hits;
    }
    static synchronized int windowSeconds(){
        load();Macro m=selected();return m==null?DEFAULT_WINDOW:m.windowSeconds;
    }
    static synchronized String spell(){
        load();Macro m=selected();return m==null?"":m.spell;
    }
    static synchronized int progress(){
        load();Macro m=selected();return m==null?0:state(m).hits;
    }
    static synchronized String lastOutcome(){load();return lastOutcome;}
    static synchronized String status(){
        load();
        return "Macros "+enabledCount()+"/"+config.macros.size()+
            " ativas | Selecionada: "+(spell().isBlank()?"nenhuma":spell())+
            " | "+hits()+" golpes em "+windowSeconds()+"s | "+lastOutcome;
    }

    /** Backwards-compatible: /macro on affects selected; global off disables ALL. */
    static synchronized void setEnabled(boolean on){
        load();
        if(!on){
            for(Macro macro:config.macros)macro.enabled=false;
            reset();save();
            lastOutcome="Todas as macros desligadas.";
            return;
        }
        setSelectedEnabled(true);
    }
    static synchronized void setSelectedEnabled(boolean on){
        load();
        Macro macro=selected();
        if(macro==null)throw new IllegalStateException("Cadastre uma magia primeiro.");
        if(on){
            if(!ModList.get().isLoaded("irons_spellbooks"))
                throw new IllegalStateException("Iron's Spells não instalado.");
            if(!candidates().contains(macro.spell))
                throw new IllegalStateException("Magia precisa estar equipada: "+macro.spell);
        }
        macro.enabled=on;
        reset();save();
        lastOutcome=macro.spell+(on?" ativada. Acerte o mesmo alvo.":" desativada.");
    }
    static synchronized void setHits(int amount){
        load();
        if(amount<2||amount>6)throw new IllegalArgumentException("Use de 2 a 6 golpes.");
        Macro macro=selected();
        if(macro==null)throw new IllegalStateException("Cadastre uma macro primeiro.");
        macro.hits=amount;reset();save();
        lastOutcome="Combo de "+macro.spell+" definido para "+amount+" golpes.";
    }
    static synchronized void setWindow(int seconds){
        load();
        if(seconds<2||seconds>10)throw new IllegalArgumentException("Use de 2 a 10s.");
        Macro macro=selected();
        if(macro==null)throw new IllegalStateException("Cadastre uma macro primeiro.");
        macro.windowSeconds=seconds;reset();save();
        lastOutcome="Janela de "+macro.spell+": "+seconds+" segundos.";
    }
    static synchronized void setSpell(String id){
        load();
        Macro macro=selected();
        if(macro==null){
            lastOutcome=addSpell(id);
            return;
        }
        String normalized=id==null?"":id.toLowerCase(Locale.ROOT).trim();
        if(!NexusMacroRosterPolicy.validSpell(normalized))
            throw new IllegalArgumentException("Use namespace:feitiço.");
        if(!candidates().contains(normalized))
            throw new IllegalStateException("Equipe a magia no Iron's Spells primeiro.");
        if(!macro.spell.equals(normalized)&&find(normalized)!=null)
            throw new IllegalArgumentException("Esta magia já tem uma macro.");
        String old=macro.spell;
        macro.spell=normalized;
        config.selectedSpell=normalized;
        STATES.remove(old);
        reset();save();
        lastOutcome="Macro alterada de "+old+" para "+normalized+".";
    }

    static synchronized void reset(){
        pending=null;
        for(NexusMacroPolicy.State s:STATES.values())s.resetAll();
        lastAnyCastAt=-1;
    }
    static synchronized void shutdown(){reset();}
    static synchronized void onAttack(AttackEntityEvent event){
        Minecraft mc=Minecraft.getInstance();
        if(!enabled()||mc.player==null||event.getEntity()!=mc.player||
            mc.level==null||mc.screen!=null||!mc.isWindowActive())return;
        if(!(event.getTarget() instanceof LivingEntity living)||!living.isAlive()||
            living==mc.player)return;
        if(mc.player.isSpectator()||mc.player.distanceToSqr(living)>25||
            !mc.player.hasLineOfSight(living))return;
        if(mc.player.getAttackStrengthScale(0.5f)<0.8f)return;
        long now=System.currentTimeMillis();
        pending=new Pending(living,living.hurtTime,now+700,mc.player.getUUID());
    }

    static synchronized void tick(){
        if(!enabled())return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.screen!=null||
            !mc.isWindowActive()||mc.player.isSpectator()){
            pending=null;
            for(var state:STATES.values())state.resetCombo();
            return;
        }
        Pending attempt=pending;
        if(attempt==null)return;
        long now=System.currentTimeMillis();
        LivingEntity target=attempt.target();
        if(now>attempt.expiresAt()||!target.isAlive()||target.level()!=mc.level||
            !attempt.playerId().equals(mc.player.getUUID())){
            pending=null;return;
        }
        if(target.hurtTime<=attempt.hurtAtAttack()||target.hurtTime<5)return;
        pending=null;
        if(!NexusMacroRosterPolicy.mayTrigger(now,lastAnyCastAt,SHARED_GAP_MS))
            return;
        // Order is explicit list order: first eligible trigger wins per hit.
        // Reset all combos after a trigger so 2 and 3-hit macros don't
        // accidentally produce consecutive casts for one attack series.
        Macro winner=null;
        for(Macro macro:config.macros){
            if(!macro.enabled)continue;
            NexusMacroPolicy.Result result=NexusMacroPolicy.register(state(macro),
                target.getUUID(),now,macro.hits,
                NexusMacroPolicy.windowMs(macro.windowSeconds),
                NexusMacroPolicy.DEFAULT_REARM_MS);
            if(result.trigger()&&winner==null)winner=macro;
            else if(result.hits()>0&&now-lastInfoAt>350){
                lastInfoAt=now;
                lastOutcome="Combo "+result.hits()+"/"+macro.hits+
                    " ("+macro.spell+") no alvo "+target.getName().getString()+".";
            }
        }
        if(winner!=null){
            // Rate limit packet dispatch even if the remote server denies it.
            lastAnyCastAt=now;
            for(var state:STATES.values())state.resetCombo();
            attemptCast(mc.player,target,winner);
        }
    }

    private static void attemptCast(Player player,LivingEntity target,Macro macro){
        if(!target.isAlive()||player.distanceToSqr(target)>20||
            !player.hasLineOfSight(target)){
            lastOutcome="Alvo distante ou fora de visão. Combo cancelado.";
            NexusCombatHud.show("macro",lastOutcome);return;
        }
        if(NexusVoiceCasting.clientIsCasting()){
            lastOutcome="Você já está conjurando. Combo cancelado.";
            NexusCombatHud.show("macro",lastOutcome);return;
        }
        NexusVoiceGrammar.Spell option=null;
        for(NexusVoiceGrammar.Spell entry:NexusVoiceCasting.equippedSpells(player))
            if(macro.spell.equals(entry.id())){option=entry;break;}
        if(option==null){
            lastOutcome="Macro: "+macro.spell+" não está equipada.";
            NexusCombatHud.show("macro",lastOutcome);return;
        }
        try{
            if(!canCast(player,option.slot(),macro.spell)){
                lastOutcome="Macro "+macro.spell+": mana, cooldown ou feitiço indisponível.";
                NexusCombatHud.show("macro",lastOutcome);return;
            }
            Class<?> packetType=Class.forName(
                "io.redspace.ironsspellbooks.network.casting.QuickCastPacket");
            Object payload=packetType.getConstructor(int.class).newInstance(option.slot());
            PacketDistributor.sendToServer((CustomPacketPayload)payload);
            lastOutcome="Combo concluído! "+macro.spell+" solicitado.";
            NexusCombatHud.show("macro",lastOutcome);
        }catch(Throwable error){
            lastOutcome="Macro não conseguiu solicitar "+macro.spell+".";
            NexusCombatHud.show("macro",lastOutcome);
        }
    }

    private static boolean canCast(Player player,int slot,String expected){
        try{
            Class<?> manager=Class.forName(
                "io.redspace.ironsspellbooks.api.magic.SpellSelectionManager");
            Class<?> dataType=Class.forName(
                "io.redspace.ironsspellbooks.api.spells.SpellData");
            Class<?> abstractSpell=Class.forName(
                "io.redspace.ironsspellbooks.api.spells.AbstractSpell");
            Object selection=manager.getConstructor(Player.class).newInstance(player);
            Object data=manager.getMethod("getSpellData",int.class).invoke(selection,slot);
            if(data==null)return false;
            Object spell=dataType.getMethod("getSpell").invoke(data);
            if(spell==null||!expected.equals(
                String.valueOf(abstractSpell.getMethod("getSpellId").invoke(spell))))
                return false;
            Class<?> cData=Class.forName(
                "io.redspace.ironsspellbooks.player.ClientMagicData");
            Object cooldowns=cData.getMethod("getCooldowns").invoke(null);
            if(cooldowns==null||Boolean.TRUE.equals(cooldowns.getClass()
                .getMethod("isOnCooldown",abstractSpell).invoke(cooldowns,spell)))
                return false;
            int baseLevel=((Number)dataType.getMethod("getLevel").invoke(data)).intValue();
            int level=((Number)abstractSpell.getMethod("getLevelFor",int.class,
                LivingEntity.class).invoke(spell,baseLevel,player)).intValue();
            int cost=((Number)abstractSpell.getMethod("getManaCost",int.class)
                .invoke(spell,level)).intValue();
            int mana=((Number)cData.getMethod("getPlayerMana").invoke(null)).intValue();
            return mana>=cost;
        }catch(Throwable e){return false;}
    }
}
