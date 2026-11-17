package com.port.nexusclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** F8 combat macro manager, modeled after the equipped-spell upkeep roster. */
final class NexusMacroScreen extends Screen {
    private static final int GOLD=0xFFE9BB68,WHITE=0xFFECE1D3,MUTED=0xFFAE9DA6;
    private final Screen parent;
    private int page;
    private long removeConfirmUntil;
    private String removeConfirmSpell="";
    private String message="Adicione magias equipadas e configure o combo de cada uma.";

    NexusMacroScreen(Screen parent){
        super(Component.literal("Nexus Aterna | Macros Arcanas"));
        this.parent=parent;
    }
    private int left(){return (width-Math.min(550,width-20))/2;}
    private int pane(){return Math.min(550,width-20);}
    private int pages(){return Math.max(1,(NexusCombatMacro.entries().size()+3)/4);}
    private String shorten(String s,int w){
        String result=String.valueOf(s);
        while(result.length()>1&&font.width(result)>w)
            result=result.substring(0,result.length()-1);
        return result;
    }
    @Override protected void init(){
        int x=left(),w=pane(),half=(w-22)/2;
        addRenderableWidget(Button.builder(Component.literal("§6+ ADICIONAR MACRO"),
            b->minecraft.setScreen(new NexusMacroPickerScreen(parent)))
            .bounds(x+7,49,half,21).build());
        addRenderableWidget(Button.builder(Component.literal("§cDESLIGAR TODAS"),
            b->{
                try{
                    NexusCombatMacro.setEnabled(false);
                    message="Todas as macros desativadas.";
                    rebuildWidgets();
                }catch(Exception error){message=error.getMessage();}
            }).bounds(x+13+half,49,half,21).build());
        List<NexusCombatMacro.View> macros=NexusCombatMacro.entries();
        page=Math.max(0,Math.min(page,pages()-1));
        int from=page*4,to=Math.min(macros.size(),from+4);
        for(int i=from;i<to;i++){
            NexusCombatMacro.View macro=macros.get(i);
            String title=(macro.spell().equals(NexusCombatMacro.spell())?"§6● ":"§f")+
                (macro.enabled()?"§a[ON] ":"§c[OFF] ")+macro.spell()+
                " §7("+macro.hits()+" golpes)";
            addRenderableWidget(Button.builder(Component.literal(shorten(title,w-35)),b->{
                message=NexusCombatMacro.selectSpell(macro.spell());
                removeConfirmUntil=0;removeConfirmSpell="";
                rebuildWidgets();
            }).bounds(x+7,88+(i-from)*24,w-14,21).build());
        }
        addRenderableWidget(Button.builder(Component.literal("◀"),b->{
            if(page>0){page--;rebuildWidgets();}
        }).bounds(x+7,188,36,20).build());
        addRenderableWidget(Button.builder(Component.literal("▶"),b->{
            if(page<pages()-1){page++;rebuildWidgets();}
        }).bounds(x+49,188,36,20).build());
        if(!NexusCombatMacro.spell().isBlank()){
            addRenderableWidget(Button.builder(Component.literal(NexusCombatMacro.selectedEnabled()?
                "§aATIVA (DESLIGAR)":"§cDESLIGADA (LIGAR)"),b->toggle())
                .bounds(x+7,231,half,21).build());
            addRenderableWidget(Button.builder(Component.literal("§6+ MAGIA NESTA MACRO"),
                b->minecraft.setScreen(new NexusMacroPickerScreen(parent,true)))
                .bounds(x+13+half,231,half,21).build());
            addRenderableWidget(Button.builder(Component.literal("− GOLPE"),
                b->hits(-1)).bounds(x+7,269,half,20).build());
            addRenderableWidget(Button.builder(Component.literal("+ GOLPE"),
                b->hits(1)).bounds(x+13+half,269,half,20).build());
            addRenderableWidget(Button.builder(Component.literal("− 1s"),
                b->window(-1)).bounds(x+7,306,half,20).build());
            addRenderableWidget(Button.builder(Component.literal("+ 1s"),
                b->window(1)).bounds(x+13+half,306,half,20).build());
            if(height>=420){
                addRenderableWidget(Button.builder(Component.literal("§6EDITAR SEQUÊNCIA"),
                    b->minecraft.setScreen(new NexusMacroSequenceScreen(parent)))
                    .bounds(x+7,347,half,20).build());
                addRenderableWidget(Button.builder(Component.literal("§cREMOVER MACRO"),
                    b->remove()).bounds(x+13+half,347,half,20).build());
            }
        }
        addRenderableWidget(Button.builder(Component.literal("VOLTAR"),b->onClose())
            .bounds(x+w-100,height-30,92,20).build());
    }
    private void toggle(){
        try{
            NexusCombatMacro.setSelectedEnabled(!NexusCombatMacro.selectedEnabled());
            message=NexusCombatMacro.lastOutcome();
            rebuildWidgets();
        }catch(Exception error){message=error.getMessage();}
    }
    private void hits(int delta){
        try{
            NexusCombatMacro.setHits(Math.max(2,Math.min(6,NexusCombatMacro.hits()+delta)));
            message=NexusCombatMacro.lastOutcome();
            rebuildWidgets();
        }catch(Exception error){message=error.getMessage();}
    }
    private void window(int delta){
        try{
            NexusCombatMacro.setWindow(Math.max(2,Math.min(10,
                NexusCombatMacro.windowSeconds()+delta)));
            message=NexusCombatMacro.lastOutcome();
            rebuildWidgets();
        }catch(Exception error){message=error.getMessage();}
    }
    private void remove(){
        String id=NexusCombatMacro.spell();
        long now=System.currentTimeMillis();
        if(!id.equals(removeConfirmSpell)||now>removeConfirmUntil){
            removeConfirmUntil=now+10_000L;
            removeConfirmSpell=id;
            message="Clique novamente em REMOVER MACRO para confirmar: "+id;
            return;
        }
        message=NexusCombatMacro.removeSpell(id);
        removeConfirmUntil=0;removeConfirmSpell="";
        page=Math.min(page,pages()-1);
        rebuildWidgets();
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float partialTick){
        int x=left(),w=pane();
        g.fill(x-5,6,x+w+5,height-6,0xEF0D0A14);
        g.fill(x-5,6,x+w+5,9,GOLD);
        g.drawCenteredString(font,"NEXUS ATERNA | MACROS ARCANAS",width/2,20,GOLD);
        g.drawString(font,"Ativas: "+NexusCombatMacro.enabledCount()+"/"+
            NexusCombatMacro.entries().size()+" (limite 8)",x+7,77,WHITE,false);
        if(NexusCombatMacro.entries().isEmpty())
            g.drawString(font,"Nenhuma macro. Clique em + ADICIONAR MACRO.",
                x+7,110,MUTED,false);
        g.drawString(font,"Página "+(page+1)+"/"+pages(),x+104,195,MUTED,false);
        if(!NexusCombatMacro.spell().isBlank()){
            g.drawString(font,shorten("Selecionada: "+NexusCombatMacro.spell(),w-14),
                x+7,215,GOLD,false);
            g.drawString(font,shorten("Sequência ("+NexusCombatMacro.sequenceSize()+
                "): "+String.join(" > ",NexusCombatMacro.sequence()),w-14),
                x+7,257,GOLD,false);
            g.drawString(font,"Golpes: "+NexusCombatMacro.hits()+" (2 a 6)"+
                " | Janela: "+NexusCombatMacro.windowSeconds()+"s (2 a 10)",
                x+7,294,WHITE,false);
            if(height>=430)
                g.drawString(font,shorten("Progresso: "+NexusCombatMacro.progress()+
                    "/"+NexusCombatMacro.hits()+" | "+NexusCombatMacro.queueStatus(),w-14),
                    x+7,336,MUTED,false);
        }
        if(height>=470){
            g.drawString(font,shorten(message,w-14),x+7,height-67,WHITE,false);
            g.drawString(font,shorten(NexusCombatMacro.lastOutcome(),w-14),
                x+7,height-51,MUTED,false);
        }
        super.render(g,mx,my,partialTick);
    }
}
