package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.config.ClientConfig;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/**
 * Per-frame eye decision for one living entity, taken while its render state is extracted and read back
 * by the eye layers, which see only the render state. Either the entity is the picker's target (only the
 * picker preview draws), or {@code helper} and {@code tracker} are both set (normal eyes draw), or
 * neither (nothing draws). {@code partialTick} is the frame's interpolation for the pupil motion.
 *
 * <p>{@link #extract} is the single "should this mob show eyes, and with what geometry" gate, shared by
 * the vanilla {@link LayerGooglyEyes} and the GeckoLib {@code GooglyGeoLayer}.
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
        HeadInfo helper = helperToRender(living);
        if (helper == null) {
            return NONE;
        }
        GooglyTracker tracker = ClientEyeRuntime.get(living, helper);
        tracker.markRendered(ClientEyeRuntime.clientTicks());
        return new EyeRenderData(false, helper, tracker, partialTick);
    }

    /**
     * The eye geometry to draw for {@code living}, or {@code null} to draw nothing: honors the client
     * global/per-entity disables, the server's {@code googlyEyesEnabled} master switch and per-mob
     * has-eyes decision (both bypassed while the picker is active, since it is an admin authoring tool
     * and not gameplay — it shows every configured mob regardless of the server switch or that mob's own
     * roll), invisibility, and a usable config.
     */
    @Nullable
    private static HeadInfo helperToRender(LivingEntity living) {
        ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        if (ClientConfig.DISABLE_GOOGLY_EYES.get() || ClientConfig.isEntityDisabled(entityType)) {
            return null;
        }
        if (!PickerState.isActive() && (!ClientEyeConfigs.googlyEyesEnabled() || !EyeState.hasEyes(living))) {
            return null;
        }
        if (living.isInvisible()) {
            return null;
        }
        HeadInfo helper = ClientEyeConfigs.resolve(entityType, living, EyeState.getVariantRoll(living));
        return helper.hasConfig() ? helper : null;
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
