package com.port.hudstudio;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** In-game HUD editor: click an element to select, drag to position and use controls to style it. */
public final class HudEditorScreen extends Screen {
    private String selected="health";
    private boolean dragging=false;
    private float grabX,grabY;

    public HudEditorScreen(){super(Component.literal("Port HUD Studio"));}

    @Override
    protected void init(){
        super.init();
        int cell=Math.min(108,Math.max(77,(width-22)/5));
        int start=Math.max(6,(width-5*cell-24)/2);
        int top=23;
        addRenderableWidget(Button.builder(Component.literal("Perfil: "+HudLayout.profile()),b->{
            HudLayout.nextProfile();minecraft.setScreen(new HudEditorScreen());
        }).bounds(start,top,cell,21).build());
        addRenderableWidget(Button.builder(Component.literal("Exibir / ocultar"),b->HudLayout.toggleWidget(selected))
            .bounds(start+cell+6,top,cell,21).build());
        addRenderableWidget(Button.builder(Component.literal("Escala +"),b->HudLayout.scaleWidget(selected,.1f))
            .bounds(start+2*(cell+6),top,cell,21).build());
        addRenderableWidget(Button.builder(Component.literal("Escala -"),b->HudLayout.scaleWidget(selected,-.1f))
            .bounds(start+3*(cell+6),top,cell,21).build());
        addRenderableWidget(Button.builder(Component.literal("Concluir"),b->onClose())
            .bounds(start+4*(cell+6),top,cell,21).build());

        int second=51;
        addRenderableWidget(Button.builder(Component.literal("Opacidade +"),b->HudLayout.opacityWidget(selected,.1f))
            .bounds(start,second,cell,21).build());
        addRenderableWidget(Button.builder(Component.literal("Opacidade -"),b->HudLayout.opacityWidget(selected,-.1f))
            .bounds(start+cell+6,second,cell,21).build());
        addRenderableWidget(Button.builder(Component.literal("Modo cinema"),b->HudLayout.toggleCinema())
            .bounds(start+2*(cell+6),second,cell,21).build());
        addRenderableWidget(Button.builder(Component.literal("Reset perfil"),b->HudLayout.resetCurrent())
            .bounds(start+3*(cell+6),second,cell,21).build());
        addRenderableWidget(Button.builder(Component.literal("Salvar HUD"),b->HudLayout.save())
            .bounds(start+4*(cell+6),second,cell,21).build());
    }

    @Override
    public void render(GuiGraphics g,int mx,int my,float partial){
        g.fill(0,0,width,height,0xC30A0B11);
        g.fill(0,0,width,4,0xFFE0B36B);
        g.drawCenteredString(font,"§6§lPORT HUD STUDIO  /  EDITOR VISUAL",width/2,9,0xFFFFD599);
        HudLayout.render(g,true,selected);
        g.fill(0,height-51,width,height,0xD8111016);
        HudLayout.Widget w=HudLayout.find(selected);
        String item=w==null?"NENHUM":selected.toUpperCase();
        g.drawCenteredString(font,"Selecionado: "+item+"  |  Perfil: "+HudLayout.profile()+
             "  |  Esconder durante cinema: "+(HudLayout.hideCinema()?"SIM":"NÃO"),width/2,height-42,0xFFFFD5A2);
        g.drawCenteredString(font,"Clique em um widget e arraste para mover. Ajuste escala, opacidade ou visibilidade pelos botões.",
             width/2,height-28,0xFFD5C6D0);
        g.drawCenteredString(font,"ESC para salvar e fechar · config/port-hud-studio.json",width/2,height-15,0xFF9C919F);
        super.render(g,mx,my,partial);
    }

    @Override
    public boolean mouseClicked(double mx,double my,int button){
        if(super.mouseClicked(mx,my,button))return true;
        if(button!=0)return false;
        var items=HudLayout.widgets();
        for(int i=items.size()-1;i>=0;i--){
            HudLayout.Widget w=items.get(i);
            if(HudLayout.hit(w,mx,my,width,height)){
                selected=w.id;dragging=true;
                grabX=(float)(mx-w.x*width);grabY=(float)(my-w.y*height);
                return true;
            }
        }
        return false;
    }
    @Override
    public boolean mouseDragged(double mx,double my,int button,double dx,double dy){
        if(dragging&&button==0){
            HudLayout.moveWidget(selected,(float)((mx-grabX)/width),(float)((my-grabY)/height));
            return true;
        }
        return super.mouseDragged(mx,my,button,dx,dy);
    }
    @Override
    public boolean mouseReleased(double mx,double my,int button){
        if(dragging&&button==0){dragging=false;HudLayout.save();return true;}
        return super.mouseReleased(mx,my,button);
    }
    @Override public void onClose(){HudLayout.save();super.onClose();}
}
