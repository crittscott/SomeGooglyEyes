package com.github.crittscott.somegoogly.eye;

import com.github.crittscott.somegoogly.config.EyeConfigModel.HeadConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.eye.state.EyeAppearance;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Resolved view of one entity eye configuration after age and weighted placement-variant selection.
 * Storage, selection, serialization, caching, and rendering transforms are owned by their respective
 * domains; this class only exposes the selected heads and eyes to consumers.
 */
public final class HeadInfo {

    private final RuntimeConfig entityConfig;
    private final List<HeadConfig> heads;

    public HeadInfo(RuntimeConfig config, int variantIndex) {
        this.entityConfig = config;
        this.heads = headsOfVariant(config, variantIndex);
    }

    /** The eye's config appearance, or the default when the requested eye is absent. */
    public EyeAppearance appearanceAt(int headIndex, int eyeIndex) {
        EyeDefinition eye = eyeAt(headIndex, eyeIndex);
        return eye != null ? eye.appearance() : EyeAppearance.DEFAULT;
    }

    /** Whether this view contains a usable enabled placement variant. */
    public boolean hasConfig() {
        return entityConfig != null && entityConfig.enabled && heads != null && !heads.isEmpty();
    }

    /** This head's model attachment token, or {@code null} when the head index is out of range. */
    @Nullable
    public String getAttachToken(int headIndex) {
        HeadConfig head = headAt(headIndex);
        return head != null ? head.attachPoint : null;
    }

    /** How many eyes this head carries; 0 for an out-of-range head. */
    public int getEyeCount(int headIndex) {
        HeadConfig head = headAt(headIndex);
        return head != null ? head.eyes.size() : 0;
    }

    /** The eye's placement scale, or the default scale when the requested eye is absent. */
    public float getEyeScale(int headIndex, int eyeIndex) {
        return placementAt(headIndex, eyeIndex).eyeScale();
    }

    /** How many heads the selected variant places eyes on; 0 when {@link #hasConfig()} is false. */
    public int getHeadCount() {
        return hasConfig() ? heads.size() : 0;
    }

    /** The selected variant's head list; its identity invalidates per-mob trackers. */
    public List<HeadConfig> headsRef() {
        return heads;
    }

    /** The eye's placement, or the default when the requested eye is absent. */
    public EyePlacement placementAt(int headIndex, int eyeIndex) {
        EyeDefinition eye = eyeAt(headIndex, eyeIndex);
        return eye != null ? eye.placement() : EyePlacement.DEFAULT;
    }

    private EyeDefinition eyeAt(int headIndex, int eyeIndex) {
        HeadConfig head = headAt(headIndex);
        if (head == null || eyeIndex < 0 || eyeIndex >= head.eyes.size()) {
            return null;
        }
        return head.eyes.get(eyeIndex);
    }

    private HeadConfig headAt(int headIndex) {
        if (!hasConfig() || headIndex < 0 || headIndex >= heads.size()) {
            return null;
        }
        return heads.get(headIndex);
    }

    private static List<HeadConfig> headsOfVariant(RuntimeConfig config, int variantIndex) {
        if (config == null || config.variants.isEmpty()) {
            return null;
        }
        int clamped = Math.max(0, Math.min(variantIndex, config.variants.size() - 1));
        return config.variants.get(clamped).heads;
    }
}
