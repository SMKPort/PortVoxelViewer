package com.port.nexusclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Opt-in control room for Nexus 4.0. No destructive automation on opening. */
final class NexusCognitionScreen extends Screen {
    private static final int GOLD=0xFFE9BB68,WHITE=0xFFECE1D3,MUTED=0xFFAE9DA6;
    private final Screen parent;
    private String message="O Nexus analisa apenas indicadores observáveis do seu cliente.";
    NexusCognitionScreen(Screen parent){
        super(Component.literal("Nexus Aterna | Consciência Contextual 4.0"));
        this.parent=parent;
    }
    private int pane(){return Math.min(550,width-20);}
    private int left(){return (width-pane())/2;}
    private String shortText(String value,int pixels){
        String s=String.valueOf(value);
        while(s.length()>1&&font.width(s)>pixels)s=s.substring(0,s.length()-1);
        return s;
    }
    @Override protected void init(){
        int x=left(),w=pane(),half=(w-22)/2;
        addRenderableWidget(Button.builder(Component.literal("§6SITUAÇÃO ATUAL"),
            b->{
                for(String line:NexusCognition.report())
                    NexusClientMod.notifyPlayer(line);
                message="Relatório enviado apenas ao seu chat local.";
            }).bounds(x+7,45,w-14,21).build());
        addRenderableWidget(Button.builder(Component.literal(
            NexusCognition.monitor()?"§aMONITOR ON":"§cMONITOR OFF"),b->{
                NexusCognition.monitor(!NexusCognition.monitor());rebuildWidgets();
            }).bounds(x+7,77,half,21).build());
        addRenderableWidget(Button.builder(Component.literal(
            NexusCognition.emergency()?"§aEMERGÊNCIA ON":"§cEMERGÊNCIA OFF"),b->{
                NexusCognition.emergency(!NexusCognition.emergency());rebuildWidgets();
            }).bounds(x+13+half,77,half,21).build());
        addRenderableWidget(Button.builder(Component.literal(
            NexusCombatInsights.enabled()?"§aHISTÓRICO ON":"§cHISTÓRICO OFF"),b->{
                NexusCombatInsights.setEnabled(!NexusCombatInsights.enabled());
                rebuildWidgets();
            }).bounds(x+7,108,half,21).build());
        addRenderableWidget(Button.builder(Component.literal(
            NexusNarrativeJournal.autoCritical()?"§aDIÁRIO AUTO ON":"§cDIÁRIO AUTO OFF"),b->{
                NexusNarrativeJournal.autoCritical(!NexusNarrativeJournal.autoCritical());
                rebuildWidgets();
            }).bounds(x+13+half,108,half,21).build());
        addRenderableWidget(Button.builder(Component.literal("§6MEMÓRIA / DIÁRIO"),
            b->minecraft.setScreen(new NexusNarrativeScreen(this)))
            .bounds(x+7,139,half,21).build());
        addRenderableWidget(Button.builder(Component.literal(
            NexusCombatMacro.emergencySuspended()?"§aRETOMAR MACROS":"MACROS NÃO SUSPENSAS"),b->{
                NexusCombatMacro.resumeFromEmergency();
                message="Macros liberadas pelo jogador. Automações não habilitadas aqui.";
                rebuildWidgets();
            }).bounds(x+13+half,139,half,21).build());
        addRenderableWidget(Button.builder(Component.literal("−5%"),
            b->changeThreshold(-5)).bounds(x+7,174,half,20).build());
        addRenderableWidget(Button.builder(Component.literal("+5%"),
            b->changeThreshold(5)).bounds(x+13+half,174,half,20).build());
        addRenderableWidget(Button.builder(Component.literal("VOLTAR"),b->onClose())
            .bounds(x+w-103,height-30,96,20).build());
    }
    private void changeThreshold(int step){
        try{
            NexusCognition.threshold(Math.max(10,Math.min(60,
                NexusCognition.threshold()+step)));
            rebuildWidgets();
        }catch(Exception e){message=e.getMessage();}
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float pt){
        int x=left(),w=pane();
        g.fill(x-5,6,x+w+5,height-6,0xEF0D0A14);
        g.fill(x-5,6,x+w+5,9,GOLD);
        g.drawCenteredString(font,"NEXUS ATERNA | CONSCIÊNCIA CONTEXTUAL 4.0",
            width/2,19,GOLD);
        g.drawString(font,"Limiar de emergência: vida ≤ "+NexusCognition.threshold()+"%",
            x+7,162,GOLD,false);
        g.drawString(font,"Análise local (sem ações ou mensagens ao servidor)",
            x+7,208,MUTED,false);
        List<String> report=NexusCognition.report();
        int maxLines=Math.max(0,Math.min(report.size(),(height-282)/16));
        for(int i=0;i<maxLines;i++)
            g.drawString(font,shortText(report.get(i),w-17),x+7,225+i*16,
                i==0?GOLD:WHITE,false);
        if(height>=410){
            g.drawString(font,shortText(NexusCombatInsights.status(),w-16),
                x+7,height-84,MUTED,false);
            g.drawString(font,shortText(message,w-16),
                x+7,height-65,WHITE,false);
        }
        super.render(g,mx,my,pt);
    }
}
