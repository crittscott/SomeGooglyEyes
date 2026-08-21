package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.config.ClientConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

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

    private static final int DEBUG_DECISION_LIMIT = 80;
    private static final Set<String> LOGGED_DEBUG_DECISIONS = new HashSet<>();

    private EyeRenderGating() {
    }

    /**
     * The eye geometry to draw for {@code living}, or {@code null} to draw nothing: honors the client
     * global/per-entity disables, the server's per-mob has-eyes decision (bypassed while the picker is
     * active, so authoring shows every configured mob), invisibility, and a usable config.
     */
    @Nullable
    public static HeadInfo helperToRender(LivingEntity living) {
        ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        if (ClientConfig.DISABLE_GOOGLY_EYES.get()) {
            logDecision(entityType, living, "global client disable");
            return null;
        }
        if (ClientConfig.isEntityDisabled(entityType)) {
            logDecision(entityType, living, "entity/mod client disable");
            return null;
        }
        if (!PickerState.isActive() && !EyeState.hasEyes(living)) {
            logDecision(entityType, living, "client entity state hasEyes=false");
            return null;
        }
        if (living.isInvisible()) {
            logDecision(entityType, living, "entity invisible");
            return null;
        }
        HeadInfo helper = HeadInfo.getHelper(entityType, living, EyeState.getVariantRoll(living));
        if (!helper.hasConfig()) {
            logDecision(entityType, living, "no usable client eye config");
            return null;
        }
        logDecision(entityType, living, "render gate passed");
        return helper;
    }

    private static void logDecision(ResourceLocation entityType, LivingEntity living, String decision) {
        String key = entityType + "|" + decision;
        if (LOGGED_DEBUG_DECISIONS.size() >= DEBUG_DECISION_LIMIT
                || !LOGGED_DEBUG_DECISIONS.add(key)) {
            return;
        }
        SomeGooglyCommon.LOGGER.info(
                "Eye render debug: type={}, entityId={}, baby={}, pickerActive={}, hasEyes={}, decision={}",
                entityType, living.getId(), living.isBaby(), PickerState.isActive(),
                EyeState.hasEyes(living), decision);
    }
}
