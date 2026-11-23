package com.port.performance;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Drag panel, drag bottom-right handle, use +/- or wheel to resize the HUD. */
final class HudPositionScreen extends Screen {
    private boolean dragging,resizing;
    private double dragOffsetX,dragOffsetY,resizeStartX;
    private float resizeStartScale;
    private String notice="Arraste o painel para mover. Arraste o canto inferior direito para redimensionar.";

    HudPositionScreen(){super(Component.literal("Port Performance - Editor de HUD"));}

    @Override
    protected void init(){
        int w=Math.min(105,Math.max(62,(width-36)/3));
        int start=(width-(3*w+12))/2;
        int row=Math.max(55,height-31);
        addRenderableWidget(Button.builder(Component.literal("Resetar"),b->{
            PerformanceHud.reset();
            notice="Posição e tamanho padrão restaurados.";
        }).bounds(start,row,w,20).build());
        addRenderableWidget(Button.builder(Component.literal("Centralizar"),b->{
            PerformanceConfig.hudX=0.5f;
            PerformanceConfig.hudY=0.5f;
            PerformanceConfig.save();
            notice="HUD centralizado.";
        }).bounds(start+w+6,row,w,20).build());
        addRenderableWidget(Button.builder(Component.literal("Concluir"),b->onClose())
            .bounds(start+2*(w+6),row,w,20).build());

        int resizeRow=Math.max(34,row-27);
        addRenderableWidget(Button.builder(Component.literal("-"),b->{
            PerformanceHud.setScale(PerformanceConfig.hudScale-0.10f);
            notice="Tamanho atualizado.";
        }).bounds(width/2-72,resizeRow,45,20).build());
        addRenderableWidget(Button.builder(Component.literal("+"),b->{
            PerformanceHud.setScale(PerformanceConfig.hudScale+0.10f);
            notice="Tamanho atualizado.";
        }).bounds(width/2+27,resizeRow,45,20).build());
    }

    @Override
    public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick){
        g.fill(0,0,width,height,0x9C111019);
        g.fill(0,0,width,3,0xFFE8B976);
        g.drawCenteredString(font,"§6§lPORT PERFORMANCE / EDITOR DE HUD",width/2,7,0xFFF9D8A0);
        g.drawCenteredString(font,"Mover: arrastar ou setas | Tamanho: canto dourado, +/- ou roda do mouse",
            width/2,22,0xFFE0CFD7);
        g.drawCenteredString(font,"Escala: "+Math.round(PerformanceConfig.hudScale*100)+
            "% | Shift + setas: 10 px | ESC: salvar",width/2,35,0xFFC2B5C3);
        PerformanceHud.draw(g,minecraft,true);
        g.drawCenteredString(font,notice,width/2,Math.max(46,height-89),0xFFF0CDA0);
        super.render(g,mouseX,mouseY,partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX,double mouseY,int button){
        if(super.mouseClicked(mouseX,mouseY,button))return true;
        int left=PerformanceHud.left(width,height),top=PerformanceHud.top(width,height);
        int pw=PerformanceHud.panelWidth(width,height),ph=PerformanceHud.panelHeight(width,height);
        if(button==0 && mouseX>=left+pw-17 && mouseX<=left+pw+4 &&
            mouseY>=top+ph-17 && mouseY<=top+ph+4){
            resizing=true;
            resizeStartX=mouseX;
            resizeStartScale=PerformanceConfig.hudScale;
            notice="Redimensionando HUD. Solte para salvar.";
            return true;
        }
        if(button==0 && mouseX>=left-3 && mouseX<=left+pw+3 &&
            mouseY>=top-3 && mouseY<=top+ph+3){
            dragging=true;
            dragOffsetX=mouseX-left;
            dragOffsetY=mouseY-top;
            notice="Movendo HUD. Solte para salvar.";
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX,double mouseY,int button,double dragX,double dragY){
        if(resizing && button==0){
            PerformanceHud.previewScale(resizeStartScale+(float)((mouseX-resizeStartX)/PerformanceHud.WIDTH));
            return true;
        }
        if(dragging && button==0){
            PerformanceHud.moveTo(mouseX-dragOffsetX,mouseY-dragOffsetY,width,height);
            return true;
        }
        return super.mouseDragged(mouseX,mouseY,button,dragX,dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX,double mouseY,int button){
        if((dragging||resizing)&&button==0){
            if(dragging)PerformanceHud.moveTo(mouseX-dragOffsetX,mouseY-dragOffsetY,width,height);
            dragging=resizing=false;
            PerformanceConfig.save();
            notice="Posição e tamanho salvos para as próximas sessões.";
            return true;
        }
        return super.mouseReleased(mouseX,mouseY,button);
    }

    @Override
    public boolean mouseScrolled(double mouseX,double mouseY,double scrollX,double scrollY){
        int x=PerformanceHud.left(width,height),y=PerformanceHud.top(width,height);
        if(mouseX>=x&&mouseX<=x+PerformanceHud.panelWidth(width,height)&&
           mouseY>=y&&mouseY<=y+PerformanceHud.panelHeight(width,height)){
            PerformanceHud.setScale(PerformanceConfig.hudScale+(scrollY>0?0.05f:-0.05f));
            notice="Tamanho atualizado com a roda do mouse.";
            return true;
        }
        return super.mouseScrolled(mouseX,mouseY,scrollX,scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode,int scanCode,int modifiers){
        int delta=((modifiers&GLFW.GLFW_MOD_SHIFT)!=0)?10:1,dx=0,dy=0;
        if(keyCode==GLFW.GLFW_KEY_LEFT)dx=-delta;
        if(keyCode==GLFW.GLFW_KEY_RIGHT)dx=delta;
        if(keyCode==GLFW.GLFW_KEY_UP)dy=-delta;
        if(keyCode==GLFW.GLFW_KEY_DOWN)dy=delta;
        if(dx!=0||dy!=0){
            PerformanceHud.moveTo(PerformanceHud.left(width,height)+dx,
                PerformanceHud.top(width,height)+dy,width,height);
            PerformanceConfig.save();
            notice="Posição ajustada e salva.";
            return true;
        }
        return super.keyPressed(keyCode,scanCode,modifiers);
    }
    @Override
    public void onClose(){
        dragging=resizing=false;
        PerformanceConfig.save();
        super.onClose();
    }
    @Override
    public boolean isPauseScreen(){return false;}
}
