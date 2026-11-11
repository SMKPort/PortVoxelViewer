package com.port.cinematic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.Locale;

/** The UI owns free-camera input, preventing movement keys from being sent as gameplay actions. */
public final class CinematicScreen extends Screen {
    private long previousFrame=0;
    private int durationSeconds=3;
    private boolean scrubDragging=false;
    private int timelineY;

    public CinematicScreen(){super(Component.literal("Port Cinematic Studio"));}

    @Override
    protected void init(){
        super.init();
        int left=Math.max(12,width/2-175),top=Math.max(42,height/2-84);
        int w=105,h=21,gap=7;
        addRenderableWidget(Button.builder(Component.literal("Câmera ON"),b->CinematicState.start())
            .bounds(left,top,w,h).build());
        addRenderableWidget(Button.builder(Component.literal("Câmera OFF"),b->CinematicState.stop())
            .bounds(left+w+gap,top,w,h).build());
        addRenderableWidget(Button.builder(Component.literal("Salvar ponto"),b->{
            if(!CinematicState.isActive())CinematicState.start();
            CinematicState.addFrame(durationSeconds);
        }).bounds(left+2*(w+gap),top,w,h).build());
        addRenderableWidget(Button.builder(Component.literal("▶ Reproduzir"),b->{
            if(CinematicState.play())Minecraft.getInstance().setScreen(null);
        }).bounds(left,top+h+gap,w,h).build());
        addRenderableWidget(Button.builder(Component.literal("Parar playback"),b->CinematicState.haltPlayback())
            .bounds(left+w+gap,top+h+gap,w,h).build());
        addRenderableWidget(Button.builder(Component.literal("Excluir último"),b->CinematicState.removeLast())
            .bounds(left+2*(w+gap),top+h+gap,w,h).build());
        addRenderableWidget(Button.builder(Component.literal("Duração -"),b->durationSeconds=Math.max(1,durationSeconds-1))
            .bounds(left,top+2*(h+gap),w,h).build());
        addRenderableWidget(Button.builder(Component.literal("Duração +"),b->durationSeconds=Math.min(120,durationSeconds+1))
            .bounds(left+w+gap,top+2*(h+gap),w,h).build());
        addRenderableWidget(Button.builder(Component.literal("Apagar cena"),b->CinematicState.clear())
            .bounds(left+2*(w+gap),top+2*(h+gap),w,h).build());
        timelineY=Math.min(height-60,top+3*(h+gap)+62);
        previousFrame=System.nanoTime();
    }

    private void keyboardMovement(){
        if(!CinematicState.isActive()||CinematicState.isPlaying())return;
        long now=System.nanoTime();
        double dt=Math.min(.08,Math.max(0,(now-previousFrame)/1_000_000_000.0));
        previousFrame=now;
        long handle=minecraft.getWindow().getWindow();
        boolean boost=GLFW.glfwGetKey(handle,GLFW.GLFW_KEY_LEFT_SHIFT)==GLFW.GLFW_PRESS;
        double step=(boost?16:5)*dt;
        double forward=(pressed(handle,GLFW.GLFW_KEY_W)?1:0)-(pressed(handle,GLFW.GLFW_KEY_S)?1:0);
        double strafe=(pressed(handle,GLFW.GLFW_KEY_D)?1:0)-(pressed(handle,GLFW.GLFW_KEY_A)?1:0);
        double height=(pressed(handle,GLFW.GLFW_KEY_E)?1:0)-(pressed(handle,GLFW.GLFW_KEY_Q)?1:0);
        if(forward!=0||strafe!=0||height!=0)CinematicState.moveLocal(forward*step,strafe*step,height*step);
    }
    private static boolean pressed(long window,int key){return GLFW.glfwGetKey(window,key)==GLFW.GLFW_PRESS;}

    @Override
    public void render(GuiGraphics g,int mx,int my,float partial){
        keyboardMovement();
        g.fill(0,0,width,height,0xE6111019);
        g.fill(0,0,width,4,0xFFE6B770);
        int cx=width/2;
        g.drawCenteredString(font,"§6§lPORT CINEMATIC STUDIO",cx,16,0xFFFFDD9B);
        g.drawCenteredString(font,"Câmera independente · sem controle do servidor",cx,29,0xFFBDB2C0);
        int infoY=Math.min(height-104,timelineY-40);
        String state=CinematicState.isPlaying()?"REPRODUZINDO":
            CinematicState.isActive()?"CÂMERA LIVRE":"DESATIVADA";
        g.drawCenteredString(font,"Estado: "+state+"   |   "+CinematicState.frames().size()+
             " keyframes   |   próximo intervalo: "+durationSeconds+" s",cx,infoY,0xFFFFE3B3);
        if(CinematicState.isActive()){
            String loc=String.format(Locale.ROOT,"X %.1f  Y %.1f  Z %.1f   /   Yaw %.0f  Pitch %.0f",
                 CinematicState.x(),CinematicState.y(),CinematicState.z(),
                 CinematicState.yaw(),CinematicState.pitch());
            g.drawCenteredString(font,loc,cx,infoY+14,0xFFDDD4DA);
        }
        int x1=Math.max(20,cx-174),x2=Math.min(width-20,cx+174);
        int barY=timelineY;
        g.fill(x1,barY,x2,barY+8,0xFF332B35);
        g.fill(x1,barY,x1+(int)((x2-x1)*CinematicState.timeline()),barY+8,0xFFC99C5E);
        int count=CinematicState.frames().size();
        for(int i=0;i<count;i++){
            int x=x1+(int)((x2-x1)*((double)i/Math.max(1,count-1)));
            g.fill(x-2,barY-4,x+2,barY+12,0xFFFFD38D);
        }
        g.drawCenteredString(font,"TIMELINE / Clique na faixa para avançar entre keyframes",cx,barY-15,0xFFA69EAD);
        g.drawCenteredString(font,"W A S D = mover · Q/E = altura · Shift = rápido · arraste mouse = girar",cx,
              Math.min(height-30,barY+32),0xFFC5B5BA);
        g.drawCenteredString(font,"ESC = sair e restaurar câmera · cena salva em config/port-cinematic-studio.json",
              cx,Math.min(height-16,barY+46),0xFF8F8494);
        super.render(g,mx,my,partial);
    }

    @Override
    public boolean mouseClicked(double mx,double my,int button){
        if(button==0&&timelineHit(mx,my)&&CinematicState.isActive()){
            scrubDragging=true;scrub(mx);return true;
        }
        return super.mouseClicked(mx,my,button);
    }
    @Override
    public boolean mouseDragged(double mx,double my,int button,double dx,double dy){
        if(button==0&&scrubDragging){scrub(mx);return true;}
        if(button==0&&CinematicState.isActive()&&!CinematicState.isPlaying()){
            CinematicState.rotate((float)(dx*.24),(float)(dy*.24));return true;
        }
        return super.mouseDragged(mx,my,button,dx,dy);
    }
    @Override
    public boolean mouseReleased(double mx,double my,int button){
        if(scrubDragging&&button==0){scrubDragging=false;return true;}
        return super.mouseReleased(mx,my,button);
    }
    private boolean timelineHit(double mx,double my){
        return my>=timelineY-8&&my<=timelineY+16&&mx>=Math.max(20,width/2-174)&&mx<=Math.min(width-20,width/2+174);
    }
    private void scrub(double mx){
        int x1=Math.max(20,width/2-174),x2=Math.min(width-20,width/2+174);
        CinematicState.seek((mx-x1)/(Math.max(1,x2-x1)));
    }
    @Override
    public void onClose(){
        if(!CinematicState.isPlaying())CinematicState.stop();
        super.onClose();
    }
}
