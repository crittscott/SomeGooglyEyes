package com.github.crittscott.somegoogly.eye;

import com.github.crittscott.somegoogly.config.EyeConfigModel.HeadConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.eye.state.EyeAppearance;
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

    /** Resolve only the selected variant's groups on one attachment, preserving within-group eye indexes. */
    public HeadInfo(RuntimeConfig config, int variantIndex, String attachToken) {
        this.entityConfig = config;
        List<HeadConfig> selected = headsOfVariant(config, variantIndex);
        this.heads = selected == null ? null : selected.stream()
                .filter(head -> attachToken.equals(head.attachPoint)).toList();
    }

    /** The selected eye's config appearance. */
    public EyeAppearance appearanceAt(int headIndex, int eyeIndex) {
        return eyeAt(headIndex, eyeIndex).appearance();
    }

    private EyeDefinition eyeAt(int headIndex, int eyeIndex) {
        return heads.get(headIndex).eyes.get(eyeIndex);
    }

    /** The selected head's model attachment token. */
    public String getAttachToken(int headIndex) {
        return heads.get(headIndex).attachPoint;
    }

    /** How many eyes the selected head carries. */
    public int getEyeCount(int headIndex) {
        return heads.get(headIndex).eyes.size();
    }

    /** How many heads the selected variant places eyes on; 0 when {@link #hasConfig()} is false. */
    public int getHeadCount() {
        return hasConfig() ? heads.size() : 0;
    }

    /** Whether this view contains a usable enabled placement variant. */
    public boolean hasConfig() {
        return entityConfig != null && entityConfig.enabled && heads != null && !heads.isEmpty();
    }

    private static List<HeadConfig> headsOfVariant(RuntimeConfig config, int variantIndex) {
        if (config == null || config.variants.isEmpty()) {
            return null;
        }
        return config.variants.get(variantIndex).heads;
    }

    /** The selected variant's head list; its identity invalidates per-mob trackers. */
    public List<HeadConfig> headsRef() {
        return heads;
    }

    /** The selected eye's placement. */
    public EyePlacement placementAt(int headIndex, int eyeIndex) {
        return eyeAt(headIndex, eyeIndex).placement();
    }
}
