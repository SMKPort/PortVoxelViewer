package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;

/** Successful autonomous chat sends, stored on the Minecraft PC. */
final class NexusAlertHistory {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH=FMLPaths.CONFIGDIR.get().resolve("nexus-alert-history.json");
    private static List<Entry> entries;
    private static final class Entry {
        String kind,text,at;
        Entry(String kind,String text){
            this.kind=kind;this.text=text;this.at=Instant.now().toString();
        }
    }
    private NexusAlertHistory(){}
    private static void load(){
        if(entries!=null)return;
        entries=new ArrayList<>();
        if(!Files.isRegularFile(PATH))return;
        try{
            Entry[] saved=JSON.fromJson(Files.readString(PATH,StandardCharsets.UTF_8),Entry[].class);
            if(saved!=null)for(Entry entry:saved){
                if(entry==null||entry.kind==null||entry.text==null||entry.at==null)continue;
                entries.add(entry);
                if(entries.size()>80)entries.remove(0);
            }
        }catch(Exception ex){System.err.println("Nexus Alerts: falha ao ler histórico: "+ex.getMessage());}
    }
    static void record(String kind,String text){
        load();
        entries.add(new Entry(kind,text));
        while(entries.size()>80)entries.remove(0);
        try{
            Files.createDirectories(PATH.getParent());
            Path tmp=PATH.resolveSibling(PATH.getFileName()+".tmp");
            Files.writeString(tmp,JSON.toJson(entries),StandardCharsets.UTF_8);
            try{Files.move(tmp,PATH,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException ex){Files.move(tmp,PATH,StandardCopyOption.REPLACE_EXISTING);}
        }catch(Exception ex){System.err.println("Nexus Alerts: falha ao salvar histórico: "+ex.getMessage());}
    }
    static JsonArray snapshot(){
        load();
        JsonArray result=new JsonArray();
        for(int i=Math.max(0,entries.size()-30);i<entries.size();i++)
            result.add(JSON.toJsonTree(entries.get(i)));
        return result;
    }
}
