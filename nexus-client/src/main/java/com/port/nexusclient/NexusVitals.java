package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Owns RP integrity and energy on the Minecraft client, not in a browser tab.
 * No game/server authority. Only local narrative state is changed here.
 */
final class NexusVitals {
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH=FMLPaths.CONFIGDIR.get().resolve("nexus-vitals.json");
    private static State state;

    private static final class Plate {
        String key;
        double integrity;
        boolean integrated;
        Plate(String key,double integrity,boolean integrated){
            this.key=key;this.integrity=integrity;this.integrated=integrated;
        }
    }
    private static final class State {
        int schema=1;
        boolean configured=false;
        boolean enabled=false;
        String player="";
        double integrity=100;
        double energy=85;
        double damageFactor=5;
        double healIntegrity=5;
        double healEnergy=3;
        int cursor=0;
        long updatedAt=0;
        String lastEvent="";
        List<Plate> segments=new ArrayList<>();
    }
    private NexusVitals(){}
    private static double clamp(double value,double min,double max,double fallback){
        return Double.isFinite(value)?Math.min(max,Math.max(min,value)):fallback;
    }
    private static double number(JsonObject data,String key,double fallback,double min,double max){
        try{
            if(!data.has(key)||data.get(key).isJsonNull())return fallback;
            return clamp(data.get(key).getAsDouble(),min,max,fallback);
        }catch(Exception error){return fallback;}
    }
    private static String name(JsonObject data,String key,String fallback){
        try{return data.has(key)?data.get(key).getAsString():fallback;}
        catch(Exception error){return fallback;}
    }
    private static void load(){
        if(state!=null)return;
        state=new State();
        if(!Files.isRegularFile(PATH))return;
        try{
            State saved=GSON.fromJson(Files.readString(PATH,StandardCharsets.UTF_8),State.class);
            if(saved!=null&&saved.schema==1){
                state=saved;
                if(state.segments==null)state.segments=new ArrayList<>();
                state.integrity=clamp(state.integrity,0,100,100);
                state.energy=clamp(state.energy,0,100,85);
                state.damageFactor=clamp(state.damageFactor,1,10,5);
                state.healIntegrity=clamp(state.healIntegrity,0,10,5);
                state.healEnergy=clamp(state.healEnergy,0,15,3);
                if(state.player==null)state.player="";
            }
        }catch(Exception ex){
            System.err.println("Nexus Vitals: não foi possível carregar estado local: "+ex.getMessage());
        }
    }
    private static void save(){
        try{
            Files.createDirectories(PATH.getParent());
            Path tmp=PATH.resolveSibling(PATH.getFileName()+".tmp");
            Files.writeString(tmp,GSON.toJson(state),StandardCharsets.UTF_8);
            try{Files.move(tmp,PATH,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException ex){
                Files.move(tmp,PATH,StandardCopyOption.REPLACE_EXISTING);
            }
        }catch(IOException ex){
            System.err.println("Nexus Vitals: não foi possível salvar estado local: "+ex.getMessage());
        }
    }
    static boolean isConfigured(){load();return state.configured;}
    static boolean isEnabled(){load();return state.enabled;}
    static double integrity(){load();return state.integrity;}
    static String status(){
        load();
        return "Integridade RP local: "+Math.round(state.integrity)+"% | energia "+
            Math.round(state.energy)+"% | monitor "+(state.enabled?"ON":"OFF")+
            (state.configured?" | salvo em config/nexus-vitals.json":" | configure no painel");
    }
    static String configure(JsonObject input,String player){
        load();
        if(player==null||!player.matches("[A-Za-z0-9_]{3,16}"))
            throw new IllegalArgumentException("Jogador inválido.");
        // Import the browser's existing RP once. Later setting changes must
        // never reset live client health and plate damage using stale browser data.
        if(!state.configured||!player.equals(state.player)){
            state=new State();state.player=player;
            state.integrity=number(input,"integrity",100,0,100);
            state.energy=number(input,"energy",85,0,100);
            state.cursor=(int)number(input,"cursor",0,0,100000);
            if(input.has("segments")&&input.get("segments").isJsonArray()){
                JsonArray array=input.getAsJsonArray("segments");
                for(JsonElement element:array){
                    if(state.segments.size()>=48)break;
                    if(!element.isJsonObject())continue;
                    JsonObject part=element.getAsJsonObject();
                    String key=name(part,"key","segment_"+state.segments.size());
                    if(key.length()>56)key=key.substring(0,56);
                    double integrity=number(part,"integrity",100,0,100);
                    boolean integrated=part.has("integrated")&&part.get("integrated").isJsonPrimitive()&&
                        part.get("integrated").getAsBoolean();
                    state.segments.add(new Plate(key,integrity,integrated));
                }
            }
            if(!state.segments.isEmpty())recalculate();
        }
        state.configured=true;
        state.enabled=input.has("enabled")&&input.get("enabled").getAsBoolean();
        state.damageFactor=number(input,"damageFactor",state.damageFactor,1,10);
        state.healIntegrity=number(input,"healIntegrity",state.healIntegrity,0,10);
        state.healEnergy=number(input,"healEnergy",state.healEnergy,0,15);
        state.updatedAt=System.currentTimeMillis();
        state.lastEvent="settings";
        save();
        return status();
    }
    private static void recalculate(){
        if(state.segments.isEmpty())return;
        double sum=0;
        for(Plate part:state.segments)sum+=clamp(part.integrity,0,100,100);
        state.integrity=clamp(Math.round(sum/state.segments.size()),0,100,100);
    }
    static void record(String type,float amount,String player){
        load();
        if(!state.configured||!state.enabled||player==null||!player.equals(state.player))return;
        double magnitude=clamp(amount,0,40,0);
        if("damage".equals(type)){
            if(!state.segments.isEmpty()){
                int next=state.cursor%state.segments.size();
                Plate part=state.segments.get(next);
                part.integrity=clamp(part.integrity-magnitude*state.damageFactor,0,100,100);
                if(part.integrity==0)part.integrated=false;
                state.cursor++;
                recalculate();
            }else{
                state.integrity=clamp(state.integrity-magnitude*state.damageFactor,0,100,100);
            }
        }else if("death".equals(type)){
            for(Plate part:state.segments){part.integrity=0;part.integrated=false;}
            state.integrity=0;
        }else if("heal".equals(type)){
            state.energy=clamp(state.energy+state.healEnergy,0,100,100);
            double remaining=magnitude*state.healIntegrity;
            if(state.segments.isEmpty()){
                state.integrity=clamp(state.integrity+remaining,0,100,100);
            }else{
                List<Plate> damaged=new ArrayList<>(state.segments);
                damaged.sort(Comparator.comparingDouble(p->p.integrity));
                for(Plate part:damaged){
                    if(remaining<=0)break;
                    double repaired=Math.min(remaining,100-part.integrity);
                    part.integrity=clamp(part.integrity+repaired,0,100,100);
                    remaining-=repaired;
                    // Healing restores plates but does not re-integrate them.
                }
                recalculate();
            }
        }else if("respawn".equals(type)){
            state.energy=clamp(state.energy+state.healEnergy,0,100,100);
        }else return;
        state.lastEvent=type;
        state.updatedAt=System.currentTimeMillis();
        save();
    }
    static JsonObject snapshot(){
        load();
        JsonObject result=new JsonObject();
        result.addProperty("managed",state.configured);
        result.addProperty("enabled",state.enabled);
        result.addProperty("integrity",state.integrity);
        result.addProperty("energy",state.energy);
        result.addProperty("damageFactor",state.damageFactor);
        result.addProperty("healIntegrity",state.healIntegrity);
        result.addProperty("healEnergy",state.healEnergy);
        result.addProperty("updatedAt",state.updatedAt);
        result.addProperty("lastEvent",state.lastEvent);
        result.add("segments",GSON.toJsonTree(state.segments));
        return result;
    }
}
