package com.port.nexusclient;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/**
 * User-triggered screenshot only. Captures on the render/client thread AFTER
 * the consent menu closes, downsamples in memory, and never saves pixels to disk.
 */
final class NexusVision {
    private static boolean pending,busy;
    private static int delay;
    private static String question="",lastAnswer="Nenhuma análise realizada.";
    private NexusVision(){}

    static boolean enabled(){return ClientConfig.allowVision;}
    static String lastAnswer(){return lastAnswer;}
    static String status(){
        return "Visão Arcana "+(enabled()?"ON":"OFF")+" | "+
            (busy?"processando":pending?"captura pendente":"ociosa")+
            " | 5 imagens/dia e 60 s entre análises (servidor)";
    }
    static void setEnabled(boolean enabled){
        ClientConfig.allowVision=enabled;ClientConfig.save();
        if(!enabled)pending=false;
    }
    static void request(String text){
        Minecraft mc=Minecraft.getInstance();
        if(!enabled()){
            NexusClientMod.notifyPlayer("Ative /nexusclient vision on primeiro.");
            return;
        }
        if(mc.player==null||mc.level==null){
            NexusClientMod.notifyPlayer("Entre em um mundo para analisar a tela.");
            return;
        }
        if(ClientConfig.token.isBlank()){
            NexusClientMod.notifyPlayer("Pareie o Nexus com o painel antes de usar o Gemini.");
            return;
        }
        String safe=text==null?"":text.trim();
        if(safe.length()<3||safe.length()>180){
            NexusClientMod.notifyPlayer("A pergunta deve ter entre 3 e 180 caracteres.");
            return;
        }
        if(busy||pending){
            NexusClientMod.notifyPlayer("A Visão Arcana já está ocupada.");
            return;
        }
        question=safe;pending=true;delay=3;
        lastAnswer="Aguardando captura autorizada...";
        NexusClientMod.notifyPlayer("Captura autorizada nesta consulta. Fechando interface para fotografar o mundo.");
    }
    static void tick(){
        if(!pending)return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null){pending=false;return;}
        if(mc.screen!=null)return;
        if(--delay>0)return;
        pending=false;busy=true;
        try(NativeImage image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            int width=image.getWidth(),height=image.getHeight();
            if(width<=0||height<=0)throw new IllegalStateException("Frame vazio.");
            double factor=Math.min(1.0,Math.min(640.0/width,360.0/height));
            int w=Math.max(1,(int)Math.round(width*factor));
            int h=Math.max(1,(int)Math.round(height*factor));
            BufferedImage preview=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
            for(int y=0;y<h;y++){
                int sourceY=Math.min(height-1,(int)(y/factor));
                for(int x=0;x<w;x++){
                    int pixel=image.getPixelRGBA(Math.min(width-1,(int)(x/factor)),sourceY);
                    int rgb=((pixel&255)<<16)|(pixel&0xFF00)|((pixel>>>16)&255);
                    preview.setRGB(x,y,rgb);
                }
            }
            ByteArrayOutputStream buffer=new ByteArrayOutputStream(110000);
            if(!ImageIO.write(preview,"jpg",buffer))
                throw new IllegalStateException("Codificador JPEG não encontrado.");
            byte[] jpeg=buffer.toByteArray();
            if(jpeg.length>390000)throw new IllegalStateException("Imagem excede o limite de 390 KB.");
            NexusClientMod.notifyPlayer("Enviando captura reduzida ao Gemini (somente esta imagem)...");
            ClientLink.analyzeVision(question,jpeg);
        }catch(Exception ex){
            complete("Não foi possível capturar a imagem: "+ex.getMessage(),false);
        }
    }
    static void complete(String reply,boolean success){
        busy=false;lastAnswer=reply==null?"Resposta vazia.":reply;
        NexusClientMod.notifyPlayer((success?"Visão Arcana: ":"Visão Arcana (falha): ")+lastAnswer);
    }
}
