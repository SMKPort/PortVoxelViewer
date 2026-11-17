package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Explicitly authorized, client-side upkeep of several equipped self-buff spells.
 * Each spell learns its own ACTUAL positive effects after a manual cast.
 *
 * The old single-Blessing configuration is migrated to schema 2. Never assume
 * "Blessing of Life" applies a persistent effect and never guess an effect ID.
 */
final class NexusBlessing {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("nexus-blessing.json");
    private static final int MAX_SPELLS=NexusUpkeepRosterPolicy.MAX_SPELLS;
    private static final long GLOBAL_CAST_GAP_MS=4_000L;

    private static final class Rule {
        String spellId="";
        String realm="";
        int thresholdSeconds=NexusBlessingPolicy.DEFAULT_SECONDS;
        boolean enabled=false;
        List<String> effects=new ArrayList<>();
    }
    private static final class Config {
        int schema=2;
        String selectedId="";
        List<Rule> spells=new ArrayList<>();
    }
    /** The original alpha.3 layout; values and enabled state are retained. */
    private static final class Legacy {
        int schema=1;
        String spellId="";
        String realm="";
        int thresholdSeconds=NexusBlessingPolicy.DEFAULT_SECONDS;
        boolean enabled=false;
        List<String> effects=new ArrayList<>();
    }
    record View(String spellId,boolean enabled,int thresholdSeconds,
                List<String> effects,boolean learnedHere) {}

    private record Learning(String spellId,Map<String,Integer> before,
                            Set<String> found,String realm,Object level,
                            long endAt,long firstObserved) {
        Learning withObserved(Set<String> effects,long at){
            return new Learning(spellId,before,effects,realm,level,endAt,at);
        }
    }
    private enum Preflight { READY, MANA, COOLDOWN, UNKNOWN }

    private static Config config;
    private static Learning learning;
    private static boolean readOnly;
    private static long lastCheckAt,lastAnyAttemptAt;
    private static final Map<String,Long> LAST_ATTEMPTS=new HashMap<>();
    private static String status="Magias de manutenção desligadas.";
    private NexusBlessing(){}

    private static synchronized void load(){
        if(config!=null)return;
        config=new Config();
        if(!Files.isRegularFile(FILE))return;
        try {
            String source=Files.readString(FILE,StandardCharsets.UTF_8);
            JsonObject object=JsonParser.parseString(source).getAsJsonObject();
            int version=object.get("schema").getAsInt();
            Config loaded;
            if(version==1){
                Legacy old=JSON.fromJson(source,Legacy.class);
                if(old==null||old.spellId==null||old.realm==null||old.effects==null)
                    throw new IllegalArgumentException("Schema legado inválido.");
                loaded=new Config();
                if(!old.spellId.isBlank()){
                    Rule migrated=new Rule();
                    migrated.spellId=old.spellId;
                    migrated.realm=old.realm;
                    migrated.thresholdSeconds=old.thresholdSeconds;
                    migrated.enabled=old.enabled;
                    migrated.effects=new ArrayList<>(old.effects);
                    loaded.spells.add(migrated);
                    loaded.selectedId=old.spellId;
                }
                // Copy original before the first schema 2 write.
                Path backup=FILE.resolveSibling("nexus-blessing-v1-backup.json");
                if(!Files.exists(backup))
                    Files.copy(FILE,backup,StandardCopyOption.COPY_ATTRIBUTES);
                status="Blessing existente migrado para múltiplas magias.";
            }else if(version==2){
                loaded=JSON.fromJson(source,Config.class);
            }else throw new IllegalArgumentException("Versão de configuração desconhecida.");
            validate(loaded);
            config=loaded;
        }catch(Exception error){
            config=new Config();readOnly=true;
            status="Arquivo de magias inválido. Original preservado, renovação bloqueada.";
        }
    }
    private static void validate(Config input){
        if(input==null||input.spells==null||input.selectedId==null||
            input.spells.size()>MAX_SPELLS)
            throw new IllegalArgumentException("Configuração de magias inválida.");
        Set<String> seen=new LinkedHashSet<>();
        for(Rule rule:input.spells){
            if(rule==null||rule.spellId==null||rule.realm==null||rule.effects==null||
               !NexusBlessingPolicy.plausibleSpellId(rule.spellId)||
               rule.effects.size()>8||rule.effects.stream().anyMatch(effect->
                    !NexusBlessingPolicy.plausibleSpellId(effect))||
               !seen.add(rule.spellId))
                throw new IllegalArgumentException("Magia duplicada ou configuração inválida.");
            rule.thresholdSeconds=NexusBlessingPolicy.seconds(rule.thresholdSeconds);
            rule.effects=new ArrayList<>(new LinkedHashSet<>(rule.effects));
            // A learned effect must have a realm before it can be authorized.
            if(rule.enabled&&(rule.effects.isEmpty()||rule.realm.isBlank()))
                rule.enabled=false;
        }
        if(!input.selectedId.isBlank()&&!seen.contains(input.selectedId))
            input.selectedId="";
        if(input.selectedId.isBlank()&&!input.spells.isEmpty())
            input.selectedId=input.spells.getFirst().spellId;
    }
    private static synchronized void save(){
        if(readOnly)throw new IllegalStateException("Arquivo inválido. Corrija antes de salvar.");
        try{
            Files.createDirectories(FILE.getParent());
            Path tmp=FILE.resolveSibling(FILE.getFileName()+".tmp");
            Files.writeString(tmp,JSON.toJson(config),StandardCharsets.UTF_8);
            try{
                Files.move(tmp,FILE,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
            }catch(AtomicMoveNotSupportedException error){
                Files.move(tmp,FILE,StandardCopyOption.REPLACE_EXISTING);
            }
        }catch(Exception error){
            throw new IllegalStateException("Não consegui salvar nexus-blessing.json.",error);
        }
    }
    private static String realm(){
        Minecraft mc=Minecraft.getInstance();
        String label;
        if(mc.getCurrentServer()!=null)
            label="multi:"+mc.getCurrentServer().ip.toLowerCase(Locale.ROOT);
        else if(mc.getSingleplayerServer()!=null)
            label="single:"+mc.getSingleplayerServer().getWorldData().getLevelName();
        else return "";
        try{
            byte[] hash=MessageDigest.getInstance("SHA-256")
                .digest(label.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        }catch(Exception error){return "";}
    }
    private static Rule selected(){
        if(config==null)return null;
        for(Rule rule:config.spells)
            if(rule.spellId.equals(config.selectedId))return rule;
        return null;
    }
    private static Rule find(String id){
        if(config==null||id==null)return null;
        for(Rule rule:config.spells)
            if(rule.spellId.equals(id))return rule;
        return null;
    }
    static synchronized boolean enabled(){
        load();
        Rule rule=selected();
        return rule!=null&&rule.enabled;
    }
    static synchronized int enabledCount(){
        load();
        return (int)config.spells.stream().filter(rule->rule.enabled).count();
    }
    static synchronized int thresholdSeconds(){
        load();
        Rule rule=selected();
        return rule==null?NexusBlessingPolicy.DEFAULT_SECONDS:rule.thresholdSeconds;
    }
    static synchronized String spellId(){load();return config.selectedId;}
    static synchronized List<String> trackedEffects(){
        load();
        Rule rule=selected();
        return rule==null?List.of():List.copyOf(rule.effects);
    }
    static synchronized List<View> entries(){
        load();
        String currentRealm=realm();
        return config.spells.stream().map(rule->new View(rule.spellId,rule.enabled,
            rule.thresholdSeconds,List.copyOf(rule.effects),
            !currentRealm.isBlank()&&currentRealm.equals(rule.realm))).toList();
    }
    static synchronized String status(){load();return status;}
    static synchronized String summary(){
        load();
        String learningInfo=learning==null?"":" | Aprendendo "+learning.spellId();
        return "Magias de manutenção "+enabledCount()+"/"+config.spells.size()+
            " ativas | Selecionada: "+(config.selectedId.isBlank()?"nenhuma":config.selectedId)+
            learningInfo+" | "+status;
    }

    /** May include every spell; only ones truly equipped can be added. */
    static synchronized List<String> candidates(){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null)return List.of();
        return NexusVoiceCasting.equippedSpells(mc.player).stream()
            .map(NexusVoiceGrammar.Spell::id).distinct().toList();
    }
    private static boolean equipped(Player player,String id){
        return NexusVoiceCasting.equippedSpells(player).stream()
            .anyMatch(spell->spell.id().equals(id));
    }

    /** New button and /blessing add: add without touching existing rules. */
    static synchronized String addSpell(String spellId){
        load();
        if(readOnly)return status;
        if(!NexusBlessingPolicy.plausibleSpellId(spellId))
            return status="Use o ID completo namespace:magia.";
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||!ModList.get().isLoaded("irons_spellbooks"))
            return status="Entre no jogo com Iron's Spells instalado.";
        if(!equipped(mc.player,spellId))
            return status="Magia não está equipada: "+spellId;
        Rule existing=find(spellId);
        if(existing!=null){
            config.selectedId=spellId;
            save();
            return status="Magia já cadastrada. Selecionada: "+spellId;
        }
        if(!NexusUpkeepRosterPolicy.mayAdd(spellId,
            config.spells.stream().map(rule->rule.spellId).toList(),candidates()))
            return status="Limite, duplicata ou magia não equipada.";

        Rule rule=new Rule();
        rule.spellId=spellId;
        config.spells.add(rule);
        config.selectedId=spellId;
        save();
        return status="Adicionada "+spellId+". Aprenda seu efeito antes de ativar.";
    }
    /** Preserve the previous alpha.3 command spelling. */
    static synchronized String setSpell(String spellId){return addSpell(spellId);}

    static synchronized String selectSpell(String spellId){
        load();
        if(find(spellId)==null)return status="Magia não cadastrada: "+spellId;
        config.selectedId=spellId;
        save();
        return status="Selecionada "+spellId+".";
    }

    static synchronized String removeSpell(String spellId){
        load();
        if(readOnly)return status;
        Rule rule=find(spellId);
        if(rule==null)return status="Magia não encontrada.";
        config.spells.remove(rule);
        LAST_ATTEMPTS.remove(spellId);
        if(learning!=null&&spellId.equals(learning.spellId()))learning=null;
        if(spellId.equals(config.selectedId))
            config.selectedId=config.spells.isEmpty()?"":config.spells.getFirst().spellId;
        save();
        return status="Removida "+spellId+". Outras magias preservadas.";
    }
    static synchronized String setThreshold(int seconds){
        load();
        Rule rule=selected();
        if(rule==null)return status="Selecione uma magia primeiro.";
        if(seconds<NexusBlessingPolicy.MIN_SECONDS||seconds>NexusBlessingPolicy.MAX_SECONDS)
            return status="Escolha de 3 a 60 segundos.";
        rule.thresholdSeconds=seconds;
        save();
        return status=rule.spellId+": renovar a "+seconds+"s do fim.";
    }

    /** Alpha.3 /blessing off and global permissions off disable ALL rules. */
    static synchronized String setEnabled(boolean on){
        load();
        if(readOnly)return status;
        if(!on){
            for(Rule rule:config.spells)rule.enabled=false;
            learning=null;
            save();
            return status="Todas as renovações automáticas desativadas.";
        }
        return setSelectedEnabled(true);
    }
    /** Per-row power switch; OFF affects only the selected spell. */
    static synchronized String setSelectedEnabled(boolean on){
        load();
        if(readOnly)return status;
        Rule rule=selected();
        if(rule==null)return status="Selecione uma magia primeiro.";
        if(!on){
            rule.enabled=false;
            save();
            return status=rule.spellId+": renovação desligada.";
        }
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||!ModList.get().isLoaded("irons_spellbooks"))
            return status="Entre no mundo com Iron's Spells.";
        if(!equipped(mc.player,rule.spellId))
            return status="Equipe "+rule.spellId+" primeiro.";
        if(rule.effects.isEmpty())
            return status="Aprenda o efeito de "+rule.spellId+" antes de ligar.";
        if(!NexusUpkeepRosterPolicy.mayEnable(true,rule.effects,rule.realm,realm()))
            return status="Aprenda "+rule.spellId+" neste servidor/mundo antes de ligar.";
        rule.enabled=true;
        save();
        return status="Renovação ativada para "+rule.spellId+
            " ("+rule.thresholdSeconds+"s antes do fim).";
    }

    private static String uniqueBlessing(Player player){
        var matches=NexusVoiceCasting.equippedSpells(player).stream()
            .filter(spell->spell.id().endsWith(":blessing")).toList();
        return matches.size()==1?matches.getFirst().id():"";
    }
    /** Captures positive effects after the user manually casts the SELECTED spell. */
    static synchronized String learn(){
        load();
        Minecraft mc=Minecraft.getInstance();
        if(readOnly)return status;
        if(mc.player==null||mc.level==null||!ModList.get().isLoaded("irons_spellbooks"))
            return status="Entre no mundo com Iron's Spells.";
        if(selected()==null){
            String detected=uniqueBlessing(mc.player);
            if(!detected.isBlank()){
                addSpell(detected);
            }
        }
        Rule rule=selected();
        if(rule==null)
            return status="Adicione uma magia pelo botão + ou /nexusclient blessing add <id>.";
        if(!equipped(mc.player,rule.spellId))
            return status="Equipe "+rule.spellId+" antes de aprender.";
        rule.enabled=false;
        save();
        long now=System.currentTimeMillis();
        learning=new Learning(rule.spellId,effects(mc.player),new LinkedHashSet<>(),
            realm(),mc.level,now+NexusBlessingPolicy.LEARN_WINDOW_MS,0);
        return status="Aprendendo "+rule.spellId+
            ": conjure a magia manualmente em você nos próximos 30s.";
    }
    private static Map<String,Integer> effects(Player player){
        Map<String,Integer> current=new LinkedHashMap<>();
        for(MobEffectInstance instance:player.getActiveEffects()){
            if(!instance.getEffect().value().isBeneficial())continue;
            var id=BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect().value());
            if(id!=null)current.put(id.toString(),instance.getDuration());
        }
        return current;
    }
    static synchronized List<String> effectsStatus(){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null)return List.of("Entre no mundo para ver os efeitos.");
        Map<String,Integer> active=effects(mc.player);
        List<String> result=new ArrayList<>();
        for(var item:active.entrySet())result.add(item.getKey()+" : "+
            Math.max(0,item.getValue()/20)+"s");
        return result.isEmpty()?List.of("Nenhum efeito positivo visível."):result;
    }
    static synchronized String selectedRemaining(){
        load();
        Rule rule=selected();
        if(rule==null||rule.effects.isEmpty())return "Efeitos: nenhum aprendido.";
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null)return "Entre no mundo para acompanhar efeitos.";
        Map<String,Integer> active=effects(mc.player);
        List<String> result=new ArrayList<>();
        for(String effect:rule.effects){
            int duration=active.getOrDefault(effect,-1);
            result.add(effect.substring(effect.indexOf(':')+1)+" "+
                (duration<0?"ausente":(Math.max(0,duration/20)+"s")));
        }
        return String.join(" | ",result);
    }
    static synchronized String trackEffect(String id){
        load();
        if(!NexusBlessingPolicy.plausibleSpellId(id))return status="ID de efeito inválido.";
        Minecraft mc=Minecraft.getInstance();
        Rule rule=selected();
        if(rule==null||mc.player==null||mc.level==null)
            return status="Selecione uma magia equipada primeiro.";
        if(!equipped(mc.player,rule.spellId))
            return status="Equipe a magia antes de vinculá-la.";
        if(!effects(mc.player).containsKey(id))
            return status="Esse efeito positivo não está ativo no jogador.";
        if(readOnly)return status;
        rule.enabled=false;
        rule.realm=realm();
        rule.effects=new ArrayList<>(List.of(id));
        if(learning!=null&&rule.spellId.equals(learning.spellId()))learning=null;
        save();
        return status=id+" associado à magia "+rule.spellId+". Ative após confirmar.";
    }

    static synchronized void tick(){
        load();
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null){learning=null;lastCheckAt=0;return;}
        long now=System.currentTimeMillis();
        if(now-lastCheckAt<NexusBlessingPolicy.CHECK_INTERVAL_MS)return;
        lastCheckAt=now;
        Player player=mc.player;
        Map<String,Integer> current=effects(player);
        if(learning!=null){
            Learning task=learning;
            if(!task.realm().equals(realm())||task.level()!=mc.level||
               find(task.spellId())==null){
                learning=null;
                status="Aprendizado cancelado: magia ou mundo mudou.";
                return;
            }
            Set<String> discovered=new LinkedHashSet<>(task.found());
            for(var effect:current.entrySet()){
                int previous=task.before().getOrDefault(effect.getKey(),-1);
                if(NexusBlessingPolicy.learned(previous,effect.getValue()))
                    discovered.add(effect.getKey());
            }
            long first=task.firstObserved();
            if(!discovered.isEmpty()&&first==0)first=now;
            learning=task.withObserved(discovered,first);
            if(now>=task.endAt()||(first>0&&now-first>=3_000L)){
                learning=null;
                Rule rule=find(task.spellId());
                if(discovered.isEmpty()){
                    status="Nenhum efeito novo/renovado detectado para "+task.spellId()+".";
                }else if(discovered.size()>8){
                    status="Muitos efeitos simultâneos. Aprendizado descartado.";
                }else{
                    rule.effects=new ArrayList<>(discovered);
                    rule.realm=realm();
                    rule.enabled=false;
                    save();
                    status="Aprendi "+task.spellId()+": "+
                        String.join(", ",discovered)+". Ative individualmente.";
                    NexusClientMod.notifyPlayer("[Nexus / Magias] "+status);
                }
            }
            return; // Learning cannot also cast another upkeep spell.
        }
        if(config.spells.stream().noneMatch(rule->rule.enabled))return;
        if(mc.screen!=null||!mc.isWindowActive()||player.isSpectator()||
           !player.isAlive()||!ModList.get().isLoaded("irons_spellbooks")||
           !NexusUpkeepRosterPolicy.mayDispatch(now,lastAnyAttemptAt,
               GLOBAL_CAST_GAP_MS,NexusVoiceCasting.clientIsCasting(),
               player.isUsingItem()))return;
        String scope=realm();
        if(scope.isBlank())return;
        // Read equipped list once per second, not once for every tracked spell.
        List<NexusVoiceGrammar.Spell> equipped=NexusVoiceCasting.equippedSpells(player);
        for(Rule rule:config.spells){
            if(!rule.enabled||rule.effects.isEmpty()||!scope.equals(rule.realm))
                continue;
            List<Integer> durations=new ArrayList<>();
            for(String effect:rule.effects)durations.add(current.getOrDefault(effect,-1));
            boolean expiring=NexusBlessingPolicy.needsRefresh(durations,rule.thresholdSeconds);
            long previous=LAST_ATTEMPTS.getOrDefault(rule.spellId,0L);
            NexusVoiceGrammar.Spell entry=null;
            for(var spell:equipped)
                if(rule.spellId.equals(spell.id())){entry=spell;break;}
            if(!NexusBlessingPolicy.allowed(true,true,false,entry!=null,
                true,expiring,now,previous))continue;
            Preflight available=checkCast(player,entry.slot(),rule.spellId);
            if(available!=Preflight.READY){
                status=switch(available){
                    case COOLDOWN -> rule.spellId+" em cooldown. Aguardando.";
                    case MANA -> "Mana insuficiente para "+rule.spellId+".";
                    default -> "Não consegui validar "+rule.spellId+". Sem conjuração.";
                };
                continue;
            }
            LAST_ATTEMPTS.put(rule.spellId,now);
            lastAnyAttemptAt=now;
            try{
                Class<?> packet=Class.forName(
                    "io.redspace.ironsspellbooks.network.casting.QuickCastPacket");
                Object payload=packet.getConstructor(int.class).newInstance(entry.slot());
                PacketDistributor.sendToServer((CustomPacketPayload)payload);
                status=rule.spellId+": efeito acabando. Renovação solicitada.";
                NexusCombatHud.show("blessing",status);
                NexusClientMod.notifyPlayer("[Nexus / Magias] "+status);
            }catch(Throwable error){
                status=rule.spellId+": erro ao solicitar conjuração.";
                NexusCombatHud.show("blessing",status);
            }
            break; // At most one upkeep QuickCast in each tick.
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
            Method getLevel=spellType.getMethod("getLevelFor",int.class,LivingEntity.class);
            int level=((Number)getLevel.invoke(spell,baseLevel,player)).intValue();
            int cost=((Number)spellType.getMethod("getManaCost",int.class)
                .invoke(spell,level)).intValue();
            int mana=((Number)magicData.getMethod("getPlayerMana").invoke(null)).intValue();
            return mana>=cost?Preflight.READY:Preflight.MANA;
        }catch(Throwable error){return Preflight.UNKNOWN;}
    }
}
