package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.lang.reflect.Method;

/**
 * Local opt-in safety automation for Iron's Spells self-heal.
 * Sends only the normal server-validated QuickCastPacket, never chat,
 * microphone data or paid AI requests.
 */
final class NexusAutoHeal {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("nexus-auto-heal.json");
    private static final String SPELL="irons_spellbooks:heal";
    private static final class Config {
        int schema=1;
        boolean enabled=false; // never enabled automatically by "permissions all"
        int thresholdPercent=NexusAutoHealPolicy.DEFAULT_PERCENT;
    }
    private static Config config;
    private static long lastAttemptAt=0,lastCheckAt=0;
    private static String lastOutcome="Aguardando autorização do Port.";
    private static int lastSlot=-1;

    private NexusAutoHeal(){}

    private static synchronized void load(){
        if(config!=null)return;
        config=new Config();
        if(!Files.isRegularFile(FILE))return;
        try{
            Config saved=JSON.fromJson(Files.readString(FILE,StandardCharsets.UTF_8),Config.class);
            if(saved!=null&&saved.schema==1){
                saved.thresholdPercent=NexusAutoHealPolicy.clampPercent(saved.thresholdPercent);
                config=saved;
            }
        }catch(Exception ex){
            lastOutcome="Configuração de cura inválida. Proteção desligada.";
        }
    }

    private static synchronized void save(){
        try{
            Files.createDirectories(FILE.getParent());
            Path tmp=FILE.resolveSibling(FILE.getFileName()+".tmp");
            Files.writeString(tmp,JSON.toJson(config),StandardCharsets.UTF_8);
            try{Files.move(tmp,FILE,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException ex){Files.move(tmp,FILE,StandardCopyOption.REPLACE_EXISTING);}
        }catch(Exception ex){
            throw new IllegalStateException("Não foi possível salvar a permissão da cura automática.",ex);
        }
    }

    static synchronized boolean enabled(){load();return config.enabled;}
    static synchronized int threshold(){load();return config.thresholdPercent;}
    static synchronized String lastOutcome(){load();return lastOutcome;}
    static synchronized int lastSlot(){return lastSlot;}

    static synchronized void setEnabled(boolean enabled){
        load();
        if(enabled&&!ModList.get().isLoaded("irons_spellbooks"))
            throw new IllegalStateException("Instale o Iron's Spells para usar Heal.");
        config.enabled=enabled;
        save();
        lastOutcome=enabled?
            "Cura automática armada. O Nexus aguardará vida baixa.":
            "Cura automática desativada pelo Port.";
        lastCheckAt=0;
    }

    static synchronized void setThreshold(int percent){
        load();
        if(percent<NexusAutoHealPolicy.MIN_PERCENT||percent>NexusAutoHealPolicy.MAX_PERCENT)
            throw new IllegalArgumentException("Escolha de 10% a 75% da vida máxima.");
        config.thresholdPercent=NexusAutoHealPolicy.clampPercent(percent);
        save();
        lastOutcome="Cura automática ajustada para "+config.thresholdPercent+"% de vida.";
    }

    static void tick(){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null)return;
        if(!enabled())return;
        long now=System.currentTimeMillis();
        if(now-lastCheckAt<1000)return;
        lastCheckAt=now;

        Player player=mc.player;
        boolean active=mc.screen==null&&mc.isWindowActive()&&player.isAlive()&&
            !player.isSpectator()&&ModList.get().isLoaded("irons_spellbooks");
        boolean casting=NexusVoiceCasting.clientIsCasting();
        if(!NexusAutoHealPolicy.shouldAttempt(true,active,casting,
            player.getHealth(),player.getMaxHealth(),threshold(),now,lastAttemptAt))return;

        // The short, instant 'heal' is intentionally the only automatic spell.
        // Greater Heal is a long cast and is not safe to launch opportunistically.
        NexusVoiceGrammar.Spell heal=null;
        for(NexusVoiceGrammar.Spell equipped:NexusVoiceCasting.equippedSpells(player)){
            if(SPELL.equals(equipped.id())){heal=equipped;break;}
        }
        if(heal==null){
            lastOutcome="Heal não está equipado; nenhuma magia foi executada.";
            return;
        }

        try{
            Check check=preflight(player,heal.slot());
            if(check==Check.COOLDOWN){
                lastOutcome="Heal em cooldown. Aguardando o Iron's Spells.";
                return;
            }
            if(check==Check.MANA){
                lastOutcome="Mana insuficiente para Heal. Sem gasto de magia.";
                return;
            }
            if(check!=Check.READY){
                lastOutcome="Não foi possível validar Heal; conjuração evitada.";
                return;
            }

            lastAttemptAt=now; // rate limit BEFORE network dispatch
            lastSlot=heal.slot();
            Class<?> packetClass=Class.forName(
                "io.redspace.ironsspellbooks.network.casting.QuickCastPacket");
            Object packet=packetClass.getConstructor(int.class).newInstance(lastSlot);
            PacketDistributor.sendToServer((CustomPacketPayload)packet);
            lastOutcome="Vida crítica: Heal solicitado (slot "+lastSlot+").";
            NexusCombatHud.show("autoheal",lastOutcome);
        }catch(Throwable ex){
            lastOutcome="Falha ao solicitar Heal. Confira a versão do Iron's Spells.";
            // No retry storm after an unexpected protocol failure.
            lastAttemptAt=now;
            NexusCombatHud.show("autoheal",lastOutcome);
        }
    }

    private enum Check { READY, MANA, COOLDOWN, UNKNOWN }

    /** Conservative client preflight; the Minecraft server is still authoritative. */
    private static Check preflight(Player player,int slot){
        try{
            Class<?> managerType=Class.forName(
                "io.redspace.ironsspellbooks.api.magic.SpellSelectionManager");
            Class<?> dataType=Class.forName(
                "io.redspace.ironsspellbooks.api.spells.SpellData");
            Class<?> spellType=Class.forName(
                "io.redspace.ironsspellbooks.api.spells.AbstractSpell");
            Object manager=managerType.getConstructor(Player.class).newInstance(player);
            Object data=managerType.getMethod("getSpellData",int.class).invoke(manager,slot);
            if(data==null)return Check.UNKNOWN;
            Object spell=dataType.getMethod("getSpell").invoke(data);
            if(spell==null||!SPELL.equals(
                String.valueOf(spellType.getMethod("getSpellId").invoke(spell))))
                return Check.UNKNOWN;

            Class<?> clientData=Class.forName(
                "io.redspace.ironsspellbooks.player.ClientMagicData");
            Object cooldowns=clientData.getMethod("getCooldowns").invoke(null);
            if(cooldowns==null)return Check.UNKNOWN;
            boolean cooling=Boolean.TRUE.equals(
                cooldowns.getClass().getMethod("isOnCooldown",spellType)
                    .invoke(cooldowns,spell));
            if(cooling)return Check.COOLDOWN;

            int level=((Number)dataType.getMethod("getLevel").invoke(data)).intValue();
            Method normalizedLevel=spellType.getMethod("getLevelFor",int.class,LivingEntity.class);
            int effective=((Number)normalizedLevel.invoke(spell,level,player)).intValue();
            int cost=((Number)spellType.getMethod("getManaCost",int.class)
                .invoke(spell,effective)).intValue();
            int available=((Number)clientData.getMethod("getPlayerMana").invoke(null)).intValue();
            return available>=cost?Check.READY:Check.MANA;
        }catch(Throwable ex){
            return Check.UNKNOWN; // Fail closed if the modpack API differs.
        }
    }
}
