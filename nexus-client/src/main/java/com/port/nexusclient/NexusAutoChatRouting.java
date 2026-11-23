package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Persist the owner-approved route used by automatic Nexus speeches. */
final class NexusAutoChatRouting {
    private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("nexus-chat-routing.json");
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static Settings current;
    record Route(String channel,String command,String format,String bodyFormat,boolean announce){}
    private static final class Settings {
        String channel="local",command="l",format="&6&l",bodyFormat="&f";
        boolean announce=true;
    }
    private NexusAutoChatRouting(){}
    private static synchronized void load(){
        if(current!=null)return;
        Settings loaded=new Settings();
        try{
            if(Files.isRegularFile(FILE)){
                Settings stored=JSON.fromJson(Files.readString(FILE,StandardCharsets.UTF_8),Settings.class);
                if(stored!=null){
                    NexusChatChannelPolicy.root(stored.channel,stored.command);
                    NexusChatChannelPolicy.format(stored.channel,stored.format);
                    NexusChatChannelPolicy.format(stored.channel,stored.bodyFormat);
                    loaded=stored;
                }
            }
        }catch(Exception ignored){
            // Invalid local file never supplies arbitrary command prefixes.
            loaded=new Settings();
            loaded.announce=false;
        }
        current=loaded;
    }
    static synchronized Route route(){
        load();
        return new Route(current.channel,current.command,current.format,current.bodyFormat,current.announce);
    }
    static synchronized String status(){
        Route r=route();
        return "Avisos automáticos: "+(r.announce?"ATIVOS":"DESLIGADOS")+
            " | canal "+("global".equals(r.channel)?"global":"/"+
            NexusChatChannelPolicy.root(r.channel,r.command))+
            " | prefixo "+r.format+" | texto "+r.bodyFormat;
    }
    static synchronized void configure(JsonObject value){
        if(value==null)throw new IllegalArgumentException("Configuração de canal ausente.");
        Settings next=new Settings();
        if(!value.has("channel")||!value.has("command")||
           !value.has("format")||!value.has("bodyFormat")||
           !value.has("announce"))
            throw new IllegalArgumentException("Configure canal, comando, estilos e anúncios.");
        next.channel=value.get("channel").getAsString();
        next.command=value.get("command").getAsString();
        next.format=value.get("format").getAsString();
        next.bodyFormat=value.get("bodyFormat").getAsString();
        if(!value.get("announce").isJsonPrimitive()||
           !value.get("announce").getAsJsonPrimitive().isBoolean())
            throw new IllegalArgumentException("Anúncios devem ser ativados ou desativados.");
        next.announce=value.get("announce").getAsBoolean();
        // Never persist values rejected by the exact same server-side safety policy.
        NexusChatChannelPolicy.root(next.channel,next.command);
        NexusChatChannelPolicy.format(next.channel,next.format);
        NexusChatChannelPolicy.format(next.channel,next.bodyFormat);
        try{
            Files.createDirectories(FILE.getParent());
            Path temp=FILE.resolveSibling(FILE.getFileName()+".tmp");
            Files.writeString(temp,JSON.toJson(next),StandardCharsets.UTF_8);
            try{Files.move(temp,FILE,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException error){
                Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING);
            }
        }catch(Exception error){throw new IllegalStateException("Não foi possível salvar o canal local.",error);}
        current=next;
    }
}
