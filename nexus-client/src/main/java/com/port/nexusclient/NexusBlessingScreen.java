package com.port.nexusclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Owner-controlled maintenance queue, with an actual + button and one row per spell. */
final class NexusBlessingScreen extends Screen {
    private static final int GOLD=0xFFE9BB68,WHITE=0xFFE8E2D8,MUTED=0xFFB9ABA4;
    private final Screen parent;
    private int page;
    private long removeConfirmUntil;
    private String confirmId="";
    private String message="Adicione uma magia, aprenda seu efeito e autorize a renovação.";

    NexusBlessingScreen(Screen parent) {
        super(Component.literal("Nexus Aterna | Magias de manutenção"));
        this.parent=parent;
    }
    private int pane(){return Math.min(550,width-20);}
    private int left(){return (width-pane())/2;}
    private int pages(){return Math.max(1,(NexusBlessing.entries().size()+3)/4);}
    private String trim(String raw,int maxWidth){
        String val=String.valueOf(raw==null?"":raw);
        while(val.length()>1&&font.width(val)>maxWidth)val=val.substring(0,val.length()-1);
        return val;
    }
    @Override protected void init(){
        int x=left(),w=pane(),half=(w-22)/2;
        addRenderableWidget(Button.builder(Component.literal("§6+ ADICIONAR MAGIA"),
            b->minecraft.setScreen(new NexusBlessingPickerScreen(parent)))
            .bounds(x+6,47,half,20).build());
        addRenderableWidget(Button.builder(Component.literal("§cDESLIGAR TODAS"),
            b->{
                message=NexusBlessing.setEnabled(false);
                rebuildWidgets();
            }).bounds(x+12+half,47,half,20).build());

        List<NexusBlessing.View> all=NexusBlessing.entries();
        page=Math.max(0,Math.min(page,pages()-1));
        int from=page*4,to=Math.min(all.size(),from+4);
        for(int i=from;i<to;i++){
            NexusBlessing.View rule=all.get(i);
            String label=(rule.spellId().equals(NexusBlessing.spellId())?"§6● ":"§f")
                +(rule.enabled()?"§a[ON] ":"§c[OFF] ")
                +trim(rule.spellId(),w-112);
            addRenderableWidget(Button.builder(Component.literal(label),b->{
                message=NexusBlessing.selectSpell(rule.spellId());
                removeConfirmUntil=0;confirmId="";
                rebuildWidgets();
            }).bounds(x+7,88+(i-from)*24,w-14,21).build());
        }
        addRenderableWidget(Button.builder(Component.literal("◀"),
            b->{if(page>0){page--;rebuildWidgets();}})
            .bounds(x+7,188,36,20).build());
        addRenderableWidget(Button.builder(Component.literal("▶"),
            b->{if(page<pages()-1){page++;rebuildWidgets();}})
            .bounds(x+48,188,36,20).build());

        if(!NexusBlessing.spellId().isBlank()){
            addRenderableWidget(Button.builder(Component.literal("§6APRENDER EFEITO (30s)"),
                b->{
                    message=NexusBlessing.learn();
                    if(message.startsWith("Aprendendo"))
                        minecraft.setScreen(null);
                }).bounds(x+7,231,half,20).build());
            addRenderableWidget(Button.builder(Component.literal(NexusBlessing.enabled()?
                    "§aATIVA (DESLIGAR)":"§cDESLIGADA (LIGAR)"),
                b->{
                    message=NexusBlessing.setSelectedEnabled(!NexusBlessing.enabled());
                    rebuildWidgets();
                }).bounds(x+13+half,231,half,20).build());
            addRenderableWidget(Button.builder(Component.literal("− 5s"),
                b->adjust(-5)).bounds(x+7,266,65,20).build());
            addRenderableWidget(Button.builder(Component.literal("+ 5s"),
                b->adjust(5)).bounds(x+78,266,65,20).build());
            addRenderableWidget(Button.builder(Component.literal("§cREMOVER MAGIA"),
                b->remove()).bounds(x+13+half,266,half,20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("VOLTAR"),b->onClose())
            .bounds(x+w-103,height-30,94,20).build());
    }

    private void adjust(int delta){
        message=NexusBlessing.setThreshold(Math.max(3,Math.min(60,
            NexusBlessing.thresholdSeconds()+delta)));
        rebuildWidgets();
    }
    private void remove(){
        String chosen=NexusBlessing.spellId();
        long now=System.currentTimeMillis();
        if(!chosen.equals(confirmId)||now>removeConfirmUntil){
            confirmId=chosen;removeConfirmUntil=now+10_000L;
            message="Clique REMOVER novamente em 10 segundos para excluir "+chosen+".";
            return;
        }
        message=NexusBlessing.removeSpell(chosen);
        confirmId="";removeConfirmUntil=0;
        page=Math.min(page,pages()-1);
        rebuildWidgets();
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}

    @Override public void render(GuiGraphics g,int mx,int my,float partialTick){
        int x=left(),w=pane();
        g.fill(x-5,6,x+w+5,height-6,0xF0140E15);
        g.fill(x-5,6,x+w+5,9,0xFFE2B55C);
        g.drawCenteredString(font,"NEXUS ATERNA | MANUTENÇÃO AUTOMÁTICA",
            width/2,18,GOLD);
        g.drawString(font,"Magias: "+NexusBlessing.enabledCount()+"/"+
            NexusBlessing.entries().size()+" ativas (limite 8)",x+7,76,WHITE,false);
        if(NexusBlessing.entries().isEmpty())
            g.drawString(font,"Nenhuma magia. Use + ADICIONAR MAGIA.",x+7,107,MUTED,false);
        g.drawString(font,"Página "+(page+1)+"/"+pages(),x+101,193,MUTED,false);
        if(!NexusBlessing.spellId().isBlank()){
            g.drawString(font,trim("Selecionada: "+NexusBlessing.spellId(),w-16),
                x+7,216,GOLD,false);
            g.drawString(font,"Antecedência: "+NexusBlessing.thresholdSeconds()+
                "s (3 a 60)",x+155,272,WHITE,false);
            if(height>=354){
                String effects=NexusBlessing.trackedEffects().isEmpty()?"NENHUM":
                    String.join(", ",NexusBlessing.trackedEffects());
                g.drawString(font,trim("Efeitos aprendidos: "+effects,w-16),
                    x+7,300,MUTED,false);
                g.drawString(font,trim("Restante: "+NexusBlessing.selectedRemaining(),w-16),
                    x+7,317,GOLD,false);
            }
        }
        if(height>=390){
            g.drawString(font,trim(message,w-16),x+7,height-68,WHITE,false);
            g.drawString(font,trim(NexusBlessing.status(),w-16),
                x+7,height-53,MUTED,false);
        }
        super.render(g,mx,my,partialTick);
    }
}
