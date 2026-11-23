package com.port.performance;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Position and scale share normalized coordinates, compatible with different GUI resolutions. */
final class PerformanceHud {
    static final int WIDTH=245,HEIGHT=119;
    private static final int MARGIN_X=7,MARGIN_Y=8;
    private PerformanceHud(){}

    static float scale(int screenWidth,int screenHeight){
        float fits=Math.min((screenWidth-2f*MARGIN_X)/WIDTH,
                            (screenHeight-2f*MARGIN_Y)/HEIGHT);
        return Math.max(0.35f,Math.min(PerformanceConfig.hudScale,fits));
    }
    static int panelWidth(int screenWidth,int screenHeight){
        return Math.round(WIDTH*scale(screenWidth,screenHeight));
    }
    static int panelHeight(int screenWidth,int screenHeight){
        return Math.round(HEIGHT*scale(screenWidth,screenHeight));
    }
    static int left(int screenWidth,int screenHeight){
        int w=panelWidth(screenWidth,screenHeight);
        if(screenWidth<=w+2*MARGIN_X)return Math.max(0,(screenWidth-w)/2);
        return MARGIN_X+Math.round((screenWidth-w-2*MARGIN_X)*PerformanceConfig.hudX);
    }
    static int top(int screenWidth,int screenHeight){
        int h=panelHeight(screenWidth,screenHeight);
        if(screenHeight<=h+2*MARGIN_Y)return Math.max(0,(screenHeight-h)/2);
        return MARGIN_Y+Math.round((screenHeight-h-2*MARGIN_Y)*PerformanceConfig.hudY);
    }
    static void moveTo(double x,double y,int screenWidth,int screenHeight){
        int rangeX=screenWidth-panelWidth(screenWidth,screenHeight)-2*MARGIN_X;
        int rangeY=screenHeight-panelHeight(screenWidth,screenHeight)-2*MARGIN_Y;
        PerformanceConfig.hudX=rangeX>0?clamp((float)((x-MARGIN_X)/rangeX),0f,1f):0f;
        PerformanceConfig.hudY=rangeY>0?clamp((float)((y-MARGIN_Y)/rangeY),0f,1f):0f;
    }
    static void previewScale(float value){
        PerformanceConfig.hudScale=clamp(value,0.55f,2.25f);
    }
    static void setScale(float value){
        previewScale(value);
        PerformanceConfig.save();
    }
    static void reset(){
        PerformanceConfig.hudX=0f;
        PerformanceConfig.hudY=0f;
        PerformanceConfig.hudScale=1f;
        PerformanceConfig.save();
    }
    private static float clamp(float n,float min,float max){
        return Float.isFinite(n)?Math.max(min,Math.min(max,n)):min;
    }

    static void draw(GuiGraphics g,Minecraft mc,boolean editMode){
        int sw=g.guiWidth(),sh=g.guiHeight();
        int x=left(sw,sh),y=top(sw,sh);
        int w=panelWidth(sw,sh),h=panelHeight(sw,sh);
        float scale=scale(sw,sh);
        g.pose().pushPose();
        g.pose().translate(x,y,0);
        g.pose().scale(scale,scale,1);
        g.fill(0,0,WIDTH,HEIGHT,0xD20F0C14);
        g.fill(0,0,WIDTH,2,0xFFE0B568);
        g.drawString(mc.font,"PORT  /  PERFORMANCE 4.0",9,8,0xFFEAC082,true);
        g.drawString(mc.font,"FPS: "+PerformanceStats.fps(),9,26,0xFFFAF7F1,true);
        g.drawString(mc.font,"PING: "+(PerformanceStats.ping()<0?"N/D":PerformanceStats.ping()+" ms"),103,26,0xFFFAF7F1,true);
        g.drawString(mc.font,"RAM: "+PerformanceStats.used()+" / "+PerformanceStats.max()+" MB",9,41,0xFFC6B5C1,true);
        g.drawString(mc.font,"1% LOW: "+Math.round(BoosterEngine.low1())+
            "  |  P95: "+String.format(java.util.Locale.ROOT,"%.1f",BoosterEngine.p95())+" ms",
            9,55,0xFFACD7D2,true);
        String m=BoosterEngine.status().split(" \\| ",2)[0].replace("Modo ","");
        g.drawString(mc.font,"BOOST: "+m.toUpperCase(java.util.Locale.ROOT)+
            "  |  CHUNKS: "+BoosterEngine.currentChunks(),9,70,0xFFE5B571,true);
        double clientCpu=HardwareMonitor.clientCpuPercent();
        double processCpu=HardwareMonitor.processCpuPercent();
        g.drawString(mc.font,"CPU CLIENTE: "+(clientCpu<0?"N/D":Math.round(clientCpu)+"%")+
            " | CPU JVM: "+(processCpu<0?"N/D":Math.round(processCpu)+"%"),9,84,0xFFB0C8F2,true);
        g.drawString(mc.font,"PERFIL: "+PerformanceConfig.boosterProfile.toUpperCase(java.util.Locale.ROOT)+
            " | RAM LIVRE: "+(HardwareMonitor.freeSystemMb()<0?"N/D":HardwareMonitor.freeSystemMb()+" MB"),
            9,97,0xFFDFCEBD,true);
        g.drawString(mc.font,PerformanceConfig.streamEnabled?"WEB: TRANSMITINDO":"WEB: OFF",
            9,109,PerformanceConfig.streamEnabled?0xFF8BDDAF:0xFFBCA3A8,true);
        g.pose().popPose();

        if(editMode){
            int border=0xFFF0C582;
            g.fill(x-2,y-2,x+w+2,y-1,border);
            g.fill(x-2,y+h+1,x+w+2,y+h+2,border);
            g.fill(x-2,y-1,x-1,y+h+1,border);
            g.fill(x+w+1,y-1,x+w+2,y+h+1,border);
            // Resize handle in bottom-right corner.
            g.fill(x+w-13,y+h-13,x+w+2,y+h+2,0xFFE0B568);
            g.fill(x+w-8,y+h-8,x+w-2,y+h-2,0xFF211922);
        }
    }
}
