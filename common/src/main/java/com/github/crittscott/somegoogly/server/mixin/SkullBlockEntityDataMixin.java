package com.github.crittscott.somegoogly.server.mixin;

import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * Gives the head block entity its eye-state compound ({@link SkullEyeState.Holder}), saved under
 * {@link SkullEyeState#DATA_KEY} with its own data and replaced on every load, so the server's save and the
 * clients' update packets carry it; a load without the key clears it. The head's item form carries the same
 * state as the {@link ModContent#HEAD_EYES} component, taken and given here as an implicit component, the way
 * the head's own profile is.
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

    @Inject(method = "applyImplicitComponents", at = @At("TAIL"))
    private void somegoogly$applyComponents(DataComponentGetter input, CallbackInfo callback) {
        EyeState.Snapshot snapshot = input.get(ModContent.HEAD_EYES.get());
        if (snapshot == null) {
            somegoogly$data = null;
        } else {
            somegoogly$data = new CompoundTag();
            snapshot.write(somegoogly$data);
        }
    }

    @Inject(method = "collectImplicitComponents", at = @At("TAIL"))
    private void somegoogly$collectComponents(DataComponentMap.Builder components, CallbackInfo callback) {
        if (somegoogly$data != null) {
            EyeState.Snapshot snapshot = EyeState.Snapshot.read(somegoogly$data);
            if (snapshot.hasEyes()) {
                components.set(ModContent.HEAD_EYES.get(), snapshot);
            }
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void somegoogly$load(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo callback) {
        somegoogly$data = tag.getCompound(SkullEyeState.DATA_KEY).orElse(null);
    }

    @Inject(method = "removeComponentsFromTag", at = @At("TAIL"))
    private void somegoogly$removeComponents(CompoundTag tag, CallbackInfo callback) {
        tag.remove(SkullEyeState.DATA_KEY);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void somegoogly$save(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo callback) {
        if (somegoogly$data != null) {
            tag.put(SkullEyeState.DATA_KEY, somegoogly$data.copy());
        }
    }
}
