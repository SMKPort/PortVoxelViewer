package dev.port.clientbooster;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Configuracao local (.minecraft/config/portclientbooster-client.toml). */
public final class BoosterConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue SHOW_HUD;
    public static final ModConfigSpec.BooleanValue ADAPT_ENTITIES;
    public static final ModConfigSpec.BooleanValue ADAPT_PARTICLES;
    public static final ModConfigSpec.IntValue TARGET_FPS;
    public static final ModConfigSpec.BooleanValue LOG_DIAGNOSTICS;
    public static final ModConfigSpec.DoubleValue MIN_ENTITY_SCALE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("port_client_booster");
        ENABLED = b.comment("Habilita o gerenciamento adaptativo do cliente.")
            .define("enabled", true);
        SHOW_HUD = b.comment("Exibe FPS aproximado e distancia relativa das entidades no canto superior.")
            .define("show_hud", true);
        ADAPT_ENTITIES = b.comment("Ajusta somente a distancia de renderizacao de entidades. Pode ocultar modelos distantes.")
            .define("adapt_entities", false);
        ADAPT_PARTICLES = b.comment("Reduz particulas cosmeticas em cenas pesadas. DESLIGADO para preservar efeitos de magia.")
            .define("adapt_particles", false);
        TARGET_FPS = b.comment("Meta para a media de FPS. Ajuste entre 30 e 240.")
            .defineInRange("target_fps", 60, 30, 240);
        MIN_ENTITY_SCALE = b.comment("Menor escala relativa permitida na distancia de entidades, 0.5 a 1.0.")
            .defineInRange("min_entity_scale", 0.85D, 0.5D, 1.0D);
        LOG_DIAGNOSTICS = b.comment("Loga amostras de FPS e frametime a cada janela de avaliacao.")
            .define("log_diagnostics", false);
        b.pop();
        SPEC = b.build();
    }
    private BoosterConfig() {}
}
