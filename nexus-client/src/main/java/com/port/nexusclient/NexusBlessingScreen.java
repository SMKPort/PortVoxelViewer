package com.port.nexusclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Learning never switches on automatic casting without a separate click. */
final class NexusBlessingScreen extends Screen {
    private static final int GOLD=0xFFE9BB68,WHITE=0xFFE8E2D8,MUTED=0xFFB9ABA4;
    private final Screen parent;
    private EditBox spellField;
    private String message="Configure o Blessing, aprenda o efeito e só depois autorize a renovação.";
    NexusBlessingScreen(Screen parent){
        super(Component.literal("Nexus Aterna | Blessing automático"));
        this.parent=parent;
    }
    private int widthBox(){return Math.min(540,width-20);}
    private int left(){return (width-widthBox())/2;}

    @Override protected void init(){
        int x=left(),w=widthBox(),half=(w-22)/2;
        addRenderableWidget(Button.builder(Component.literal("§6APRENDER EFEITO (30s)"),
            b->{
                message=NexusBlessing.learn();
                if(message.startsWith("Aprendendo"))minecraft.setScreen(null);
            }).bounds(x+7,65,half,20).build());
        addRenderableWidget(Button.builder(Component.literal(
            NexusBlessing.enabled()?"§aRENOVAÇÃO ON (DESLIGAR)":"§cRENOVAÇÃO OFF (LIGAR)"),
            b->{
                message=NexusBlessing.setEnabled(!NexusBlessing.enabled());
                rebuildWidgets();
            }).bounds(x+13+half,65,half,20).build());
        addRenderableWidget(Button.builder(Component.literal("− 5s"),b->adjust(-5))
            .bounds(x+7,109,75,20).build());
        addRenderableWidget(Button.builder(Component.literal("+ 5s"),b->adjust(5))
            .bounds(x+88,109,75,20).build());
        spellField=new EditBox(font,x+7,165,w-14,20,Component.literal("ID da magia"));
        spellField.setMaxLength(100);
        spellField.setValue(NexusBlessing.spellId());
        spellField.setHint(Component.literal("ID exato: namespace:blessing"));
        addRenderableWidget(spellField);
        addRenderableWidget(Button.builder(Component.literal("SALVAR MAGIA (DESLIGA E REAPRENDE)"),
            b->{
                message=NexusBlessing.setSpell(spellField.getValue());
                rebuildWidgets();
            }).bounds(x+7,191,w-14,20).build());
        addRenderableWidget(Button.builder(Component.literal("VOLTAR"),
            b->onClose()).bounds(x+w-103,height-30,94,20).build());
    }
    private void adjust(int delta){
        message=NexusBlessing.setThreshold(Math.max(3,Math.min(60,
            NexusBlessing.thresholdSeconds()+delta)));
        rebuildWidgets();
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){
        int x=left(),w=widthBox();
        graphics.fill(x-5,6,x+w+5,height-6,0xF0140E15);
        graphics.fill(x-5,6,x+w+5,9,0xFFE2B55C);
        graphics.drawCenteredString(font,"NEXUS ATERNA | BLESSING AUTOMÁTICO",
            width/2,20,GOLD);
        graphics.drawString(font,shorten("Magia: "+NexusBlessing.spellId(),w-16),
            x+8,39,WHITE,false);
        graphics.drawString(font,"Antecedência: "+NexusBlessing.thresholdSeconds()+
            "s (3 a 60 segundos)",x+175,115,GOLD,false);
        graphics.drawString(font,"ID de Blessing (obtenha em /nexusclient blessing spells)",
            x+7,150,MUTED,false);
        if(height>=315){
            List<String> effects=NexusBlessing.trackedEffects();
            graphics.drawString(font,shorten("Efeito(s) aprendido(s): "+
                (effects.isEmpty()?"nenhum":String.join(", ",effects)),w-16),
                x+8,231,GOLD,false);
            graphics.drawString(font,shorten("Estado: "+NexusBlessing.status(),w-16),
                x+8,248,WHITE,false);
            graphics.drawString(font,shorten(message,w-16),x+8,
                Math.min(height-53,274),MUTED,false);
        }
        super.render(graphics,mouseX,mouseY,partialTick);
    }
    private String shorten(String raw,int pixels){
        String text=raw==null?"":raw;
        while(text.length()>1&&font.width(text)>pixels)text=text.substring(0,text.length()-1);
        return text;
    }
}
