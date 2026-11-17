package com.port.nexusclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Only spells really equipped in Iron's Spells can be chosen for upkeep. */
final class NexusBlessingPickerScreen extends Screen {
    private static final int GOLD=0xFFE9BB68,WHITE=0xFFE8E2D8,MUTED=0xFFB9ABA4;
    private final Screen parent;
    private int page;
    private String message="Selecione uma magia equipada. Ela começa desligada.";

    NexusBlessingPickerScreen(Screen parent) {
        super(Component.literal("Nexus Aterna | Adicionar magia"));
        this.parent=parent;
    }
    private int pane(){return Math.min(550,width-20);}
    private int left(){return (width-pane())/2;}
    private List<String> options(){return NexusBlessing.candidates();}
    private int pages(){return Math.max(1,(options().size()+5)/6);}
    private String shorten(String text,int maxWidth){
        String s=text;
        while(s.length()>1&&font.width(s)>maxWidth)s=s.substring(0,s.length()-1);
        return s;
    }
    private String name(String id){
        String text=id.substring(id.indexOf(':')+1).replace('_',' ').replace('-',' ');
        if(text.isBlank())return id;
        return Character.toUpperCase(text.charAt(0))+text.substring(1);
    }
    @Override protected void init(){
        int x=left(),w=pane();
        List<String> spells=options();
        Set<String> added=NexusBlessing.entries().stream()
            .map(NexusBlessing.View::spellId).collect(Collectors.toSet());
        page=Math.max(0,Math.min(page,pages()-1));
        for(int i=page*6;i<Math.min(spells.size(),(page+1)*6);i++){
            String id=spells.get(i);
            boolean existing=added.contains(id);
            String label=(existing?"§a✓ ":"§6+ ")+name(id)+"  §7["+id+"]";
            addRenderableWidget(Button.builder(Component.literal(shorten(label,w-45)),b->{
                message=NexusBlessing.addSpell(id);
                if(NexusBlessing.spellId().equals(id))
                    minecraft.setScreen(new NexusBlessingScreen(parent));
            }).bounds(x+7,78+(i%6)*28,w-14,23).build());
        }
        addRenderableWidget(Button.builder(Component.literal("◀"),
            b->{if(page>0){page--;rebuildWidgets();}})
            .bounds(x+7,height-61,40,20).build());
        addRenderableWidget(Button.builder(Component.literal("▶"),
            b->{if(page<pages()-1){page++;rebuildWidgets();}})
            .bounds(x+52,height-61,40,20).build());
        addRenderableWidget(Button.builder(Component.literal("VOLTAR"),
            b->onClose()).bounds(x+w-100,height-30,93,20).build());
    }
    @Override public void onClose(){minecraft.setScreen(new NexusBlessingScreen(parent));}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float partialTick){
        int x=left(),w=pane();
        g.fill(x-5,6,x+w+5,height-6,0xF0140E15);
        g.fill(x-5,6,x+w+5,9,0xFFE2B55C);
        g.drawCenteredString(font,"+ ADICIONAR MAGIA EQUIPADA",width/2,19,GOLD);
        g.drawString(font,"As magias novas precisam aprender o efeito antes de ativar.",
            x+7,46,MUTED,false);
        g.drawString(font,"Magias disponíveis: "+options().size(),x+7,62,WHITE,false);
        if(options().isEmpty())
            g.drawString(font,"Nenhuma magia detectada na seleção do Iron's Spells.",
                x+7,112,MUTED,false);
        if(height>=320){
            g.drawString(font,shorten(message,w-14),x+7,height-89,WHITE,false);
            g.drawString(font,"Página "+(page+1)+"/"+pages(),x+110,height-56,GOLD,false);
        }
        super.render(g,mx,my,partialTick);
    }
}
