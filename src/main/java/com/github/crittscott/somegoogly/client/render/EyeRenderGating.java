package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.config.ClientConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/**
 * The single "should this mob show eyes, and with what geometry" gate, shared by the vanilla
 * {@link LayerGooglyEyes} and the GeckoLib {@code GooglyGeoLayer} so the two can't drift apart on the
 * decision (they already share the drawing via {@link GooglyEyeRenderer}).
 *
 * <p>The picker-target case is <b>not</b> here: the two layers diverge on it (the vanilla layer suppresses
 * itself and lets {@code PickerLayer} draw the preview; the geo layer has no separate preview layer and
 * previews inline), so each handles that branch before calling this.
 */
public final class EyeRenderGating {

    private EyeRenderGating() {
    }

    /**
     * The eye geometry to draw for {@code living}, or {@code null} to draw nothing: honors the client
     * global/per-entity disables, the server's per-mob has-eyes decision (bypassed while the picker is
     * active, so authoring shows every configured mob), invisibility, and a usable config.
     */
    @Nullable
    public static HeadInfo helperToRender(LivingEntity living) {
        if (ClientConfig.DISABLE_GOOGLY_EYES.get()) {
            return null;
        }
        ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        if (ClientConfig.isEntityDisabled(entityType)) {
            return null;
        }
        if (!PickerState.active && !EyeState.hasEyes(living)) {
            return null;
        }
        if (living.isInvisible()) {
            return null;
        }
        HeadInfo helper = HeadInfo.getHelper(entityType, living);
        return helper.hasConfig() ? helper : null;
    }
}
