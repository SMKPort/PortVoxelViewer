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
import java.util.Locale;

/** Local opt-in diagnostic history. Requests are never called confirmed casts. */
final class NexusCombatInsights {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("nexus-combat-insights.json");
    private static final int MAX_EVENTS=120;
    private static final class Event {
        long at;String macro,kind,spell,reason;
        Event(long at,String macro,String kind,String spell,String reason){
            this.at=at;this.macro=macro;this.kind=kind;this.spell=spell;this.reason=reason;
        }
    }
    private static final class Store {
        int schema=1;boolean enabled=false;
        List<Event> events=new ArrayList<>();
    }
    private static Store store;
    private NexusCombatInsights(){}

    private static synchronized void load(){
        if(store!=null)return;
        store=new Store();
        if(!Files.isRegularFile(FILE))return;
        try{
            Store saved=JSON.fromJson(Files.readString(FILE,StandardCharsets.UTF_8),Store.class);
            if(saved!=null&&saved.schema==1&&saved.events!=null){
                saved.events.removeIf(e->e==null||e.kind==null||e.macro==null||
                    e.spell==null||e.reason==null||e.at<=0);
                if(saved.events.size()>MAX_EVENTS)
                    saved.events=new ArrayList<>(saved.events.subList(
                        saved.events.size()-MAX_EVENTS,saved.events.size()));
                store=saved;
            }
        }catch(Exception error){
            // Do not overwrite unreadable user data.
            store=new Store();
            System.err.println("Nexus Insights: arquivo inválido, leitura bloqueada.");
        }
    }
    private static void save(){
        try{
            Files.createDirectories(FILE.getParent());
            Path temp=FILE.resolveSibling(FILE.getFileName()+".tmp");
            Files.writeString(temp,JSON.toJson(store),StandardCharsets.UTF_8);
            try{Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException error){
                Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING);}
        }catch(Exception error){
            System.err.println("Nexus Insights: falha ao salvar histórico local.");
        }
    }
    static synchronized boolean enabled(){load();return store.enabled;}
    static synchronized void setEnabled(boolean on){load();store.enabled=on;save();}
    private static String safe(String input,int max){
        String value=String.valueOf(input==null?"":input)
            .replaceAll("[\\p{Cntrl}§]"," ").trim();
        return value.substring(0,Math.min(max,value.length()));
    }
    static synchronized void record(String macro,String kind,String spell,String reason){
        load();
        if(!store.enabled)return;
        if(!List.of("start","request","blocked","complete","cancel").contains(kind))return;
        store.events.add(new Event(System.currentTimeMillis(),safe(macro,100),
            kind,safe(spell,100),safe(reason,190)));
        while(store.events.size()>MAX_EVENTS)store.events.remove(0);
        save();
    }
    static synchronized List<String> recent(int max){
        load();
        List<String> out=new ArrayList<>();
        for(int i=store.events.size()-1;i>=0&&out.size()<Math.max(0,Math.min(max,12));i--){
            Event e=store.events.get(i);
            out.add("["+java.time.Instant.ofEpochMilli(e.at)+"] "+
                e.macro+" | "+e.kind+" | "+e.spell+" | "+e.reason);
        }
        return out.isEmpty()?List.of("Ainda não existem eventos registrados."):List.copyOf(out);
    }
    static synchronized String status(){
        load();
        long requests=store.events.stream().filter(e->e.kind.equals("request")).count();
        long blocked=store.events.stream().filter(e->e.kind.equals("blocked")).count();
        return "Análise local "+(store.enabled?"ON":"OFF")+
            " | "+requests+" pedidos enviados | "+blocked+
            " bloqueios de pré-teste | "+store.events.size()+" eventos.";
    }
    static synchronized String suggestion(){
        load();
        long cooldown=store.events.stream().filter(e->e.kind.equals("blocked")&&
            e.reason.toLowerCase(Locale.ROOT).contains("cooldown")).count();
        long mana=store.events.stream().filter(e->e.kind.equals("blocked")&&
            e.reason.toLowerCase(Locale.ROOT).contains("mana")).count();
        long noSpell=store.events.stream().filter(e->e.kind.equals("blocked")&&
            e.reason.toLowerCase(Locale.ROOT).contains("equipad")).count();
        if(cooldown>=2)return "Sugestão: confira os cooldowns e aumente o intervalo entre as magias.";
        if(mana>=2)return "Sugestão: reduza o número de magias ou espere recuperar mana.";
        if(noSpell>=1)return "Sugestão: verifique as magias equipadas no Iron's Spells.";
        return "Sem padrão de falhas suficiente para sugerir ajustes. Nenhuma macro foi alterada.";
    }
    static synchronized void clear(){
        load();store.events.clear();save();
    }
}
