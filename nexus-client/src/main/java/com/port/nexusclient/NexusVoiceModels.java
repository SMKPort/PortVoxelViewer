package com.port.nexusclient;

import net.neoforged.fml.loading.FMLPaths;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * One-time, optional HTTPS download from the official Vosk model host.
 * Mic audio is never uploaded. Downloads only begin on an explicit user command.
 */
final class NexusVoiceModels {
    private static final String PT="vosk-model-small-pt-0.3";
    private static final String EN="vosk-model-small-en-us-0.15";
    private static final AtomicBoolean BUSY=new AtomicBoolean();
    private static volatile String state="Modelo ainda não verificado.";
    private NexusVoiceModels(){}

    static Path model(String lang){
        String id="en".equals(lang)?EN:PT;
        return FMLPaths.CONFIGDIR.get().resolve("nexus-voice").resolve(id);
    }
    static boolean installed(String lang){
        Path base=model(lang);
        return Files.isRegularFile(base.resolve("am/final.mdl")) &&
            Files.isDirectory(base.resolve("conf")) &&
            Files.isDirectory(base.resolve("graph"));
    }
    static String status(){return state;}
    static boolean downloading(){return BUSY.get();}

    static boolean install(String lang){
        if(!"en".equals(lang)&&!"pt".equals(lang)){
            state="Idioma inválido.";return false;
        }
        if(installed(lang)){
            state="Modelo "+lang.toUpperCase()+" já instalado.";return true;
        }
        if(!BUSY.compareAndSet(false,true)){
            state="Já existe um download em andamento.";return false;
        }
        Thread worker=new Thread(()->{
            Path dest=model(lang),staging=dest.resolveSibling(dest.getFileName()+".part");
            String id="en".equals(lang)?EN:PT;
            try{
                state="Baixando Vosk "+lang.toUpperCase()+" (~31 a 40 MB)...";
                if(Files.exists(staging))deleteTree(staging);
                Files.createDirectories(staging);
                HttpURLConnection connection=(HttpURLConnection)URI.create(
                    "https://alphacephei.com/vosk/models/"+id+".zip").toURL().openConnection();
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(60000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent","NexusAterna-Voice/2.8");
                if(connection.getResponseCode()!=200)
                    throw new IllegalStateException("Servidor de modelos HTTP "+connection.getResponseCode());
                long expanded=0;
                try(InputStream input=connection.getInputStream();
                    ZipInputStream zip=new ZipInputStream(input)){
                    ZipEntry entry;
                    byte[] buffer=new byte[32768];
                    while((entry=zip.getNextEntry())!=null){
                        String full=entry.getName().replace('\\','/');
                        int slash=full.indexOf('/');
                        if(slash<0)continue;
                        String relative=full.substring(slash+1);
                        if(relative.isBlank())continue;
                        Path out=staging.resolve(relative).normalize();
                        if(!out.startsWith(staging))throw new SecurityException("Arquivo ZIP inválido.");
                        if(entry.isDirectory()){Files.createDirectories(out);continue;}
                        Files.createDirectories(out.getParent());
                        try(var file=Files.newOutputStream(out)){
                            int count;
                            while((count=zip.read(buffer))!=-1){
                                expanded+=count;
                                if(expanded>350L*1024*1024)
                                    throw new IllegalStateException("Modelo excedeu o limite de tamanho.");
                                file.write(buffer,0,count);
                            }
                        }
                        zip.closeEntry();
                    }
                }finally{connection.disconnect();}
                if(!Files.isRegularFile(staging.resolve("am/final.mdl"))||
                   !Files.isDirectory(staging.resolve("conf"))||
                   !Files.isDirectory(staging.resolve("graph")))
                    throw new IllegalStateException("Download incompleto: arquivos do Vosk ausentes.");
                Files.createDirectories(dest.getParent());
                if(Files.exists(dest))deleteTree(dest);
                try{Files.move(staging,dest,StandardCopyOption.ATOMIC_MOVE);}
                catch(AtomicMoveNotSupportedException ignored){Files.move(staging,dest);}
                state="Vosk "+lang.toUpperCase()+" instalado. Ative a conjuração por voz.";
            }catch(Exception ex){
                state="Falha no modelo: "+String.valueOf(ex.getMessage()).substring(
                    0,Math.min(115,String.valueOf(ex.getMessage()).length()));
                try{if(Files.exists(staging))deleteTree(staging);}catch(Exception ignored){}
            }finally{BUSY.set(false);}
        },"Nexus-Vosk-Model-Download");
        worker.setDaemon(true);
        worker.start();
        return true;
    }

    private static void deleteTree(Path dir)throws java.io.IOException{
        if(!Files.exists(dir))return;
        try(var walk=Files.walk(dir)){
            for(Path item:walk.sorted(java.util.Comparator.reverseOrder()).toList())
                Files.deleteIfExists(item);
        }
    }
}
