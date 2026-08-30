package com.github.crittscott.somegoogly.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Closed, faceted eye geometry used by mob eyes, picker previews, and eye items.
 *
 * <p>The cornea and iris are two complete shallow cylinders, each with front and rear caps. The
 * iris stays physically in front of the cornea, so their surfaces never overlap or need z-fighting
 * offsets. The 16-sided rim reads as round at item scale while remaining inexpensive.
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
    /** ModelPart cube coordinates are pixels; custom vertices use the same conversion. */
    private static final float MODEL_UNIT = 1.0F / 16.0F;
    // SIDES precedes the RING_* tables by necessity: their initializers read it by simple name, and a
    // forward reference there is a compile error (JLS 8.3.3).
    private static final int SIDES = 16;
    private static final float[] RING_X = new float[SIDES];
    private static final float[] RING_Y = new float[SIDES];

    private float irisOffsetX;
    private float irisOffsetY;

    static {
        for (int i = 0; i < SIDES; i++) {
            double angle = Math.PI * 2.0 * i / SIDES;
            RING_X[i] = (float) Math.cos(angle);
            RING_Y[i] = (float) Math.sin(angle);
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

    /**
     * Position the iris from a unit-disk pupil position {@code (normX, normY)} in [-1,1], mapped
     * linearly onto the full travel circle so the iris edge just kisses the cornea rim at
     * {@code |norm| = 1}. The renderer applies the {@code pupilSize} (irisScale) on the pose, so the
     * offset is expressed in the iris's own pre-scale units (hence the {@code / pupilSize}).
     */
    public void moveIris(float normX, float normY, float pupilSize) {
        if (pupilSize <= 0F) {
            irisOffsetX = 0F;
            irisOffsetY = 0F;
            return;
        }

        // Max cornea-unit travel of the iris CENTER that keeps the iris disk inside the cornea.
        float maxCenter = CORNEA_RADIUS - IRIS_RADIUS * pupilSize;
        float preScale = maxCenter / pupilSize;
        // Render space is -Y up / -X right, so a physics +Y (up) pupil must render toward -Y.
        // Without this flip the pupil rests at the top and falls upward.
        irisOffsetX = -normX * preScale;
        irisOffsetY = -normY * preScale;
    }

    public void renderCornea(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                              float red, float green, float blue, float alpha) {
        renderCylinder(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha,
                0F, 0F, CORNEA_RADIUS, CORNEA_FRONT_Z, CORNEA_BACK_Z);
    }

    private static void renderCylinder(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                                       int packedOverlay, float red, float green, float blue, float alpha,
                                       float centerX, float centerY, float radius, float frontZ, float backZ) {
        PoseStack.Pose pose = poseStack.last();

        for (int i = 0; i < SIDES; i++) {
            int next = (i + 1) % SIDES;

            float x0 = centerX + RING_X[i] * radius;
            float y0 = centerY + RING_Y[i] * radius;
            float x1 = centerX + RING_X[next] * radius;
            float y1 = centerY + RING_Y[next] * radius;

            // Entity-cutout buffers draw quads, so each cap triangle is a degenerate quad.
            emit(buffer, pose, centerX, centerY, frontZ, 0F, 0F, -1F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emit(buffer, pose, x1, y1, frontZ, 0F, 0F, -1F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emit(buffer, pose, x0, y0, frontZ, 0F, 0F, -1F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emit(buffer, pose, centerX, centerY, frontZ, 0F, 0F, -1F,
                    packedLight, packedOverlay, red, green, blue, alpha);

            emit(buffer, pose, centerX, centerY, backZ, 0F, 0F, 1F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emit(buffer, pose, x0, y0, backZ, 0F, 0F, 1F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emit(buffer, pose, x1, y1, backZ, 0F, 0F, 1F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emit(buffer, pose, centerX, centerY, backZ, 0F, 0F, 1F,
                    packedLight, packedOverlay, red, green, blue, alpha);

            // Per-vertex radial normals make the rim shade smoothly.
            emit(buffer, pose, x0, y0, frontZ, RING_X[i], RING_Y[i], 0F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emit(buffer, pose, x1, y1, frontZ, RING_X[next], RING_Y[next], 0F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emit(buffer, pose, x1, y1, backZ, RING_X[next], RING_Y[next], 0F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emit(buffer, pose, x0, y0, backZ, RING_X[i], RING_Y[i], 0F,
                    packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    public void renderIris(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                            float red, float green, float blue, float alpha) {
        renderCylinder(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha,
                irisOffsetX, irisOffsetY, IRIS_RADIUS, IRIS_FRONT_Z, IRIS_BACK_Z);
    }
}
