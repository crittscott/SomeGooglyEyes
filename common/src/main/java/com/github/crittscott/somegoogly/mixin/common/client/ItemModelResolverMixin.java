package com.github.crittscott.somegoogly.mixin.common.client;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.PresentationEyes;
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
 * The renderers an item state draws through never see its stack, so the stack a state's layers come from and the
 * animation chosen for it are kept on the state ({@link ItemStackRenderStateMixin}) until the draw.
 */
@Mixin(ItemModelResolver.class)
abstract class ItemModelResolverMixin {

    @Inject(method = "appendItemLayers", at = @At("HEAD"))
    private void somegoogly$recordItem(ItemStackRenderState renderState, ItemStack stack,
                                       ItemDisplayContext displayContext, @Nullable Level level,
                                       @Nullable LivingEntity entity, int seed, CallbackInfo callback) {
        ((SkullEyeRenderer.ItemHolder) renderState).somegoogly$setItem(stack);
        ((PresentationEyes.Holder) renderState).somegoogly$setDraw(displayContext == ItemDisplayContext.GUI
                ? ClientEyeRuntime.itemDraw(stack) : ClientEyeRuntime.handContext(entity, stack, displayContext));
    }
}
