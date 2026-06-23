package com.github.crittscott.somegoogly.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Closed, faceted eye geometry used by mob eyes, picker previews, and eye items.
 *
 * <p>The cornea and iris are two complete shallow cylinders, each with front and rear caps. The
 * iris stays physically in front of the cornea, so their surfaces never overlap or need z-fighting
 * offsets. The 16-sided rim reads as round at item scale while remaining inexpensive.
 */
public final class ModelGooglyEye {

    private static final int SIDES = 16;
    /** ModelPart cube coordinates are pixels; custom vertices use the same conversion. */
    private static final float MODEL_UNIT = 1.0F / 16.0F;

    // Local dimensions preserve the former model's visible diameter and approximate depth.
    private static final float CORNEA_RADIUS = 1.865F;
    private static final float CORNEA_FRONT_Z = -1.00F;
    private static final float CORNEA_BACK_Z = 0.00F;
    private static final float IRIS_RADIUS = 1.00F;
    private static final float IRIS_FRONT_Z = -1.50F;
    private static final float IRIS_BACK_Z = -1.05F;

    private static final float[] RING_X = new float[SIDES];
    private static final float[] RING_Y = new float[SIDES];

    static {
        for (int i = 0; i < SIDES; i++) {
            double angle = Math.PI * 2.0 * i / SIDES;
            RING_X[i] = (float) Math.cos(angle);
            RING_Y[i] = (float) Math.sin(angle);
        }
    }

    private float irisOffsetX;
    private float irisOffsetY;

    public void renderCornea(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                              float red, float green, float blue, float alpha) {
        renderCylinder(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha,
                0F, 0F, CORNEA_RADIUS, CORNEA_FRONT_Z, CORNEA_BACK_Z);
    }

    public void renderIris(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                            float red, float green, float blue, float alpha) {
        renderCylinder(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha,
                irisOffsetX, irisOffsetY, IRIS_RADIUS, IRIS_FRONT_Z, IRIS_BACK_Z);
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

        // Max cornea-unit travel of the iris CENTRE that keeps the iris disk inside the cornea.
        float maxCenter = CORNEA_RADIUS - IRIS_RADIUS * pupilSize;
        float preScale = maxCenter / pupilSize;
        // Render space is -Y up / -X right, so a physics +Y (up) pupil must render toward -Y.
        // Without this flip the pupil rests at the top and falls upward.
        irisOffsetX = -normX * preScale;
        irisOffsetY = -normY * preScale;
    }

    private static void renderCylinder(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                                       int packedOverlay, float red, float green, float blue, float alpha,
                                       float centerX, float centerY, float radius, float frontZ, float backZ) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        for (int i = 0; i < SIDES; i++) {
            int next = (i + 1) % SIDES;

            float x0 = centerX + RING_X[i] * radius;
            float y0 = centerY + RING_Y[i] * radius;
            float x1 = centerX + RING_X[next] * radius;
            float y1 = centerY + RING_Y[next] * radius;

            // Entity-cutout buffers draw quads, so each cap triangle is a degenerate quad.
            emit(buffer, matrix, normalMatrix, centerX, centerY, frontZ, 0F, 0F, -1F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emit(buffer, matrix, normalMatrix, x1, y1, frontZ, 0F, 0F, -1F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emit(buffer, matrix, normalMatrix, x0, y0, frontZ, 0F, 0F, -1F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emit(buffer, matrix, normalMatrix, centerX, centerY, frontZ, 0F, 0F, -1F,
                    packedLight, packedOverlay, red, green, blue, alpha);

            emit(buffer, matrix, normalMatrix, centerX, centerY, backZ, 0F, 0F, 1F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emit(buffer, matrix, normalMatrix, x0, y0, backZ, 0F, 0F, 1F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emit(buffer, matrix, normalMatrix, x1, y1, backZ, 0F, 0F, 1F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emit(buffer, matrix, normalMatrix, centerX, centerY, backZ, 0F, 0F, 1F,
                    packedLight, packedOverlay, red, green, blue, alpha);

            // Per-vertex radial normals make the rim shade smoothly.
            emit(buffer, matrix, normalMatrix, x0, y0, frontZ, RING_X[i], RING_Y[i], 0F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emit(buffer, matrix, normalMatrix, x1, y1, frontZ, RING_X[next], RING_Y[next], 0F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emit(buffer, matrix, normalMatrix, x1, y1, backZ, RING_X[next], RING_Y[next], 0F,
                    packedLight, packedOverlay, red, green, blue, alpha);
            emit(buffer, matrix, normalMatrix, x0, y0, backZ, RING_X[i], RING_Y[i], 0F,
                    packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    private static void emit(VertexConsumer buffer, Matrix4f matrix, Matrix3f normalMatrix,
                             float x, float y, float z, float normalX, float normalY, float normalZ,
                             int packedLight, int packedOverlay, float red, float green, float blue,
                             float alpha) {
        buffer.vertex(matrix, x * MODEL_UNIT, y * MODEL_UNIT, z * MODEL_UNIT)
                .color(red, green, blue, alpha)
                .uv(0.5F, 0.5F)
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(normalMatrix, normalX, normalY, normalZ)
                .endVertex();
    }
}
