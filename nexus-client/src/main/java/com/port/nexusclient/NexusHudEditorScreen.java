package com.port.nexusclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class NexusHudEditorScreen extends Screen {
    private final Screen parent;
    private String selected="combat";
    private String dragging="";
    private double dragOffsetX,dragOffsetY;
    private String note="Arraste os painéis para reposicionar. Escala e visibilidade são independentes.";

    NexusHudEditorScreen(Screen parent){
        super(Component.literal("Nexus Aterna | Editor de HUD"));
        this.parent=parent;
    }

    @Override protected void init(){
        int x=12;
        addRenderableWidget(Button.builder(Component.literal("COMBATE"),b->{selected="combat";rebuildWidgets();})
            .bounds(x,34,79,20).build());
        addRenderableWidget(Button.builder(Component.literal("MAGIA"),b->{selected="magic";rebuildWidgets();})
            .bounds(x+85,34,79,20).build());
        addRenderableWidget(Button.builder(Component.literal("−"),b->resize(-.1f))
            .bounds(x,61,30,20).build());
        addRenderableWidget(Button.builder(Component.literal("+"),b->resize(.1f))
            .bounds(x+36,61,30,20).build());
        addRenderableWidget(Button.builder(Component.literal(NexusHudSettings.get(selected).enabled?"§aVISÍVEL":"§cOCULTO"),
            b->{
                NexusHudSettings.Layout l=NexusHudSettings.get(selected);
                NexusHudSettings.enabled(selected,!l.enabled);
                rebuildWidgets();
            }).bounds(x+74,61,90,20).build());
        addRenderableWidget(Button.builder(Component.literal("RESTAURAR POSIÇÕES"),b->{
            NexusHudSettings.reset();
            note="Posições e escalas restauradas.";
            rebuildWidgets();
        }).bounds(x,88,164,20).build());
        addRenderableWidget(Button.builder(Component.literal("CONCLUÍDO"),b->onClose())
            .bounds(width-100,height-31,88,20).build());
    }
    private void resize(float delta){
        NexusHudSettings.Layout l=NexusHudSettings.get(selected);
        NexusHudSettings.scale(selected,l.scale+delta);
        note="Escala "+selected+": "+Math.round(NexusHudSettings.get(selected).scale*100)+"%";
        rebuildWidgets();
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(super.mouseClicked(mx,my,button))return true;
        if(button==0){
            for(String id:new String[]{"combat","magic"}){
                int x=NexusCombatHud.posX(id,width),y=NexusCombatHud.posY(id,height);
                int w=NexusCombatHud.panelWidth(id),h=NexusCombatHud.panelHeight(id);
                if(mx>=x&&mx<=x+w&&my>=y&&my<=y+h){
                    selected=id;dragging=id;dragOffsetX=mx-x;dragOffsetY=my-y;
                    return true;
                }
            }
        }
        return false;
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){
        if(button==0&&!dragging.isEmpty()){
            int panelW=NexusCombatHud.panelWidth(dragging),panelH=NexusCombatHud.panelHeight(dragging);
            float x=(float)((mx-dragOffsetX-6)/Math.max(1,width-panelW-12));
            float y=(float)((my-dragOffsetY-6)/Math.max(1,height-panelH-12));
            NexusHudSettings.place(dragging,x,y);
            return true;
        }
        return super.mouseDragged(mx,my,button,dx,dy);
    }
    @Override public boolean mouseReleased(double mx,double my,int button){
        if(button==0&&!dragging.isEmpty()){
            String item=dragging;dragging="";
            note="Nova posição do "+item+" salva no computador.";
            rebuildWidgets();
            return true;
        }
        return super.mouseReleased(mx,my,button);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        g.fill(3,3,width-3,27,0xD80D0910);
        g.drawCenteredString(font,"NEXUS ATERNA | EDITOR DE HUD",width/2,8,0xFFE9BB68);
        NexusCombatHud.renderPreview(g);
        g.drawString(font,selected.equals("combat")?"Painel selecionado: COMBATE":"Painel selecionado: MAGIA",
            12,119,0xFFE9BB68,false);
        g.drawString(font,"Escala: "+Math.round(NexusHudSettings.get(selected).scale*100)+"%",
            12,134,0xFFE9E0DC,false);
        if(height>=275)
            g.drawString(font,shorten(note,width-30),12,height-47,0xFFD0C4A6,false);
        super.render(g,mx,my,partial);
    }
    private String shorten(String text,int max){
        while(!text.isEmpty()&&font.width(text)>max)text=text.substring(0,text.length()-1);
        return text;
    }
}
