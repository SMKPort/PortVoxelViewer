package com.port.performance;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * HUD rendering and placement shared by the live overlay and the editor.
 * Positions are normalized to the usable viewport, surviving GUI-scale and
 * resolution changes without allowing the panel to leave the visible area.
 */
final class PerformanceHud {
    static final int WIDTH=210;
    static final int HEIGHT=84;
    private static final int MARGIN_X=7;
    private static final int MARGIN_Y=8;

    private PerformanceHud(){}

    static int left(int width){
        if(width<=WIDTH+2*MARGIN_X)return Math.max(0,(width-WIDTH)/2);
        int usable=width-WIDTH-2*MARGIN_X;
        return MARGIN_X+Math.round(usable*PerformanceConfig.hudX);
    }

    static int top(int height){
        if(height<=HEIGHT+2*MARGIN_Y)return Math.max(0,(height-HEIGHT)/2);
        int usable=height-HEIGHT-2*MARGIN_Y;
        return MARGIN_Y+Math.round(usable*PerformanceConfig.hudY);
    }

    static void moveTo(double x,double y,int screenWidth,int screenHeight){
        int rangeX=screenWidth-WIDTH-2*MARGIN_X;
        int rangeY=screenHeight-HEIGHT-2*MARGIN_Y;
        PerformanceConfig.hudX=rangeX>0
            ?clamp((float)((x-MARGIN_X)/rangeX)):0f;
        PerformanceConfig.hudY=rangeY>0
            ?clamp((float)((y-MARGIN_Y)/rangeY)):0f;
    }

    static void reset(){
        PerformanceConfig.hudX=0f;
        PerformanceConfig.hudY=0f;
        PerformanceConfig.save();
    }

    private static float clamp(float n){
        if(!Float.isFinite(n))return 0f;
        return Math.max(0f,Math.min(1f,n));
    }

    static void draw(GuiGraphics graphics,Minecraft mc,boolean editMode){
        int x=left(graphics.guiWidth()),y=top(graphics.guiHeight());
        graphics.fill(x,y,x+WIDTH,y+HEIGHT,0xD20F0C14);
        graphics.fill(x,y,x+WIDTH,y+2,0xFFE0B568);
        graphics.drawString(mc.font,"PORT  /  PERFORMANCE",x+9,y+8,0xFFEAC082,true);
        graphics.drawString(mc.font,"FPS: "+PerformanceStats.fps(),x+9,y+24,0xFFFAF7F1,true);
        graphics.drawString(mc.font,"PING: "+(PerformanceStats.ping()<0?"N/D":PerformanceStats.ping()+" ms"),x+92,y+24,0xFFFAF7F1,true);
        graphics.drawString(mc.font,"RAM: "+PerformanceStats.used()+" / "+PerformanceStats.max()+" MB",x+9,y+39,0xFFC6B5C1,true);
        graphics.drawString(mc.font,PerformanceConfig.streamEnabled?"WEB: TRANSMITINDO":"WEB: OFF",x+9,y+53,
            PerformanceConfig.streamEnabled?0xFF8BDDAF:0xFFBCA3A8,true);
        String boostMode=BoosterEngine.status().split(" \\| ",2)[0].replace("Modo ","").toUpperCase(java.util.Locale.ROOT);
        graphics.drawString(mc.font,"BOOSTER: "+boostMode+" / META "+PerformanceConfig.boosterTargetFps,
            x+9,y+67,0xFFE5B571,true);

        if(editMode){
            // Visible draggable outline without obscuring the regular metrics.
            int border=0xFFF0C582;
            graphics.fill(x-2,y-2,x+WIDTH+2,y-1,border);
            graphics.fill(x-2,y+HEIGHT+1,x+WIDTH+2,y+HEIGHT+2,border);
            graphics.fill(x-2,y-1,x-1,y+HEIGHT+1,border);
            graphics.fill(x+WIDTH+1,y-1,x+WIDTH+2,y+HEIGHT+1,border);
        }
    }
}
