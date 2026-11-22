package com.port.nexusclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Explicit single-shot image consent and local hologram controls. */
final class NexusVisionScreen extends Screen {
    private static final int GOLD=0xFFE9BB68,SOFT=0xFFBCAEA4,GREEN=0xFFA3E1B2;
    private final Screen parent;
    private EditBox question;
    private String message="Nenhuma imagem é enviada automaticamente.";
    NexusVisionScreen(Screen parent){
        super(Component.literal("Nexus Aterna | Visão Arcana e Holograma"));
        this.parent=parent;
    }
    private int panelW(){return Math.min(560,width-16);}
    private int left(){return (width-panelW())/2;}
    @Override protected void init(){
        int x=left(),w=panelW(),half=(w-22)/2;
        int y=Math.max(40,height/2-127);
        question=new EditBox(font,x+8,y+28,w-16,20,Component.literal("Pergunta sobre a tela"));
        question.setMaxLength(180);
        question.setValue("O que aparece nesta cena do Minecraft?");
        addRenderableWidget(question);
        addRenderableWidget(Button.builder(Component.literal(
            "Visão Arcana: "+(NexusVision.enabled()?"§aATIVA":"§cDESLIGADA")),b->{
            NexusVision.setEnabled(!NexusVision.enabled());
            rebuild();
        }).bounds(x+8,y+56,half,20).build());
        addRenderableWidget(Button.builder(Component.literal("§6ANALISAR TELA UMA VEZ"),b->{
            String q=question.getValue();
            minecraft.setScreen(null);
            NexusVision.request(q);
        }).bounds(x+14+half,y+56,half,20).build());
        addRenderableWidget(Button.builder(Component.literal("Holograma: "+
            (NexusHologram.enabled()?"§aON":"§cOFF")),b->{
            NexusHologram.setEnabled(!NexusHologram.enabled());rebuild();
        }).bounds(x+8,y+91,half,20).build());
        addRenderableWidget(Button.builder(Component.literal("Modelo: "+NexusHologram.model()),b->{
            NexusHologram.nextModel();rebuild();
        }).bounds(x+14+half,y+91,half,20).build());
        addRenderableWidget(Button.builder(Component.literal("Tamanho −"),b->{
            NexusHologram.adjustScale(-.1);rebuild();
        }).bounds(x+8,y+119,half,20).build());
        addRenderableWidget(Button.builder(Component.literal("Tamanho +"),b->{
            NexusHologram.adjustScale(.1);rebuild();
        }).bounds(x+14+half,y+119,half,20).build());
        addRenderableWidget(Button.builder(Component.literal("Distância −"),b->{
            NexusHologram.adjustDistance(-.25);rebuild();
        }).bounds(x+8,y+147,half,20).build());
        addRenderableWidget(Button.builder(Component.literal("Distância +"),b->{
            NexusHologram.adjustDistance(.25);rebuild();
        }).bounds(x+14+half,y+147,half,20).build());
        addRenderableWidget(Button.builder(Component.literal("VOLTAR"),b->onClose())
            .bounds(x+8,y+201,w-16,20).build());
    }
    private void rebuild(){
        String old=question.getValue();
        minecraft.setScreen(new NexusVisionScreen(parent));
        if(minecraft.screen instanceof NexusVisionScreen screen)
            screen.question.setValue(old);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick){
        int x=left(),w=panelW(),y=Math.max(40,height/2-127);
        g.fill(x,Math.max(7,y-24),x+w,Math.min(height-12,y+239),0xED120F17);
        g.fill(x,Math.max(7,y-24),x+w,Math.max(9,y-21),0xFFD3A85D);
        g.drawCenteredString(font,"NEXUS ATERNA | VISÃO ARCANA",width/2,y-15,GOLD);
        g.drawString(font,"Pergunta enviada com UMA captura, somente após o clique:",x+8,y+10,SOFT,false);
        g.drawString(font,"Holograma em partículas locais, sem consumo de API:",x+8,y+82,GOLD,false);
        g.drawString(font,"Tamanho "+String.format(java.util.Locale.ROOT,"%.1f",NexusHologram.scale())+
            " | Distância "+String.format(java.util.Locale.ROOT,"%.1f",NexusHologram.distance()),
            x+8,y+177,SOFT,false);
        g.drawString(font,message,x+8,y+188,GREEN,false);
        super.render(g,mouseX,mouseY,partialTick);
    }
}
