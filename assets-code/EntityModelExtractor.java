package com.github.crittscott.assets;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Extracts entity model structure from the running client's renderers.
 *
 * <p>All naming comes from sources that survive reobfuscation - {@code getChildren()} map
 * keys, {@link HierarchicalModel#root()}, and {@code AgeableListModel.headParts()} - never
 * from reflected field names, which are SRG-mangled at production runtime.
 */
public class EntityModelExtractor {

    private static final Set<String> HEAD_NAMES =
            Set.of("head", "centerhead", "headmain", "lefthead", "righthead");

    /** A model part paired with a display name (roots have no intrinsic name). */
    private record NamedPart(String name, ModelPart part) {}

    // --- model resolution -------------------------------------------------

    public static EntityModel<?> resolveModel(EntityType<?> type) {
        EntityRenderer<?> renderer =
                Minecraft.getInstance().getEntityRenderDispatcher().renderers.get(type);
        if (renderer instanceof LivingEntityRenderer<?, ?> living) {
            return living.getModel();
        }
        return null;
    }

    /**
     * The PNG this entity is skinned with, via the renderer's public
     * {@code getTextureLocation(entity)}. Needs a live instance (and therefore a client level),
     * so it spawns a throwaway default instance - never added to the world. Variant-driven
     * textures (horse markings, cat/villager variants) resolve to their default. Returns
     * {@code null} if no level is loaded, the entity can't be created, or anything throws.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ResourceLocation resolveTexture(EntityType<?> type) {
        try {
            Level level = Minecraft.getInstance().level;
            if (level == null) {
                return null;
            }
            Entity entity = type.create(level);
            if (entity == null) {
                return null;
            }
            EntityRenderer<?> renderer =
                    Minecraft.getInstance().getEntityRenderDispatcher().renderers.get(type);
            if (renderer == null) {
                return null;
            }
            return ((EntityRenderer) renderer).getTextureLocation(entity);
        } catch (Exception e) {
            return null;
        }
    }

    // --- skeleton ---------------------------------------------------------

    public static SkeletonData extractSkeleton(ResourceLocation id, EntityModel<?> model) {
        SkeletonData data = new SkeletonData();
        data.entityType = id.toString();
        data.modelClass = model.getClass().getName();
        for (NamedPart root : resolveRoots(model)) {
            data.roots.add(buildSkeleton(root.name(), root.part()));
        }
        return data;
    }

    private static PartNode buildSkeleton(String name, ModelPart part) {
        PartNode node = new PartNode(name);
        for (Map.Entry<String, ModelPart> child : children(part).entrySet()) {
            node.children.add(buildSkeleton(child.getKey(), child.getValue()));
        }
        return node;
    }

    // --- geometry ---------------------------------------------------------

    public static GeometryData extractGeometry(ResourceLocation id, EntityModel<?> model) {
        return extractGeometry(id, model, null);
    }

    /**
     * @param realTextureSize actual texture {@code [width, height]} in pixels when known; the
     *     box-UV origins are scaled to it instead of the size inferred from the UVs, keeping the
     *     geometry consistent with an embedded texture. Pass {@code null} to fall back to inference.
     */
    public static GeometryData extractGeometry(ResourceLocation id, EntityModel<?> model, int[] realTextureSize) {
        GeometryData data = new GeometryData();
        data.entityType = id.toString();
        data.modelClass = model.getClass().getName();
        UvAccumulator uv = new UvAccumulator();
        for (NamedPart root : resolveRoots(model)) {
            data.roots.add(buildGeometry(root.name(), root.part(), uv));
        }
        data.textureSize = (realTextureSize != null) ? realTextureSize : uv.deriveTextureSize();
        for (GeometryPart root : data.roots) {
            applyTextureSize(root, data.textureSize[0], data.textureSize[1]);
        }
        return data;
    }

    private static GeometryPart buildGeometry(String name, ModelPart part, UvAccumulator uv) {
        GeometryPart gp = new GeometryPart();
        gp.name = name;
        float[][] pose = bindPose(part);
        gp.translation = pose[0];
        gp.rotation = pose[1];
        gp.scale = new float[]{part.xScale, part.yScale, part.zScale};
        for (ModelPart.Cube cube : cubes(part)) {
            gp.cubes.add(toGeometryCube(cube, uv));
        }
        for (Map.Entry<String, ModelPart> child : children(part).entrySet()) {
            gp.children.add(buildGeometry(child.getKey(), child.getValue(), uv));
        }
        return gp;
    }

    // --- head -------------------------------------------------------------

    public static HeadData extractHead(ResourceLocation id, EntityModel<?> model) {
        HeadData data = new HeadData();
        data.entityType = id.toString();
        data.modelClass = model.getClass().getName();

        // AgeableListModel exposes the head parts canonically (covers the classic mobs).
        if (model instanceof AgeableListModel<?> ageable) {
            Iterator<ModelPart> heads =
                    ((AgeableListModelAccessor) (Object) ageable).invokeHeadParts().iterator();
            if (heads.hasNext()) {
                fillHead(data, "head", heads.next());
                return data;
            }
        }
        // Otherwise search the part tree for a child named like a head.
        for (NamedPart root : resolveRoots(model)) {
            if (searchHead(root.name(), root.part(), "", data)) {
                return data;
            }
        }
        data.found = false;
        return data;
    }

    private static boolean searchHead(String name, ModelPart part, String parentPath, HeadData data) {
        String path = parentPath.isEmpty() ? name : parentPath + "/" + name;
        if (HEAD_NAMES.contains(name.toLowerCase(Locale.ROOT))) {
            fillHead(data, path, part);
            return true;
        }
        for (Map.Entry<String, ModelPart> child : children(part).entrySet()) {
            if (searchHead(child.getKey(), child.getValue(), path, data)) {
                return true;
            }
        }
        return false;
    }

    private static void fillHead(HeadData data, String path, ModelPart part) {
        data.found = true;
        data.path = path;
        float[][] pose = bindPose(part);
        data.translation = pose[0];
        data.rotation = pose[1];
        List<ModelPart.Cube> cubeList = cubes(part);
        if (!cubeList.isEmpty()) {
            data.cube = toCube(cubeList.get(0));
        }
    }

    // --- root resolution --------------------------------------------------

    private static List<NamedPart> resolveRoots(EntityModel<?> model) {
        List<NamedPart> roots = new ArrayList<>();
        if (model instanceof HierarchicalModel<?> hierarchical) {
            roots.add(new NamedPart("root", hierarchical.root()));
        } else if (model instanceof AgeableListModel<?> ageable) {
            AgeableListModelAccessor accessor = (AgeableListModelAccessor) (Object) ageable;
            index(roots, "head", accessor.invokeHeadParts());
            index(roots, "body", accessor.invokeBodyParts());
        } else {
            roots.addAll(reflectRoots(model));
        }
        return roots;
    }

    private static void index(List<NamedPart> roots, String base, Iterable<ModelPart> parts) {
        int i = 0;
        for (ModelPart part : parts) {
            roots.add(new NamedPart(i == 0 ? base : base + i, part));
            i++;
        }
    }

    /**
     * Fallback for models that are neither hierarchical nor ageable-list: collect
     * {@code ModelPart} fields by TYPE (never by name) and keep those that are not a
     * descendant of another collected part.
     */
    private static List<NamedPart> reflectRoots(EntityModel<?> model) {
        Set<ModelPart> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ModelPart> ordered = new ArrayList<>();
        for (Class<?> c = model.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (field.getType() == ModelPart.class) {
                    try {
                        field.setAccessible(true);
                        ModelPart part = (ModelPart) field.get(model);
                        if (part != null && seen.add(part)) {
                            ordered.add(part);
                        }
                    } catch (ReflectiveOperationException | RuntimeException ignored) {
                        // inaccessible field; skip
                    }
                }
            }
        }
        Set<ModelPart> descendants = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ModelPart part : ordered) {
            collectDescendants(part, descendants);
        }
        List<NamedPart> roots = new ArrayList<>();
        int i = 0;
        for (ModelPart part : ordered) {
            if (!descendants.contains(part)) {
                roots.add(new NamedPart("part" + (i++), part));
            }
        }
        return roots;
    }

    private static void collectDescendants(ModelPart part, Set<ModelPart> out) {
        for (ModelPart child : children(part).values()) {
            if (out.add(child)) {
                collectDescendants(child, out);
            }
        }
    }

    // --- helpers ----------------------------------------------------------

    /** Bind-pose [translation, rotation], falling back to live fields if no pose was baked. */
    private static float[][] bindPose(ModelPart part) {
        PartPose pose = part.getInitialPose();
        float tx = pose.x, ty = pose.y, tz = pose.z;
        float rx = pose.xRot, ry = pose.yRot, rz = pose.zRot;
        if (tx == 0 && ty == 0 && tz == 0 && rx == 0 && ry == 0 && rz == 0) {
            tx = part.x; ty = part.y; tz = part.z;
            rx = part.xRot; ry = part.yRot; rz = part.zRot;
        }
        return new float[][]{{tx, ty, tz}, {rx, ry, rz}};
    }

    private static GeometryCube toCube(ModelPart.Cube cube) {
        GeometryCube gc = new GeometryCube();
        gc.from = new float[]{cube.minX, cube.minY, cube.minZ};
        gc.to = new float[]{cube.maxX, cube.maxY, cube.maxZ};
        return gc;
    }

    /** Geometry-mode cube: raw box plus inflate and box-UV origin recovered from baked vertices. */
    private static GeometryCube toGeometryCube(ModelPart.Cube cube, UvAccumulator uv) {
        GeometryCube gc = new GeometryCube();
        gc.from = new float[]{cube.minX, cube.minY, cube.minZ};
        gc.to = new float[]{cube.maxX, cube.maxY, cube.maxZ};

        CapturingVertexConsumer capture = new CapturingVertexConsumer();
        cube.compile(new PoseStack().last(), capture, 0, 0, 1f, 1f, 1f, 1f);
        List<CapturingVertexConsumer.Vert> verts = capture.vertices;
        if (!verts.isEmpty()) {
            float vMinX = Float.MAX_VALUE, vMinY = Float.MAX_VALUE, vMinZ = Float.MAX_VALUE;
            float minU = Float.MAX_VALUE, minV = Float.MAX_VALUE;
            for (CapturingVertexConsumer.Vert v : verts) {
                vMinX = Math.min(vMinX, v.x);
                vMinY = Math.min(vMinY, v.y);
                vMinZ = Math.min(vMinZ, v.z);
                minU = Math.min(minU, v.u);
                minV = Math.min(minV, v.v);
                uv.add(v.u, v.v);
            }
            // Vertices extend beyond the raw box by the CubeDeformation grow, per axis.
            gc.inflate = new float[]{
                    round(cube.minX - vMinX),
                    round(cube.minY - vMinY),
                    round(cube.minZ - vMinZ)
            };
            // Box-UV origin = minimum UV; stored normalized here, scaled to pixels once the
            // texture size is known (see applyTextureSize).
            gc.uvOffset = new float[]{minU, minV};
        }
        return gc;
    }

    /** Second pass: convert each cube's normalized box-UV origin to texture pixels. */
    private static void applyTextureSize(GeometryPart part, int texW, int texH) {
        for (GeometryCube cube : part.cubes) {
            if (cube.uvOffset != null) {
                cube.uvOffset = new float[]{
                        Math.round(cube.uvOffset[0] * texW),
                        Math.round(cube.uvOffset[1] * texH)
                };
            }
        }
        for (GeometryPart child : part.children) {
            applyTextureSize(child, texW, texH);
        }
    }

    private static float round(float value) {
        return Math.round(value * 10000.0F) / 10000.0F;
    }

    /** Collects every UV emitted across a model to infer its texture grid resolution. */
    private static class UvAccumulator {
        private final List<Float> us = new ArrayList<>();
        private final List<Float> vs = new ArrayList<>();

        void add(float u, float v) {
            us.add(u);
            vs.add(v);
        }

        int[] deriveTextureSize() {
            return new int[]{deriveDimension(us), deriveDimension(vs)};
        }

        /** Smallest standard texture size that makes every normalized UV land on a pixel. */
        private int deriveDimension(List<Float> values) {
            if (values.isEmpty()) {
                return 64;
            }
            for (int candidate : new int[]{16, 32, 64, 128, 256, 512}) {
                boolean allInteger = true;
                for (float value : values) {
                    float scaled = value * candidate;
                    if (Math.abs(scaled - Math.round(scaled)) > 1e-3F) {
                        allInteger = false;
                        break;
                    }
                }
                if (allInteger) {
                    return candidate;
                }
            }
            return 64;
        }
    }

    private static Map<String, ModelPart> children(ModelPart part) {
        return ((ModelPartAccessor) (Object) part).getChildren();
    }

    private static List<ModelPart.Cube> cubes(ModelPart part) {
        return ((ModelPartAccessor) (Object) part).getCubes();
    }
}
