package com.github.crittscott.somegoogly.head;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeadInfo {
    private static final Map<ResourceLocation, ModelPart> headModelCache = new HashMap<>();
    private static final Map<ResourceLocation, EntityConfig> entityConfigs = new HashMap<>();
    private static final Map<ResourceLocation, HeadInfo> headInfoCache = new HashMap<>();
    private static final Marker MARK = MarkerManager.getMarker(HeadInfo.class.getSimpleName());

    public boolean noFaceInfo = false;
    public ModelPart headModel = null;
    public Object multiModel = null;

    private ResourceLocation entityName;
    private EntityConfig entityConfig;

    public HeadInfo(ResourceLocation entityName) {
        this.entityName = entityName;
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

    public int getHeadCount(LivingEntity entity) {
        return entityConfig != null ? entityConfig.heads.size() : 0;
    }

    public int getEyeCount(LivingEntity entity) {
        if (entityConfig == null || entityConfig.heads.isEmpty()) return 0;
        return entityConfig.heads.get(0).eyes.size();
    }

    public HeadInfo getHeadInfo(LivingEntity entity, int headIndex) {
        return this;
    }

    public boolean doesEyeGlow(LivingEntity entity, int eyeIndex) {
        if (entityConfig == null || entityConfig.heads.isEmpty()) return false;
        HeadConfig head = entityConfig.heads.get(0);
        if (eyeIndex >= head.eyes.size()) return false;
        return head.eyes.get(eyeIndex).glows;
    }

    public boolean affectedByInvisibility(LivingEntity entity, int eyeIndex) {
        if (entityConfig == null || entityConfig.heads.isEmpty()) return true;
        HeadConfig head = entityConfig.heads.get(0);
        if (eyeIndex >= head.eyes.size()) return true;
        return head.eyes.get(eyeIndex).affectedByInvisibility;
    }

    public float getEyeScale(LivingEntity entity, PoseStack poseStack, float partialTicks, int eyeIndex) {
        if (entityConfig == null || entityConfig.heads.isEmpty()) return 0.75f;
        HeadConfig head = entityConfig.heads.get(0);
        if (eyeIndex >= head.eyes.size()) return 0.75f;
        return (float) head.eyes.get(eyeIndex).eyeScale;
    }

    public float getIrisScale(LivingEntity entity, PoseStack poseStack, float partialTicks, int eyeIndex) {
        if (entityConfig == null || entityConfig.heads.isEmpty()) return 0.6f;
        HeadConfig head = entityConfig.heads.get(0);
        if (eyeIndex >= head.eyes.size()) return 0.6f;
        return (float) head.eyes.get(eyeIndex).irisScale;
    }

    public float getEyeSideOffset(LivingEntity entity, PoseStack poseStack, float partialTicks, int eyeIndex) {
        if (entityConfig == null || entityConfig.heads.isEmpty()) return 0.0f;
        HeadConfig head = entityConfig.heads.get(0);
        if (eyeIndex >= head.eyes.size()) return 0.0f;
        return (float) head.eyes.get(eyeIndex).sideOffset;
    }

    public float getEyeRotation(LivingEntity entity, PoseStack poseStack, float partialTicks, int eyeIndex) {
        if (entityConfig == null || entityConfig.heads.isEmpty()) return 0.0f;
        HeadConfig head = entityConfig.heads.get(0);
        if (eyeIndex >= head.eyes.size()) return 0.0f;
        return (float) head.eyes.get(eyeIndex).yRotation;
    }

    public float getEyeTopRotation(LivingEntity entity, PoseStack poseStack, float partialTicks, int eyeIndex) {
        if (entityConfig == null || entityConfig.heads.isEmpty()) return 0.0f;
        HeadConfig head = entityConfig.heads.get(0);
        if (eyeIndex >= head.eyes.size()) return 0.0f;
        return (float) head.eyes.get(eyeIndex).xRotation;
    }

    public float[] getEyeOffsetFromJoint(LivingEntity entity, PoseStack poseStack, float partialTicks, int eyeIndex) {
        if (entityConfig == null || entityConfig.heads.isEmpty()) {
            return new float[]{eyeIndex == 0 ? -0.13f : 0.13f, -0.25f, -0.25f};
        }
        HeadConfig head = entityConfig.heads.get(0);
        if (eyeIndex >= head.eyes.size()) {
            return new float[]{eyeIndex == 0 ? -0.13f : 0.13f, -0.25f, -0.25f};
        }
        double[] pos = head.eyes.get(eyeIndex).position;
        return new float[]{(float) pos[0], (float) pos[1], (float) pos[2]};
    }

    public float[] getCorneaColours(LivingEntity entity, PoseStack poseStack, float partialTicks, int eyeIndex) {
        if (entityConfig == null || entityConfig.heads.isEmpty()) {
            return new float[]{1.0f, 1.0f, 1.0f};
        }
        HeadConfig head = entityConfig.heads.get(0);
        if (eyeIndex >= head.eyes.size()) {
            return new float[]{1.0f, 1.0f, 1.0f};
        }
        double[] colors = head.eyes.get(eyeIndex).corneaColors;
        return new float[]{(float) colors[0], (float) colors[1], (float) colors[2]};
    }

    public float[] getIrisColours(LivingEntity entity, PoseStack poseStack, float partialTicks, int eyeIndex) {
        if (entityConfig == null || entityConfig.heads.isEmpty()) {
            return new float[]{0.0f, 0.0f, 0.0f};
        }
        HeadConfig head = entityConfig.heads.get(0);
        if (eyeIndex >= head.eyes.size()) {
            return new float[]{0.0f, 0.0f, 0.0f};
        }
        double[] colors = head.eyes.get(eyeIndex).irisColors;
        return new float[]{(float) colors[0], (float) colors[1], (float) colors[2]};
    }

    public boolean setup(LivingEntity entity, LivingEntityRenderer<?, ?> renderer) {
        return entityConfig != null;
    }

    public void setHeadModel(LivingEntity entity, LivingEntityRenderer<?, ?> renderer) {
        this.headModel = findHeadModelPart(renderer.getModel(), entityName);
        //SomeGoogly.LOGGER.debug(MARK,"found head model part {} for entity {}", this.headModel, entityName);
    }

    public void correctPosition(LivingEntity entity, PoseStack poseStack, float partialTicks) {
        if (headModel != null) {
            headModel.translateAndRotate(poseStack);
        }
    }

    public void preChildEntHeadRenderCalls(LivingEntity entity, PoseStack poseStack, LivingEntityRenderer<?, ?> renderer) {
    }

    private ModelPart findHeadModelPart(EntityModel<?> model, ResourceLocation entityName) {
        // Check cache first
        if (headModelCache.containsKey(entityName)) {
            return headModelCache.get(entityName);
        }

        if (entityConfig == null || entityConfig.heads.isEmpty()) {
            headModelCache.put(entityName, null);
            return null;
        }

        String headFieldName = entityConfig.heads.get(0).attachPoint;

        // Search for the specific field across the entire class hierarchy
        Class<?> currentClass = model.getClass();
        while (currentClass != null) {
            try {
                Field field = currentClass.getDeclaredField(headFieldName);
                if (field.getType() == ModelPart.class) {
                    field.setAccessible(true);
                    ModelPart headPart = (ModelPart) field.get(model);
                    headModelCache.put(entityName, headPart);
                    return headPart;
                }
            } catch (NoSuchFieldException e) {
                // Field not found in this class, try parent class
            } catch (Exception e) {
                // Other reflection errors, try parent class
            }
            currentClass = currentClass.getSuperclass();
        }

        // Field not found anywhere in the hierarchy
        headModelCache.put(entityName, null);
        return null;
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