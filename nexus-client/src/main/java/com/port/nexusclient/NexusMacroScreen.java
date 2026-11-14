package com.port.nexusclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Initial macro editor: hit count, time window and spell to QuickCast. */
final class NexusMacroScreen extends Screen {
    private static final int GOLD=0xFFE9BB68,WHITE=0xFFECE1D3,MUTED=0xFFAE9DA6;
    private final Screen parent;
    private EditBox spellField;
    private String message="Configure uma macro. Nenhuma magia será lançada ao salvar.";

    NexusMacroScreen(Screen parent){
        super(Component.literal("Nexus Aterna | Macros Arcanas"));
        this.parent=parent;
    }
    private int left(){return (width-Math.min(550,width-20))/2;}
    private int pane(){return Math.min(550,width-20);}
    @Override protected void init(){
        int x=left(),w=pane();
        addRenderableWidget(Button.builder(Component.literal(
            NexusCombatMacro.enabled()?"§aMACRO ATIVA (DESLIGAR)":"§cMACRO DESLIGADA (LIGAR)"),
            b->toggle()).bounds(x+8,56,w-16,21).build());
        addRenderableWidget(Button.builder(Component.literal("−"),b->hits(-1))
            .bounds(x+8,102,41,20).build());
        addRenderableWidget(Button.builder(Component.literal("+"),b->hits(1))
            .bounds(x+55,102,41,20).build());
        addRenderableWidget(Button.builder(Component.literal("−"),b->window(-1))
            .bounds(x+8,148,41,20).build());
        addRenderableWidget(Button.builder(Component.literal("+"),b->window(1))
            .bounds(x+55,148,41,20).build());
        spellField=new EditBox(font,x+8,193,w-16,20,Component.literal("ID do feitiço"));
        spellField.setMaxLength(100);
        spellField.setValue(NexusCombatMacro.spell());
        addRenderableWidget(spellField);
        addRenderableWidget(Button.builder(Component.literal("SALVAR FEITIÇO DA MACRO"),b->saveSpell())
            .bounds(x+8,221,w-16,20).build());
        addRenderableWidget(Button.builder(Component.literal("VOLTAR"),b->onClose())
            .bounds(x+w-100,height-30,92,20).build());
    }
    private void toggle(){
        try{
            NexusCombatMacro.setEnabled(!NexusCombatMacro.enabled());
            message=NexusCombatMacro.enabled()?"Julgamento Divino armado.":"Macro desligada.";
            rebuildWidgets();
        }catch(Exception e){message=e.getMessage();}
    }
    private void hits(int delta){
        try{
            NexusCombatMacro.setHits(Math.max(2,Math.min(6,NexusCombatMacro.hits()+delta)));
            message="Golpes necessários: "+NexusCombatMacro.hits();
            rebuildWidgets();
        }catch(Exception e){message=e.getMessage();}
    }
    private void window(int delta){
        try{
            NexusCombatMacro.setWindow(Math.max(2,Math.min(10,
                NexusCombatMacro.windowSeconds()+delta)));
            message="Janela: "+NexusCombatMacro.windowSeconds()+" segundos.";
            rebuildWidgets();
        }catch(Exception e){message=e.getMessage();}
    }
    private void saveSpell(){
        try{
            NexusCombatMacro.setSpell(spellField.getValue());
            message="Feitiço salvo. O servidor validará o lançamento.";
        }catch(Exception e){message=e.getMessage();}
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float partialTick){
        int x=left(),w=pane();
        g.fill(x-5,6,x+w+5,height-6,0xEF0D0A14);
        g.fill(x-5,6,x+w+5,9,GOLD);
        g.drawCenteredString(font,"NEXUS ATERNA  |  MACROS ARCANAS",width/2,20,GOLD);
        g.drawString(font,"Macro: Julgamento Divino (mesmo alvo)",x+8,40,WHITE,false);
        g.drawString(font,"Golpes: "+NexusCombatMacro.hits()+"  (2 a 6)",x+110,106,WHITE,false);
        g.drawString(font,"Janela: "+NexusCombatMacro.windowSeconds()+"s  (2 a 10)",x+110,152,WHITE,false);
        g.drawString(font,"Feitiço equipado (namespace:identificador)",x+8,179,MUTED,false);
        if(height>=330){
            g.drawString(font,shorten("Progresso: "+NexusCombatMacro.progress()+"/"+
                NexusCombatMacro.hits()+"  |  Em combate, mana e cooldown são respeitados.",w-16),
                x+8,258,MUTED,false);
            g.drawString(font,shorten(message,w-16),x+8,273,WHITE,false);
            g.drawString(font,shorten(NexusCombatMacro.lastOutcome(),w-16),x+8,288,MUTED,false);
        }
        super.render(g,mx,my,partialTick);
    }
    private String shorten(String str,int widthLimit){
        String val=String.valueOf(str);
        while(val.length()>1&&font.width(val)>widthLimit)val=val.substring(0,val.length()-1);
        return val;
    }
}
