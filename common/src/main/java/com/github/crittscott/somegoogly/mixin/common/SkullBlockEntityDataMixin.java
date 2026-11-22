package com.github.crittscott.somegoogly.mixin.common;

import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives the head block entity its eye-state compound ({@link SkullEyeState.Holder}), saved under
 * {@link SkullEyeState#DATA_KEY} with its own data while it has eyes and replaced on every load, so the server's save, the
 * clients' update packets, and a placed head item all carry it. A load without the key clears it. The update tag
 * carries the compound even without eyes, so the client clears them.
 */
@Mixin(SkullBlockEntity.class)
abstract class SkullBlockEntityDataMixin implements SkullEyeState.Holder {

    @Unique
    @Nullable
    private CompoundTag somegoogly$data;

    @Override
    @Nullable
    public CompoundTag somegoogly$find() {
        return somegoogly$data;
    }

    @Override
    public CompoundTag somegoogly$get() {
        if (somegoogly$data == null) {
            somegoogly$data = new CompoundTag();
        }
        return somegoogly$data;
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void somegoogly$load(CompoundTag tag, CallbackInfo callback) {
        somegoogly$data = SkullEyeState.loadedData(tag);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void somegoogly$save(CompoundTag tag, CallbackInfo callback) {
        somegoogly$saveEyes(tag);
    }

    @Inject(method = "getUpdateTag", at = @At("RETURN"))
    private void somegoogly$updateTag(CallbackInfoReturnable<CompoundTag> callback) {
        somegoogly$syncEyes(callback.getReturnValue());
    }
}
