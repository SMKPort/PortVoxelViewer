package dev.port.armourbend.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.port.armourbend.BendContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingRendererMixin {
    private static final String RENDER = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";

    @Inject(method = RENDER, at = @At("HEAD"))
    private void portarmourbend$begin(LivingEntity entity, float yaw, float partialTicks, PoseStack poseStack,
                                     MultiBufferSource buffers, int light, CallbackInfo ci) {
        BendContext.enter(entity);
    }

    @Inject(method = RENDER, at = @At("RETURN"))
    private void portarmourbend$end(LivingEntity entity, float yaw, float partialTicks, PoseStack poseStack,
                                   MultiBufferSource buffers, int light, CallbackInfo ci) {
        BendContext.exit();
    }
}
