package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/**
 * Per-frame eye decision for one living entity, taken while its render state is extracted and read back
 * by the eye layers, which see only the render state. Either the entity is the picker's target (only the
 * picker preview draws), or {@code helper} and {@code tracker} are both set (normal eyes draw), or
 * neither (nothing draws). {@code partialTick} is the frame's interpolation for the pupil motion.
 */
public record EyeRenderData(boolean pickerTarget, @Nullable HeadInfo helper, @Nullable GooglyTracker tracker,
                            float partialTick) {

    public static final EyeRenderData NONE = new EyeRenderData(false, null, null, 0.0F);
    private static final EyeRenderData PICKER_TARGET = new EyeRenderData(true, null, null, 0.0F);

    /** Take this frame's decision for {@code living}; marks its tracker rendered when eyes will draw. */
    public static EyeRenderData extract(LivingEntity living, float partialTick) {
        if (PickerState.isActiveTarget(living)) {
            return PICKER_TARGET;
        }
        HeadInfo helper = EyeRenderGating.helperToRender(living);
        if (helper == null) {
            return NONE;
        }
        GooglyTracker tracker = ClientEyeRuntime.get(living, helper);
        tracker.markRendered(ClientEyeRuntime.clientTicks());
        return new EyeRenderData(false, helper, tracker, partialTick);
    }

    /** The decision stored on {@code state} by the last extraction. */
    public static EyeRenderData of(LivingEntityRenderState state) {
        return ((Holder) state).somegoogly$eyeData();
    }

    /** Implemented on {@link LivingEntityRenderState} by the mod's render-state mixin. */
    public interface Holder {
        EyeRenderData somegoogly$eyeData();

        void somegoogly$setEyeData(EyeRenderData data);
    }
}
