package com.github.crittscott.somegoogly.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Set;

/**
 * Closed, faceted eye geometry used by mob eyes, picker previews, and eye items.
 *
 * <p>The cornea and iris are two shallow cylinders. The iris stays physically in front of the cornea,
 * so their surfaces never overlap or need z-fighting offsets; its rear cap would face only the cornea's
 * front cap, so it is not drawn. The 16-sided rim reads as round at item scale while remaining
 * inexpensive, and each cap is drawn as a fan of quads over its ring. A mob eye may also draw upper and
 * lower eyelids in front of both.
 *
 * <p>Each part is emitted from a submitted custom-geometry callback, which runs after every eye has been
 * submitted, so the geometry holds no per-eye state: everything an eye needs arrives as arguments.
 */
public final class ModelGooglyEye {

    /**
     * The Z pose-scale that gives the eye its standard thickness (relative to its width). Every render
     * site multiplies this by the eye's own {@code depth} multiplier (1 = standard); the geometry below
     * stays fixed. The model extends toward -Z from its origin (the back face sits at Z = 0), so a
     * larger depth pushes the front face further out of the model without moving the attach point.
     */
    public static final float BASE_DEPTH = 0.4F;

    // Local dimensions of the cornea/iris cylinders (pixels; see MODEL_UNIT), sized so the assembled
    // eye has the standard googly proportions at BASE_DEPTH.
    private static final float CORNEA_BACK_Z = 0.00F;
    private static final float CORNEA_FRONT_Z = -1.00F;
    private static final float CORNEA_RADIUS = 1.865F;
    private static final float IRIS_BACK_Z = -1.05F;
    private static final float IRIS_FRONT_Z = -1.50F;
    private static final float IRIS_RADIUS = 1.00F;
    // The eyelids: a little wider than the cornea, from the cornea's back to just ahead of the iris.
    private static final float LID_FRONT_Z = -1.699F;
    private static final float LID_RADIUS = 1.95F;
    /** Arc segments along the lid's curved rim; even, since its front face is drawn two triangles a quad. */
    private static final int LID_SEGMENTS = 16;
    /**
     * The largest pupil size (iris scale) that still fits inside the cornea, where the iris rim meets the
     * cornea rim and the pupil has no room left to travel.
     */
    public static final float MAX_PUPIL_SIZE = CORNEA_RADIUS / IRIS_RADIUS;
    /** ModelPart cube coordinates are pixels; custom vertices use the same conversion. */
    private static final float MODEL_UNIT = 1.0F / 16.0F;
    // SIDES precedes the RING_* tables by necessity: their initializers read it by simple name, and a
    // forward reference there is a compile error (JLS 8.3.3).
    private static final int SIDES = 16;
    private static final float[] RING_X = new float[SIDES];
    private static final float[] RING_Y = new float[SIDES];
    // Lid rim scratch reused by every renderLids call: callbacks run one at a time on the render thread, and
    // one call finishes before the next starts.
    private static final float[] LID_X = new float[LID_SEGMENTS + 1];
    private static final float[] LID_Y = new float[LID_SEGMENTS + 1];
    private static final float[] LID_NX = new float[LID_SEGMENTS + 1];
    private static final float[] LID_NY = new float[LID_SEGMENTS + 1];

    static {
        for (int i = 0; i < SIDES; i++) {
            double angle = Math.PI * 2.0 * i / SIDES;
            RING_X[i] = (float) Math.cos(angle);
            RING_Y[i] = (float) Math.sin(angle);
        }
    }

    private ModelGooglyEye() {
    }

    /**
     * Add the corners of the box enclosing the whole eye, lids included, transformed by {@code pose}, for
     * vanilla's item extents.
     */
    public static void addExtents(Matrix4f pose, Set<Vector3f> output) {
        float r = LID_RADIUS * MODEL_UNIT;
        float front = LID_FRONT_Z * MODEL_UNIT;
        for (float x : new float[]{-r, r}) {
            for (float y : new float[]{-r, r}) {
                for (float z : new float[]{0F, front}) {
                    output.add(pose.transformPosition(x, y, z, new Vector3f()));
                }
            }
        }
    }

    private static void emit(VertexConsumer buffer, PoseStack.Pose pose,
                             float x, float y, float z, float normalX, float normalY, float normalZ,
                             int packedLight, int packedOverlay, float red, float green, float blue,
                             float alpha) {
        buffer.addVertex(pose.pose(), x * MODEL_UNIT, y * MODEL_UNIT, z * MODEL_UNIT)
                .setColor(red, green, blue, alpha)
                .setUv(0.5F, 0.5F)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    /** Draw the cornea cylinder in the given color. */
    public static void renderCornea(PoseStack.Pose pose, VertexConsumer buffer, int packedLight, int packedOverlay,
                                    float red, float green, float blue, float alpha) {
        renderCylinder(pose, buffer, packedLight, packedOverlay, red, green, blue, alpha,
                0F, 0F, CORNEA_RADIUS, CORNEA_FRONT_Z, CORNEA_BACK_Z, true);
    }

    private static void renderCylinder(PoseStack.Pose pose, VertexConsumer buffer, int packedLight,
                                       int packedOverlay, float red, float green, float blue, float alpha,
                                       float centerX, float centerY, float radius, float frontZ, float backZ,
                                       boolean backCap) {
        // Entity-cutout buffers draw quads; ring points 0, i, i+1, i+2 span two fan triangles of the cap,
        // so SIDES - 2 triangles take (SIDES - 2) / 2 quads. The front cap faces -Z, so it winds backward.
        for (int i = 1; i < SIDES - 1; i += 2) {
            emitRing(buffer, pose, centerX, centerY, radius, 0, frontZ, 0F, 0F, -1F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emitRing(buffer, pose, centerX, centerY, radius, i + 2, frontZ, 0F, 0F, -1F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emitRing(buffer, pose, centerX, centerY, radius, i + 1, frontZ, 0F, 0F, -1F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emitRing(buffer, pose, centerX, centerY, radius, i, frontZ, 0F, 0F, -1F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            if (backCap) {
                emitRing(buffer, pose, centerX, centerY, radius, 0, backZ, 0F, 0F, 1F,
                        packedLight, packedOverlay, red, green, blue, alpha);
                emitRing(buffer, pose, centerX, centerY, radius, i, backZ, 0F, 0F, 1F,
                        packedLight, packedOverlay, red, green, blue, alpha);
                emitRing(buffer, pose, centerX, centerY, radius, i + 1, backZ, 0F, 0F, 1F,
                        packedLight, packedOverlay, red, green, blue, alpha);
                emitRing(buffer, pose, centerX, centerY, radius, i + 2, backZ, 0F, 0F, 1F,
                        packedLight, packedOverlay, red, green, blue, alpha);
            }
        }

        // Per-vertex radial normals make the rim shade smoothly.
        for (int i = 0; i < SIDES; i++) {
            int next = (i + 1) % SIDES;
            emitRing(buffer, pose, centerX, centerY, radius, i, frontZ, RING_X[i], RING_Y[i], 0F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emitRing(buffer, pose, centerX, centerY, radius, next, frontZ, RING_X[next], RING_Y[next], 0F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emitRing(buffer, pose, centerX, centerY, radius, next, backZ, RING_X[next], RING_Y[next], 0F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emitRing(buffer, pose, centerX, centerY, radius, i, backZ, RING_X[i], RING_Y[i], 0F,
                    packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    private static void emitRing(VertexConsumer buffer, PoseStack.Pose pose, float centerX, float centerY,
                                 float radius, int index, float z, float normalX, float normalY, float normalZ,
                                 int packedLight, int packedOverlay, float red, float green, float blue,
                                 float alpha) {
        emit(buffer, pose, centerX + RING_X[index] * radius, centerY + RING_Y[index] * radius, z,
                normalX, normalY, normalZ, packedLight, packedOverlay, red, green, blue, alpha);
    }

    /**
     * Draw the iris cylinder in the given color at the unit-disk pupil position {@code (normX, normY)} in
     * [-1,1], mapped linearly onto the full travel circle so the iris edge just kisses the cornea rim at
     * {@code |norm| = 1}. The caller applies the {@code pupilSize} (irisScale) on the pose, so the offset is
     * expressed in the iris's own pre-scale units (hence the {@code / pupilSize}).
     */
    public static void renderIris(PoseStack.Pose pose, VertexConsumer buffer, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha,
                                  float normX, float normY, float pupilSize) {
        float offsetX = 0F;
        float offsetY = 0F;
        if (pupilSize > 0F) {
            // Max cornea-unit travel of the iris CENTER that keeps the iris disk inside the cornea.
            float maxCenter = CORNEA_RADIUS - IRIS_RADIUS * pupilSize;
            float preScale = maxCenter / pupilSize;
            // Render space is -Y up / -X right, so a physics +Y (up) pupil must render toward -Y.
            // Without this flip the pupil rests at the top and falls upward.
            offsetX = -normX * preScale;
            offsetY = -normY * preScale;
        }
        renderCylinder(pose, buffer, packedLight, packedOverlay, red, green, blue, alpha,
                offsetX, offsetY, IRIS_RADIUS, IRIS_FRONT_Z, IRIS_BACK_Z, false);
    }

    /**
     * Draw an upper and a lower eyelid, each closed until it covers {@code coverage} of the eye's height
     * (0 none, at most half), in the given color. Each lid is the part of a disk a little wider than the
     * cornea beyond a horizontal edge, as deep as the whole eye, so it caps the cornea from the front and
     * side; its front face sits ahead of the iris, which shows only between the two edges. Neither has a
     * back face, which would face only the head.
     */
    public static void renderLids(PoseStack.Pose pose, VertexConsumer buffer, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha, float coverage) {
        if (coverage <= 0F) {
            return;
        }

        // The upper lid's shape. Render space is -Y up, so it spans y <= edge. Its rim runs by increasing
        // angle (the cornea ring's direction) from one end of the edge over the top to the other.
        float edge = LID_RADIUS * (2F * Math.min(coverage, 0.5F) - 1F);
        double halfEdgeAngle = Math.asin(edge / LID_RADIUS);
        double start = Math.PI - halfEdgeAngle;
        double span = Math.PI + 2.0 * halfEdgeAngle;
        for (int i = 0; i <= LID_SEGMENTS; i++) {
            double angle = start + span * i / LID_SEGMENTS;
            LID_NX[i] = (float) Math.cos(angle);
            LID_NY[i] = (float) Math.sin(angle);
            LID_X[i] = LID_NX[i] * LID_RADIUS;
            LID_Y[i] = LID_NY[i] * LID_RADIUS;
        }

        // The lower lid is the upper turned half a circle about the eye's center (side -1), which keeps the
        // winding.
        for (int side = 1; side >= -1; side -= 2) {
            // Front face: a fan from the edge's midpoint, two triangles a quad, wound backward like the
            // cornea's.
            for (int i = 0; i < LID_SEGMENTS; i += 2) {
                emit(buffer, pose, 0F, side * edge, LID_FRONT_Z, 0F, 0F, -1F,
                        packedLight, packedOverlay, red, green, blue, alpha);
                emit(buffer, pose, side * LID_X[i + 2], side * LID_Y[i + 2], LID_FRONT_Z, 0F, 0F, -1F,
                        packedLight, packedOverlay, red, green, blue, alpha);
                emit(buffer, pose, side * LID_X[i + 1], side * LID_Y[i + 1], LID_FRONT_Z, 0F, 0F, -1F,
                        packedLight, packedOverlay, red, green, blue, alpha);
                emit(buffer, pose, side * LID_X[i], side * LID_Y[i], LID_FRONT_Z, 0F, 0F, -1F,
                        packedLight, packedOverlay, red, green, blue, alpha);
            }

            // Curved rim, with radial normals like the cornea's.
            for (int i = 0; i < LID_SEGMENTS; i++) {
                emit(buffer, pose, side * LID_X[i], side * LID_Y[i], LID_FRONT_Z,
                        side * LID_NX[i], side * LID_NY[i], 0F,
                        packedLight, packedOverlay, red, green, blue, alpha);
                emit(buffer, pose, side * LID_X[i + 1], side * LID_Y[i + 1], LID_FRONT_Z,
                        side * LID_NX[i + 1], side * LID_NY[i + 1], 0F,
                        packedLight, packedOverlay, red, green, blue, alpha);
                emit(buffer, pose, side * LID_X[i + 1], side * LID_Y[i + 1], CORNEA_BACK_Z,
                        side * LID_NX[i + 1], side * LID_NY[i + 1], 0F,
                        packedLight, packedOverlay, red, green, blue, alpha);
                emit(buffer, pose, side * LID_X[i], side * LID_Y[i], CORNEA_BACK_Z,
                        side * LID_NX[i], side * LID_NY[i], 0F,
                        packedLight, packedOverlay, red, green, blue, alpha);
            }

            // The flat edge, facing the eye's center, closing the rim from its last point back to its first.
            int last = LID_SEGMENTS;
            emit(buffer, pose, side * LID_X[last], side * LID_Y[last], LID_FRONT_Z, 0F, side, 0F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emit(buffer, pose, side * LID_X[0], side * LID_Y[0], LID_FRONT_Z, 0F, side, 0F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emit(buffer, pose, side * LID_X[0], side * LID_Y[0], CORNEA_BACK_Z, 0F, side, 0F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emit(buffer, pose, side * LID_X[last], side * LID_Y[last], CORNEA_BACK_Z, 0F, side, 0F,
                    packedLight, packedOverlay, red, green, blue, alpha);
        }
    }
}
