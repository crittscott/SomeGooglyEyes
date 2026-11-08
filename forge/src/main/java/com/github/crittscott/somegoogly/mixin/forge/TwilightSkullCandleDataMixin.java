package com.github.crittscott.somegoogly.mixin.forge;

import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds a skull candle's eyes, and the owner profile Twilight Forest's own update tag leaves out, to the tag that
 * syncs the block to clients. Twilight Forest is an optional mod with no API for this, and its skull candle
 * builds its own update tag, which leaves out the eye data {@code SkullBlockEntityDataMixin} saves.
 */
@Pseudo
@Mixin(targets = "twilightforest.block.entity.SkullCandleBlockEntity", remap = false)
abstract class TwilightSkullCandleDataMixin {
    // Twilight Forest overrides this vanilla method in both named and Forge runtime mappings.
    @Inject(method = {"getUpdateTag", "m_5995_"}, at = @At("RETURN"))
    private void somegoogly$syncEyes(CallbackInfoReturnable<CompoundTag> callback) {
        SkullBlockEntity skull = (SkullBlockEntity) (Object) this;
        CompoundTag tag = callback.getReturnValue();
        ((SkullEyeState.Holder) skull).somegoogly$saveEyes(tag);
        var owner = skull.getOwnerProfile();
        if (owner != null) {
            tag.put("SkullOwner", NbtUtils.writeGameProfile(new CompoundTag(), owner));
        }
    }
}
