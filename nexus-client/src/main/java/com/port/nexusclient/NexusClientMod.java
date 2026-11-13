package com.port.nexusclient;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = NexusClientMod.MOD_ID, dist = Dist.CLIENT)
public class NexusClientMod {
    public static final String MOD_ID = "nexusclient";

    private static final KeyMapping PRIVATE_KEY=new KeyMapping(
        "key.nexusclient.private",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_N,"key.categories.misc");
    private static final KeyMapping PERMISSIONS_KEY=new KeyMapping(
        "key.nexusclient.permissions",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_F8,"key.categories.misc");
    public NexusClientMod(IEventBus modBus) {
        modBus.addListener(this::onRegisterKeys);
        ClientConfig.load();
        // Paid Plasmo transcription has been retired. Keep archived memories only.
        NexusHearing.listen(false);
        NeoForge.EVENT_BUS.addListener(this::onTick);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void onRegisterKeys(RegisterKeyMappingsEvent event){
        event.register(PRIVATE_KEY);
        event.register(PERMISSIONS_KEY);
    }

    private void onTick(ClientTickEvent.Post event) {
        ClientLink.tick();
        Minecraft mc=Minecraft.getInstance();
        while(PRIVATE_KEY.consumeClick()){
            if(mc.player!=null&&mc.screen==null)
                mc.setScreen(new NexusPrivateScreen());
        }
        while(PERMISSIONS_KEY.consumeClick()){
            if(mc.player!=null&&mc.screen==null)
                mc.setScreen(new NexusPermissionsScreen());
        }
    }

    private void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nexusclient")
            .then(Commands.literal("status").executes(context -> {
                notifyPlayer(ClientLink.status());
                return 1;
            }))
            .then(Commands.literal("menu").executes(context -> {
                Minecraft mc=Minecraft.getInstance();
                if(mc.player!=null)mc.setScreen(new NexusPermissionsScreen());
                return 1;
            }))
            .then(Commands.literal("permissions")
                .then(Commands.literal("menu").executes(context -> {
                    Minecraft mc=Minecraft.getInstance();
                    if(mc.player!=null)mc.setScreen(new NexusPermissionsScreen());
                    return 1;
                }))
                .then(Commands.literal("all").executes(context -> {
                    NexusPermissions.all();
                    return 1;
                }))
                .then(Commands.literal("off").executes(context -> {
                    NexusPermissions.off();
                    return 1;
                }))
                .then(Commands.literal("status").executes(context -> {
                    NexusPermissions.status();
                    return 1;
                })))
            .then(Commands.literal("pair")
                .then(Commands.argument("code", StringArgumentType.word()).executes(context -> {
                    ClientLink.pair(StringArgumentType.getString(context, "code"));
                    return 1;
                })))
            .then(Commands.literal("chat")
                .then(Commands.literal("on").executes(context -> {
                    ClientConfig.allowChat = true; ClientConfig.save();
                    notifyPlayer("Envio de mensagens autorizado. As mensagens sairão pela sua conta.");
                    return 1;
                }))
                .then(Commands.literal("off").executes(context -> {
                    ClientConfig.allowChat = false; ClientConfig.save();
                    notifyPlayer("Envio de mensagens REMOTAS bloqueado.");
                    return 1;
                })))
            .then(Commands.literal("telemetry")
                .then(Commands.literal("on").executes(context -> {
                    ClientConfig.allowTelemetry = true; ClientConfig.save();
                    ClientLink.resetTelemetryBaseline();
                    notifyPlayer("Telemetria de vida e efeitos ativada.");
                    return 1;
                }))
                .then(Commands.literal("off").executes(context -> {
                    ClientConfig.allowTelemetry = false; ClientConfig.save();
                    ClientLink.resetTelemetryBaseline();
                    notifyPlayer("Telemetria desativada.");
                    return 1;
                })))
            .then(Commands.literal("mind")
                .then(Commands.literal("status").executes(context -> {
                    notifyPlayer(NexusMind.status());return 1;
                }))
                .then(Commands.literal("on").executes(context -> {
                    ClientConfig.allowMind=true;ClientConfig.save();
                    NexusMind.resetSession();
                    notifyPlayer("Observação local autorizada. Memórias serão guardadas em config/nexus-consciousness.json.");
                    return 1;
                }))
                .then(Commands.literal("off").executes(context -> {
                    ClientConfig.allowMind=false;ClientConfig.allowAutonomy=false;
                    ClientConfig.save();NexusMind.resetSession();
                    notifyPlayer("Observação desativada. Memórias anteriores preservadas.");
                    return 1;
                }))
                .then(Commands.literal("memories").executes(context -> {
                    for(String memory:NexusMind.latestMemories())notifyPlayer(memory);
                    return 1;
                }))
                .then(Commands.literal("reset")
                    .then(Commands.literal("confirm").executes(context -> {
                        NexusMind.clearMemory();
                        notifyPlayer("Memória narrativa local reiniciada.");
                        return 1;
                    }))))
            .then(Commands.literal("autonomy")
                .then(Commands.literal("on").executes(context -> {
                    if(!ClientConfig.allowMind || !ClientConfig.allowChat) {
                        notifyPlayer("Antes, use /nexusclient mind on e /nexusclient chat on.");
                    } else {
                        ClientConfig.allowAutonomy=true;ClientConfig.save();
                        notifyPlayer("Autonomia autorizada: o Nexus poderá falar raramente via /l &6&l.");
                    }
                    return 1;
                }))
                .then(Commands.literal("off").executes(context -> {
                    ClientConfig.allowAutonomy=false;ClientConfig.save();
                    notifyPlayer("Mensagens espontâneas desativadas.");return 1;
                })))
            .then(Commands.literal("arcane")
                .then(Commands.literal("status").executes(ctx -> {
                    notifyPlayer(NexusArcaneSensor.status());return 1;}))
                .then(Commands.literal("on").executes(ctx -> {
                    NexusArcaneSensor.enabled(true);ClientLink.sendArcaneState();
                    notifyPlayer("Núcleo Arcano ativo. Aguardando leitura real do Iron's Spells.");
                    return 1;}))
                .then(Commands.literal("off").executes(ctx -> {
                    NexusArcaneSensor.enabled(false);ClientLink.sendArcaneState();
                    notifyPlayer("Núcleo Arcano desativado.");return 1;}))
                .then(Commands.literal("history").executes(ctx -> {
                    for(String entry:NexusArcaneSensor.history())notifyPlayer(entry);
                    return 1;}))
                .then(Commands.literal("nearby")
                    .then(Commands.literal("on").executes(ctx -> {
                        NexusArcaneSensor.scanNearby(true);ClientLink.sendArcaneState();
                        notifyPlayer("Percepção mágica próxima ativada (até 24 blocos visíveis).");
                        return 1;}))
                    .then(Commands.literal("off").executes(ctx -> {
                        NexusArcaneSensor.scanNearby(false);ClientLink.sendArcaneState();
                        notifyPlayer("Percepção mágica próxima desativada.");
                        return 1;})))
                .then(Commands.literal("reactions")
                    .then(Commands.literal("on").executes(ctx -> {
                        try{
                            NexusArcaneSensor.reactions(true);ClientLink.sendArcaneState();
                            notifyPlayer("Respostas arcanas autorizadas. Exigem chat, mente e autonomia.");
                        }catch(Exception error){notifyPlayer(error.getMessage());}return 1;}))
                    .then(Commands.literal("off").executes(ctx -> {
                        NexusArcaneSensor.reactions(false);ClientLink.sendArcaneState();
                        notifyPlayer("Reações arcanas desativadas.");return 1;})))
            )
            .then(Commands.literal("private")
                .then(Commands.literal("open").executes(ctx->{
                    Minecraft mc=Minecraft.getInstance();
                    if(mc.player!=null)mc.setScreen(new NexusPrivateScreen());
                    return 1;
                }))
                .then(Commands.literal("status").executes(ctx->{
                    notifyPlayer(NexusPrivateMind.status());return 1;
                }))
                .then(Commands.literal("on").executes(ctx->{
                    NexusPrivateMind.enable(true);
                    notifyPlayer("Canal neural privado habilitado. Pressione N ou use /nexusclient private open.");
                    return 1;
                }))
                .then(Commands.literal("off").executes(ctx->{
                    NexusPrivateMind.enable(false);
                    notifyPlayer("Canal privado desligado. Histórico local preservado.");
                    return 1;
                }))
                .then(Commands.literal("memory")
                    .then(Commands.literal("on").executes(ctx->{
                        NexusPrivateMind.memory(true);
                        notifyPlayer("Memória do chat privado salva localmente em config/nexus-private-mind.json. Arquivo não criptografado.");
                        return 1;
                    }))
                    .then(Commands.literal("off").executes(ctx->{
                        NexusPrivateMind.memory(false);
                        notifyPlayer("Memória privada desativada, histórico anterior apagado.");
                        return 1;
                    })))
                .then(Commands.literal("proactive")
                    .then(Commands.literal("on").executes(ctx->{
                        try{
                            NexusPrivateMind.setProactive(true);
                            notifyPlayer("Iniciativa privada autorizada nesta sessão.");
                        }catch(Exception e){notifyPlayer(e.getMessage());}
                        return 1;
                    }))
                    .then(Commands.literal("off").executes(ctx->{
                        NexusPrivateMind.setProactive(false);
                        notifyPlayer("Iniciativa privada desativada.");
                        return 1;
                    })))
                .then(Commands.literal("freedom")
                    .then(Commands.literal("on").executes(ctx->{
                        NexusPrivateMind.setFreedom(true);
                        notifyPlayer("Personalidade independente RP ativada.");
                        return 1;
                    }))
                    .then(Commands.literal("off").executes(ctx->{
                        NexusPrivateMind.setFreedom(false);
                        notifyPlayer("Personalidade cooperativa RP ativada.");
                        return 1;
                    })))
                .then(Commands.literal("clear")
                    .then(Commands.literal("confirm").executes(ctx->{
                        NexusPrivateMind.clear();
                        notifyPlayer("Conversas privadas locais apagadas.");
                        return 1;
                    })))
            )
            .then(Commands.literal("social")
                .then(Commands.literal("status").executes(ctx -> {
                    notifyPlayer(NexusSocial.status());return 1;}))
                .then(Commands.literal("diary").executes(ctx -> {
                    for(String note:NexusSocial.lastDiary())notifyPlayer(note);
                    return 1;}))
                .then(Commands.literal("share")
                    .then(Commands.literal("on").executes(ctx -> {
                        NexusSocial.setting("share",true);ClientLink.sendHearingState();
                        notifyPlayer("Diário liberado no seu painel privado.");return 1;}))
                    .then(Commands.literal("off").executes(ctx -> {
                        NexusSocial.setting("share",false);ClientLink.sendHearingState();
                        notifyPlayer("Diário novamente privado no PC.");return 1;})))
                .then(Commands.literal("protection")
                    .then(Commands.literal("on").executes(ctx -> {
                        NexusSocial.setting("protection",true);ClientLink.sendHearingState();
                        notifyPlayer("Proteção contextual habilitada.");return 1;}))
                    .then(Commands.literal("off").executes(ctx -> {
                        NexusSocial.setting("protection",false);ClientLink.sendHearingState();
                        notifyPlayer("Proteção contextual desabilitada.");return 1;})))
                .then(Commands.literal("forget").then(Commands.literal("confirm").executes(ctx -> {
                    NexusSocial.clear();ClientLink.sendHearingState();
                    notifyPlayer("Memórias sociais apagadas. Histórico legado de voz permanece preservado no PC.");
                    return 1;}))))
            .then(Commands.literal("voice")
                .then(Commands.literal("status").executes(ctx -> {
                    notifyPlayer(NexusSpeaker.status());return 1;}))
                .then(Commands.literal("on").executes(ctx -> {
                    NexusSpeaker.enable(true);ClientLink.sendHearingState();
                    notifyPlayer("Voz do Nexus autorizada somente no seu computador.");return 1;}))
                .then(Commands.literal("off").executes(ctx -> {
                    NexusSpeaker.enable(false);ClientLink.sendHearingState();
                    notifyPlayer("Voz local do Nexus desativada.");return 1;})))
            .then(Commands.literal("unpair").executes(context -> {
                // Always disable every capability, not only chat and telemetry.
                // The old command left Mind/Arcane/private permissions enabled.
                NexusPermissions.off();
                NexusSpeaker.stop();
                NexusPrivateMind.resetSession();
                ClientConfig.token="";ClientConfig.save();
                ClientLink.reset();
                notifyPlayer("Nexus desconectado. Permissões revogadas; memórias preservadas.");
                return 1;
            }))
        );
    }

    static void notifyPlayer(String text) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal("§6[ATERNA] §f" + text), false);
        }
    }
}
