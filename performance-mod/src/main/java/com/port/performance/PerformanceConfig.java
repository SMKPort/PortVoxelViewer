package com.port.performance;

import net.neoforged.fml.loading.FMLPaths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Properties;

final class PerformanceConfig {
    private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("port-performance-monitor.properties");
    static final String DEFAULT_URL="https://port-performance-monitor.onrender.com";
    static String url=DEFAULT_URL;
    static String token="";
    static boolean hudEnabled=true;
    static boolean streamEnabled=false;
    static boolean webControlEnabled=false;
    static int boosterTargetFps=60;
    static double pendingRestoreOriginal=-1,pendingRestoreApplied=-1;

    static void load(){
        Properties values=new Properties();
        if(Files.isRegularFile(FILE)){
            try(InputStream in=Files.newInputStream(FILE)){values.load(in);}
            catch(Exception ex){System.err.println("Port Performance: erro lendo configuração.");}
        }
        String chosen=values.getProperty("url",DEFAULT_URL).trim();
        if(chosen.startsWith("https://")&&chosen.length()<240)url=chosen.replaceAll("/+$","");
        token=values.getProperty("token","").trim().toLowerCase();
        if(!token.matches("[a-f0-9]{32}"))regenerateKey();
        hudEnabled=Boolean.parseBoolean(values.getProperty("hudEnabled","true"));
        try{
            boosterTargetFps=Math.max(30,Math.min(240,Integer.parseInt(values.getProperty("boosterTargetFps","60"))));
            pendingRestoreOriginal=Double.parseDouble(values.getProperty("pendingRestoreOriginal","-1"));
            pendingRestoreApplied=Double.parseDouble(values.getProperty("pendingRestoreApplied","-1"));
            if(!Double.isFinite(pendingRestoreOriginal)||pendingRestoreOriginal<=0)pendingRestoreOriginal=-1;
            if(!Double.isFinite(pendingRestoreApplied)||pendingRestoreApplied<=0)pendingRestoreApplied=-1;
        }catch(Exception ignore){
            boosterTargetFps=60;pendingRestoreOriginal=-1;pendingRestoreApplied=-1;
        }
        // Always require active permission after launching Minecraft.
        streamEnabled=false;
        webControlEnabled=false;
        save();
    }

    static void regenerateKey(){
        byte[] bytes=new byte[16];
        new SecureRandom().nextBytes(bytes);
        token=HexFormat.of().formatHex(bytes);
        streamEnabled=false;
        save();
    }

    static void notePendingRestore(double original,double applied){
        pendingRestoreOriginal=original;pendingRestoreApplied=applied;save();
    }
    static void clearPendingRestore(){
        if(pendingRestoreOriginal<0&&pendingRestoreApplied<0)return;
        pendingRestoreOriginal=-1;pendingRestoreApplied=-1;save();
    }

    static void save(){
        Properties values=new Properties();
        values.setProperty("url",url);
        values.setProperty("token",token);
        values.setProperty("hudEnabled",Boolean.toString(hudEnabled));
        values.setProperty("boosterTargetFps",Integer.toString(boosterTargetFps));
        if(pendingRestoreOriginal>0&&pendingRestoreApplied>0){
            values.setProperty("pendingRestoreOriginal",Double.toString(pendingRestoreOriginal));
            values.setProperty("pendingRestoreApplied",Double.toString(pendingRestoreApplied));
        }
        // Intentionally never persist streaming authorization across restarts.
        try{
            Files.createDirectories(FILE.getParent());
            try(OutputStream out=Files.newOutputStream(FILE)){
                values.store(out,"Port Performance Monitor. Chave privada, não compartilhe este arquivo.");
            }
        }catch(Exception ex){System.err.println("Port Performance: não foi possível gravar configurações.");}
    }

    private PerformanceConfig(){}
}
