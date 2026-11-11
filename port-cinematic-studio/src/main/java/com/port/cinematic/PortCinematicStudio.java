package com.port.cinematic;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only studio: opens a camera editor without moving the real player. */
@Mod(value=PortCinematicStudio.MOD_ID,dist=Dist.CLIENT)
public final class PortCinematicStudio {
    public static final String MOD_ID="portcinema";
    public PortCinematicStudio(){
        CinematicState.load();
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event)->CinematicState.tick());
        NeoForge.EVENT_BUS.addListener(this::commands);
    }
    private void commands(RegisterClientCommandsEvent event){
        event.getDispatcher().register(Commands.literal("portcinema")
            .executes(ctx->{openEditor();return 1;})
            .then(Commands.literal("open").executes(ctx->{openEditor();return 1;}))
            .then(Commands.literal("camera").executes(ctx->{
                if(CinematicState.start())say("Câmera ativada. Use /portcinema open para abrir o editor.");
                else say("Entre num mundo antes de iniciar a câmera.");
                return 1;
            }))
            .then(Commands.literal("frame").executes(ctx->{
                if(CinematicState.start()&&CinematicState.addFrame(3))say("Keyframe salvo (3 segundos para o próximo ponto).");
                else say("Não foi possível adicionar keyframe.");
                return 1;
            }))
            .then(Commands.literal("play").executes(ctx->{
                if(CinematicState.play())say("Sequência iniciada.");
                else say("Adicione pelo menos dois keyframes para reproduzir.");
                return 1;
            }))
            .then(Commands.literal("stop").executes(ctx->{
                CinematicState.stop();say("Câmera restaurada.");return 1;
            }))
            .then(Commands.literal("status").executes(ctx->{
                say((CinematicState.isPlaying()?"Reproduzindo":CinematicState.isActive()?"Câmera livre":"Desativado")+
                    " | Keyframes: "+CinematicState.frames().size());
                return 1;
            }))
        );
    }
    private void openEditor(){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null)return;
        mc.setScreen(new CinematicScreen());
    }
    private static void say(String msg){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=null)mc.player.displayClientMessage(Component.literal("§6[PORT CINEMA] §f"+msg),false);
    }
}
