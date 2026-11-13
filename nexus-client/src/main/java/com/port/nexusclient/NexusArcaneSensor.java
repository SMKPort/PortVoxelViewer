package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashMap;

/**
 * Client-only optional compatibility with Iron's Spells n Spellbooks (1.21.1).
 * Uses public static client-synced methods reflectively so updating the addon or
 * starting Minecraft without the magic mod never causes a hard dependency crash.
 * This is OBSERVATION, not an actual mana/arm boost or server-side spell detector.
 */
final class NexusArcaneSensor {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("nexus-arcane.json");
    private static State state;
    private static Bridge bridge;
    private static boolean attempted;
    private static boolean previousCasting,lowManaArmed,lowManaWarned,baseline;
    private static long lastNearbyScan;
    private static final Map<java.util.UUID,String> knownNearby=new HashMap<>();
    private static final List<Nearby> nearNow=new ArrayList<>();
    private static String previousSpell="";
    private static double previousHoly=Double.NaN;
    private static long lastReaction,sessionCastCount;
    private static final long REACTION_COOLDOWN_MS=25000;
    static final class Nearby {
        String player,spellId,spellName,school;
        int level;
        double distance;
        Nearby(String player,String spellId,String spellName,String school,int level,double distance){
            this.player=player;this.spellId=spellId;this.spellName=spellName;
            this.school=school;this.level=level;this.distance=distance;
        }
    }
    static final class Entry {
        long at;
        String type,spellId,school,message;
        int level;
        Entry(long at,String type,String spellId,String school,String message,int level){
            this.at=at;this.type=type;this.spellId=spellId;this.school=school;
            this.message=message;this.level=level;
        }
    }
    private static final class State {
        boolean enabled=false;
        boolean reactions=false;
        boolean scanNearby=false;
        int sensitivity=22;
        long updatedAt;
        long totalCasts,totalNearby;
        String lastError="";
        List<Entry> events=new ArrayList<>();
    }
    private static final class Bridge {
        final Method mana,casting,spellId,spellLevel,completion,nearbyData;
        final Method lookupSpell;
        Bridge()throws ReflectiveOperationException{
            Class<?> magic=Class.forName("io.redspace.ironsspellbooks.player.ClientMagicData");
            mana=magic.getMethod("getPlayerMana");
            casting=magic.getMethod("isCasting");
            spellId=magic.getMethod("getCastingSpellId");
            spellLevel=magic.getMethod("getCastingSpellLevel");
            completion=magic.getMethod("getCastCompletionPercent");
            Method optional;
            try{optional=magic.getMethod("getSyncedSpellData",LivingEntity.class);}
            catch(NoSuchMethodException e){optional=null;}
            nearbyData=optional;
            Class<?> spells=Class.forName("io.redspace.ironsspellbooks.api.registry.SpellRegistry");
            lookupSpell=spells.getMethod("getSpell",String.class);
        }
        Object getNearby(Player player){
            if(nearbyData==null)return null;
            try{return nearbyData.invoke(null,player);}catch(Exception e){return null;}
        }
        boolean nearbyCasting(Object data){
            if(data==null)return false;
            try{return Boolean.TRUE.equals(data.getClass().getMethod("isCasting").invoke(data));}
            catch(Exception e){return false;}
        }
        String nearbySpell(Object data){
            try{return String.valueOf(data.getClass().getMethod("getCastingSpellId").invoke(data));}
            catch(Exception e){return "";}
        }
        int nearbyLevel(Object data){
            try{return ((Number)data.getClass().getMethod("getCastingSpellLevel").invoke(data)).intValue();}
            catch(Exception e){return 0;}
        }
        double mana(){try{return ((Number)mana.invoke(null)).doubleValue();}catch(Exception e){return -1;}}
        boolean casting(){try{return (Boolean)casting.invoke(null);}catch(Exception e){return false;}}
        String spellId(){try{return String.valueOf(spellId.invoke(null));}catch(Exception e){return "";}}
        int level(){try{return ((Number)spellLevel.invoke(null)).intValue();}catch(Exception e){return 0;}}
        double progress(){
            try{return NexusArcaneRules.bounded(((Number)completion.invoke(null)).doubleValue(),0,1,0);}
            catch(Exception e){return 0;}
        }
        String spellName(String spell){
            try{
                Object spellObject=lookupSpell.invoke(null,spell);
                Method displayName=spellObject.getClass().getMethod("getDisplayName",Player.class);
                Object component=displayName.invoke(spellObject,net.minecraft.client.Minecraft.getInstance().player);
                if(component instanceof net.minecraft.network.chat.Component c){
                    String label=c.getString().trim();
                    if(!label.isEmpty()&&label.length()<=70)return label;
                }
            }catch(Exception|LinkageError ignored){}
            return NexusArcaneRules.spellLabel(spell);
        }
        String school(String spell){
            try{
                Object spellObject=lookupSpell.invoke(null,spell);
                Object school=spellObject.getClass().getMethod("getSchoolType").invoke(spellObject);
                return String.valueOf(school.getClass().getMethod("getId").invoke(school))
                    .replaceAll("[^a-zA-Z0-9_:.-]","");
            }catch(Exception|LinkageError ignored){return "";}
        }
    }
    private static boolean available;
    private static double mana=-1,maxMana=-1,spellPower=-1,holyPower=-1,progress;
    private static boolean casting;
    private static String currentSpell="",currentName="",currentSchool="";
    private static int currentLevel;
    private static long readAt;
    private NexusArcaneSensor(){}
    private static void load(){
        if(state!=null)return;
        state=new State();
        if(!Files.isRegularFile(FILE))return;
        try{
            State old=JSON.fromJson(Files.readString(FILE,StandardCharsets.UTF_8),State.class);
            if(old!=null){
                state=old;
                if(state.events==null)state.events=new ArrayList<>();
                if(state.events.size()>80)
                    state.events=new ArrayList<>(state.events.subList(state.events.size()-80,state.events.size()));
                // Fail closed for interactive announcements on game launch.
                state.reactions=false;
            }
        }catch(Exception ex){System.err.println("Nexus Arcane: configuração inválida.");}
    }
    private static void persist(){
        try{
            Files.createDirectories(FILE.getParent());
            Path temp=FILE.resolveSibling(FILE.getFileName()+".tmp");
            Files.writeString(temp,JSON.toJson(state),StandardCharsets.UTF_8);
            try{Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException ex){Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING);}
        }catch(IOException ex){System.err.println("Nexus Arcane: falha ao salvar histórico.");}
    }
    private static void resetReadings(){
        available=false;mana=-1;maxMana=-1;spellPower=-1;holyPower=-1;
        progress=0;casting=false;currentSpell="";currentName="";currentSchool="";
        nearNow.clear();knownNearby.clear();lastNearbyScan=0;
        currentLevel=0;readAt=0;baseline=false;previousCasting=false;
        previousSpell="";previousHoly=Double.NaN;lowManaArmed=false;lowManaWarned=false;
    }
    static synchronized void resetSession(){resetReadings();bridge=null;attempted=false;sessionCastCount=0;}
    static synchronized boolean settingEnabled(){load();return state.enabled;}
    static synchronized boolean nearbyEnabled(){load();return state.scanNearby;}
    static synchronized boolean reactionsEnabled(){load();return state.reactions;}

    static synchronized void enabled(boolean on){
        load();state.enabled=on;
        if(!on)resetReadings();
        persist();
    }
    static synchronized void scanNearby(boolean on){
        load();state.scanNearby=on;
        nearNow.clear();knownNearby.clear();lastNearbyScan=0;
        persist();
    }
    static synchronized void reactions(boolean on){
        load();if(on&&!state.enabled)throw new IllegalStateException("Ative o Núcleo Arcano antes das reações.");
        state.reactions=on;persist();
    }
    static synchronized void configure(JsonObject data){
        load();
        if(!data.has("enabled")||!data.get("enabled").isJsonPrimitive()||
            !data.get("enabled").getAsJsonPrimitive().isBoolean()||
            !data.has("reactions")||!data.get("reactions").getAsJsonPrimitive().isBoolean())
            throw new IllegalArgumentException("Configuração arcana inválida.");
        boolean enabled=data.get("enabled").getAsBoolean();
        boolean reactions=data.get("reactions").getAsBoolean();
        if(reactions&&!enabled)throw new IllegalArgumentException("Ative o Núcleo antes das reações.");
        boolean nearby=data.has("scanNearby")&&data.get("scanNearby").getAsBoolean();
        if(state.scanNearby!=nearby){nearNow.clear();knownNearby.clear();lastNearbyScan=0;}
        state.enabled=enabled;state.reactions=reactions;state.scanNearby=nearby;
        if(!enabled)resetReadings();
        persist();
    }
    static synchronized String status(){
        load();
        return "Arcane Sensor "+(state.enabled?"ON":"OFF")+
            " | Iron's Spells "+(available?"DETECTADO":bridge!=null?"AGUARDANDO DADOS":"INDISPONÍVEL")+
            " | mana "+(maxMana>0?Math.round(mana)+"/"+Math.round(maxMana):"sem leitura")+
            " | Próximos "+(state.scanNearby?"ON":"OFF")+
            " | Conjurações "+state.totalCasts+
            " | avisos "+(state.reactions?"ON":"OFF");
    }
    static synchronized List<String> history(){
        load();List<String> result=new ArrayList<>();
        for(int i=state.events.size()-1;i>=0&&result.size()<7;i--)
            result.add("["+java.time.Instant.ofEpochMilli(state.events.get(i).at)+"] "+state.events.get(i).message);
        return result.isEmpty()?List.of("Nenhum acontecimento arcano registrado."):result;
    }
    static synchronized JsonObject snapshot(){
        load();
        JsonObject data=new JsonObject();
        data.addProperty("enabled",state.enabled);
        data.addProperty("reactions",state.reactions);
        data.addProperty("scanNearby",state.scanNearby);
        data.add("nearby",JSON.toJsonTree(state.enabled&&state.scanNearby?nearNow:List.of()));
        data.addProperty("present",state.enabled&&bridge!=null);
        data.addProperty("available",state.enabled&&available);
        data.addProperty("source","Iron's Spells n Spellbooks (cliente)");
        if(state.enabled&&available){
            if(mana>=0)data.addProperty("mana",mana);
            if(maxMana>0)data.addProperty("maxMana",maxMana);
            double ratio=NexusArcaneRules.percent(mana,maxMana);
            if(ratio>=0)data.addProperty("manaPercent",ratio);
            data.addProperty("casting",casting);
            data.addProperty("castProgress",progress);
            data.addProperty("spellId",currentSpell);
            data.addProperty("spellName",currentName);
            data.addProperty("school",currentSchool);
            data.addProperty("spellLevel",currentLevel);
            if(spellPower>=0)data.addProperty("spellPower",spellPower);
            if(holyPower>=0)data.addProperty("holyPower",holyPower);
        }
        data.addProperty("totalCasts",state.totalCasts);
        data.addProperty("totalNearby",state.totalNearby);
        data.addProperty("lastReadAt",readAt);
        JsonObject arm=NexusVitals.snapshot();
        boolean managed=arm.has("managed")&&arm.get("managed").getAsBoolean();
        boolean active=arm.has("enabled")&&arm.get("enabled").getAsBoolean();
        data.addProperty("armAvailable",managed&&active);
        if(managed&&active){
            data.addProperty("armIntegrity",arm.get("integrity").getAsDouble());
            data.addProperty("armEnergy",arm.get("energy").getAsDouble());
        }
        data.addProperty("lastError",state.enabled?state.lastError:"");
        data.add("events",JSON.toJsonTree(state.events));
        return data;
    }
    static synchronized void sample(Player player){
        load();
        if(!state.enabled||player==null)return;
        if(!ModList.get().isLoaded("irons_spellbooks")){
            state.lastError="Iron's Spells não está instalado.";
            resetReadings();return;
        }
        if(!attempted){
            attempted=true;
            try{bridge=new Bridge();state.lastError="";}
            catch(ReflectiveOperationException|LinkageError ex){
                bridge=null;state.lastError="API do Iron's Spells incompatível. Confira a versão.";
                System.err.println("Nexus Arcane: "+ex.getClass().getSimpleName()+": "+ex.getMessage());
                return;
            }
        }
        if(bridge==null)return;
        mana=bridge.mana();
        maxMana=attribute(player,"MAX_MANA");
        spellPower=attribute(player,"SPELL_POWER");
        holyPower=attribute(player,"HOLY_SPELL_POWER");
        casting=bridge.casting();
        currentSpell=casting?bridge.spellId():"";
        currentLevel=casting?Math.max(0,bridge.level()):0;
        progress=casting?bridge.progress():0;
        if(casting&&!currentSpell.isBlank()){
            if(!NexusArcaneRules.sameSpell(currentSpell,previousSpell)||!previousCasting){
                currentName=bridge.spellName(currentSpell);
                currentSchool=bridge.school(currentSpell);
            }
        }else{currentName="";currentSchool="";}
        long now=System.currentTimeMillis();readAt=now;
        available=mana>=0||maxMana>0||casting;
        if(!available){
            state.lastError="Aguardando a sincronização dos dados mágicos do servidor.";
            return;
        }
        state.lastError="";
        double percent=NexusArcaneRules.percent(mana,maxMana);
        if(!baseline){
            baseline=true;previousCasting=casting;previousSpell=currentSpell;previousHoly=holyPower;
            lowManaArmed=true;lowManaWarned=NexusArcaneRules.manaLow(percent);
            return;
        }
        if(casting&&(!previousCasting||!NexusArcaneRules.sameSpell(currentSpell,previousSpell))){
            state.totalCasts++;sessionCastCount++;
            addEvent("cast_start",currentSpell,currentSchool,currentLevel,
                "Conjuração iniciada: "+currentName+" (nível "+currentLevel+")",percent);
        }
        if(previousCasting&&(!casting||!NexusArcaneRules.sameSpell(currentSpell,previousSpell))){
            addEvent("cast_end",previousSpell,"",0,
                NexusArcaneRules.eventTitle("cast_end",previousSpell,"",0),percent);
        }
        if(percent>=0){
            if(!lowManaWarned&&NexusArcaneRules.manaLow(percent)){
                lowManaWarned=true;
                addEvent("mana_low","","",0,
                    "Reserva de mana baixa ("+Math.round(percent)+"%)",percent);
            }else if(lowManaWarned&&NexusArcaneRules.manaRecovered(percent)){
                lowManaWarned=false;
                addEvent("mana_recovered","","",0,
                    "Mana recuperada ("+Math.round(percent)+"%)",percent);
            }
        }
        if(holyPower>=0&&Double.isFinite(previousHoly)&&previousHoly>=0&&
            Math.abs(holyPower-previousHoly)>=0.15){
            addEvent("holy_power","","holy",0,
                "Poder Holy alterado: "+String.format(Locale.ROOT,"%.2f",previousHoly)+
                    " → "+String.format(Locale.ROOT,"%.2f",holyPower),percent);
        }
        if(state.scanNearby&&now-lastNearbyScan>=500){lastNearbyScan=now;scan(player,percent);}
        previousCasting=casting;previousSpell=currentSpell;previousHoly=holyPower;
    }
    private static void scan(Player player,double manaPercentage){
        nearNow.clear();
        Map<java.util.UUID,String> current=new HashMap<>();
        if(bridge==null||bridge.nearbyData==null)return;
        for(Player other:player.level().players()){
            if(other==null||other==player||other.isRemoved()||
                player.distanceToSqr(other)>576||nearNow.size()>=12)continue;
            Object synced=bridge.getNearby(other);
            if(!bridge.nearbyCasting(synced))continue;
            String spell=bridge.nearbySpell(synced);
            if(!spell.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||spell.length()>90)continue;
            String name=other.getGameProfile().getName();
            if(name==null||!name.matches("[A-Za-z0-9_]{3,16}"))continue;
            current.put(other.getUUID(),spell);
            String school=bridge.school(spell),label=bridge.spellName(spell);
            int level=Math.max(0,bridge.nearbyLevel(synced));
            double distance=Math.sqrt(player.distanceToSqr(other));
            nearNow.add(new Nearby(name,spell,label,school,level,distance));
            if(!NexusArcaneRules.sameSpell(knownNearby.get(other.getUUID()),spell)){
                state.totalNearby++;
                addEvent("nearby_cast",spell,school,level,
                    "Conjuração próxima: "+name+" canaliza "+label+" ("+
                    Math.round(distance)+" blocos)",manaPercentage);
            }
        }
        knownNearby.clear();
        knownNearby.putAll(current);
    }
    private static double attribute(Player player,String field){
        try{
            Class<?> attributes=Class.forName("io.redspace.ironsspellbooks.api.registry.AttributeRegistry");
            Object raw=attributes.getField(field).get(null);
            if(raw instanceof Holder<?> holder){
                @SuppressWarnings("unchecked")
                Holder<Attribute> typed=(Holder<Attribute>)holder;
                return NexusArcaneRules.bounded(player.getAttributeValue(typed),0,1000000,-1);
            }
        }catch(ReflectiveOperationException|LinkageError|ClassCastException ignored){}
        return -1;
    }
    private static void addEvent(String type,String spellId,String school,int level,String message,double percent){
        long now=System.currentTimeMillis();
        state.events.add(new Entry(now,type,spellId,school,message,level));
        NexusPrivateMind.noticeArcane(type,message);
        while(state.events.size()>80)state.events.remove(0);
        if(ClientConfig.allowMind&&(type.equals("cast_start")||type.equals("mana_low")||type.equals("holy_power")))
            NexusSocial.onArcaneEvent(type,message,now);
        state.updatedAt=now;
        persist();
        if(state.reactions&&now-lastReaction>REACTION_COOLDOWN_MS){
            String rp=NexusArcaneRules.reply(type,spellId,percent);
            if(!rp.isEmpty()&&ClientLink.speak(rp,"mind"))lastReaction=now;
        }
    }
}
