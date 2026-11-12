package com.port.metamorph;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client-side cosmetic accessories. No server authority or gameplay effects.
 */
@Mod(value=PortMetamorph.MOD_ID,dist=Dist.CLIENT)
public final class PortMetamorph {
    public static final String MOD_ID="portmetamorph";

    public PortMetamorph(){
        MorphState.load();
        RemoteBridge.start();
        NeoForge.EVENT_BUS.addListener(this::register);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::render);
    }

    private void render(RenderPlayerEvent.Post event){MorphRenderer.render(event);}
    private void tick(ClientTickEvent.Post event){MorphState.tick();}

    private void register(RegisterClientCommandsEvent event){
        event.getDispatcher().register(Commands.literal("metamorph")
            .executes(ctx->{open();return 1;})
            .then(Commands.literal("open").executes(ctx->{open();return 1;}))
            .then(Commands.literal("on").executes(ctx->{
                MorphState.setEnabled(true);say("Transformação ativada.");return 1;
            }))
            .then(Commands.literal("off").executes(ctx->{
                MorphState.setEnabled(false);say("Transformação desativada.");return 1;
            }))
            .then(Commands.literal("form")
                .then(Commands.literal("normal").executes(ctx->{preset("normal");return 1;}))
                .then(Commands.literal("nexus").executes(ctx->{preset("nexus");return 1;}))
                .then(Commands.literal("angel").executes(ctx->{preset("angel");return 1;}))
                .then(Commands.literal("hybrid").executes(ctx->{preset("hybrid");return 1;}))
                .then(Commands.literal("custom").executes(ctx->{preset("custom");return 1;})))
            .then(Commands.literal("pair").executes(ctx->{
                RemoteBridge.pair();say("Solicitação enviada ao painel. Confira o código no chat.");
                return 1;
            }))
            .then(Commands.literal("unpair").executes(ctx->{RemoteBridge.disconnect();return 1;}))
            .then(Commands.literal("web").executes(ctx->{
                say("Painel: "+RemoteBridge.WEB_URL);return 1;
            }))
            .then(Commands.literal("reset").executes(ctx->{
                MorphState.reset();say("Aparência padrão restaurada.");return 1;
            }))
            .then(Commands.literal("status").executes(ctx->{
                var s=MorphState.current();
                say("Forma "+s.name+" | "+(s.enabled?"ativa":"inativa")+
                    " | "+RemoteBridge.status());
                return 1;
            }))
        );
    }
    private static void preset(String name){
        MorphState.applyPreset(name);say("Forma selecionada: "+MorphState.current().name);
    }
    private static void open(){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=null)mc.setScreen(new MetamorphScreen());
    }
    public static void say(String message){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=null)mc.player.displayClientMessage(
            Component.literal("§6[PORT METAMORPH] §f"+message),false);
    }
}
