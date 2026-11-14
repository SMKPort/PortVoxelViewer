package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Macro 01: three observed melee hits against the SAME living target
 * within a short window -> normal server-validated Iron's Spells QuickCast.
 *
 * The client cannot authoritatively verify server damage without a server mod.
 * A pending attack is counted only after the target's visible hurt animation
 * advances; never count a raw mouse click as a confirmed combo hit.
 */
final class NexusCombatMacro {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("nexus-combat-macros.json");
    private static final String DEFAULT_SPELL="irons_spellbooks:divine_smite";
    private static final NexusMacroPolicy.State STREAK=new NexusMacroPolicy.State();
    private static final class Settings {
        int schema=1;
        boolean enabled=false;
        int hits=3;
        int windowSeconds=5;
        String spell=DEFAULT_SPELL;
    }
    private static Settings config;
    private static Pending pending;
    private record Pending(LivingEntity target,int hurtAtAttack,long expiresAt,UUID playerId){}
    private static String lastOutcome="Macro desligada. Ative no F8 > Combate.";
    private static long lastInfoAt;

    private NexusCombatMacro(){}

    private static synchronized void load(){
        if(config!=null)return;
        config=new Settings();
        if(!Files.isRegularFile(FILE))return;
        try{
            Settings saved=JSON.fromJson(Files.readString(FILE,StandardCharsets.UTF_8),Settings.class);
            if(saved!=null&&saved.schema==1){
                saved.hits=Math.max(2,Math.min(6,saved.hits));
                saved.windowSeconds=Math.max(2,Math.min(10,saved.windowSeconds));
                if(saved.spell==null||!saved.spell.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
                    saved.spell=DEFAULT_SPELL;
                config=saved;
            }
        }catch(Exception e){lastOutcome="Configuração de macros inválida. Macro desligada.";}
    }
    private static synchronized void save(){
        try{
            Files.createDirectories(FILE.getParent());
            Path tmp=FILE.resolveSibling(FILE.getFileName()+".tmp");
            Files.writeString(tmp,JSON.toJson(config),StandardCharsets.UTF_8);
            try{Files.move(tmp,FILE,StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){
                Files.move(tmp,FILE,StandardCopyOption.REPLACE_EXISTING);
            }
        }catch(Exception e){throw new IllegalStateException("Erro ao salvar macro.",e);}
    }

    static synchronized boolean enabled(){load();return config.enabled;}
    static synchronized int hits(){load();return config.hits;}
    static synchronized int windowSeconds(){load();return config.windowSeconds;}
    static synchronized String spell(){load();return config.spell;}
    static synchronized String status(){
        load();
        return "Macro "+(config.enabled?"ON":"OFF")+" | "+
            config.hits+" golpes em "+config.windowSeconds+"s | "+
            config.spell+" | "+lastOutcome;
    }
    static synchronized int progress(){load();return STREAK.hits;}
    static synchronized String lastOutcome(){load();return lastOutcome;}

    static synchronized void setEnabled(boolean on){
        load();
        if(on&&!ModList.get().isLoaded("irons_spellbooks"))
            throw new IllegalStateException("Iron's Spells não instalado.");
        config.enabled=on;save();reset();
        lastOutcome=on?"Macro armada. Acerte o mesmo alvo consecutivamente.":
            "Macro desativada pelo Port.";
    }
    static synchronized void setHits(int amount){
        load();
        if(amount<2||amount>6)throw new IllegalArgumentException("Use de 2 a 6 golpes.");
        config.hits=amount;save();reset();
        lastOutcome="Combo configurado para "+amount+" golpes no mesmo alvo.";
    }
    static synchronized void setWindow(int seconds){
        load();
        if(seconds<2||seconds>10)throw new IllegalArgumentException("Use de 2 a 10 segundos.");
        config.windowSeconds=seconds;save();reset();
        lastOutcome="Janela do combo: "+seconds+" segundos.";
    }
    static synchronized void setSpell(String spellId){
        load();
        String id=spellId==null?"":spellId.toLowerCase(java.util.Locale.ROOT).trim();
        if(!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
            throw new IllegalArgumentException("Use o identificador namespace:feitiço.");
        config.spell=id;save();reset();
        lastOutcome="Macro agora conjura "+id+" quando o combo é concluído.";
    }
    static synchronized void reset(){
        pending=null;STREAK.resetAll();
    }

    /** Registers a candidate only. The next client ticks must show a hurt animation. */
    static void onAttack(AttackEntityEvent event){
        Minecraft mc=Minecraft.getInstance();
        if(!enabled()||mc.player==null||event.getEntity()!=mc.player||
           mc.level==null||mc.screen!=null||!mc.isWindowActive())return;
        if(!(event.getTarget() instanceof LivingEntity living)||!living.isAlive()||
            living==mc.player)return;
        if(mc.player.isSpectator()||mc.player.distanceToSqr(living)>25||
            !mc.player.hasLineOfSight(living))return;
        // Ignore spam clicks: cooldown strength must be adequate for a melee hit.
        if(mc.player.getAttackStrengthScale(0.5f)<0.8f)return;
        long now=System.currentTimeMillis();
        pending=new Pending(living,living.hurtTime,now+700,mc.player.getUUID());
    }

    static void tick(){
        if(!enabled())return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.screen!=null||
            !mc.isWindowActive()||mc.player.isSpectator()){
            pending=null;STREAK.resetCombo();return;
        }
        Pending p=pending;
        if(p==null)return;
        long now=System.currentTimeMillis();
        LivingEntity target=p.target();
        if(now>p.expiresAt()||!target.isAlive()||target.level()!=mc.level||
           !p.playerId().equals(mc.player.getUUID())){
            pending=null;return;
        }
        // Hurt animation is an observation from the server-facing client state.
        // Unlike an AttackEntityEvent alone, it requires feedback after the hit.
        if(target.hurtTime<=p.hurtAtAttack()||target.hurtTime<5)return;
        pending=null;
        NexusMacroPolicy.Result result=NexusMacroPolicy.register(STREAK,
            target.getUUID(),now,hits(),NexusMacroPolicy.windowMs(windowSeconds()),
            NexusMacroPolicy.DEFAULT_REARM_MS);
        if(result.trigger()){
            attemptCast(mc.player,target);
        }else if(result.hits()>0&&now-lastInfoAt>250){
            lastInfoAt=now;
            lastOutcome="Combo "+result.hits()+"/"+hits()+
                " no alvo "+target.getName().getString()+".";
        }
    }

    private static void attemptCast(Player player,LivingEntity target){
        long now=System.currentTimeMillis();
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
        String spellId=spell();
        for(NexusVoiceGrammar.Spell entry:NexusVoiceCasting.equippedSpells(player))
            if(spellId.equals(entry.id())){option=entry;break;}
        if(option==null){
            lastOutcome="Macro: "+spellId+" não está equipado.";
            NexusCombatHud.show("macro",lastOutcome);return;
        }
        try{
            if(!canCast(player,option.slot(),spellId)){
                lastOutcome="Macro: mana, cooldown ou feitiço indisponível.";
                NexusCombatHud.show("macro",lastOutcome);return;
            }
            Class<?> packetType=Class.forName(
                "io.redspace.ironsspellbooks.network.casting.QuickCastPacket");
            Object payload=packetType.getConstructor(int.class).newInstance(option.slot());
            PacketDistributor.sendToServer((CustomPacketPayload)payload);
            lastOutcome="Combo concluído! "+spellId+" solicitado.";
            NexusCombatHud.show("macro",lastOutcome);
        }catch(Throwable e){
            lastOutcome="Macro não conseguiu solicitar o feitiço. Confira o Iron's Spells.";
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
