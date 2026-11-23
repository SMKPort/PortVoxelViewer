package com.port.performance;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value=PortPerformanceMod.MOD_ID,dist=Dist.CLIENT)
public final class PortPerformanceMod {
    public static final String MOD_ID="portperf";

    public PortPerformanceMod(){
        PerformanceConfig.load();
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::frame);
        NeoForge.EVENT_BUS.addListener(this::renderHud);
        NeoForge.EVENT_BUS.addListener(this::commands);
    }

    private void tick(ClientTickEvent.Post event){PerformanceStats.tick();}
    private void frame(RenderFrameEvent.Post event){BoosterEngine.onFrame();}

    private void renderHud(RenderGuiEvent.Post event){
        Minecraft mc=Minecraft.getInstance();
        if(!PerformanceConfig.hudEnabled||mc.player==null||mc.options.hideGui
            ||mc.screen instanceof HudPositionScreen)return;
        PerformanceHud.draw(event.getGuiGraphics(),mc,false);
    }

    private static void openHudEditor(){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null){
            local("Entre em um mundo para editar o HUD.");
            return;
        }
        mc.setScreen(new HudPositionScreen());
    }

    private void commands(RegisterClientCommandsEvent event){
        event.getDispatcher().register(Commands.literal("portperf")
            .then(Commands.literal("key").executes(ctx->{
                local("Chave do seu monitor (não compartilhe): "+PerformanceConfig.token);
                local("Cole os 32 caracteres no site "+PerformanceConfig.url);
                return 1;
            }))
            .then(Commands.literal("status").executes(ctx->{
                local("HUD: "+(PerformanceConfig.hudEnabled?"ON":"OFF")+
                    " | Transmissão: "+(PerformanceConfig.streamEnabled?"ON":"OFF"));
                local("FPS: "+PerformanceStats.fps()+" | Ping: "+PerformanceStats.ping()+
                    " ms | Heap usada: "+PerformanceStats.used()+" MB");
                local("CPU JVM: "+Math.round(HardwareMonitor.processCpuPercent())+
                    "% (de "+HardwareMonitor.logicalProcessors()+" processadores lógicos), thread cliente: "+
                    Math.round(HardwareMonitor.clientCpuPercent())+"%. RAM livre: "+
                    HardwareMonitor.freeSystemMb()+" MB.");
                local("Site: "+PerformanceConfig.url+" | "+PerformanceStats.lastResult());
                local("BOOSTER: "+BoosterEngine.status());
                local("Controle web: "+(PerformanceConfig.webControlEnabled?"autorizado":"bloqueado"));
                local("HUD posição: "+Math.round(PerformanceConfig.hudX*100)+"% / "+
                    Math.round(PerformanceConfig.hudY*100)+"%, escala "+Math.round(PerformanceConfig.hudScale*100)+"%. Use /portperf hud edit.");
                return 1;
            }))
            .then(Commands.literal("hardware").executes(ctx->{
                local("PORT PERFORMANCE 4.0 | Perfil "+PerformanceConfig.boosterProfile+
                    " | "+HardwareMonitor.logicalProcessors()+" processadores lógicos.");
                local("CPU do processo: "+Math.round(HardwareMonitor.processCpuPercent())+
                    "% do total | Thread cliente: "+Math.round(HardwareMonitor.clientCpuPercent())+"% de um lógico.");
                local("RAM física disponível: "+HardwareMonitor.freeSystemMb()+" / "+
                    HardwareMonitor.totalSystemMb()+" MB | Indicador: "+HardwareMonitor.hint());
                local("Sem controle de afinidade P/E, temperatura ou medição de GPU no Java.");
                return 1;
            }))
            .then(Commands.literal("stream")
                .then(Commands.literal("on").executes(ctx->{
                    PerformanceConfig.streamEnabled=true;
                    local("Transmissão HTTPS ATIVADA. Você autorizou o envio das métricas a "+PerformanceConfig.url);
                    return 1;
                }))
                .then(Commands.literal("off").executes(ctx->{
                    PerformanceConfig.streamEnabled=false;
                    PerformanceConfig.webControlEnabled=false;
                    local("Transmissão HTTPS DESATIVADA. Controle remoto também bloqueado.");
                    return 1;
                })))
            .then(Commands.literal("hud")
                .executes(ctx->{openHudEditor();return 1;})
                .then(Commands.literal("edit").executes(ctx->{openHudEditor();return 1;}))
                .then(Commands.literal("reset").executes(ctx->{
                    PerformanceHud.reset();local("Posição e tamanho do HUD restaurados.");return 1;
                }))
                .then(Commands.literal("scale")
                    .then(Commands.argument("percent",IntegerArgumentType.integer(55,225)).executes(ctx->{
                        PerformanceHud.setScale(IntegerArgumentType.getInteger(ctx,"percent")/100f);
                        local("Tamanho do HUD: "+Math.round(PerformanceConfig.hudScale*100)+"%.");
                        return 1;
                    })))
                .then(Commands.literal("on").executes(ctx->{
                    PerformanceConfig.hudEnabled=true;PerformanceConfig.save();
                    local("HUD ativado.");return 1;
                }))
                .then(Commands.literal("off").executes(ctx->{
                    PerformanceConfig.hudEnabled=false;PerformanceConfig.save();
                    local("HUD desativado.");return 1;
                })))
            .then(Commands.literal("boost")
                .then(Commands.literal("status").executes(ctx->{local(BoosterEngine.status());return 1;}))
                .then(Commands.literal("observe").executes(ctx->{
                    BoosterEngine.observe();local(BoosterEngine.status());return 1;
                }))
                .then(Commands.literal("auto").executes(ctx->{
                    BoosterEngine.adaptive();local("Booster contínuo ativado, perfil " + PerformanceConfig.boosterProfile + " e mudanças reversíveis.");
                    return 1;
                }))
                .then(Commands.literal("off").executes(ctx->{
                    BoosterEngine.disable();local(BoosterEngine.status());return 1;
                }))
                .then(Commands.literal("reset").executes(ctx->{
                    BoosterEngine.reset();local(BoosterEngine.status());return 1;
                }))
                .then(Commands.literal("profile")
                    .then(Commands.literal("igpu").executes(ctx->{
                        BoosterEngine.profile("igpu");
                        local("Perfil gráfico integrado aplicado. Chunks e blend adaptativos.");
                        return 1;
                    }))
                    .then(Commands.literal("balanced").executes(ctx->{
                        BoosterEngine.profile("balanced");
                        local("Perfil conservador aplicado. Apenas chunks adaptativos.");
                        return 1;
                    })))
                .then(Commands.literal("blend")
                    .then(Commands.literal("on").executes(ctx->{
                        BoosterEngine.allowBiomeBlend(true);local("Blend de biomas adaptativo permitido.");
                        return 1;
                    }))
                    .then(Commands.literal("off").executes(ctx->{
                        BoosterEngine.allowBiomeBlend(false);local("Blend de biomas preservado.");
                        return 1;
                    })))
                .then(Commands.literal("clouds")
                    .then(Commands.literal("on").executes(ctx->{
                        BoosterEngine.allowClouds(true);
                        local("Nuvens adaptativas permitidas: aparência do céu pode mudar.");
                        return 1;
                    }))
                    .then(Commands.literal("off").executes(ctx->{
                        BoosterEngine.allowClouds(false);local("Qualidade das nuvens preservada.");
                        return 1;
                    })))
                .then(Commands.literal("chunks")
                    .then(Commands.literal("on").executes(ctx->{
                        BoosterEngine.allowChunks(true);local("Ajuste automático de chunks ativado.");return 1;
                    }))
                    .then(Commands.literal("off").executes(ctx->{
                        BoosterEngine.allowChunks(false);local("Ajuste automático de chunks desativado.");return 1;
                    })))
                .then(Commands.literal("entities")
                    .then(Commands.literal("on").executes(ctx->{
                        BoosterEngine.allowEntities(true);local("Ajuste de distância de entidades ativado. Modelos distantes podem sumir.");return 1;
                    }))
                    .then(Commands.literal("off").executes(ctx->{
                        BoosterEngine.allowEntities(false);local("Distância de entidades preservada.");return 1;
                    })))
                .then(Commands.literal("target")
                    .then(Commands.argument("fps",IntegerArgumentType.integer(30,240)).executes(ctx->{
                        BoosterEngine.setTarget(IntegerArgumentType.getInteger(ctx,"fps"));
                        local("Meta do booster: "+PerformanceConfig.boosterTargetFps+" FPS.");
                        return 1;
                    }))))
            .then(Commands.literal("webcontrol")
                .then(Commands.literal("on").executes(ctx->{
                    PerformanceConfig.webControlEnabled=true;
                    local("Controle remoto do booster autorizado nesta sessão."+
                      (PerformanceConfig.streamEnabled?"":" Ative /portperf stream on para comunicar com o site."));
                    return 1;
                }))
                .then(Commands.literal("off").executes(ctx->{
                    PerformanceConfig.webControlEnabled=false;
                    local("Controle remoto do booster bloqueado.");return 1;
                })))
            .then(Commands.literal("resetkey").executes(ctx->{
                PerformanceConfig.webControlEnabled=false;
                PerformanceConfig.regenerateKey();
                local("Chave regenerada. Transmissão desligada. Rode /portperf key para ver a nova chave.");
                return 1;
            }))
        );
    }

    private static void local(String message){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=null)mc.player.displayClientMessage(Component.literal("§6[PORT PERF] §f"+message),false);
    }
}
