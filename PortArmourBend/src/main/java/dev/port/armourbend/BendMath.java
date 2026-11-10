package dev.port.armourbend;

import dev.kosmx.playerAnim.core.util.Pair;
import moe.plushie.armourers_workshop.api.core.math.IPoseStack;
import moe.plushie.armourers_workshop.core.math.OpenQuaternionf;
import moe.plushie.armourers_workshop.core.math.OpenVector3f;

public final class BendMath {
    private BendMath() {}

    public static void apply(IPoseStack pose, Pair<Float, Float> bend) {
        if (bend == null) return;
        float axisAngle = bend.getLeft();
        float rotation = bend.getRight() * BendConfig.multiplier;
        if (!Float.isFinite(axisAngle) || !Float.isFinite(rotation) || Math.abs(rotation) < 0.00001F) return;
        OpenVector3f axis = new OpenVector3f((float) Math.cos(axisAngle), 0.0F, (float) Math.sin(axisAngle));
        pose.rotate(new OpenQuaternionf(axis, rotation, false));
    }
}
