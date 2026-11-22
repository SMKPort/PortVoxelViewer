package com.port.nexusclient;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Iterator;

/**
 * On-demand screenshot analysis. Only a compressed screenshot from the
 * owner's explicit request goes to the HTTPS Gemini free-quota relay.
 * No always-on capture, no uploads of world files or game telemetry.
 */
final class NexusArcaneVision {
    private static boolean enabled=false,waiting=false;
    private static long lastAt=0;
    private static int requests=0;
    private static long day=0;
    private static String lastResult="Nenhuma análise solicitada.";
    private NexusArcaneVision(){}

    static boolean enabled(){return enabled;}
    static boolean waiting(){return waiting;}
    static void setEnabled(boolean on){
        if(on&&ClientConfig.token.isBlank())
            throw new IllegalStateException("Pareie o Nexus com o site antes de ativar a Visão Arcana.");
        enabled=on;
        if(!on)waiting=false;
    }
    static String status(){
        return "Visão Arcana "+(enabled?"ATIVA":"OFF")+
            " | "+(waiting?"consultando imagem":"pronta")+
            " | máximo 5 consultas/dia no cliente | "+lastResult;
    }
    static void capture(String question){
        Minecraft mc=Minecraft.getInstance();
        if(!enabled){NexusClientMod.notifyPlayer("Visão Arcana desativada: /nexusclient vision on.");return;}
        if(mc.player==null||mc.getConnection()==null||mc.level==null||mc.screen!=null){
            NexusClientMod.notifyPlayer("Entre no jogo e feche as telas para usar a Visão Arcana.");
            return;
        }
        if(ClientConfig.token.isBlank()){
            NexusClientMod.notifyPlayer("Pareie o Nexus para consultar o Gemini.");
            return;
        }
        if(waiting){NexusClientMod.notifyPlayer("Visão Arcana já está analisando uma captura.");return;}
        long now=System.currentTimeMillis(),currentDay=now/86400000L;
        if(day!=currentDay){day=currentDay;requests=0;}
        if(requests>=5||now-lastAt<60000L){
            NexusClientMod.notifyPlayer("Limite gratuito: no máximo 5 capturas/dia, intervalo de 60s.");
            return;
        }
        String prompt=(question==null?"":question).trim();
        if(prompt.length()>180)prompt=prompt.substring(0,180);
        if(prompt.isEmpty())prompt="Analise o que aparece na minha tela do Minecraft.";
        try(NativeImage screenshot=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            String jpeg=encodeJpeg(screenshot);
            if(jpeg.length()>540000)throw new IllegalStateException("Captura muito grande.");
            waiting=true;lastAt=now;requests++;
            lastResult="Captura enviada com autorização.";
            NexusArcaneHolograms.pulse();
            NexusClientMod.notifyPlayer("Visão Arcana: analisando captura pelo Gemini gratuito...");
            ClientLink.askArcaneVision(prompt,jpeg);
        }catch(Exception ex){
            waiting=false;
            NexusClientMod.notifyPlayer("Falha ao capturar imagem: "+ex.getMessage());
        }
    }
    private static String encodeJpeg(NativeImage image)throws Exception{
        int originalW=image.getWidth(),originalH=image.getHeight();
        if(originalW<1||originalH<1)throw new IllegalArgumentException("Framebuffer vazio.");
        double ratio=Math.min(1.0,Math.min(768.0/originalW,432.0/originalH));
        int w=Math.max(1,(int)(originalW*ratio)),h=Math.max(1,(int)(originalH*ratio));
        BufferedImage scaled=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int srcX=Math.min(originalW-1,(int)((x+0.5)/ratio));
            int srcY=Math.min(originalH-1,(int)((y+0.5)/ratio));
            int abgr=image.getPixelRGBA(srcX,srcY);
            int rgb=((abgr&255)<<16)|(abgr&0xFF00)|((abgr>>>16)&255);
            scaled.setRGB(x,y,rgb);
        }
        Iterator<ImageWriter> writers=ImageIO.getImageWritersByFormatName("jpeg");
        if(!writers.hasNext())throw new IllegalStateException("Codificador JPEG não encontrado.");
        ImageWriter writer=writers.next();
        try(ByteArrayOutputStream out=new ByteArrayOutputStream();
            MemoryCacheImageOutputStream stream=new MemoryCacheImageOutputStream(out)){
            writer.setOutput(stream);
            ImageWriteParam params=writer.getDefaultWriteParam();
            if(params.canWriteCompressed()){
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(.64f);
            }
            writer.write(null,new javax.imageio.IIOImage(scaled,null,null),params);
            stream.flush();
            return Base64.getEncoder().encodeToString(out.toByteArray());
        }finally{writer.dispose();}
    }
    static void answered(String text){
        waiting=false;
        if(!enabled())return;
        lastResult=String.valueOf(text).replaceAll("[\\p{Cntrl}]"," ").trim();
        if(lastResult.length()>800)lastResult=lastResult.substring(0,800);
        var mc=Minecraft.getInstance();
        if(mc.player!=null){
            int max=180;
            for(int i=0;i<lastResult.length()&&i<720;i+=max){
                String part=lastResult.substring(i,Math.min(i+max,lastResult.length()));
                mc.player.displayClientMessage(Component.literal("§6[Nexus Visão] §f"+part),false);
            }
        }
        NexusArcaneHolograms.pulse();
    }
    static void failed(String reason){
        waiting=false;
        if(!enabled())return;
        lastResult=String.valueOf(reason).replaceAll("[\\p{Cntrl}]"," ").trim();
        NexusClientMod.notifyPlayer("Visão Arcana: "+lastResult);
    }
}
