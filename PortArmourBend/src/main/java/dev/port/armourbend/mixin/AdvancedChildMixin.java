package dev.port.armourbend.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.kosmx.playerAnim.core.util.Pair;
import dev.port.armourbend.ArmSide;
import dev.port.armourbend.BendConfig;
import dev.port.armourbend.BendContext;
import dev.port.armourbend.BendMath;
import moe.plushie.armourers_workshop.api.core.math.IPoseStack;
import moe.plushie.armourers_workshop.core.client.bake.BakedSkinPart;
import moe.plushie.armourers_workshop.core.client.skinrender.SkinRenderer;
import moe.plushie.armourers_workshop.core.skin.part.SkinPartTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SkinRenderer.class, remap = false)
public abstract class AdvancedChildMixin {
    @WrapOperation(
        method = "renderChild",
        at = @At(value = "INVOKE", target = "Lmoe/plushie/armourers_workshop/core/skin/part/SkinPartTransform;apply(Lmoe/plushie/armourers_workshop/api/core/math/IPoseStack;)V", remap = false),
        remap = false,
        require = 0
    )
    private static void portarmourbend$namedForearm(SkinPartTransform transform, IPoseStack pose,
                                                     Operation<Void> original,
                                                     @Local(name = "part") BakedSkinPart part) {
        original.call(transform, pose);
        if (!BendConfig.namedChildren) return;
        ArmSide side = ArmSide.fromChildName(part.name());
        if (side == null) return;
        Pair<Float, Float> bend = BendContext.activeBend(side);
        BendMath.apply(pose, bend);
    }
}
