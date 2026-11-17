package com.port.nexusclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Select an actually equipped spell to add as a new hit-triggered macro. */
final class NexusMacroPickerScreen extends Screen {
    private static final int GOLD=0xFFE9BB68,WHITE=0xFFECE1D3,MUTED=0xFFAE9DA6;
    private final Screen parent;
    private int page;
    private String message="Escolha uma magia equipada. A macro começa desligada.";
    NexusMacroPickerScreen(Screen parent){
        super(Component.literal("Nexus Aterna | Adicionar macro"));
        this.parent=parent;
    }
    private int pane(){return Math.min(550,width-20);}
    private int left(){return (width-pane())/2;}
    private List<String> options(){return NexusCombatMacro.candidates();}
    private int pages(){return Math.max(1,(options().size()+5)/6);}
    private String shorten(String raw,int maxWidth){
        String s=String.valueOf(raw);
        while(s.length()>1&&font.width(s)>maxWidth)s=s.substring(0,s.length()-1);
        return s;
    }
    private String friendly(String id){
        String text=id.substring(id.indexOf(':')+1).replace('_',' ').replace('-',' ');
        return text.isBlank()?id:Character.toUpperCase(text.charAt(0))+text.substring(1);
    }
    @Override protected void init(){
        int x=left(),w=pane();
        Set<String> registered=NexusCombatMacro.entries().stream()
            .map(NexusCombatMacro.View::spell).collect(Collectors.toSet());
        List<String> equipped=options();
        page=Math.max(0,Math.min(page,pages()-1));
        for(int i=page*6;i<Math.min(equipped.size(),(page+1)*6);i++){
            String id=equipped.get(i);
            boolean already=registered.contains(id);
            String title=(already?"§a✓ ":"§6+ ")+friendly(id)+"  §7["+id+"]";
            addRenderableWidget(Button.builder(Component.literal(shorten(title,w-43)),b->{
                try{
                    message=NexusCombatMacro.addSpell(id);
                    if(NexusCombatMacro.spell().equals(id))
                        minecraft.setScreen(new NexusMacroScreen(parent));
                }catch(Exception error){message=error.getMessage();}
            }).bounds(x+7,79+(i%6)*28,w-14,22).build());
        }
        addRenderableWidget(Button.builder(Component.literal("◀"),b->{
            if(page>0){page--;rebuildWidgets();}
        }).bounds(x+7,height-61,40,20).build());
        addRenderableWidget(Button.builder(Component.literal("▶"),b->{
            if(page<pages()-1){page++;rebuildWidgets();}
        }).bounds(x+52,height-61,40,20).build());
        addRenderableWidget(Button.builder(Component.literal("VOLTAR"),b->onClose())
            .bounds(x+w-100,height-30,93,20).build());
    }
    @Override public void onClose(){minecraft.setScreen(new NexusMacroScreen(parent));}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float partialTick){
        int x=left(),w=pane();
        g.fill(x-5,6,x+w+5,height-6,0xEF0D0A14);
        g.fill(x-5,6,x+w+5,9,GOLD);
        g.drawCenteredString(font,"+ ADICIONAR MACRO DE COMBATE",width/2,20,GOLD);
        g.drawString(font,"Só magias equipadas; nenhuma conjuração ao cadastrar.",
            x+7,48,MUTED,false);
        g.drawString(font,"Magias disponíveis: "+options().size(),x+7,64,WHITE,false);
        if(options().isEmpty())
            g.drawString(font,"Nenhuma magia detectada no Iron's Spells.",
                x+7,116,MUTED,false);
        if(height>=320){
            g.drawString(font,shorten(message,w-14),x+7,height-88,WHITE,false);
            g.drawString(font,"Página "+(page+1)+"/"+pages(),x+111,height-56,GOLD,false);
        }
        super.render(g,mx,my,partialTick);
    }
}
