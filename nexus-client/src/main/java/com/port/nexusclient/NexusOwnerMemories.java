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
 * Explicit memories entered by Port. Separate from model-generated impressions
 * and from the private chat history. No automatic network sync.
 */
final class NexusOwnerMemories {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH=FMLPaths.CONFIGDIR.get().resolve("nexus-owner-memories.json");
    private static final int MAX_COUNT=200;
    static final class Memory {
        long id,updatedAt;
        String topic,text;
        Memory(long id,String topic,String text,long updatedAt){
            this.id=id;this.topic=topic;this.text=text;this.updatedAt=updatedAt;
        }
    }
    private static final class Store {
        int schema=1;
        long nextId=1;
        List<Memory> entries=new ArrayList<>();
    }
    private static Store store;

    private NexusOwnerMemories(){}
    private static void load(){
        if(store!=null)return;
        store=new Store();
        if(!Files.isRegularFile(PATH))return;
        try{
            Store saved=JSON.fromJson(Files.readString(PATH,StandardCharsets.UTF_8),Store.class);
            if(saved!=null&&saved.schema==1&&saved.entries!=null){
                saved.entries.removeIf(m->m==null||m.id<=0||m.topic==null||m.text==null);
                if(saved.entries.size()>MAX_COUNT)
                    saved.entries=new ArrayList<>(saved.entries.subList(saved.entries.size()-MAX_COUNT,saved.entries.size()));
                saved.nextId=Math.max(saved.nextId,
                    saved.entries.stream().mapToLong(m->m.id).max().orElse(0)+1);
                store=saved;
            }
        }catch(Exception ex){System.err.println("Nexus: arquivo de memórias manuais inválido; original preservado.");}
    }
    static synchronized List<Memory> list(){
        load();
        return store.entries.stream().sorted(Comparator.comparingLong((Memory m)->m.updatedAt).reversed())
            .map(m->new Memory(m.id,m.topic,m.text,m.updatedAt)).toList();
    }
    static synchronized Memory save(long id,String rawTopic,String rawText){
        load();
        String topic=clean(rawTopic,80),body=clean(rawText,480);
        if(topic.length()<2)throw new IllegalArgumentException("Informe o assunto da memória.");
        if(body.length()<3)throw new IllegalArgumentException("Escreva o que o Nexus deve lembrar.");
        Memory existing=null;
        if(id>0){
            for(Memory entry:store.entries)if(entry.id==id){existing=entry;break;}
            if(existing==null)throw new IllegalArgumentException("Memória não encontrada.");
        }else{
            if(store.entries.size()>=MAX_COUNT)
                throw new IllegalStateException("Limite de 200 memórias. Apague ou edite uma existente.");
            existing=new Memory(store.nextId++,topic,body,System.currentTimeMillis());
            store.entries.add(existing);
        }
        existing.topic=topic;existing.text=body;existing.updatedAt=System.currentTimeMillis();
        persist();
        return new Memory(existing.id,existing.topic,existing.text,existing.updatedAt);
    }
    static synchronized void delete(long id){
        load();
        if(id<=0||!store.entries.removeIf(m->m.id==id))
            throw new IllegalArgumentException("Memória não encontrada.");
        persist();
    }
    static synchronized List<Memory> relevant(String question,int limit){
        load();
        List<NexusMemoryRecall.Note> notes=store.entries.stream()
            .map(m->new NexusMemoryRecall.Note(m.id,m.topic,m.text,m.updatedAt)).toList();
        List<Memory> out=new ArrayList<>();
        for(NexusMemoryRecall.Note note:NexusMemoryRecall.select(question,notes,limit)){
            out.add(new Memory(note.id(),note.topic(),note.text(),note.updatedAt()));
        }
        return List.copyOf(out);
    }
    private static String clean(String raw,int max){
        String text=String.valueOf(raw==null?"":raw)
            .replaceAll("[\\p{Cntrl}§]"," ").replaceAll("\\s+"," ").trim();
        return text.substring(0,Math.min(max,text.length()));
    }
    private static void persist(){
        try{
            Files.createDirectories(PATH.getParent());
            Path tmp=PATH.resolveSibling(PATH.getFileName()+".tmp");
            Files.writeString(tmp,JSON.toJson(store),StandardCharsets.UTF_8);
            try{Files.move(tmp,PATH,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException ex){Files.move(tmp,PATH,StandardCopyOption.REPLACE_EXISTING);}
        }catch(Exception ex){throw new IllegalStateException("Não foi possível salvar a memória no disco.",ex);}
    }
}
