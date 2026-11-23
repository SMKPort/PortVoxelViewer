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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Local RP speech preferences, persistent even with the browser closed. */
final class NexusAlertSettings {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH=FMLPaths.CONFIGDIR.get().resolve("nexus-alerts.json");
    private static Settings state;
    private static final String[] TYPES={"heavy","streak","structure","risk","recovery","critical","mind","arcane"};
    private static final Set<String> ROUTES=Set.of("off","hud","chat","both");
    private static final class Settings {
        boolean heavy=true,streak=true,structure=true,risk=true,recovery=true,critical=true;
        int cooldownSeconds=45;
        Map<String,String> routes=new LinkedHashMap<>();
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
    static synchronized String route(String type){
        load();
        String explicit=state.routes==null?null:state.routes.get(type);
        if(explicit!=null&&ROUTES.contains(explicit))return explicit;
        return switch(type){
            case "heavy" -> state.heavy?"hud":"off";
            case "streak" -> state.streak?"hud":"off";
            case "structure" -> state.structure?"hud":"off";
            case "risk" -> state.risk?"hud":"off";
            case "recovery" -> state.recovery?"hud":"off";
            case "critical" -> state.critical?"hud":"off";
            case "mind","arcane" -> "hud";
            default -> "off";
        };
    }
    static boolean allows(String type){return !"off".equals(route(type));}
    static boolean displayHud(String type){
        String destination=route(type);return "hud".equals(destination)||"both".equals(destination);
    }
    static boolean displayChat(String type){
        String destination=route(type);return "chat".equals(destination)||"both".equals(destination);
    }
    static long cooldownMillis(){load();return state.cooldownSeconds*1000L;}
    static synchronized JsonObject snapshot(){
        load();
        JsonObject out=JSON.toJsonTree(state).getAsJsonObject();
        JsonObject routes=new JsonObject();
        for(String type:TYPES)routes.addProperty(type,route(type));
        out.add("routes",routes);
        return out;
    }
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
        JsonObject provided=input.has("routes")&&input.get("routes").isJsonObject()
            ?input.getAsJsonObject("routes"):null;
        for(String type:TYPES){
            String destination;
            if(provided==null||!provided.has(type)){
                destination=switch(type){
                    case "heavy" -> updated.heavy?"hud":"off";
                    case "streak" -> updated.streak?"hud":"off";
                    case "structure" -> updated.structure?"hud":"off";
                    case "risk" -> updated.risk?"hud":"off";
                    case "recovery" -> updated.recovery?"hud":"off";
                    case "critical" -> updated.critical?"hud":"off";
                    default -> "hud";
                };
            }else{
                if(!provided.get(type).isJsonPrimitive()||
                    !provided.get(type).getAsJsonPrimitive().isString())
                    throw new IllegalArgumentException("Destino do alerta inválido: "+type);
                destination=provided.get(type).getAsString();
                if(!ROUTES.contains(destination))throw new IllegalArgumentException(
                    "Destino inválido para "+type+": use off, hud, chat ou both.");
            }
            updated.routes.put(type,destination);
        }
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
