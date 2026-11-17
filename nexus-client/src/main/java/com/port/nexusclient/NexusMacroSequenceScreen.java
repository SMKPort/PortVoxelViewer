package com.port.nexusclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Sequence editor: first spell anchors the combo; extras may be reordered or removed. */
final class NexusMacroSequenceScreen extends Screen {
    private static final int GOLD=0xFFE9BB68,WHITE=0xFFECE1D3,MUTED=0xFFAE9DA6;
    private final Screen parent;
    private String message="A primeira magia identifica a macro. Edite as demais abaixo.";

    NexusMacroSequenceScreen(Screen parent){
        super(Component.literal("Nexus Aterna | Editar sequência"));
        this.parent=parent;
    }
    private int pane(){return Math.min(550,width-20);}
    private int left(){return (width-pane())/2;}
    private String fit(String raw,int pixels){
        String text=raw;
        while(text.length()>1&&font.width(text)>pixels)
            text=text.substring(0,text.length()-1);
        return text;
    }
    @Override protected void init(){
        int x=left(),w=pane();
        List<String> spells=NexusCombatMacro.sequence();
        for(int i=0;i<Math.min(spells.size(),6);i++){
            final String id=spells.get(i);
            int y=85+i*34;
            addRenderableWidget(Button.builder(Component.literal(
                fit((i+1)+". "+id+(i==0?" [INÍCIO]":""),w-145)),b->{})
                .bounds(x+7,y,w-147,22).build()).active=false;
            if(i>0){
                Button up=Button.builder(Component.literal("↑"),
                    b->move(id,-1)).bounds(x+w-135,y,37,22).build();
                up.active=i>1;
                addRenderableWidget(up);
                Button down=Button.builder(Component.literal("↓"),
                    b->move(id,1)).bounds(x+w-94,y,37,22).build();
                down.active=i<spells.size()-1;
                addRenderableWidget(down);
                addRenderableWidget(Button.builder(Component.literal("§cX"),
                    b->remove(id)).bounds(x+w-53,y,37,22).build());
            }
        }
        int lastSpellBottom=spells.isEmpty()?85:
            85+(Math.min(spells.size(),6)-1)*34+22;
        if(height-106>=lastSpellBottom+6){
            addRenderableWidget(Button.builder(Component.literal("− 1s"),
                b->changeInterval(-1)).bounds(x+7,height-106,69,20).build());
            addRenderableWidget(Button.builder(Component.literal("+ 1s"),
                b->changeInterval(1)).bounds(x+83,height-106,69,20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("§6+ ADICIONAR MAGIA"),
            b->minecraft.setScreen(new NexusMacroPickerScreen(parent,true)))
            .bounds(x+7,height-62,w-116,22).build());
        addRenderableWidget(Button.builder(Component.literal("VOLTAR"),
            b->onClose()).bounds(x+w-103,height-30,96,22).build());
    }
    private void move(String id,int direction){
        message=NexusCombatMacro.moveInSequence(id,direction);
        rebuildWidgets();
    }
    private void remove(String id){
        message=NexusCombatMacro.removeFromSequence(id);
        rebuildWidgets();
    }
    private void changeInterval(int delta){
        try{
            int next=Math.max(NexusMacroSequencePolicy.MIN_INTERVAL_SECONDS,
                Math.min(NexusMacroSequencePolicy.MAX_INTERVAL_SECONDS,
                    NexusCombatMacro.intervalSeconds()+delta));
            NexusCombatMacro.setIntervalSeconds(next);
            message=NexusCombatMacro.lastOutcome();
            rebuildWidgets();
        }catch(Exception error){message=error.getMessage();}
    }
    @Override public void onClose(){minecraft.setScreen(new NexusMacroScreen(parent));}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float partialTick){
        int x=left(),w=pane();
        g.fill(x-5,6,x+w+5,height-6,0xEF0D0A14);
        g.fill(x-5,6,x+w+5,9,GOLD);
        g.drawCenteredString(font,"NEXUS | ORDEM DAS MAGIAS",width/2,21,GOLD);
        g.drawString(font,fit("Macro: "+NexusCombatMacro.spell(),w-14),
            x+7,44,WHITE,false);
        g.drawString(font,"Magias na sequência: "+NexusCombatMacro.sequenceSize()+"/6",
            x+7,61,MUTED,false);
        g.drawString(font,"Intervalo: "+NexusCombatMacro.intervalSeconds()+"s (2–30)",
            x+Math.max(185,w-192),61,GOLD,false);
        int lastBottom=NexusCombatMacro.sequenceSize()==0?85:
            85+(Math.min(NexusCombatMacro.sequenceSize(),6)-1)*34+22;
        if(height-106>=lastBottom+6)
            g.drawString(font,"Entre magias: "+NexusCombatMacro.intervalSeconds()+"s",
                x+165,height-100,WHITE,false);
        if(height>=Math.max(375,lastBottom+116))
            g.drawString(font,fit(message,w-14),x+7,height-83,MUTED,false);
        super.render(g,mx,my,partialTick);
    }
}
