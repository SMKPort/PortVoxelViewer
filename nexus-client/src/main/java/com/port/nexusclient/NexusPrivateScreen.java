package com.port.nexusclient;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * On-device GUI. Messages stay on the player's screen and are never sent
 * as vanilla chat, /l commands, Plasmo voice or broadcast packets.
 */
final class NexusPrivateScreen extends Screen {
    private EditBox entry;
    private int scroll=0;
    NexusPrivateScreen(){super(Component.literal("Nexus Aterna · Canal Neural Privado"));}

    @Override
    protected void init(){
        int widthBox=Math.min(490,width-28);
        int x=(width-widthBox)/2;
        int y=height-39;
        entry=new EditBox(font,x,y,widthBox-86,20,Component.literal("Converse com Nexus"));
        entry.setMaxLength(450);
        entry.setHint(Component.literal("Port, o que deseja me dizer?"));
        addRenderableWidget(entry);
        addRenderableWidget(Button.builder(Component.literal("ENVIAR"),b->send())
            .bounds(x+widthBox-79,y,79,20).build());
        addRenderableWidget(Button.builder(Component.literal("LIMPAR HISTÓRICO"),b->{
            if(hasShiftDown()){
                NexusPrivateMind.clear();scroll=0;
            }else{
                NexusClientMod.notifyPlayer("No canal privado, pressione SHIFT e clique em LIMPAR HISTÓRICO para confirmar.");
            }
        }).bounds(x,y-26,132,20).build());
        addRenderableWidget(Button.builder(Component.literal("FECHAR"),b->onClose())
            .bounds(x+widthBox-70,Math.max(8,y-26),70,20).build());
        setInitialFocus(entry);
    }
    private void send(){
        if(entry==null)return;
        String message=entry.getValue();
        if(message==null||message.trim().isEmpty())return;
        try{
            NexusPrivateMind.send(message);
            entry.setValue("");
            scroll=0;
        }catch(IllegalArgumentException|IllegalStateException error){
            NexusClientMod.notifyPlayer("§c[Privado] §f"+error.getMessage());
        }
    }
    @Override
    public boolean keyPressed(int key,int scanCode,int modifiers){
        if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER){
            if(entry!=null&&entry.isFocused()){send();return true;}
        }
        if(key==GLFW.GLFW_KEY_PAGE_UP){
            scroll=Math.min(100,scroll+3);return true;
        }
        if(key==GLFW.GLFW_KEY_PAGE_DOWN){
            scroll=Math.max(0,scroll-3);return true;
        }
        return super.keyPressed(key,scanCode,modifiers);
    }
    @Override
    public boolean mouseScrolled(double mouseX,double mouseY,double scrollX,double scrollY){
        if(scrollY>0)scroll=Math.min(100,scroll+2);
        if(scrollY<0)scroll=Math.max(0,scroll-2);
        return true;
    }
    @Override
    public boolean isPauseScreen(){return false;}
    @Override
    public void render(GuiGraphics graphics,int mouseX,int mouseY,float delta){
        int w=Math.min(490,width-28),x=(width-w)/2;
        int top=19,bottom=height-70;
        graphics.fill(x-6,top-12,x+w+6,bottom+5,0xE80F0911);
        graphics.fill(x-6,top-12,x+w+6,top-9,0xFFD5A74F);
        graphics.drawString(font,"NEXUS ATERNA  /  CANAL NEURAL PRIVADO",x,top,0xFFF3DAB2,true);
        graphics.drawString(font,"Conversa visível somente neste cliente · N para abrir",x,top+14,0xFFB1A7B7,false);
        String summary=NexusPrivateMind.status();
        graphics.drawString(font,trim(summary,w-6),x,top+29,0xFFBDB0A3,false);
        graphics.drawString(font,NexusPrivateMind.waiting()?
            "O Nexus está formulando uma resposta privada...":
            "Roleplay independente · sem mensagens no /l",x,top+44,0xFFD6A85A,false);

        List<NexusPrivateMind.Line> lines=NexusPrivateMind.history();
        List<ScreenRow> rendered=new ArrayList<>();
        for(NexusPrivateMind.Line line:lines){
            boolean port="user".equals(line.kind);
            int header=port?0xFFDBB28E:"thought".equals(line.kind)?0xFFD29DE2:0xFFF2D58E;
            String name=port?"PORT":"system".equals(line.kind)?"SISTEMA":
                "thought".equals(line.kind)?"NEXUS · PENSAMENTO":"NEXUS ATERNA";
            rendered.add(new ScreenRow(Component.literal(name).getVisualOrderText(),header));
            for(FormattedCharSequence part:font.split(Component.literal(line.text),w-12))
                rendered.add(new ScreenRow(part,0xFFE1DAD3));
            rendered.add(new ScreenRow(Component.literal(" ").getVisualOrderText(),0xFFE1DAD3));
        }
        int firstY=top+66;
        if(bottom>firstY){
            int maxRows=Math.max(1,(bottom-firstY)/11);
            scroll=Math.min(scroll,Math.max(0,rendered.size()-maxRows));
            int from=Math.max(0,rendered.size()-maxRows-scroll);
            int to=Math.min(rendered.size(),from+maxRows);
            int y=firstY;
            graphics.enableScissor(x-2,y-3,x+w+2,bottom);
            for(int i=from;i<to;i++){
                ScreenRow row=rendered.get(i);
                graphics.drawString(font,row.text(),x+3,y,row.color(),false);
                y+=11;
            }
            graphics.disableScissor();
        }
        if(!lines.isEmpty())
            graphics.drawString(font,
                "Mensagens locais: "+lines.size()+" | PgUp/PgDn para percorrer",
                x,Math.max(top+55,bottom-8),0xFFAA9D91,false);
        // Render widgets after the panel to avoid covering the message box and buttons.
        super.render(graphics,mouseX,mouseY,delta);
    }
    private record ScreenRow(FormattedCharSequence text,int color){}
    private String trim(String value,int width){
        if(font.width(value)<=width)return value;
        while(!value.isEmpty()&&font.width(value+"…")>width)
            value=value.substring(0,value.length()-1);
        return value+"…";
    }
}
