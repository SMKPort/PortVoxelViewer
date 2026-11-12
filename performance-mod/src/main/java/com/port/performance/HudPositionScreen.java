package com.port.performance;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Drag and keyboard editor of the existing Port Performance HUD. */
final class HudPositionScreen extends Screen {
    private boolean dragging=false;
    private double dragOffsetX=0,dragOffsetY=0;
    private String notice="Clique no painel e arraste para qualquer região da tela.";

    HudPositionScreen(){
        super(Component.literal("Port Performance - Posicionar HUD"));
    }

    @Override
    protected void init(){
        int w=Math.min(105,Math.max(62,(width-36)/3));
        int total=3*w+12;
        int start=(width-total)/2;
        int y=Math.max(17,height-31);
        addRenderableWidget(Button.builder(Component.literal("Resetar"),button->{
            PerformanceHud.reset();
            notice="Posição padrão restaurada.";
        }).bounds(start,y,w,20).build());
        addRenderableWidget(Button.builder(Component.literal("Centralizar"),button->{
            PerformanceConfig.hudX=0.5f;
            PerformanceConfig.hudY=0.5f;
            PerformanceConfig.save();
            notice="HUD centralizado.";
        }).bounds(start+w+6,y,w,20).build());
        addRenderableWidget(Button.builder(Component.literal("Concluir"),button->onClose())
            .bounds(start+2*(w+6),y,w,20).build());
    }

    @Override
    public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){
        // See the world behind the editor while preventing other UI clicks.
        graphics.fill(0,0,width,height,0x9C111019);
        graphics.fill(0,0,width,3,0xFFE8B976);
        graphics.drawCenteredString(font,"§6§lPORT PERFORMANCE / EDITOR DE HUD",width/2,7,0xFFF9D8A0);
        graphics.drawCenteredString(font,"Arraste o quadro dourado ou use as setas do teclado.",width/2,22,0xFFE0CFD7);
        graphics.drawCenteredString(font,"Shift + setas: mover mais rápido | ESC: salvar e fechar",
            width/2,35,0xFFC2B5C3);

        PerformanceHud.draw(graphics,minecraft,true);
        graphics.drawCenteredString(font,notice,width/2,Math.max(55,height-46),0xFFF0CDA0);
        super.render(graphics,mouseX,mouseY,partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX,double mouseY,int button){
        if(super.mouseClicked(mouseX,mouseY,button))return true;
        int left=PerformanceHud.left(width),top=PerformanceHud.top(height);
        if(button==0&&mouseX>=left-3&&mouseX<=left+PerformanceHud.WIDTH+3
            &&mouseY>=top-3&&mouseY<=top+PerformanceHud.HEIGHT+3){
            dragging=true;
            dragOffsetX=mouseX-left;
            dragOffsetY=mouseY-top;
            notice="Movendo HUD. Solte o mouse para salvar.";
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX,double mouseY,int button,double dragX,double dragY){
        if(dragging&&button==0){
            PerformanceHud.moveTo(mouseX-dragOffsetX,mouseY-dragOffsetY,width,height);
            return true;
        }
        return super.mouseDragged(mouseX,mouseY,button,dragX,dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX,double mouseY,int button){
        if(dragging&&button==0){
            PerformanceHud.moveTo(mouseX-dragOffsetX,mouseY-dragOffsetY,width,height);
            dragging=false;
            PerformanceConfig.save();
            notice="Posição salva. Ela será mantida nas próximas sessões.";
            return true;
        }
        return super.mouseReleased(mouseX,mouseY,button);
    }

    @Override
    public boolean keyPressed(int keyCode,int scanCode,int modifiers){
        int delta=((modifiers&GLFW.GLFW_MOD_SHIFT)!=0)?10:1;
        int dx=0,dy=0;
        if(keyCode==GLFW.GLFW_KEY_LEFT)dx=-delta;
        if(keyCode==GLFW.GLFW_KEY_RIGHT)dx=delta;
        if(keyCode==GLFW.GLFW_KEY_UP)dy=-delta;
        if(keyCode==GLFW.GLFW_KEY_DOWN)dy=delta;
        if(dx!=0||dy!=0){
            PerformanceHud.moveTo(PerformanceHud.left(width)+dx,
                PerformanceHud.top(height)+dy,width,height);
            PerformanceConfig.save();
            notice="Posição ajustada pelas setas e salva.";
            return true;
        }
        return super.keyPressed(keyCode,scanCode,modifiers);
    }

    @Override
    public void onClose(){
        dragging=false;
        PerformanceConfig.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen(){return false;}
}
