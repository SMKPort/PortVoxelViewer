package com.port.metamorph;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Compact in-game cockpit. Advanced parameters live in the paired web panel. */
public final class MetamorphScreen extends Screen {
    private int x,w;
    private String notice="Escolha uma forma. As alterações são somente visuais.";

    public MetamorphScreen(){super(Component.literal("Port Metamorph"));}
    @Override protected void init(){
        x=Math.max(6,width/2-165);w=Math.min(330,width-12);
        int col=(w-8)/2;
        addRenderableWidget(Button.builder(Component.literal("◆ Normal"),b->choose("normal"))
            .bounds(x,58,col,22).build());
        addRenderableWidget(Button.builder(Component.literal("⚙ Nexus Aterna"),b->choose("nexus"))
            .bounds(x+col+8,58,col,22).build());
        addRenderableWidget(Button.builder(Component.literal("✦ Angelical"),b->choose("angel"))
            .bounds(x,86,col,22).build());
        addRenderableWidget(Button.builder(Component.literal("✧ Híbrida"),b->choose("hybrid"))
            .bounds(x+col+8,86,col,22).build());
        addRenderableWidget(Button.builder(Component.literal("Ativar"),b->{
            MorphState.setEnabled(true);notice="Transformação ativada.";
        }).bounds(x,121,col,22).build());
        addRenderableWidget(Button.builder(Component.literal("Desativar"),b->{
            MorphState.setEnabled(false);notice="Transformação desativada.";
        }).bounds(x+col+8,121,col,22).build());
        addRenderableWidget(Button.builder(Component.literal("Parear com site"),b->{
            RemoteBridge.pair();notice="Verifique o chat: o código aparecerá após o registro.";
        }).bounds(x,160,col,22).build());
        addRenderableWidget(Button.builder(Component.literal("Endereço do site"),b->{
            PortMetamorph.say(RemoteBridge.WEB_URL);
            notice="Link enviado ao chat.";
        }).bounds(x+col+8,160,col,22).build());
        addRenderableWidget(Button.builder(Component.literal("Fechar"),b->onClose())
            .bounds(x,height-35,w,20).build());
    }
    private void choose(String preset){
        MorphState.applyPreset(preset);
        notice="Forma selecionada: "+MorphState.current().name;
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        g.fill(0,0,width,height,0xF0120E17);
        g.fill(0,0,width,4,0xFFE1B971);
        g.drawCenteredString(font,"§6§lPORT METAMORPH",width/2,15,0xFFF4D4A1);
        g.drawCenteredString(font,"MODULAR FORM CONTROL / NEOFORGE 1.21.1",width/2,31,0xFFBBB1C4);
        int y=197;
        g.fill(x,y,x+w,y+56,0xFF221B27);
        g.drawString(font,"Forma: "+MorphState.current().name,x+9,y+7,0xFFF0D3A2,true);
        g.drawString(font,"Estado: "+(MorphState.current().enabled?"ATIVO":"INATIVO"),x+9,y+21,
            MorphState.current().enabled?0xFFA8D9B6:0xFFC9A9A9,true);
        String linkStatus=RemoteBridge.status();
        if(linkStatus.length()>51)linkStatus=linkStatus.substring(0,50)+"…";
        g.drawString(font,"Web: "+linkStatus,x+9,y+36,0xFFBDB4C8,true);
        g.drawCenteredString(font,notice,width/2,height-51,0xFFDDC7BC);
        super.render(g,mouseX,mouseY,partial);
    }
    @Override public boolean isPauseScreen(){return false;}
}
