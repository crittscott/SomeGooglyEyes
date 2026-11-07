package com.github.crittscott.somegoogly.mixin.common.client;

import com.github.crittscott.somegoogly.client.render.GooglyEyeItemRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Captures the eye item's display context in its extracted, deferred render argument. */
@Mixin(SpecialModelWrapper.class)
abstract class SpecialModelWrapperMixin {

    @WrapOperation(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/special/SpecialModelRenderer;extractArgument(Lnet/minecraft/world/item/ItemStack;)Ljava/lang/Object;"))
    private Object somegoogly$extractEyeContext(SpecialModelRenderer<?> renderer, ItemStack stack,
                                               Operation<Object> original, ItemStackRenderState state,
                                               ItemStack source, ItemModelResolver resolver,
                                               ItemDisplayContext context, @Nullable ClientLevel level,
                                               @Nullable ItemOwner owner, int seed) {
        return renderer instanceof GooglyEyeItemRenderer eyes
                ? eyes.extractArgument(stack, context) : original.call(renderer, stack);
    }
}
