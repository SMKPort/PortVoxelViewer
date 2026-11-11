package com.port.hudstudio;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value=PortHudStudio.MOD_ID,dist=Dist.CLIENT)
public final class PortHudStudio {
    public static final String MOD_ID="porthud";

    public PortHudStudio(){
        HudLayout.load();
        NeoForge.EVENT_BUS.addListener(this::commands);
        NeoForge.EVENT_BUS.addListener(this::render);
    }

    private void render(RenderGuiEvent.Post event){
        if(Minecraft.getInstance().screen instanceof HudEditorScreen)return;
        HudLayout.render(event.getGuiGraphics(),false,"");
    }

    private void commands(RegisterClientCommandsEvent event){
        event.getDispatcher().register(Commands.literal("porthud")
            .executes(ctx->{open();return 1;})
            .then(Commands.literal("edit").executes(ctx->{open();return 1;}))
            .then(Commands.literal("on").executes(ctx->{
                if(!HudLayout.enabled())HudLayout.toggle();say("HUD ativado.");return 1;
            }))
            .then(Commands.literal("off").executes(ctx->{
                if(HudLayout.enabled())HudLayout.toggle();say("HUD ocultado.");return 1;
            }))
            .then(Commands.literal("next").executes(ctx->{
                say("Perfil: "+HudLayout.nextProfile());return 1;
            }))
            .then(Commands.literal("status").executes(ctx->{
                say("Perfil: "+HudLayout.profile()+" | HUD "+(HudLayout.enabled()?"ON":"OFF")+
                  " | Modo cinema: "+(HudLayout.hideCinema()?"AUTO":"MANUAL"));
                return 1;
            }))
        );
    }
    private static void open(){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=null)mc.setScreen(new HudEditorScreen());
    }
    private static void say(String msg){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=null)mc.player.displayClientMessage(Component.literal("§6[PORT HUD] §f"+msg),false);
    }
}
