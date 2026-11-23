package com.port.nexusclient;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

/**
 * Owner-approved, single-frame screenshot analysis for Minecraft 1.21.1.
 * All uploads are manual. No persistent images, automatic polling or x-ray data.
 */
final class NexusArcaneVision {
    record Result(String focus,String text,boolean ok,long at){}
    private static final int DAILY_LIMIT=5,COOLDOWN_MS=60_000;
    private static final ArrayDeque<Result> HISTORY=new ArrayDeque<>();
    private static boolean enabled=false,pending=false,waiting=false,centerCrop=false;
    private static long lastAt=0,day=0,requestSerial=0,activeRequest=0;
    private static int requests=0,delay=0;
    private static NexusVisionModes.Focus focus=NexusVisionModes.Focus.GERAL;
    private static String pendingQuestion="";
    private static NexusVisionModes.Focus pendingFocus=NexusVisionModes.Focus.GERAL;
    private static boolean pendingCrop=false;
    private static String lastResult="Nenhuma análise solicitada.";

    private NexusArcaneVision(){}
    static boolean enabled(){return enabled;}
    static boolean busy(){return waiting||pending;}
    static NexusVisionModes.Focus focus(){return focus;}
    static boolean centerCrop(){return centerCrop;}
    static String lastResult(){return lastResult;}
    static List<Result> history(){return List.copyOf(HISTORY);}
    static void setFocus(NexusVisionModes.Focus selected){
        focus=selected==null?NexusVisionModes.Focus.GERAL:selected;
    }
    static void nextFocus(){focus=NexusVisionModes.next(focus);}
    static void setCenterCrop(boolean value){centerCrop=value;}
    static void setEnabled(boolean on){
        if(on&&ClientConfig.token.isBlank())
            throw new IllegalStateException("Pareie o Nexus com o site antes de ativar a Visão Arcana.");
        enabled=on;
        if(!on){
            pending=false;waiting=false;delay=0;
            activeRequest=++requestSerial; // Ignore late callbacks after revocation.
        }
    }
    private static void refreshQuota(){
        long today=System.currentTimeMillis()/86_400_000L;
        if(today!=day){day=today;requests=0;}
    }
    static int remaining(){refreshQuota();return Math.max(0,DAILY_LIMIT-requests);}
    static long cooldownSeconds(){
        return Math.max(0,(lastAt+COOLDOWN_MS-System.currentTimeMillis()+999L)/1000L);
    }
    static String status(){
        return "Visão Arcana "+(enabled?"ATIVA":"OFF")+
            " | "+(pending?"captura pendente":waiting?"analisando":"pronta")+
            " | foco: "+focus.label+
            " | imagem: "+(centerCrop?"centro ampliado":"tela completa")+
            " | consultas restantes nesta sessão: "+remaining()+
            " | intervalo: "+cooldownSeconds()+"s";
    }

    /** Explicit action, never triggered periodically or by the AI. */
    static void capture(String question){
        capture(question,focus,centerCrop);
    }
    static void capture(String question,NexusVisionModes.Focus selected,boolean crop){
        Minecraft mc=Minecraft.getInstance();
        if(!enabled){NexusClientMod.notifyPlayer("Ative /nexusclient vision on primeiro.");return;}
        if(mc.player==null||mc.getConnection()==null||mc.level==null){
            NexusClientMod.notifyPlayer("Entre em um mundo para analisar a imagem.");return;
        }
        if(ClientConfig.token.isBlank()){
            NexusClientMod.notifyPlayer("Pareie o Nexus com a Render para consultar o Gemini.");return;
        }
        if(busy()){NexusClientMod.notifyPlayer("A Visão Arcana já está ocupada.");return;}
        if(remaining()==0||cooldownSeconds()>0){
            NexusClientMod.notifyPlayer("Visão Arcana: "+remaining()+
                " consultas restantes. Próxima captura em "+cooldownSeconds()+"s.");
            return;
        }
        if(mc.screen!=null){
            NexusClientMod.notifyPlayer("Feche a interface antes de capturar o mundo.");return;
        }
        pendingQuestion=NexusVisionModes.prompt(selected,question);
        pendingFocus=selected==null?NexusVisionModes.Focus.GERAL:selected;
        pendingCrop=crop;
        pending=true;
        delay=4; // Let the game draw a clean frame after the F9 settings screen closes.
        lastResult="Preparando uma captura autorizada...";
        NexusClientMod.notifyPlayer("Visão Arcana: preparando 1 captura ("+
            pendingFocus.label+(crop?", centro ampliado":"")+")...");
    }

    /** Called on client tick, but captures only when a request was explicitly queued. */
    static void tick(){
        if(!pending)return;
        Minecraft mc=Minecraft.getInstance();
        if(!enabled||mc.player==null||mc.level==null){pending=false;return;}
        if(mc.screen!=null)return;
        if(--delay>0)return;
        pending=false;
        try(NativeImage image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
            String jpeg=encodeJpeg(image,pendingCrop);
            if(jpeg.length()>540_000)throw new IllegalStateException("Imagem acima de 540 KB em Base64.");
            waiting=true;
            lastAt=System.currentTimeMillis();
            requests++;
            activeRequest=++requestSerial;
            long id=activeRequest;
            lastResult="Imagem enviada, aguardando a análise...";
            NexusClientMod.notifyPlayer("Visão Arcana: enviando apenas este JPEG ao Gemini gratuito...");
            ClientLink.askArcaneVision(pendingQuestion,jpeg,id);
        }catch(Exception error){
            waiting=false;
            lastResult="Não foi possível capturar a imagem: "+error.getMessage();
            NexusClientMod.notifyPlayer("Visão Arcana: "+lastResult);
        }
    }
    private static String encodeJpeg(NativeImage image,boolean crop)throws Exception{
        int originalW=image.getWidth(),originalH=image.getHeight();
        if(originalW<1||originalH<1)throw new IllegalArgumentException("Imagem vazia.");
        int cropW=crop?Math.max(1,(int)(originalW*.63)):originalW;
        int cropH=crop?Math.max(1,(int)(originalH*.63)):originalH;
        int startX=(originalW-cropW)/2,startY=(originalH-cropH)/2;
        double ratio=Math.min(1.0,Math.min(768.0/cropW,432.0/cropH));
        int w=Math.max(1,(int)(cropW*ratio)),h=Math.max(1,(int)(cropH*ratio));
        BufferedImage scaled=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<h;y++){
            int srcY=startY+Math.min(cropH-1,(int)((y+0.5)/ratio));
            for(int x=0;x<w;x++){
                int srcX=startX+Math.min(cropW-1,(int)((x+0.5)/ratio));
                int abgr=image.getPixelRGBA(srcX,srcY);
                int rgb=((abgr&255)<<16)|(abgr&0xFF00)|((abgr>>>16)&255);
                scaled.setRGB(x,y,rgb);
            }
        }
        Iterator<ImageWriter> iter=ImageIO.getImageWritersByFormatName("jpeg");
        if(!iter.hasNext())throw new IllegalStateException("Codificador JPEG indisponível.");
        ImageWriter writer=iter.next();
        try(ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            MemoryCacheImageOutputStream output=new MemoryCacheImageOutputStream(bytes)){
            writer.setOutput(output);
            ImageWriteParam params=writer.getDefaultWriteParam();
            if(params.canWriteCompressed()){
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(.68f);
            }
            writer.write(null,new IIOImage(scaled,null,null),params);
            output.flush();
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        }finally{writer.dispose();}
    }
    private static void store(String value,boolean ok){
        lastResult=value==null?"Resposta vazia.":value.replaceAll("[\\p{Cntrl}]"," ").trim();
        if(lastResult.length()>800)lastResult=lastResult.substring(0,800);
        HISTORY.addFirst(new Result(pendingFocus.label,lastResult,ok,System.currentTimeMillis()));
        while(HISTORY.size()>6)HISTORY.removeLast();
    }
    static void answered(long requestId,String text){
        if(!enabled||!waiting||requestId!=activeRequest)return;
        waiting=false;
        store(text,true);
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null)return;
        String remaining=lastResult;
        while(!remaining.isBlank()){
            int cut=Math.min(170,remaining.length());
            if(cut<remaining.length()){
                int space=remaining.lastIndexOf(' ',cut);
                if(space>80)cut=space;
            }
            String part=remaining.substring(0,cut).trim();
            if(!part.isBlank())mc.player.displayClientMessage(
                Component.literal("§6[Nexus Visão] §f"+part),false);
            remaining=remaining.substring(cut).trim();
        }
        NexusClientMod.notifyPlayer("Relatório disponível em F9 (histórico local).");
    }
    static void failed(long requestId,String reason){
        if(!enabled||!waiting||requestId!=activeRequest)return;
        waiting=false;
        store(reason,false);
        NexusClientMod.notifyPlayer("Visão Arcana: "+lastResult);
    }
}
