package com.port.performance;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value=PortPerformanceMod.MOD_ID,dist=Dist.CLIENT)
public final class PortPerformanceMod {
    public static final String MOD_ID="portperf";

    public PortPerformanceMod(){
        PerformanceConfig.load();
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::renderHud);
        NeoForge.EVENT_BUS.addListener(this::commands);
    }

    private void tick(ClientTickEvent.Post event){PerformanceStats.tick();}

    private void renderHud(RenderGuiEvent.Post event){
        Minecraft mc=Minecraft.getInstance();
        if(!PerformanceConfig.hudEnabled||mc.player==null)return;
        var graphics=event.getGuiGraphics();
        int x=7,y=8,w=181,h=69;
        graphics.fill(x,y,x+w,y+h,0xD20F0C14);
        graphics.fill(x,y,x+w,y+2,0xFFE0B568);
        graphics.drawString(mc.font,"PORT  /  PERFORMANCE",x+9,y+8,0xFFEAC082,true);
        graphics.drawString(mc.font,"FPS: "+PerformanceStats.fps(),x+9,y+24,0xFFFAF7F1,true);
        graphics.drawString(mc.font,"PING: "+(PerformanceStats.ping()<0?"N/D":PerformanceStats.ping()+" ms"),x+92,y+24,0xFFFAF7F1,true);
        graphics.drawString(mc.font,"RAM: "+PerformanceStats.used()+" / "+PerformanceStats.max()+" MB",x+9,y+39,0xFFC6B5C1,true);
        graphics.drawString(mc.font,PerformanceConfig.streamEnabled?"WEB: TRANSMITINDO":"WEB: OFF",x+9,y+53,
          PerformanceConfig.streamEnabled?0xFF8BDDAF:0xFFBCA3A8,true);
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
                local("Site: "+PerformanceConfig.url+" | "+PerformanceStats.lastResult());
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
                    local("Transmissão HTTPS DESATIVADA. Nenhuma nova amostra será enviada.");
                    return 1;
                })))
            .then(Commands.literal("hud")
                .then(Commands.literal("on").executes(ctx->{
                    PerformanceConfig.hudEnabled=true;PerformanceConfig.save();
                    local("HUD ativado.");return 1;
                }))
                .then(Commands.literal("off").executes(ctx->{
                    PerformanceConfig.hudEnabled=false;PerformanceConfig.save();
                    local("HUD desativado.");return 1;
                })))
            .then(Commands.literal("resetkey").executes(ctx->{
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
