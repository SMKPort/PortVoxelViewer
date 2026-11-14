package com.port.nexusclient;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Purely visual combat and Iron's Spells HUD. Never emits chat or API requests. */
final class NexusCombatHud {
    private static final int WIDTH=212,BASE_HEIGHT=74,MAGIC_HEIGHT=79;
    private static final int GOLD=0xFFE3BC76,RED=0xFFFF7777,WHITE=0xFFEDE0CF;
    private static final ArrayDeque<Alert> ALERTS=new ArrayDeque<>();
    private record Alert(String kind,String message,long expiresAt){}

    private NexusCombatHud(){}
    static synchronized boolean show(String kind,String raw){
        if(!NexusHudSettings.get("combat").enabled)return false;
        if(raw==null||raw.isBlank())return false;
        String message=raw.replaceAll("[\\p{Cntrl}]"," ").trim();
        message=message.substring(0,Math.min(205,message.length()));
        long until=System.currentTimeMillis()+9000;
        ALERTS.addFirst(new Alert(kind,message,until));
        while(ALERTS.size()>3)ALERTS.removeLast();
        return true;
    }
    static synchronized void clear(){ALERTS.clear();}
    static synchronized List<String> active(){
        long now=System.currentTimeMillis();
        ALERTS.removeIf(a->now>a.expiresAt());
        return ALERTS.stream().map(Alert::message).toList();
    }
    static int panelWidth(String id){
        NexusHudSettings.Layout l=NexusHudSettings.get(id);
        return Math.round(WIDTH*l.scale);
    }
    static int panelHeight(String id){
        NexusHudSettings.Layout l=NexusHudSettings.get(id);
        return Math.round(("magic".equals(id)?MAGIC_HEIGHT:BASE_HEIGHT)*l.scale);
    }
    static int posX(String id,int w){
        NexusHudSettings.Layout l=NexusHudSettings.get(id);
        int panel=panelWidth(id),margin=6;
        return margin+Math.round(l.x*Math.max(0,w-panel-margin*2));
    }
    static int posY(String id,int h){
        NexusHudSettings.Layout l=NexusHudSettings.get(id);
        int panel=panelHeight(id),margin=6;
        return margin+Math.round(l.y*Math.max(0,h-panel-margin*2));
    }

    static void render(GuiGraphics graphics){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.options.hideGui||mc.screen!=null)return;
        if(NexusHudSettings.get("combat").enabled&&!active().isEmpty())
            drawCombat(graphics,false);
        if(NexusHudSettings.get("magic").enabled&&NexusArcaneSensor.settingEnabled())
            drawMagic(graphics,false);
    }
    static void renderPreview(GuiGraphics g){
        drawCombat(g,true);
        drawMagic(g,true);
    }
    private static void begin(GuiGraphics g,String id,int width,int height){
        float scale=NexusHudSettings.get(id).scale;
        g.pose().pushPose();
        g.pose().translate(posX(id,g.guiWidth()),posY(id,g.guiHeight()),0);
        g.pose().scale(scale,scale,1f);
        g.fill(0,0,width,height,0xDC0E0A12);
        g.fill(0,0,width,2,0xFFE0B457);
        g.fill(0,height-2,width,height,0xFF48252A);
    }
    private static void drawCombat(GuiGraphics g,boolean preview){
        Minecraft mc=Minecraft.getInstance();
        if(mc.font==null)return;
        begin(g,"combat",WIDTH,BASE_HEIGHT);
        g.drawString(mc.font,"NEXUS / ALERTAS DE COMBATE",7,6,GOLD,false);
        float hp=mc.player==null?16:mc.player.getHealth();
        float max=mc.player==null?20:Math.max(1,mc.player.getMaxHealth());
        int health=(int)Math.round(Math.max(0,Math.min(1,hp/max))*198);
        g.fill(7,19,205,25,0xFF30242D);
        g.fill(7,19,7+health,25,hp/max<=.3?0xFFFF5E5E:0xFFAD493D);
        g.drawString(mc.font,"VIDA "+Math.round(hp)+"/"+Math.round(max),8,28,WHITE,false);
        if(NexusVitals.isEnabled())
            g.drawString(mc.font,"PLACA "+Math.round(NexusVitals.integrity())+"%",122,28,GOLD,false);
        String text=preview?"Impacto elevado. Port, recomendo buscar cobertura.":null;
        if(!preview){
            List<String> messages=active();
            if(!messages.isEmpty())text=messages.get(0);
        }
        if(text==null)text="Nenhuma ameaça registrada.";
        int y=42;
        for(FormattedCharSequence line:mc.font.split(Component.literal(text),WIDTH-14)){
            g.drawString(mc.font,line,7,y,RED,false);
            y+=10;if(y>BASE_HEIGHT-9)break;
        }
        g.pose().popPose();
    }
    private static void drawMagic(GuiGraphics g,boolean preview){
        Minecraft mc=Minecraft.getInstance();
        if(mc.font==null)return;
        begin(g,"magic",WIDTH,MAGIC_HEIGHT);
        g.drawString(mc.font,"NEXUS / IRON'S SPELLS",7,6,GOLD,false);
        if(preview){
            g.drawString(mc.font,"MANA  82 / 110",7,21,WHITE,false);
            g.fill(7,33,205,40,0xFF252232);
            g.fill(7,33,155,40,0xFF8D69E9);
            g.drawString(mc.font,"Sunbeam  |  Canalização",7,50,WHITE,false);
        }else{
            JsonObject state=NexusArcaneSensor.snapshot();
            boolean available=state.has("available")&&state.get("available").getAsBoolean();
            if(available){
                double mana=state.has("mana")?state.get("mana").getAsDouble():0;
                double max=state.has("maxMana")?state.get("maxMana").getAsDouble():0;
                boolean hasMax=max>0;
                int percent=hasMax?(int)Math.round(Math.max(0,Math.min(1,mana/max))*100):0;
                g.drawString(mc.font,"MANA "+Math.round(mana)+(hasMax?" / "+Math.round(max):""),7,21,WHITE,false);
                g.fill(7,33,205,40,0xFF252232);
                g.fill(7,33,7+Math.round(198*percent/100f),40,0xFF8D69E9);
                String spell=state.has("casting")&&state.get("casting").getAsBoolean()?
                    "CONJURANDO: "+state.get("spellName").getAsString():"Nenhum feitiço ativo";
                int i=0;
                for(FormattedCharSequence line:mc.font.split(Component.literal(spell),198)){
                    g.drawString(mc.font,line,7,50+i*10,WHITE,false);
                    if(++i>=2)break;
                }
            }else{
                g.drawString(mc.font,"Aguardando dados do Iron's Spells...",7,26,WHITE,false);
                g.drawString(mc.font,"Detecção local, sem IA paga.",7,45,0xFFAF9EBD,false);
            }
        }
        g.pose().popPose();
    }
}
