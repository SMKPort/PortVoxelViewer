package dev.port.armourbend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BendConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("portarmourbend.json");

    public static float multiplier = 1.0F;
    public static boolean nativeHands = true;
    public static boolean namedChildren = true;

    private BendConfig() {}

    public static void load() {
        try {
            if (!Files.exists(FILE)) {
                Files.createDirectories(FILE.getParent());
                Files.writeString(FILE, GSON.toJson(new Values()), StandardCharsets.UTF_8);
            }
            Values values = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), Values.class);
            if (values == null) return;
            if (Float.isFinite(values.bendMultiplier) && Math.abs(values.bendMultiplier) <= 3.0F) {
                multiplier = values.bendMultiplier;
            }
            nativeHands = values.enableNativeHandParts;
            namedChildren = values.enableNamedForearmChildren;
        } catch (IOException | RuntimeException ex) {
            System.err.println("[PortArmourBend] Unable to load config: " + ex.getMessage());
        }
    }

    public static final class Values {
        public float bendMultiplier = 1.0F;
        public boolean enableNativeHandParts = true;
        public boolean enableNamedForearmChildren = true;
    }
}
