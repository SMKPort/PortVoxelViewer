package com.port.nexusclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/** Owner-controlled screenshot consent and six-entry in-memory report viewer. */
final class NexusVisionPanel extends Screen {
    private static final int GOLD=0xFFE6BE72,SOFT=0xFFB4A9A4,WHITE=0xFFEDE3D6;
    private static final int GREEN=0xFF97DEA9,RED=0xFFFFA6A1;
    private final Screen parent;
    private final String initialQuestion;
    private EditBox question;
    private String note="Uma imagem é enviada SOMENTE quando você clicar em ANALISAR.";
    private int selected=0;

    NexusVisionPanel(Screen parent){this(parent,"O que você consegue identificar?",0);}
    private NexusVisionPanel(Screen parent,String initialQuestion,int selected){
        super(Component.literal("Nexus Aterna | Visão Arcana 4.0"));
        this.parent=parent;this.initialQuestion=initialQuestion;this.selected=selected;
    }
    private int panelWidth(){return Math.min(width-16,570);}
    private int left(){return (width-panelWidth())/2;}
    private int top(){return Math.max(24,(height-335)/2);}

    @Override protected void init(){
        int x=left(),y=top(),w=panelWidth(),half=(w-26)/2;
        question=new EditBox(font,x+10,y+48,w-20,20,Component.literal("Pergunta sobre o Minecraft"));
        question.setMaxLength(180);
        question.setValue(initialQuestion);
        addRenderableWidget(question);
        addRenderableWidget(Button.builder(Component.literal(
            "Visão: "+(NexusArcaneVision.enabled()?"§aON":"§cOFF")),b->{
            try{
                NexusArcaneVision.setEnabled(!NexusArcaneVision.enabled());
                rebuild();
            }catch(Exception ex){note=ex.getMessage();}
        }).bounds(x+10,y+77,half,20).build());
        addRenderableWidget(Button.builder(Component.literal("Foco: "+NexusArcaneVision.focus().label),b->{
            NexusArcaneVision.nextFocus();rebuild();
        }).bounds(x+16+half,y+77,half,20).build());

        addRenderableWidget(Button.builder(Component.literal(
            "Imagem: "+(NexusArcaneVision.centerCrop()?"CENTRO AMPLIADO":"TELA TODA")),b->{
            NexusArcaneVision.setCenterCrop(!NexusArcaneVision.centerCrop());
            rebuild();
        }).bounds(x+10,y+106,w-20,20).build());
        addRenderableWidget(Button.builder(Component.literal("§6ANALISAR MINHA TELA AGORA"),b->{
            if(!NexusArcaneVision.enabled()){note="Ative a Visão Arcana primeiro.";return;}
            String q=question.getValue();
            minecraft.setScreen(null);
            NexusArcaneVision.capture(q);
        }).bounds(x+10,y+136,w-20,22).build());

        addRenderableWidget(Button.builder(Component.literal("◀ ANTERIOR"),b->{
            selected=Math.min(Math.max(0,NexusArcaneVision.history().size()-1),selected+1);
        }).bounds(x+10,y+198,half,20).build());
        addRenderableWidget(Button.builder(Component.literal("PRÓXIMO ▶"),b->{
            selected=Math.max(0,selected-1);
        }).bounds(x+16+half,y+198,half,20).build());
        addRenderableWidget(Button.builder(Component.literal("VOLTAR"),b->onClose())
            .bounds(x+10,y+306,w-20,20).build());
    }
    private void rebuild(){
        String typed=question==null?initialQuestion:question.getValue();
        minecraft.setScreen(new NexusVisionPanel(parent,typed,selected));
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick){
        int x=left(),y=top(),w=panelWidth();
        g.fill(x-4,y-14,x+w+4,Math.min(height-8,y+335),0xF0120F17);
        g.fill(x-4,y-14,x+w+4,y-11,0xFFD8AB5B);
        g.drawCenteredString(font,"NEXUS ATERNA | VISÃO ARCANA",width/2,y-6,GOLD);
        g.drawString(font,"A IA analisa pixels visíveis, não vê através de blocos.",x+10,y+17,SOFT,false);
        g.drawString(font,"Faça sua pergunta para a próxima captura:",x+10,y+34,WHITE,false);
        g.drawString(font,"Quota local: "+NexusArcaneVision.remaining()+
            " de 5 | Pausa: "+NexusArcaneVision.cooldownSeconds()+"s | "+
            (NexusArcaneVision.busy()?"PROCESSANDO":"PRONTO"),x+10,y+168,
            NexusArcaneVision.busy()?GOLD:GREEN,false);
        g.drawString(font,shorten(note,w-20),x+10,y+183,SOFT,false);
        List<NexusArcaneVision.Result> history=NexusArcaneVision.history();
        if(history.isEmpty()){
            g.drawString(font,"HISTÓRICO DE ANÁLISES (até 6, somente nesta sessão)",
                x+10,y+222,GOLD,false);
            g.drawString(font,"Ainda não existem respostas do Gemini.",x+10,y+240,SOFT,false);
        }else{
            int idx=Math.min(selected,history.size()-1);
            NexusArcaneVision.Result result=history.get(idx);
            String timestamp=DateFormat.getTimeInstance(DateFormat.SHORT)
                .format(new Date(result.at()));
            g.drawString(font,"RELATÓRIO "+(idx+1)+"/"+history.size()+
                " | "+result.focus()+" | "+timestamp,x+10,y+224,
                result.ok()?GOLD:RED,false);
            List<FormattedCharSequence> lines=font.split(
                Component.literal(result.text()),Math.max(100,w-24));
            int lineY=y+241;
            for(int i=0;i<Math.min(6,lines.size());i++){
                g.drawString(font,lines.get(i),x+10,lineY+i*10,WHITE,false);
            }
            if(lines.size()>6)g.drawString(font,"(Texto completo no chat local do jogo)",x+10,y+300,SOFT,false);
        }
        super.render(g,mouseX,mouseY,partialTick);
    }
    private String shorten(String input,int maxWidth){
        if(input==null)return "";
        String v=input;
        while(v.length()>1&&font.width(v)>maxWidth)v=v.substring(0,v.length()-1);
        return v.length()<input.length()?v+"...":v;
    }
}
