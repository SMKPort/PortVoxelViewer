package com.port.nexusclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Local device selector for VoiceCast. Enumeration does not open the microphone;
 * capture begins only when the owner explicitly enables VoiceCast.
 */
final class NexusMicrophoneScreen extends Screen {
    private static final int GOLD=0xFFE9BB68,WHITE=0xFFECE1D3;
    private static final int MUTED=0xFFB8A5A2,ERROR=0xFFFFA3A3;
    private final Screen parent;
    private List<String> devices=List.of();
    private int page;
    private String message="Escolha o microfone de entrada usado pelo VoiceCast.";

    NexusMicrophoneScreen(Screen parent){
        super(Component.literal("Nexus Aterna | Microfone do VoiceCast"));
        this.parent=parent;
    }

    private int panelWidth(){return Math.min(width-20,530);}
    private int left(){return (width-panelWidth())/2;}
    private int rows(){return Math.max(1,Math.min(5,(height-186)/27));}
    private int pages(){return Math.max(1,(devices.size()+rows()-1)/rows());}

    @Override protected void init(){
        if(devices.isEmpty())
            devices=NexusMicrophoneChoices.withDefault(NexusMicrophones.list());
        int x=left(),w=panelWidth();
        int rows=rows();
        page=Math.max(0,Math.min(page,pages()-1));
        String selected=NexusVoiceCasting.selectedMicrophone();
        for(int i=page*rows;i<Math.min(devices.size(),(page+1)*rows);i++){
            String name=devices.get(i);
            String title=NexusMicrophones.displayName(name);
            boolean chosen=selected.equals(name);
            int y=91+(i%rows)*27;
            String label=(chosen?"§a[SELECIONADO] ":"§f")+trim(title,w-125);
            addRenderableWidget(Button.builder(Component.literal(label),
                button->choose(name)).bounds(x+8,y,w-16,21).build());
        }
        int navY=height-66;
        addRenderableWidget(Button.builder(Component.literal("◀"),button->{
            if(page>0){page--;rebuildWidgets();}
        }).bounds(x+8,navY,44,20).build());
        addRenderableWidget(Button.builder(Component.literal("▶"),button->{
            if(page<pages()-1){page++;rebuildWidgets();}
        }).bounds(x+57,navY,44,20).build());
        addRenderableWidget(Button.builder(Component.literal("ATUALIZAR DISPOSITIVOS"),button->refresh())
            .bounds(x+109,navY,w-117,20).build());
        addRenderableWidget(Button.builder(Component.literal("VOLTAR"),button->onClose())
            .bounds(x+w-90,height-30,82,20).build());
    }

    private void refresh(){
        devices=NexusMicrophoneChoices.withDefault(NexusMicrophones.list());
        page=0;
        message="Lista de microfones atualizada ("+(devices.size()-1)+" dispositivo(s)).";
        rebuildWidgets();
    }

    private void choose(String name){
        try{
            NexusVoiceCasting.setMicrophone(name);
            message="Microfone salvo: "+NexusMicrophones.displayName(name)+
                (NexusVoiceCasting.enabled()?". Trocando captura...":".");
            rebuildWidgets();
        }catch(Exception ex){
            message=String.valueOf(ex.getMessage());
        }
    }

    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        int x=left(),w=panelWidth();
        g.fill(x-5,8,x+w+5,height-8,0xED100C14);
        g.fill(x-5,8,x+w+5,11,GOLD);
        g.drawCenteredString(font,"NEXUS ATERNA  |  MICROFONE DO VOICECAST",
            width/2,20,GOLD);
        g.drawString(font,"Selecionado: "+trim(NexusMicrophones.displayName(
            NexusVoiceCasting.selectedMicrophone()),w-100),x+8,43,WHITE,false);
        String active=NexusVoiceCasting.capturingMicrophone();
        g.drawString(font,"Em uso: "+trim(active.isEmpty()?"nenhum (captura desligada)":active,w-56),
            x+8,57,MUTED,false);
        if(!NexusMicrophoneChoices.available(NexusVoiceCasting.selectedMicrophone(),devices))
            g.drawString(font,"Dispositivo desconectado. Escolha outro ou use o padrão.",
                x+8,75,ERROR,false);
        else
            g.drawString(font,"A lista não abre o microfone. VoiceCast só grava quando ativado.",
                x+8,75,MUTED,false);
        if(height>=270)
            g.drawString(font,trim(message,w-15),x+8,height-89,MUTED,false);
        g.drawString(font,"Dispositivos: "+(devices.size()-1)+"   |   Página "+
            (page+1)+"/"+pages(),x+8,height-40,MUTED,false);
        super.render(g,mx,my,partial);
    }

    private String trim(String value,int maxWidth){
        String s=String.valueOf(value);
        if(font.width(s)<=maxWidth)return s;
        while(s.length()>1&&font.width(s+"…")>maxWidth)s=s.substring(0,s.length()-1);
        return s+"…";
    }
}
