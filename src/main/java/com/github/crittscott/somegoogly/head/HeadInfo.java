package com.github.crittscott.somegoogly.head;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-entity eye configuration, loaded from {@code assets/somegoogly/entity_configs/*.json}.
 *
 * <p>As of M1 this class is pure data: it answers "how many heads, which attachment part, and what
 * eyes" for an entity. The actual part resolution and positioning is done by the
 * {@link com.github.crittscott.somegoogly.render.resolver.EyeAttachmentResolver}s, by string name —
 * no field reflection. Each head is honoured independently (multi-head mobs like the wither).
 *
 * <p>The {@code attachPoint} in config is the string part token handed to the resolver (e.g.
 * {@code head}, {@code leftHead}); it is matched against the model's part names, normalised so
 * camelCase tokens match snake_case keys.
 */
public class HeadInfo {
    private static final Map<ResourceLocation, EntityConfig> entityConfigs = new HashMap<>();
    private static final Map<ResourceLocation, HeadInfo> headInfoCache = new HashMap<>();
    private static final Marker MARK = MarkerManager.getMarker(HeadInfo.class.getSimpleName());

    private final EntityConfig entityConfig;

    public HeadInfo(ResourceLocation entityName) {
        this.entityConfig = entityConfigs.get(entityName);
    }

    public static void loadConfigs() {
        String configPath = "/assets/somegoogly/entity_configs/";
        String[] configFiles = {"minecraft.json"};

        Gson gson = new Gson();
        Type configType = new TypeToken<Map<String, List<EntityConfig>>>() {}.getType();

        for (String fileName : configFiles) {
            try {
                InputStream stream = HeadInfo.class.getResourceAsStream(configPath + fileName);
                if (stream != null) {
                    InputStreamReader reader = new InputStreamReader(stream);
                    Map<String, List<EntityConfig>> fileData = gson.fromJson(reader, configType);

                    for (EntityConfig config : fileData.get("entities")) {
                        ResourceLocation entityName = new ResourceLocation(config.entity);
                        entityConfigs.put(entityName, config);
                    }
                    reader.close();
                    SomeGoogly.LOGGER.info(MARK, "Loaded config file: {}", fileName);
                }
            } catch (Exception e) {
                SomeGoogly.LOGGER.error(MARK, "Failed to load config file: {}", fileName, e);
            }
        }
    }

    public static HeadInfo getHelper(ResourceLocation entityName) {
        return headInfoCache.computeIfAbsent(entityName, HeadInfo::new);
    }

    /** Whether this entity has any configured eyes at all. */
    public boolean hasConfig() {
        return entityConfig != null && entityConfig.heads != null && !entityConfig.heads.isEmpty();
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

    // Inner classes for JSON structure
    public static class EntityConfig {
        public String entity;
        public List<HeadConfig> heads;
    }

    public static class HeadConfig {
        public String attachPoint;
        public String[] navigationPath;
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
