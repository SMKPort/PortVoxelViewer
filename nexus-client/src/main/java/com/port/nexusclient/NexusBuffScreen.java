package com.port.nexusclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Safe local hotbar whitelist editor. Browsing never activates an item. */
final class NexusBuffScreen extends Screen {
    private static final int GOLD=0xFFE7BD73,MUTED=0xFFBBAFB2,WHITE=0xFFF4EDE2;
    private final Screen parent;
    private int page;
    private long selectedId,deleteConfirmUntil;
    private String message="Segure o item na hotbar e cadastre-o. Automação desligada por padrão.";

    NexusBuffScreen(Screen parent){
        super(Component.literal("Nexus Aterna | Gerenciador de Buffs"));
        this.parent=parent;
    }
    private int panelWidth(){return Math.min(width-20,550);}
    private int left(){return (width-panelWidth())/2;}
    private int perPage(){return Math.max(1,Math.min(4,(height-295)/27));}
    private int pages(){return Math.max(1,(NexusBuffManager.entries().size()+perPage()-1)/perPage());}
    private NexusBuffManager.Entry selected(){
        for(var e:NexusBuffManager.entries())if(e.id==selectedId)return e;
        return null;
    }
    private void reopen(){rebuildWidgets();}

    @Override protected void init(){
        int x=left(),w=panelWidth(),half=(w-22)/2;
        addRenderableWidget(Button.builder(Component.literal("CADASTRAR NA MÃO (MANUAL)"),
            b->register(NexusBuffManager.MANUAL)).bounds(x+6,48,half,20).build());
        addRenderableWidget(Button.builder(Component.literal("CADASTRAR NA MÃO (COMBATE)"),
            b->register(NexusBuffManager.COMBAT)).bounds(x+12+half,48,half,20).build());
        addRenderableWidget(Button.builder(Component.literal(NexusBuffManager.enabled()?
            "§aAUTOMAÇÃO ON  (DESLIGAR)":"§cAUTOMAÇÃO OFF  (LIGAR)"),
            b->toggle()).bounds(x+6,76,w-12,20).build());
        addRenderableWidget(Button.builder(Component.literal(NexusBuffManager.inventoryEnabled()?
            "§aINVENTÁRIO ON  (BUSCAR BUFFS EM OUTRAS LINHAS)":
            "§cINVENTÁRIO OFF  (BUSCAR BUFFS EM OUTRAS LINHAS)"),
            b->{
                NexusBuffManager.inventoryEnabled(!NexusBuffManager.inventoryEnabled());
                message=NexusBuffManager.status();
                reopen();
            }).bounds(x+6,104,w-12,20).build());

        List<NexusBuffManager.Entry> entries=NexusBuffManager.entries();
        page=Math.max(0,Math.min(page,pages()-1));
        for(int i=page*perPage();i<Math.min(entries.size(),(page+1)*perPage());i++){
            var e=entries.get(i);
            int row=138+(i%perPage())*27;
            String title=(e.id==selectedId?"§6● ":"§f")+
                e.name+"  ["+(e.mode.equals(NexusBuffManager.COMBAT)?"AUTO":"MANUAL")+"]";
            addRenderableWidget(Button.builder(Component.literal(shorten(title,w-32)),
                b->choose(e.id)).bounds(x+6,row,w-12,21).build());
        }

        int navY=height-126;
        addRenderableWidget(Button.builder(Component.literal("◀"),b->{
            if(page>0){page--;reopen();}
        }).bounds(x+6,navY,38,20).build());
        addRenderableWidget(Button.builder(Component.literal("▶"),b->{
            if(page<pages()-1){page++;reopen();}
        }).bounds(x+48,navY,38,20).build());

        if(selected()!=null){
            var e=selected();
            addRenderableWidget(Button.builder(Component.literal("USAR AGORA"),
                b->use()).bounds(x+6,height-97,half,20).build());
            addRenderableWidget(Button.builder(Component.literal(
                e.mode.equals(NexusBuffManager.COMBAT)?"MUDAR PARA MANUAL":"MUDAR PARA COMBATE"),
                b->mode()).bounds(x+12+half,height-97,half,20).build());
            addRenderableWidget(Button.builder(Component.literal(e.allowSingle?
                "§cÚLTIMA: PERMITIDA (PROTEGER)":"§aÚLTIMA: PROTEGIDA (LIBERAR)"),
                b->lastUnit()).bounds(x+6,height-73,half,20).build());
            addRenderableWidget(Button.builder(Component.literal("§cAPAGAR CADASTRO"),
                b->delete()).bounds(x+12+half,height-73,half,20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("VOLTAR"),
            b->onClose()).bounds(x+w-94,height-31,88,20).build());
    }
    private void register(String mode){
        try{
            var e=NexusBuffManager.registerHeld(mode);
            selectedId=e.id;
            message="Cadastrado "+e.name+
                (mode.equals(NexusBuffManager.COMBAT)?
                    ". Ative a automação separadamente.":". Somente uso manual.");
            reopen();
        }catch(Exception ex){message=ex.getMessage();}
    }
    private void toggle(){
        try{
            NexusBuffManager.setEnabled(!NexusBuffManager.enabled());
            message=NexusBuffManager.status();reopen();
        }catch(Exception ex){message=ex.getMessage();}
    }
    private void choose(long id){selectedId=id;deleteConfirmUntil=0;reopen();}
    private void use(){
        try{
            NexusBuffManager.useNow(selectedId);
            message="Uso solicitado. Feche a tela e não troque de item durante o uso.";
        }catch(Exception ex){message=ex.getMessage();}
    }
    private void mode(){
        try{
            var e=selected();
            if(e==null)return;
            NexusBuffManager.setMode(e.id,
                e.mode.equals(NexusBuffManager.COMBAT)?
                    NexusBuffManager.MANUAL:NexusBuffManager.COMBAT);
            message=NexusBuffManager.status();reopen();
        }catch(Exception ex){message=ex.getMessage();}
    }
    private void lastUnit(){
        try{
            var e=selected();if(e==null)return;
            NexusBuffManager.allowSingle(e.id,!e.allowSingle);
            message=NexusBuffManager.status();reopen();
        }catch(Exception ex){message=ex.getMessage();}
    }
    private void delete(){
        long now=System.currentTimeMillis();
        if(now>deleteConfirmUntil){
            deleteConfirmUntil=now+10000;
            message="Clique novamente em APAGAR para confirmar.";
            return;
        }
        try{
            NexusBuffManager.remove(selectedId);selectedId=0;deleteConfirmUntil=0;
            message="Registro apagado. O item no inventário foi preservado.";
            reopen();
        }catch(Exception ex){message=ex.getMessage();}
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}

    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick){
        int x=left(),w=panelWidth();
        g.fill(x-5,8,x+w+5,height-8,0xEF0F0A15);
        g.fill(x-5,8,x+w+5,11,GOLD);
        g.drawCenteredString(font,"NEXUS ATERNA | GERENCIADOR DE BUFFS",width/2,20,GOLD);
        g.drawString(font,"Lista de buffs permitidos: "+NexusBuffManager.entries().size()+
            "/8   |   página "+(page+1)+"/"+pages(),x+7,128,MUTED,false);
        if(selected()!=null){
            g.drawString(font,shorten("Selecionado: "+selected().itemId,w-104),
                x+95,height-121,WHITE,false);
        }
        if(height>=310)
            g.drawString(font,shorten(message,w-15),x+7,height-48,MUTED,false);
        super.render(g,mouseX,mouseY,partialTick);
    }
    private String shorten(String text,int maxWidth){
        String s=String.valueOf(text);
        while(s.length()>1&&font.width(s)>maxWidth)s=s.substring(0,s.length()-1);
        return s;
    }
}
