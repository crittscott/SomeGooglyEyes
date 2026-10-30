package com.github.crittscott.somegoogly.client.mixin;

import com.github.crittscott.somegoogly.client.render.SkullEyeRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * A head item draws through a special model renderer that never sees its stack, so the stack an item render
 * state's layers come from is kept on the state ({@link ItemStackRenderStateMixin}) for {@link SkullEyeRenderer}.
 */
@Mixin(ItemModelResolver.class)
abstract class ItemModelResolverMixin {

    @Inject(method = "appendItemLayers", at = @At("HEAD"))
    private void somegoogly$recordItem(ItemStackRenderState renderState, ItemStack stack,
                                       ItemDisplayContext displayContext, @Nullable Level level,
                                       @Nullable LivingEntity entity, int seed, CallbackInfo callback) {
        ((SkullEyeRenderer.ItemHolder) renderState).somegoogly$setItem(stack);
    }
}
