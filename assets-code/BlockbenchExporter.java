package com.github.crittscott.assets;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Converts a {@link GeometryData} tree into a Blockbench {@code .bbmodel} (JSON) document.
 *
 * <p>Each model part becomes an outliner group and each cube becomes an element. Pivots are
 * accumulated by translation down the tree; Blockbench reproduces MC's rotation composition
 * through group nesting, so no matrix math is needed here.
 *
 * <p><b>Coordinate flip:</b> vanilla entity models are built inverted (the renderer applies a
 * 180-degree flip). {@link #flipY} negates Y so the model sits upright in Blockbench and
 * {@link #toBlockbenchRotation} carries the matching sign convention. Bind-pose rotations are
 * zero on almost every part, so this only visibly affects parts with a non-zero rest rotation
 * (spider legs, ghast tentacles). If those look wrong, adjust the signs in those two methods.
 *
 * <p><b>UV:</b> cubes use box UV ({@code uv_offset} + {@code box_uv}); Blockbench derives the
 * per-face mapping. {@code resolution} is the inferred texture size from {@link GeometryData}.
 */
public class BlockbenchExporter {

    public static BBModel export(GeometryData geometry) {
        return export(geometry, null);
    }

    /**
     * @param texture the entity's skin, embedded as a base64 {@code data:} URL so the model opens
     *     already textured; {@code null} exports geometry only. The texture's real pixel size sets
     *     the box-UV {@code resolution}, matching the UV origins baked by
     *     {@link EntityModelExtractor#extractGeometry}. Box-UV models with a single texture map it
     *     to every face automatically.
     */
    public static BBModel export(GeometryData geometry, TextureUtils.LoadedTexture texture) {
        BBModel model = new BBModel();
        model.name = geometry.entityType;
        if (geometry.textureSize != null) {
            model.resolution.width = geometry.textureSize[0];
            model.resolution.height = geometry.textureSize[1];
        }
        for (GeometryPart root : geometry.roots) {
            model.outliner.add(exportGroup(root, new float[]{0, 0, 0}, model.elements));
        }
        if (texture != null) {
            model.resolution.width = texture.width();
            model.resolution.height = texture.height();
            Texture tex = new Texture();
            tex.name = texture.name();
            tex.width = texture.width();
            tex.height = texture.height();
            tex.uv_width = texture.width();
            tex.uv_height = texture.height();
            tex.uuid = UUID.randomUUID().toString();
            tex.source = texture.source();
            model.textures.add(tex);
        }
        return model;
    }

    /** @param parentOrigin accumulated pivot in UN-flipped model space (pixels). */
    private static Group exportGroup(GeometryPart part, float[] parentOrigin, List<Element> elements) {
        float[] origin = {
                parentOrigin[0] + part.translation[0],
                parentOrigin[1] + part.translation[1],
                parentOrigin[2] + part.translation[2]
        };

        Group group = new Group();
        group.name = part.name;
        group.origin = flipY(origin);
        group.rotation = toBlockbenchRotation(part.rotation);
        group.uuid = UUID.randomUUID().toString();

        for (GeometryCube cube : part.cubes) {
            Element element = exportCube(part.name, cube, origin);
            elements.add(element);
            group.children.add(element.uuid);
        }
        for (GeometryPart child : part.children) {
            group.children.add(exportGroup(child, origin, elements));
        }
        return group;
    }

    private static Element exportCube(String name, GeometryCube cube, float[] origin) {
        // Global rest-pose box = pivot + local box.
        float gx0 = origin[0] + cube.from[0];
        float gy0 = origin[1] + cube.from[1];
        float gz0 = origin[2] + cube.from[2];
        float gx1 = origin[0] + cube.to[0];
        float gy1 = origin[1] + cube.to[1];
        float gz1 = origin[2] + cube.to[2];

        Element element = new Element();
        element.name = name;
        // Negating Y inverts the Y bounds, so min/max swap.
        element.from = new float[]{gx0, -gy1, gz0};
        element.to = new float[]{gx1, -gy0, gz1};
        element.origin = flipY(origin);
        element.rotation = new float[]{0, 0, 0};
        element.inflate = inflateScalar(cube.inflate);
        element.uuid = UUID.randomUUID().toString();
        if (cube.uvOffset != null) {
            element.uv_offset = new int[]{Math.round(cube.uvOffset[0]), Math.round(cube.uvOffset[1])};
        }
        return element;
    }

    private static float[] flipY(float[] p) {
        return new float[]{p[0], -p[1], p[2]};
    }

    /** Radians to degrees with the Y-flip reflection sign convention. Tune here if needed. */
    private static float[] toBlockbenchRotation(float[] radians) {
        float deg = (float) (180.0 / Math.PI);
        return new float[]{-radians[0] * deg, -radians[1] * deg, radians[2] * deg};
    }

    /** Blockbench exposes a single inflate value; use the largest per-axis grow. */
    private static float inflateScalar(float[] inflate) {
        if (inflate == null) {
            return 0f;
        }
        return Math.max(inflate[0], Math.max(inflate[1], inflate[2]));
    }

    // --- bbmodel document (serialized to JSON by Gson) --------------------

    public static class BBModel {
        public Meta meta = new Meta();
        public String name;
        public Resolution resolution = new Resolution();
        public List<Element> elements = new ArrayList<>();
        public List<Object> outliner = new ArrayList<>();
        public List<Object> textures = new ArrayList<>();
    }

    public static class Meta {
        public String format_version = "4.5";
        public String model_format = "free";
        public boolean box_uv = true;
    }

    public static class Resolution {
        public int width = 64;
        public int height = 64;
    }

    public static class Element {
        public String name;
        public boolean box_uv = true;
        public int[] uv_offset = {0, 0};
        public float[] from;
        public float[] to;
        public int autouv = 0;
        public int color = 0;
        public float inflate;
        public float[] origin;
        public float[] rotation;
        public String uuid;
    }

    public static class Group {
        public String name;
        public float[] origin;
        public float[] rotation;
        public String uuid;
        public List<Object> children = new ArrayList<>();
    }

    /** An embedded texture: the PNG bytes live in {@code source} as a base64 {@code data:} URL. */
    public static class Texture {
        public String name;
        public String id = "0";
        public boolean particle = false;
        public boolean visible = true;
        public String render_mode = "default";
        public int width;
        public int height;
        public int uv_width;
        public int uv_height;
        public String uuid;
        public String source;
    }
}
