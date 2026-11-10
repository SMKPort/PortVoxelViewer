package dev.port.clientbooster;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/** Ponto de entrada exclusivamente do cliente. Sem registro de itens ou pacotes. */
@Mod(value = PortClientBooster.MOD_ID, dist = Dist.CLIENT)
public final class PortClientBooster {
    public static final String MOD_ID = "portclientbooster";

    public PortClientBooster(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, BoosterConfig.SPEC);
        NeoForge.EVENT_BUS.register(new AdaptivePerformanceController());
    }
}
