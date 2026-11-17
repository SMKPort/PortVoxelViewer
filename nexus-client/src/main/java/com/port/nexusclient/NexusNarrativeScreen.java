package com.port.nexusclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Owner-entered narrative events, separate from memories and telemetry. */
final class NexusNarrativeScreen extends Screen {
    private static final int GOLD=0xFFE9BB68,WHITE=0xFFECE1D3,MUTED=0xFFAE9DA6;
    private final Screen parent;
    private EditBox title,description,query;
    private List<String> matches=List.of();
    private String message="Você decide o que o Nexus deve registrar.";
    NexusNarrativeScreen(Screen parent){
        super(Component.literal("Nexus Aterna | Diário Narrativo 4.0"));
        this.parent=parent;
    }
    private int pane(){return Math.min(550,width-20);}
    private int left(){return (width-pane())/2;}
    private String cut(String raw,int maxWidth){
        String s=String.valueOf(raw);
        while(s.length()>1&&font.width(s)>maxWidth)s=s.substring(0,s.length()-1);
        return s;
    }
    @Override protected void init(){
        int x=left(),w=pane(),third=(w-26)/3;
        title=new EditBox(font,x+7,68,w-14,20,Component.literal("Título"));
        title.setMaxLength(80);
        title.setHint(Component.literal("Ex.: Conselho Celestial"));
        addRenderableWidget(title);
        description=new EditBox(font,x+7,101,w-14,20,Component.literal("Acontecimento"));
        description.setMaxLength(440);
        description.setHint(Component.literal("Registre o que aconteceu"));
        addRenderableWidget(description);
        addRenderableWidget(Button.builder(Component.literal("§aSALVAR EVENTO"),b->{
            try{
                message=NexusNarrativeJournal.add(title.getValue(),description.getValue());
                title.setValue("");description.setValue("");
                matches=NexusNarrativeJournal.latest(6);
            }catch(Exception error){message=error.getMessage();}
        }).bounds(x+7,133,w-14,20).build());
        query=new EditBox(font,x+7,177,w-14,20,Component.literal("Pesquisar"));
        query.setMaxLength(100);
        query.setHint(Component.literal("Tema ou personagem"));
        addRenderableWidget(query);
        addRenderableWidget(Button.builder(Component.literal("§6PESQUISAR"),b->{
            matches=NexusNarrativeJournal.recall(query.getValue(),6);
            message="Busca local concluída. Nenhuma lembrança foi inventada.";
        }).bounds(x+7,208,third,20).build());
        addRenderableWidget(Button.builder(Component.literal("RECENTES"),b->{
            matches=NexusNarrativeJournal.latest(6);
        }).bounds(x+13+third,208,third,20).build());
        addRenderableWidget(Button.builder(Component.literal("MEMÓRIAS ANTIGAS"),b->
            minecraft.setScreen(new NexusMemoryScreen(this)))
            .bounds(x+19+2*third,208,third,20).build());
        addRenderableWidget(Button.builder(Component.literal("VOLTAR"),b->onClose())
            .bounds(x+w-103,height-30,96,20).build());
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float pt){
        int x=left(),w=pane();
        g.fill(x-5,6,x+w+5,height-6,0xEF0D0A14);
        g.fill(x-5,6,x+w+5,9,GOLD);
        g.drawCenteredString(font,"NEXUS | MEMÓRIA NARRATIVA",width/2,20,GOLD);
        g.drawString(font,"Título do acontecimento",x+7,51,MUTED,false);
        g.drawString(font,"Descrição",x+7,88,MUTED,false);
        g.drawString(font,"Consultar o diário:",x+7,164,GOLD,false);
        if(height>=315){
            for(int i=0;i<Math.min(matches.size(),(height-300)/16);i++)
                g.drawString(font,cut(matches.get(i),w-16),x+7,240+i*16,
                    WHITE,false);
            g.drawString(font,cut(message,w-16),x+7,height-58,MUTED,false);
        }
        super.render(g,mx,my,pt);
    }
}
