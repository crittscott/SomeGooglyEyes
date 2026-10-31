package com.github.crittscott.somegoogly.client.mixin;

import com.github.crittscott.somegoogly.client.render.SkullEyeRenderer;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.registry.ModContent;
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
 * The head's eye state also joins the state's model identity, which vanilla uses to reuse a drawn GUI item, so
 * an eyed head item and a plain one of the same head are not drawn as one.
 */
@Mixin(ItemModelResolver.class)
abstract class ItemModelResolverMixin {

    @Inject(method = "appendItemLayers", at = @At("HEAD"))
    private void somegoogly$recordItem(ItemStackRenderState renderState, ItemStack stack,
                                       ItemDisplayContext displayContext, @Nullable Level level,
                                       @Nullable LivingEntity entity, int seed, CallbackInfo callback) {
        ((SkullEyeRenderer.ItemHolder) renderState).somegoogly$setItem(stack);
        EyeState.Snapshot headEyes = stack.get(ModContent.HEAD_EYES.get());
        if (headEyes != null) {
            renderState.appendModelIdentityElement(headEyes);
        }
    }
}
