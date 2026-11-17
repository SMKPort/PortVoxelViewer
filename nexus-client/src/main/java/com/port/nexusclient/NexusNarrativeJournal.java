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
import java.util.Comparator;
import java.util.List;

/**
 * Private local journal. Manual entries are explicitly supplied by the owner.
 * Automatic safety events are recorded only with separate consent.
 * Does not record other players' speech or send content to chat/Render.
 */
final class NexusNarrativeJournal {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("nexus-narrative-journal.json");
    private static final int LIMIT=160;
    private static final class Entry {
        long id,at;String category,title,detail;
        Entry(long id,long at,String category,String title,String detail){
            this.id=id;this.at=at;this.category=category;this.title=title;this.detail=detail;
        }
    }
    private static final class Store {
        int schema=1;long nextId=1;boolean autoCritical=false;
        List<Entry> entries=new ArrayList<>();
    }
    private static Store store;
    private NexusNarrativeJournal(){}
    private static void load(){
        if(store!=null)return;
        store=new Store();
        if(!Files.isRegularFile(FILE))return;
        try{
            Store saved=JSON.fromJson(Files.readString(FILE,StandardCharsets.UTF_8),Store.class);
            if(saved!=null&&saved.schema==1&&saved.entries!=null){
                saved.entries.removeIf(e->e==null||e.id<=0||e.at<=0||
                    e.title==null||e.detail==null||e.category==null);
                if(saved.entries.size()>LIMIT)saved.entries=new ArrayList<>(
                    saved.entries.subList(saved.entries.size()-LIMIT,saved.entries.size()));
                saved.nextId=Math.max(saved.nextId,saved.entries.stream()
                    .mapToLong(e->e.id).max().orElse(0)+1);
                store=saved;
            }
        }catch(Exception error){System.err.println("Nexus Journal: arquivo inválido, sem sobrescrever.");}
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
        }catch(Exception error){throw new IllegalStateException("Não foi possível salvar o diário.",error);}
    }
    private static String clean(String raw,int length){
        String result=String.valueOf(raw==null?"":raw)
            .replaceAll("[\\p{Cntrl}§]"," ").replaceAll("\\s+"," ").trim();
        return result.substring(0,Math.min(length,result.length()));
    }
    private static void append(String type,String rawTitle,String rawDetail){
        String title=clean(rawTitle,80),detail=clean(rawDetail,440);
        if(title.length()<3||detail.length()<3)
            throw new IllegalArgumentException("Informe um título e uma descrição.");
        store.entries.add(new Entry(store.nextId++,System.currentTimeMillis(),
            type,title,detail));
        while(store.entries.size()>LIMIT)store.entries.remove(0);
        save();
    }
    static synchronized String add(String title,String detail){
        load();append("manual",title,detail);
        return "Acontecimento salvo no diário narrativo local.";
    }
    static synchronized boolean autoCritical(){load();return store.autoCritical;}
    static synchronized void autoCritical(boolean yes){load();store.autoCritical=yes;save();}
    static synchronized void recordCritical(int percent,String dimension){
        load();
        if(!store.autoCritical)return;
        append("sistema","Risco crítico observado",
            "Vida observada em "+percent+"% na dimensão "+clean(dimension,70)+
            ". Registro do cliente, sem confirmação de eventos externos.");
    }
    static synchronized List<String> latest(int count){
        load();
        List<String> out=new ArrayList<>();
        for(int i=store.entries.size()-1;i>=0&&out.size()<Math.min(count,10);i--){
            Entry entry=store.entries.get(i);
            out.add("#"+entry.id+" ["+entry.category+"] "+entry.title+": "+entry.detail);
        }
        return out.isEmpty()?List.of("Diário vazio."):List.copyOf(out);
    }
    static synchronized List<String> recall(String query,int count){
        load();
        List<NexusMemoryRecall.Note> notes=store.entries.stream()
            .map(e->new NexusMemoryRecall.Note(e.id,e.title,e.detail,e.at)).toList();
        List<String> out=new ArrayList<>();
        for(NexusMemoryRecall.Note note:NexusMemoryRecall.select(query,notes,count))
            out.add("#"+note.id()+" "+note.topic()+": "+note.text());
        return out.isEmpty()?List.of("Nenhum acontecimento correspondente no diário."):
            List.copyOf(out);
    }
    static synchronized String delete(long id){
        load();
        if(!store.entries.removeIf(e->e.id==id))
            return "Registro não encontrado.";
        save();return "Registro "+id+" removido.";
    }
    static synchronized String status(){
        load();return "Diário "+store.entries.size()+"/"+LIMIT+
            " | alertas automáticos "+(store.autoCritical?"ON":"OFF")+
            " | nunca transmitido ao servidor.";
    }
}
