package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Only text reports (never screenshots), and only after owner opt-in. */
final class NexusVisionHistoryStore {
    private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("nexus-vision-reports.json");
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    static final class Entry {
        String focus,text;
        boolean ok;
        long at;
        Entry(String focus,String text,boolean ok,long at){
            this.focus=focus;this.text=text;this.ok=ok;this.at=at;
        }
    }
    private static final class State {
        boolean enabled=false;
        List<Entry> entries=new ArrayList<>();
    }
    private static State state;
    private NexusVisionHistoryStore(){}
    private static synchronized void load(){
        if(state!=null)return;
        state=new State();
        try{
            if(!Files.isRegularFile(FILE))return;
            State saved=JSON.fromJson(Files.readString(FILE,StandardCharsets.UTF_8),State.class);
            if(saved==null||saved.entries==null)return;
            saved.entries.removeIf(e->e==null||e.focus==null||e.text==null||
                e.focus.length()>60||e.text.length()>800||e.at<=0);
            if(saved.entries.size()>6)saved.entries=new ArrayList<>(saved.entries.subList(0,6));
            state=saved;
        }catch(Exception ignored){state=new State();}
    }
    static synchronized boolean enabled(){load();return state.enabled;}
    static synchronized List<Entry> entries(){
        load();
        return state.enabled?List.copyOf(state.entries):List.of();
    }
    static synchronized void setEnabled(boolean enable){
        load();
        state.enabled=enable;
        persist();
    }
    static synchronized void add(String focus,String text,boolean ok,long at){
        load();
        if(!state.enabled)return;
        state.entries.add(0,new Entry(focus,text,ok,at));
        while(state.entries.size()>6)state.entries.remove(state.entries.size()-1);
        persist();
    }
    static synchronized void clear(){
        load();
        state.entries.clear();
        persist();
    }
    private static void persist(){
        try{
            Files.createDirectories(FILE.getParent());
            Path temp=FILE.resolveSibling(FILE.getFileName()+".tmp");
            Files.writeString(temp,JSON.toJson(state),StandardCharsets.UTF_8);
            try{Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING);}
        }catch(Exception e){throw new IllegalStateException("Não foi possível salvar o histórico de visão.",e);}
    }
}
