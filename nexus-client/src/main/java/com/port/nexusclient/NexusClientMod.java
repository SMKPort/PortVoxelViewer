package com.port.nexusclient;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = NexusClientMod.MOD_ID, dist = Dist.CLIENT)
public class NexusClientMod {
    public static final String MOD_ID = "nexusclient";

    public NexusClientMod() {
        ClientConfig.load();
        NeoForge.EVENT_BUS.addListener(this::onTick);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void onTick(ClientTickEvent.Post event) {
        ClientLink.tick();
    }

    private void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nexusclient")
            .then(Commands.literal("status").executes(context -> {
                notifyPlayer(ClientLink.status());
                return 1;
            }))
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
                    notifyPlayer("Telemetria de vida e efeitos ativada.");
                    return 1;
                }))
                .then(Commands.literal("off").executes(context -> {
                    ClientConfig.allowTelemetry = false; ClientConfig.save();
                    notifyPlayer("Telemetria desativada.");
                    return 1;
                })))
            .then(Commands.literal("unpair").executes(context -> {
                ClientConfig.token = "";ClientConfig.allowChat = false;
                ClientConfig.allowTelemetry = false;ClientConfig.save();
                ClientLink.reset();
                notifyPlayer("Nexus Client desconectado e permissões desativadas.");
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
