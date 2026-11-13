package com.port.nexusclient;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import net.neoforged.fml.loading.FMLPaths;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Append-only local transcript journal, never audio. Existing 1.7.0 memories are
 * loaded from the legacy JSON file. The journal has a hard 24 MiB quota and
 * refuses new writes when full instead of silently deleting old memories.
 */
final class NexusHearingMemory {
    private static final Gson JSON=new Gson();
    static final long QUOTA_BYTES=24L*1024*1024;
    private static final int CACHE_LIMIT=6000;
    private final Path archive,legacy;
    private final List<Entry> cache=new ArrayList<>();
    private boolean loaded;
    private long count;
    private boolean full;

    static final class Entry {
        String speaker,text,at;
        Entry(String speaker,String text,String at){
            this.speaker=speaker;this.text=text;this.at=at;
        }
    }

    NexusHearingMemory(){
        this(FMLPaths.CONFIGDIR.get());
    }
    NexusHearingMemory(Path directory){
        archive=directory.resolve("nexus-hearing-memory.jsonl");
        legacy=directory.resolve("nexus-hearing-memories.json");
    }
    synchronized void load(){
        if(loaded)return;
        loaded=true;cache.clear();count=0;
        if(Files.isRegularFile(legacy)){
            try{
                Entry[] old=JSON.fromJson(Files.readString(legacy,StandardCharsets.UTF_8),Entry[].class);
                if(old!=null)for(Entry e:old)addCached(e);
            }catch(Exception ex){
                System.err.println("Nexus Hearing: não foi possível ler memórias anteriores.");
            }
        }
        if(Files.isRegularFile(archive)){
            try(BufferedReader br=Files.newBufferedReader(archive,StandardCharsets.UTF_8)){
                String line;
                while((line=br.readLine())!=null){
                    try{addCached(JSON.fromJson(line,Entry.class));}
                    catch(JsonSyntaxException ex){ /* ignore a partial or malformed tail record */ }
                }
            }catch(IOException ex){
                System.err.println("Nexus Hearing: não foi possível ler o arquivo de memórias.");
            }
        }
        try{full=Files.isRegularFile(archive)&&Files.size(archive)>=QUOTA_BYTES;}
        catch(IOException ex){full=false;}
    }
    private void addCached(Entry e){
        if(e==null||e.speaker==null||!e.speaker.matches("[A-Za-z0-9_]{3,16}")||
           e.text==null||e.text.isBlank()||e.at==null||e.text.length()>500)return;
        count++;
        cache.add(e);
        if(cache.size()>CACHE_LIMIT)cache.remove(0);
    }
    synchronized boolean record(String speaker,String text,long capturedAt){
        load();
        if(full||speaker==null||!speaker.matches("[A-Za-z0-9_]{3,16}"))return false;
        String cleaned=String.valueOf(text).replaceAll("[\\p{Cntrl}]"," ").trim();
        if(cleaned.isEmpty())return false;
        cleaned=cleaned.substring(0,Math.min(400,cleaned.length()));
        Entry entry=new Entry(speaker,cleaned,Instant.ofEpochMilli(Math.max(0,capturedAt)).toString());
        try{
            Files.createDirectories(archive.getParent());
            byte[] data=(JSON.toJson(entry)+"\n").getBytes(StandardCharsets.UTF_8);
            if((Files.isRegularFile(archive)?Files.size(archive):0)+data.length>QUOTA_BYTES){
                full=true;return false;
            }
            Files.write(archive,data,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            addCached(entry);
            return true;
        }catch(IOException ex){
            System.err.println("Nexus Hearing: erro ao persistir transcrição.");
            return false;
        }
    }
    synchronized boolean full(){load();return full;}
    synchronized long count(){load();return count;}
    synchronized List<String> recent(int limit){
        load();
        List<String> result=new ArrayList<>();
        for(int i=cache.size()-1;i>=0&&result.size()<limit;i--){
            Entry e=cache.get(i);
            result.add("["+e.at+"] "+e.speaker+": "+e.text);
        }
        return result.isEmpty()?List.of("Nenhuma memória de voz salva."):result;
    }
    synchronized List<String> forSpeaker(String speaker,int limit){
        load();List<String> result=new ArrayList<>();
        for(int i=cache.size()-1;i>=0&&result.size()<limit;i--){
            Entry e=cache.get(i);
            if(e.speaker.equalsIgnoreCase(speaker))result.add(e.text);
        }
        java.util.Collections.reverse(result);
        return result;
    }
    synchronized void clear(){
        try{
            Files.deleteIfExists(archive);Files.deleteIfExists(legacy);
            cache.clear();count=0;full=false;loaded=true;
        }catch(IOException ex){throw new IllegalStateException("Não foi possível apagar o histórico de audição.",ex);}
    }
    synchronized long forget(String speaker){
        load();
        List<Entry> others=new ArrayList<>();
        if(Files.isRegularFile(legacy)){
            try{
                Entry[] entries=JSON.fromJson(Files.readString(legacy,StandardCharsets.UTF_8),Entry[].class);
                if(entries!=null)for(Entry e:entries)if(e!=null&&!speaker.equalsIgnoreCase(e.speaker))others.add(e);
            }catch(Exception e){throw new IllegalStateException("Não foi possível processar o arquivo anterior.",e);}
        }
        if(Files.isRegularFile(archive)){
            try(BufferedReader br=Files.newBufferedReader(archive,StandardCharsets.UTF_8)){
                String line;
                while((line=br.readLine())!=null){
                    try{
                        Entry e=JSON.fromJson(line,Entry.class);
                        if(e!=null&&!speaker.equalsIgnoreCase(e.speaker))others.add(e);
                    }catch(JsonSyntaxException ex){ /* malformed line isn't a valid memory */ }
                }
            }catch(IOException ex){throw new IllegalStateException("Não foi possível processar o arquivo de memórias.",ex);}
        }
        long removed=count-others.size();
        try{
            Files.createDirectories(archive.getParent());
            Path temp=archive.resolveSibling(archive.getFileName()+".tmp");
            try(BufferedWriter writer=Files.newBufferedWriter(temp,StandardCharsets.UTF_8)){
                for(Entry e:others){writer.write(JSON.toJson(e));writer.newLine();}
            }
            try{Files.move(temp,archive,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException ex){
                Files.move(temp,archive,StandardCopyOption.REPLACE_EXISTING);
            }
            Files.deleteIfExists(legacy);
            loaded=false;load();return Math.max(0,removed);
        }catch(IOException ex){throw new IllegalStateException("Não foi possível apagar as memórias do jogador.",ex);}
    }
}
