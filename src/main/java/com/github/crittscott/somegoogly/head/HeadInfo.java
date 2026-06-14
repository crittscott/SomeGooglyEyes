package com.github.crittscott.somegoogly.head;

import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-entity, per-age eye configuration, as seen by the client renderer.
 *
 * <p>This is pure data: it answers "how many heads, which attachment part, and what eyes" for an
 * entity. Part resolution and positioning is done by the
 * {@link com.github.crittscott.somegoogly.render.resolver.EyeAttachmentResolver}s, by string name.
 *
 * <p>Configs are loaded from datapacks on the server, selected by mod version + age, and synced to
 * the client; this class reads the client-side selected copy ({@link ClientEyeConfigs}). The
 * {@code attachPoint} in config is the string part token handed to the resolver.
 */
public class HeadInfo {
    private static final Map<CacheKey, HeadInfo> headInfoCache = new HashMap<>();

    private final RuntimeConfig entityConfig;

    public HeadInfo(ResourceLocation entityName, boolean baby) {
        this.entityConfig = ClientEyeConfigs.get(entityName, baby);
    }

    public static HeadInfo getHelper(ResourceLocation entityName, LivingEntity entity) {
        return getHelper(entityName, entity.isBaby());
    }

    public static HeadInfo getHelper(ResourceLocation entityName, boolean baby) {
        return headInfoCache.computeIfAbsent(new CacheKey(entityName, baby), key -> new HeadInfo(key.entityName, key.baby));
    }

    /** Drop cached helpers; called when the client receives new configs or disconnects. */
    public static void clearCache() {
        headInfoCache.clear();
    }

    /** Whether this entity has configured eyes that are enabled. */
    public boolean hasConfig() {
        return entityConfig != null && entityConfig.isEnabled()
                && entityConfig.heads != null && !entityConfig.heads.isEmpty();
    }

    public int getHeadCount() {
        return hasConfig() ? entityConfig.heads.size() : 0;
    }

    /** The part token (string name) the resolver should attach this head's eyes to. */
    public String getAttachToken(int headIndex) {
        HeadConfig head = headAt(headIndex);
        return head != null ? head.attachPoint : "head";
    }

    public int getEyeCount(int headIndex) {
        HeadConfig head = headAt(headIndex);
        return head != null && head.eyes != null ? head.eyes.size() : 0;
    }

    public boolean doesEyeGlow(int headIndex, int eyeIndex) {
        EyeConfig eye = eyeAt(headIndex, eyeIndex);
        return eye != null && eye.glows;
    }

    public boolean affectedByInvisibility(int headIndex, int eyeIndex) {
        EyeConfig eye = eyeAt(headIndex, eyeIndex);
        return eye == null || eye.affectedByInvisibility;
    }

    public float getEyeScale(int headIndex, int eyeIndex) {
        EyeConfig eye = eyeAt(headIndex, eyeIndex);
        return eye != null ? (float) eye.eyeScale : 0.75f;
    }

    public float getIrisScale(int headIndex, int eyeIndex) {
        EyeConfig eye = eyeAt(headIndex, eyeIndex);
        return eye != null ? (float) eye.irisScale : 0.6f;
    }

    public float getEyeSideOffset(int headIndex, int eyeIndex) {
        EyeConfig eye = eyeAt(headIndex, eyeIndex);
        return eye != null ? (float) eye.sideOffset : 0.0f;
    }

    public float getEyeRotation(int headIndex, int eyeIndex) {
        EyeConfig eye = eyeAt(headIndex, eyeIndex);
        return eye != null ? (float) eye.yRotation : 0.0f;
    }

    public float getEyeTopRotation(int headIndex, int eyeIndex) {
        EyeConfig eye = eyeAt(headIndex, eyeIndex);
        return eye != null ? (float) eye.xRotation : 0.0f;
    }

    public float[] getEyeOffsetFromJoint(int headIndex, int eyeIndex) {
        EyeConfig eye = eyeAt(headIndex, eyeIndex);
        if (eye == null || eye.position == null || eye.position.length < 3) {
            return new float[]{eyeIndex == 0 ? -0.13f : 0.13f, -0.25f, -0.25f};
        }
        return new float[]{(float) eye.position[0], (float) eye.position[1], (float) eye.position[2]};
    }

    public float[] getCorneaColours(int headIndex, int eyeIndex) {
        EyeConfig eye = eyeAt(headIndex, eyeIndex);
        if (eye == null || eye.corneaColors == null || eye.corneaColors.length < 3) {
            return new float[]{1.0f, 1.0f, 1.0f};
        }
        return new float[]{(float) eye.corneaColors[0], (float) eye.corneaColors[1], (float) eye.corneaColors[2]};
    }

    public float[] getIrisColours(int headIndex, int eyeIndex) {
        EyeConfig eye = eyeAt(headIndex, eyeIndex);
        if (eye == null || eye.irisColors == null || eye.irisColors.length < 3) {
            return new float[]{0.0f, 0.0f, 0.0f};
        }
        return new float[]{(float) eye.irisColors[0], (float) eye.irisColors[1], (float) eye.irisColors[2]};
    }

    private HeadConfig headAt(int headIndex) {
        if (!hasConfig() || headIndex < 0 || headIndex >= entityConfig.heads.size()) {
            return null;
        }
        return entityConfig.heads.get(headIndex);
    }

    private EyeConfig eyeAt(int headIndex, int eyeIndex) {
        HeadConfig head = headAt(headIndex);
        if (head == null || head.eyes == null || eyeIndex < 0 || eyeIndex >= head.eyes.size()) {
            return null;
        }
        return head.eyes.get(eyeIndex);
    }

    public RuntimeConfig config() {
        return entityConfig;
    }

    private record CacheKey(ResourceLocation entityName, boolean baby) {
    }

    // Raw datapack file structure (one file per entity; entity id comes from the file path).
    public static class ConfigFile {
        public List<VersionedEntry> entries;
    }

    // One selectable entry in a datapack file.
    public static class VersionedEntry {
        public String version;
        public String age;
        public Boolean enabled;
        public List<HeadConfig> heads;

        /** Defaults to enabled when the field is absent. */
        public boolean isEnabled() {
            return enabled == null || enabled;
        }
    }

    // Runtime structure selected by version and age, then synced to clients.
    public static class RuntimeConfig {
        public Boolean enabled;
        public List<HeadConfig> heads;

        /** Defaults to enabled when the field is absent. */
        public boolean isEnabled() {
            return enabled == null || enabled;
        }
    }

    public static class RuntimeConfigSet {
        public RuntimeConfig adult;
        public RuntimeConfig baby;
        public RuntimeConfig any;

        public RuntimeConfig get(boolean isBaby) {
            RuntimeConfig ageConfig = isBaby ? baby : adult;
            return ageConfig != null ? ageConfig : any;
        }

        public boolean hasAnyConfig() {
            return adult != null || baby != null || any != null;
        }
    }

    public static class HeadConfig {
        public String attachPoint;
        public List<EyeConfig> eyes;
    }

    public static class EyeConfig {
        public double[] position;
        public double eyeScale;
        public double irisScale;
        public double sideOffset;
        public double yRotation;
        public double xRotation;
        public double[] corneaColors;
        public double[] irisColors;
        public boolean glows;
        public boolean affectedByInvisibility;
    }
}
