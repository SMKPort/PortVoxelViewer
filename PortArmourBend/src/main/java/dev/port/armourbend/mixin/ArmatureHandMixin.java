package dev.port.armourbend.mixin;

import dev.kosmx.playerAnim.core.util.Pair;
import dev.port.armourbend.ArmSide;
import dev.port.armourbend.BendConfig;
import dev.port.armourbend.BendContext;
import dev.port.armourbend.BendMath;
import moe.plushie.armourers_workshop.api.armature.IJointTransform;
import moe.plushie.armourers_workshop.core.client.bake.BakedArmature;
import moe.plushie.armourers_workshop.core.client.bake.BakedSkinPart;
import moe.plushie.armourers_workshop.core.skin.part.SkinPartTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BakedArmature.class, remap = false)
public abstract class ArmatureHandMixin {
    @Inject(method = "transformByPart", at = @At("RETURN"), cancellable = true, remap = false)
    private void portarmourbend$bendNativeHand(BakedSkinPart part, CallbackInfoReturnable<IJointTransform> cir) {
        if (!BendConfig.nativeHands) return;
        ArmSide side;
        if (part.type() == SkinPartTypes.BIPPED_RIGHT_HAND) side = ArmSide.RIGHT;
        else if (part.type() == SkinPartTypes.BIPPED_LEFT_HAND) side = ArmSide.LEFT;
        else return;

        IJointTransform original = cir.getReturnValue();
        if (original == null) return;
        Pair<Float, Float> bend = BendContext.activeBend(side);
        if (bend == null) return;
        cir.setReturnValue(pose -> {
            original.apply(pose);
            BendMath.apply(pose, bend);
        });
    }
}
