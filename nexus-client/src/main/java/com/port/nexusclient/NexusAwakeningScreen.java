package com.port.nexusclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The awakening controls do not grant permissions merely by opening. */
final class NexusAwakeningScreen extends Screen {
    private static final int GOLD=0xFFE9BB68,WHITE=0xFFECE1D3,MUTED=0xFFAE9DA6;
    private final Screen parent;
    private String message="O Nexus só conhece os dados que seus sensores realmente conseguem ler.";

    NexusAwakeningScreen(Screen parent){
        super(Component.literal("Nexus Aterna | O Despertar"));
        this.parent=parent;
    }
    private int pane(){return Math.min(550,width-20);}
    private int left(){return (width-pane())/2;}
    private String fit(String value,int pixels){
        String s=String.valueOf(value);
        while(s.length()>1&&font.width(s)>pixels)s=s.substring(0,s.length()-1);
        return s;
    }
    @Override protected void init(){
        int x=left(),w=pane(),half=(w-22)/2;
        addRenderableWidget(Button.builder(Component.literal("§6ANALISAR SITUAÇÃO"),
            b->{
                NexusAwakening.report(false);
                message="Análise enviada ao seu chat local.";
            }).bounds(x+7,66,w-14,22).build());
        addRenderableWidget(Button.builder(Component.literal(NexusAwakening.protectionEnabled()?
            "§aPROTEÇÃO ON":"§cPROTEÇÃO OFF"),b->{
                message=NexusAwakening.protection(!NexusAwakening.protectionEnabled());
                rebuildWidgets();
            }).bounds(x+7,102,half,22).build());
        addRenderableWidget(Button.builder(Component.literal(NexusAwakening.warningsEnabled()?
            "§aAVISOS ON":"§cAVISOS OFF"),b->{
                message=NexusAwakening.warnings(!NexusAwakening.warningsEnabled());
                rebuildWidgets();
            }).bounds(x+13+half,102,half,22).build());
        addRenderableWidget(Button.builder(Component.literal(NexusAwakening.journalEnabled()?
            "§aDIÁRIO LOCAL ON":"§cDIÁRIO LOCAL OFF"),b->{
                message=NexusAwakening.journal(!NexusAwakening.journalEnabled());
                rebuildWidgets();
            }).bounds(x+7,143,half,22).build());
        addRenderableWidget(Button.builder(Component.literal("§6EVENTOS RECENTES"),b->{
            for(String text:NexusAwakening.journalEntries())
                NexusClientMod.notifyPlayer(text);
            message="Últimos eventos enviados ao chat local.";
        }).bounds(x+13+half,143,half,22).build());
        addRenderableWidget(Button.builder(Component.literal("− 5%"),
            b->adjust(-5)).bounds(x+7,186,half,22).build());
        addRenderableWidget(Button.builder(Component.literal("+ 5%"),
            b->adjust(5)).bounds(x+13+half,186,half,22).build());
        if(height>=310){
            addRenderableWidget(Button.builder(Component.literal("§6GERENCIAR MEMÓRIAS NARRATIVAS"),
                b->minecraft.setScreen(new NexusMemoryScreen(this)))
                .bounds(x+7,231,w-14,22).build());
        }
        addRenderableWidget(Button.builder(Component.literal("VOLTAR"),b->onClose())
            .bounds(x+w-101,height-30,94,20).build());
    }
    private void adjust(int delta){
        try{
            message=NexusAwakening.threshold(Math.max(15,
                Math.min(60,NexusAwakening.criticalPercent()+delta)));
            rebuildWidgets();
        }catch(Exception error){message=error.getMessage();}
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float partialTick){
        int x=left(),w=pane();
        g.fill(x-5,6,x+w+5,height-6,0xEF0D0A14);
        g.fill(x-5,6,x+w+5,9,GOLD);
        g.drawCenteredString(font,"NEXUS ATERNA 4.0 | O DESPERTAR",width/2,22,GOLD);
        g.drawString(font,"Análise contextual: local, sem IA externa obrigatória",
            x+7,46,MUTED,false);
        g.drawString(font,"Protocolos: suspendem apenas novas macros ofensivas",
            x+7,92,WHITE,false);
        g.drawString(font,"Registro opcional: não grava conversas, nomes ou coordenadas",
            x+7,132,MUTED,false);
        g.drawString(font,"Limiar crítico: "+NexusAwakening.criticalPercent()+"%",
            x+7,175,GOLD,false);
        g.drawString(font,"Vida crítica: proteção não ativa Heal por conta própria.",
            x+7,217,MUTED,false);
        if(height>=330)g.drawString(font,fit(message,w-14),
            x+7,height-58,WHITE,false);
        super.render(g,mx,my,partialTick);
    }
}
