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
import net.neoforged.fml.ModList;

@Mod(value = NexusClientMod.MOD_ID, dist = Dist.CLIENT)
public class NexusClientMod {
    public static final String MOD_ID = "nexusclient";

    public NexusClientMod() {
        ClientConfig.load();
        if(ModList.get().isLoaded("plasmovoice")){
            try{NexusPlasmoAddon.attach();}
            catch(RuntimeException|LinkageError error){
                System.err.println("Nexus Hearing: Plasmo Voice indisponível: "+error.getMessage());
            }
        }
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
            .then(Commands.literal("hearing")
                .then(Commands.literal("status").executes(ctx -> {notifyPlayer(NexusHearing.status());return 1;}))
                .then(Commands.literal("on").executes(ctx -> {try{NexusHearing.listen(true);ClientLink.sendHearingState();
                    notifyPlayer("Audição ATIVA para jogadores com consentimento na lista.");
                }catch(Exception e){notifyPlayer(e.getMessage());}return 1;}))
                .then(Commands.literal("off").executes(ctx -> {NexusHearing.listen(false);ClientLink.sendHearingState();
                    notifyPlayer("Audição DESATIVADA.");return 1;}))
                .then(Commands.literal("allow").then(Commands.argument("player",StringArgumentType.word())
                    .executes(ctx -> {try{
                        String name=StringArgumentType.getString(ctx,"player");
                        if("@a".equals(name)){
                            int online=NexusHearing.collectiveOn();
                            ClientLink.sendHearingState();
                            notifyPlayer("@a selecionado: "+online+
                                " jogador(es) online previamente autorizado(s). "+
                                "Nenhuma permissão nova foi concedida. Para começar: /nexusclient hearing on.");
                        }else{
                            NexusHearing.allow(name);ClientLink.sendHearingState();
                            notifyPlayer(name+" adicionado à lista após consentimento.");
                        }
                    }catch(Exception e){notifyPlayer(e.getMessage());}return 1;})))
                .then(Commands.literal("deny").then(Commands.argument("player",StringArgumentType.word())
                    .executes(ctx -> {try{
                        String name=StringArgumentType.getString(ctx,"player");
                        if("@a".equals(name)){
                            NexusHearing.collectiveOff();
                            notifyPlayer("Audição coletiva @a INTERROMPIDA. Autorizações individuais preservadas.");
                        }else{
                            NexusHearing.deny(name);
                            notifyPlayer(name+" removido, com exclusão de suas memórias.");
                        }
                        ClientLink.sendHearingState();
                    }catch(Exception error){notifyPlayer(error.getMessage());}return 1;})))
                .then(Commands.literal("memory")
                    .then(Commands.literal("on").executes(ctx -> {NexusHearing.remember(true);ClientLink.sendHearingState();
                        notifyPlayer("Memória textual de audição ATIVA.");return 1;}))
                    .then(Commands.literal("off").executes(ctx -> {NexusHearing.remember(false);ClientLink.sendHearingState();
                        notifyPlayer("Memória de audição DESATIVADA.");return 1;})))
                .then(Commands.literal("reply")
                    .then(Commands.literal("on").executes(ctx -> {NexusHearing.answer(true);ClientLink.sendHearingState();
                        notifyPlayer("Respostas por voz ATIVAS (por chat).");return 1;}))
                    .then(Commands.literal("off").executes(ctx -> {NexusHearing.answer(false);ClientLink.sendHearingState();
                        notifyPlayer("Respostas por voz DESATIVADAS.");return 1;})))
                .then(Commands.literal("memories").executes(ctx -> {for(String line:NexusHearing.recentMemories())
                    notifyPlayer(line);return 1;}))
                .then(Commands.literal("forget").then(Commands.literal("confirm").executes(ctx -> {
                    NexusHearing.clearMemory();ClientLink.sendHearingState();notifyPlayer("Todas as memórias textuais de voz apagadas.");return 1;}))))
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
                    notifyPlayer("Memórias derivadas apagadas. Para transcrições: hearing forget confirm.");
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
                NexusHearing.listen(false);
                NexusSpeaker.stop();
                ClientConfig.token = "";ClientConfig.allowChat = false;
                ClientConfig.allowTelemetry = false;ClientConfig.allowAutonomy=false;ClientConfig.save();
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
