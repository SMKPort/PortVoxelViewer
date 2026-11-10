package dev.port.armourbend;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = PortArmourBend.MOD_ID, dist = Dist.CLIENT)
public final class PortArmourBend {
    public static final String MOD_ID = "portarmourbend";

    public PortArmourBend() {
        BendConfig.load();
    }
}
