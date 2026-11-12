package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;

/** Local RP speech preferences, persistent even with the browser closed. */
final class NexusAlertSettings {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH=FMLPaths.CONFIGDIR.get().resolve("nexus-alerts.json");
    private static Settings state;
    private static final class Settings {
        boolean heavy=true,streak=true,structure=true,risk=true,recovery=true,critical=true;
        int cooldownSeconds=45;
    }
    private NexusAlertSettings(){}
    private static void load(){
        if(state!=null)return;
        state=new Settings();
        if(!Files.isRegularFile(PATH))return;
        try{
            Settings saved=JSON.fromJson(Files.readString(PATH,StandardCharsets.UTF_8),Settings.class);
            if(saved!=null){
                state=saved;
                state.cooldownSeconds=Math.max(15,Math.min(180,state.cooldownSeconds));
            }
        }catch(Exception ex){System.err.println("Nexus Alerts: falha ao ler preferências: "+ex.getMessage());}
    }
    static boolean allows(String type){
        load();
        return switch(type){
            case "heavy" -> state.heavy;
            case "streak" -> state.streak;
            case "structure" -> state.structure;
            case "risk" -> state.risk;
            case "recovery" -> state.recovery;
            case "critical" -> state.critical;
            default -> true;
        };
    }
    static long cooldownMillis(){load();return state.cooldownSeconds*1000L;}
    static JsonObject snapshot(){load();return JSON.toJsonTree(state).getAsJsonObject();}
    static void configure(JsonObject input){
        load();
        for(String name:new String[]{"heavy","streak","structure","risk","recovery","critical"}){
            if(!input.has(name)||!input.get(name).isJsonPrimitive()||
                !input.get(name).getAsJsonPrimitive().isBoolean())
                throw new IllegalArgumentException("Valor inválido em "+name+".");
        }
        if(!input.has("cooldownSeconds")||!input.get("cooldownSeconds").isJsonPrimitive()||
            !input.get("cooldownSeconds").getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("Intervalo entre alertas inválido.");
        int seconds=input.get("cooldownSeconds").getAsInt();
        if(seconds<15||seconds>180)throw new IllegalArgumentException("Intervalo permitido: 15 a 180 segundos.");
        Settings updated=new Settings();
        updated.heavy=input.get("heavy").getAsBoolean();
        updated.streak=input.get("streak").getAsBoolean();
        updated.structure=input.get("structure").getAsBoolean();
        updated.risk=input.get("risk").getAsBoolean();
        updated.recovery=input.get("recovery").getAsBoolean();
        updated.critical=input.get("critical").getAsBoolean();
        updated.cooldownSeconds=seconds;
        try{
            Files.createDirectories(PATH.getParent());
            Path tmp=PATH.resolveSibling(PATH.getFileName()+".tmp");
            Files.writeString(tmp,JSON.toJson(updated),StandardCharsets.UTF_8);
            try{Files.move(tmp,PATH,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException ex){Files.move(tmp,PATH,StandardCopyOption.REPLACE_EXISTING);}
            state=updated;
        }catch(Exception ex){throw new IllegalStateException("Não foi possível salvar preferências dos alertas.",ex);}
    }
}
