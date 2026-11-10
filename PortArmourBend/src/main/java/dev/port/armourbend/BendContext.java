package dev.port.armourbend;

import dev.kosmx.playerAnim.core.util.Pair;
import dev.kosmx.playerAnim.impl.Helper;
import dev.kosmx.playerAnim.impl.IAnimatedPlayer;
import dev.kosmx.playerAnim.impl.animation.AnimationApplier;
import net.minecraft.world.entity.LivingEntity;
import java.util.ArrayDeque;
import java.util.Deque;

public final class BendContext {
    private static final ThreadLocal<Deque<LivingEntity>> RENDERING = ThreadLocal.withInitial(ArrayDeque::new);

    private BendContext() {}

    public static void enter(LivingEntity player) {
        RENDERING.get().push(player);
    }

    public static void exit() {
        Deque<LivingEntity> stack = RENDERING.get();
        if (!stack.isEmpty()) stack.pop();
        if (stack.isEmpty()) RENDERING.remove();
    }

    public static Pair<Float, Float> activeBend(ArmSide side) {
        if (!Helper.isBendEnabled()) return null;
        LivingEntity entity = RENDERING.get().peek();
        if (!(entity instanceof IAnimatedPlayer animated)) return null;
        AnimationApplier animation = animated.playerAnimator_getAnimation();
        if (animation == null || !animation.isActive()) return null;
        return animation.getBend(side == ArmSide.RIGHT ? "rightArm" : "leftArm");
    }
}
