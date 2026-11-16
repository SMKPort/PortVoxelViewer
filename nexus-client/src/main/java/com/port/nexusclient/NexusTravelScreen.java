package com.port.nexusclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Optional F8 browser. Travel still needs a separate affirmative click/voice command. */
final class NexusTravelScreen extends Screen {
    private static final int GOLD=0xFFE9BB68,WHITE=0xFFE8E2D8,MUTED=0xFFB9ABA4;
    private final Screen parent;
    private int page;
    private int knownCount;
    private String aliasInput="";
    private String message="Clique em um destino para solicitar a viagem.";
    private EditBox aliasBox;

    NexusTravelScreen(Screen parent){
        super(Component.literal("Nexus Aterna | Arcane Travel"));
        this.parent=parent;
    }
    private int panelW(){return Math.min(520,width-18);}
    private int left(){return (width-panelW())/2;}

    @Override protected void init(){
        int x=left(),w=panelW(),top=82,step=25;
        List<NexusTravelPolicy.Destination> destinations=NexusArcaneTravel.known();
        knownCount=destinations.size();
        int perPage=Math.max(2,Math.min(9,(height-163)/step));
        int pages=Math.max(1,(destinations.size()+perPage-1)/perPage);
        page=Math.max(0,Math.min(page,pages-1));

        aliasBox=new EditBox(font,x+9,53,w-18,18,Component.literal("Apelido do favorito"));
        aliasBox.setMaxLength(36);
        aliasBox.setValue(aliasInput);
        aliasBox.setHint(Component.literal("Apelido opcional: casa, oficina, céu..."));
        aliasBox.setResponder(value->aliasInput=value);
        addRenderableWidget(aliasBox);

        for(int i=page*perPage;i<Math.min(destinations.size(),(page+1)*perPage);i++){
            final NexusTravelPolicy.Destination dest=destinations.get(i);
            int y=top+(i-page*perPage)*step,buttonW=w-89;
            addRenderableWidget(Button.builder(
                Component.literal(shortLabel(dest.name(),buttonW-16)+" §8["+shortLabel(dest.dimension(),75)+"]"),
                b->{message=NexusArcaneTravel.requestDestination(dest);})
                .bounds(x+8,y,buttonW,20).build());
            addRenderableWidget(Button.builder(Component.literal("§6★ FAVORITO"),b->{
                String nickname=aliasInput.isBlank()?dest.name():aliasInput;
                message=NexusArcaneTravel.favoriteById(nickname,dest.id());
            }).bounds(x+w-77,y,69,20).build());
        }

        int navY=height-76,navW=(w-26)/4;
        addRenderableWidget(Button.builder(Component.literal("§6CONFIRMAR"),
            b->{message=NexusArcaneTravel.confirm();})
            .bounds(x+7,navY,navW,20).build());
        addRenderableWidget(Button.builder(Component.literal("§cCANCELAR"),
            b->{message=NexusArcaneTravel.cancel();})
            .bounds(x+12+navW,navY,navW,20).build());
        addRenderableWidget(Button.builder(Component.literal("◀ "+(page+1)+"/"+pages+" ▶"),
            b->{page=(page+1)%pages;rebuild();})
            .bounds(x+17+2*navW,navY,navW,20).build());
        addRenderableWidget(Button.builder(Component.literal("ATUALIZAR"),
            b->rebuild())
            .bounds(x+22+3*navW,navY,navW,20).build());
        addRenderableWidget(Button.builder(Component.literal("VOLTAR"),
            b->onClose()).bounds(x+7,height-48,w-14,20).build());
    }

    private void rebuild(){
        aliasInput=aliasBox==null?"":aliasBox.getValue();
        clearWidgets();
        init();
    }
    private String shortLabel(String text,int pixels){
        if(text==null)return "?";
        if(font.width(text)<=pixels)return text;
        String s=text;
        while(s.length()>1&&font.width(s+"...")>pixels)s=s.substring(0,s.length()-1);
        return s+"...";
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){
        graphics.fill(left()-5,8,left()+panelW()+5,height-8,0xF0140E15);
        graphics.fill(left()-5,8,left()+panelW()+5,11,0xFFE2B55C);
        graphics.drawCenteredString(font,"NEXUS ATERNA | ARCANE TRAVEL",width/2,18,GOLD);
        graphics.drawCenteredString(font,"Waystones ativadas: "+knownCount+
            " | Só via menu autorizado do Waystones",width/2,35,MUTED);
        int statusY=height-99;
        graphics.drawString(font,shortLabel(message,panelW()-20),left()+10,statusY,WHITE,false);
        super.render(graphics,mouseX,mouseY,partialTick);
    }
}
