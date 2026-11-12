package com.port.macrostudio;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/** Hotkeys and commands run on the Minecraft client, with no server plugin. */
@Mod(value=PortMacroStudio.MOD_ID,dist=Dist.CLIENT)
public final class PortMacroStudio {
    public static final String MOD_ID="portmacro";
    private int previousKeys=0;

    public PortMacroStudio(){
        MacroLibrary.load();
        NeoForge.EVENT_BUS.addListener(this::clientTick);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        NeoForge.EVENT_BUS.addListener(this::renderStatus);
    }

    private void clientTick(ClientTickEvent.Post event){
        MacroRunner.tick();
        Minecraft mc=Minecraft.getInstance();
        if(mc.screen!=null||mc.player==null||!mc.isWindowActive()){previousKeys=0;return;}
        long handle=mc.getWindow().getWindow();
        if(handle==0)return;
        boolean ctrl=pressed(handle,GLFW.GLFW_KEY_LEFT_CONTROL)||pressed(handle,GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean alt=pressed(handle,GLFW.GLFW_KEY_LEFT_ALT)||pressed(handle,GLFW.GLFW_KEY_RIGHT_ALT);
        if(!(ctrl&&alt)){previousKeys=0;return;}
        int mask=0;
        if(pressed(handle,GLFW.GLFW_KEY_M))mask|=1;
        for(int n=0;n<4;n++)if(pressed(handle,GLFW.GLFW_KEY_1+n))mask|=(2<<n);
        int fresh=mask&~previousKeys;previousKeys=mask;
        if((fresh&1)!=0){mc.setScreen(new MacroStudioScreen());return;}
        for(int n=0;n<4;n++){
            if((fresh&(2<<n))!=0){MacroLibrary.select(n);MacroRunner.start(n);break;}
        }
    }

    private static boolean pressed(long handle,int key){
        return GLFW.glfwGetKey(handle,key)==GLFW.GLFW_PRESS;
    }

    private void registerCommands(RegisterClientCommandsEvent event){
        event.getDispatcher().register(Commands.literal("portmacro")
            .executes(ctx->{open();return 1;})
            .then(Commands.literal("open").executes(ctx->{open();return 1;}))
            .then(Commands.literal("play")
                .then(Commands.argument("slot",IntegerArgumentType.integer(1,4))
                    .executes(ctx->{
                        int slot=IntegerArgumentType.getInteger(ctx,"slot")-1;
                        MacroLibrary.select(slot);
                        MacroRunner.start(slot);
                        return 1;
                    })))
            .then(Commands.literal("stop").executes(ctx->{
                MacroRunner.stop("Macro interrompida pelo usuário.");return 1;
            }))
            .then(Commands.literal("pause").executes(ctx->{
                if(MacroRunner.running()&&!MacroRunner.paused())MacroRunner.pauseToggle();
                return 1;
            }))
            .then(Commands.literal("resume").executes(ctx->{
                if(MacroRunner.running()&&MacroRunner.paused())MacroRunner.pauseToggle();
                return 1;
            }))
            .then(Commands.literal("chat")
                .then(Commands.literal("on").executes(ctx->{
                    MacroRunner.chatPermission(true);return 1;
                }))
                .then(Commands.literal("off").executes(ctx->{
                    MacroRunner.chatPermission(false);return 1;
                })))
            .then(Commands.literal("status").executes(ctx->{
                say("Macro "+(MacroLibrary.selected()+1)+": "+MacroLibrary.selectedMacro().name+
                    " | "+MacroRunner.status());
                say("Chat: "+(MacroRunner.allowChat()?"autorizado":"bloqueado")+
                    " | Editor: CTRL+ALT+M | Executar: CTRL+ALT+1..4");
                return 1;
            }))
        );
    }

    private void renderStatus(RenderGuiEvent.Post event){
        Minecraft mc=Minecraft.getInstance();
        if(!MacroRunner.running()||mc.player==null||mc.options.hideGui)return;
        var g=event.getGuiGraphics();
        int w=210,x=Math.max(3,g.guiWidth()-w-6),y=7;
        g.fill(x,y,x+w,y+29,0xD81A151D);
        g.fill(x,y,x+40,y+2,0xFFE4B26C);
        g.drawString(mc.font,"PORT MACRO / "+(MacroRunner.paused()?"PAUSADO":"EXECUTANDO"),
            x+7,y+5,0xFFEAC07F,true);
        g.drawString(mc.font,MacroLibrary.selectedMacro().name,x+7,y+18,0xFFE7DEDD,true);
    }

    private static void open(){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=null)mc.setScreen(new MacroStudioScreen());
    }
    private static void say(String message){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=null)mc.player.displayClientMessage(Component.literal("§6[PORT MACRO] §f"+message),false);
    }
}
