package dev.port.clientbooster;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.neoforged.fml.ModList;

/** Detecta pelo nome apresentado, sem depender de classes internas de outros mods. */
public final class ModEnvironmentSummary {
    private ModEnvironmentSummary() {}

    public static String describe() {
        List<String> names = ModList.get().getMods().stream()
            .map(info -> info.getDisplayName().toLowerCase(Locale.ROOT))
            .toList();
        List<String> found = new ArrayList<>();
        if (contains(names, "sodium")) found.add("Sodium");
        if (contains(names, "iris shaders")) found.add("Iris");
        if (contains(names, "entity culling")) found.add("Entity Culling");
        if (contains(names, "immediatelyfast")) found.add("ImmediatelyFast");
        if (contains(names, "dynamic fps")) found.add("Dynamic FPS");
        if (contains(names, "modernfix")) found.add("ModernFix");
        if (contains(names, "ferritecore")) found.add("FerriteCore");
        if (contains(names, "alltheleaks")) found.add("AllTheLeaks");
        if (contains(names, "armourer")) found.add("Armourer's Workshop");
        if (contains(names, "emotecraft")) found.add("Emotecraft");
        if (contains(names, "geckolib")) found.add("GeckoLib");
        if (contains(names, "playeranimator")) found.add("playerAnimator");
        return found.isEmpty() ? "Nenhum alvo conhecido identificado" : String.join(", ", found);
    }

    private static boolean contains(List<String> names, String needle) {
        return names.stream().anyMatch(name -> name.contains(needle));
    }
}
