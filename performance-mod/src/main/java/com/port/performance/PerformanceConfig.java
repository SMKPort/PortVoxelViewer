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
    static float hudX=0f,hudY=0f,hudScale=1f;
    static boolean boosterAdjustChunks=true,boosterAdjustEntities=false;
    static boolean boosterAdjustBiomeBlend=true,boosterAdjustClouds=false;
    static String boosterProfile="igpu";
    static int pendingBlendOriginal=-1,pendingBlendApplied=-1;
    static String pendingCloudOriginal="",pendingCloudApplied="";
    static int boosterMinRenderDistance=6;
    static int pendingRenderOriginal=-1,pendingRenderApplied=-1;
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
        hudX=parsePosition(values.getProperty("hudX","0"));
        hudY=parsePosition(values.getProperty("hudY","0"));
        hudScale=parseScale(values.getProperty("hudScale","1.0"));
        boosterAdjustChunks=Boolean.parseBoolean(values.getProperty("boosterAdjustChunks","true"));
        boosterAdjustEntities=Boolean.parseBoolean(values.getProperty("boosterAdjustEntities","false"));
        boosterAdjustBiomeBlend=Boolean.parseBoolean(values.getProperty("boosterAdjustBiomeBlend","true"));
        boosterAdjustClouds=Boolean.parseBoolean(values.getProperty("boosterAdjustClouds","false"));
        boosterProfile="balanced".equals(values.getProperty("boosterProfile","igpu"))?"balanced":"igpu";
        try {
            pendingBlendOriginal=Integer.parseInt(values.getProperty("pendingBlendOriginal","-1"));
            pendingBlendApplied=Integer.parseInt(values.getProperty("pendingBlendApplied","-1"));
            if(pendingBlendOriginal<0||pendingBlendApplied<0){pendingBlendOriginal=-1;pendingBlendApplied=-1;}
        }catch(NumberFormatException ex){pendingBlendOriginal=-1;pendingBlendApplied=-1;}
        pendingCloudOriginal=values.getProperty("pendingCloudOriginal","");
        pendingCloudApplied=values.getProperty("pendingCloudApplied","");
        try { boosterMinRenderDistance=Math.max(4,Math.min(24,Integer.parseInt(values.getProperty("boosterMinRenderDistance","6")))); }
        catch(NumberFormatException ex){boosterMinRenderDistance=6;}
        try{
            pendingRenderOriginal=Integer.parseInt(values.getProperty("pendingRenderOriginal","-1"));
            pendingRenderApplied=Integer.parseInt(values.getProperty("pendingRenderApplied","-1"));
            if(pendingRenderOriginal<=0||pendingRenderApplied<=0){pendingRenderOriginal=-1;pendingRenderApplied=-1;}
        }catch(NumberFormatException ex){pendingRenderOriginal=-1;pendingRenderApplied=-1;}
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

    private static float parsePosition(String raw){
        try{
            float value=Float.parseFloat(raw);
            if(Float.isFinite(value))return Math.max(0f,Math.min(1f,value));
        }catch(NumberFormatException ignored){}
        return 0f;
    }

    private static float parseScale(String raw){
        try{
            float f=Float.parseFloat(raw);
            return Float.isFinite(f)?Math.max(0.55f,Math.min(2.25f,f)):1.0f;
        }catch(NumberFormatException ex){return 1.0f;}
    }
    static void notePendingRenderRestore(int original,int applied){
        pendingRenderOriginal=original;
        pendingRenderApplied=applied;
        save();
    }
    static void clearPendingRenderRestore(){
        if(pendingRenderOriginal<0&&pendingRenderApplied<0)return;
        pendingRenderOriginal=-1;
        pendingRenderApplied=-1;
        save();
    }

    static void notePendingBlendRestore(int original,int applied){
        pendingBlendOriginal=original;
        pendingBlendApplied=applied;
        save();
    }
    static void clearPendingBlendRestore(){
        if(pendingBlendOriginal<0&&pendingBlendApplied<0)return;
        pendingBlendOriginal=pendingBlendApplied=-1;
        save();
    }
    static void notePendingCloudRestore(String original,String applied){
        pendingCloudOriginal=original;
        pendingCloudApplied=applied;
        save();
    }
    static void clearPendingCloudRestore(){
        if(pendingCloudOriginal.isBlank()&&pendingCloudApplied.isBlank())return;
        pendingCloudOriginal=pendingCloudApplied="";
        save();
    }
    static void selectProfile(String profile){
        boosterProfile="balanced".equals(profile)?"balanced":"igpu";
        boosterAdjustBiomeBlend="igpu".equals(boosterProfile);
        boosterAdjustClouds=false; // preserve sky by default in every profile
        boosterAdjustEntities=false; // preserve Armourer's Workshop
        boosterAdjustChunks=true;
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
        values.setProperty("hudX",Float.toString(hudX));
        values.setProperty("hudY",Float.toString(hudY));
        values.setProperty("hudScale",Float.toString(hudScale));
        values.setProperty("boosterAdjustChunks",Boolean.toString(boosterAdjustChunks));
        values.setProperty("boosterAdjustEntities",Boolean.toString(boosterAdjustEntities));
        values.setProperty("boosterAdjustBiomeBlend",Boolean.toString(boosterAdjustBiomeBlend));
        values.setProperty("boosterAdjustClouds",Boolean.toString(boosterAdjustClouds));
        values.setProperty("boosterProfile",boosterProfile);
        if(pendingBlendOriginal>=0&&pendingBlendApplied>=0){
            values.setProperty("pendingBlendOriginal",Integer.toString(pendingBlendOriginal));
            values.setProperty("pendingBlendApplied",Integer.toString(pendingBlendApplied));
        }
        if(!pendingCloudOriginal.isBlank()&&!pendingCloudApplied.isBlank()){
            values.setProperty("pendingCloudOriginal",pendingCloudOriginal);
            values.setProperty("pendingCloudApplied",pendingCloudApplied);
        }
        values.setProperty("boosterMinRenderDistance",Integer.toString(boosterMinRenderDistance));
        if(pendingRenderOriginal>0&&pendingRenderApplied>0){
            values.setProperty("pendingRenderOriginal",Integer.toString(pendingRenderOriginal));
            values.setProperty("pendingRenderApplied",Integer.toString(pendingRenderApplied));
        }
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
