package com.github.crittscott.somegoogly.client.picker;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

import java.util.OptionalDouble;

/**
 * Draws the Blockbench/Blender-style transform gizmo for the selected part: RGB = XYZ axes
 * (3 blocks each way), a cube only on each positive end, and an origin marker. Rendered in the
 * part's local frame with depth-testing off (via {@link LinesNoDepth}) so the origin — buried
 * inside the mob mesh — stays visible.
 */
public final class Gizmo {

    private static final float AXIS_LEN = 3.0f;
    private static final float END_CUBE = 0.10f;
    private static final float ORIGIN_CUBE = 0.06f;

    private Gizmo() {
    }

    private static void axis(PoseStack ps, VertexConsumer vc, float x, float y, float z,
                             float r, float g, float b) {
        line(ps, vc, 0, 0, 0, x, y, z, r, g, b);                       // positive (bright)
        line(ps, vc, 0, 0, 0, -x, -y, -z, r * 0.4f, g * 0.4f, b * 0.4f); // negative (dim)
    }

    private static void cube(PoseStack ps, VertexConsumer vc, float cx, float cy, float cz,
                             float h, float r, float g, float b) {
        LevelRenderer.renderLineBox(ps, vc, cx - h, cy - h, cz - h, cx + h, cy + h, cz + h, r, g, b, 1.0f);
    }

    public static void draw(PoseStack poseStack, MultiBufferSource buffers) {
        VertexConsumer vc = buffers.getBuffer(LinesNoDepth.GIZMO_LINES);

        // Axes: bright toward +, dim toward - (the + end also gets a cube as the only label).
        axis(poseStack, vc, AXIS_LEN, 0, 0, 1.0f, 0.15f, 0.15f); // X red
        axis(poseStack, vc, 0, AXIS_LEN, 0, 0.15f, 1.0f, 0.15f); // Y green
        axis(poseStack, vc, 0, 0, AXIS_LEN, 0.25f, 0.45f, 1.0f); // Z blue

        // Positive-end cubes.
        cube(poseStack, vc, AXIS_LEN, 0, 0, END_CUBE, 1.0f, 0.15f, 0.15f);
        cube(poseStack, vc, 0, AXIS_LEN, 0, END_CUBE, 0.15f, 1.0f, 0.15f);
        cube(poseStack, vc, 0, 0, AXIS_LEN, END_CUBE, 0.25f, 0.45f, 1.0f);

        // Origin marker.
        cube(poseStack, vc, 0, 0, 0, ORIGIN_CUBE, 1.0f, 1.0f, 1.0f);
    }

    private static void line(PoseStack ps, VertexConsumer vc,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float r, float g, float b) {
        Matrix4f m = ps.last().pose();
        PoseStack.Pose pose = ps.last();
        float nx = x2 - x1, ny = y2 - y1, nz = z2 - z1;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 1.0e-5f) {
            nx /= len;
            ny /= len;
            nz /= len;
        }
        vc.addVertex(m, x1, y1, z1).setColor(r, g, b, 1.0f).setNormal(pose, nx, ny, nz);
        vc.addVertex(m, x2, y2, z2).setColor(r, g, b, 1.0f).setNormal(pose, nx, ny, nz);
    }

    /**
     * A lines render type identical to vanilla {@link RenderType#lines()} but with depth-testing OFF, so
     * the gizmo (and its origin, buried inside the mob mesh) is always visible. Subclassing
     * {@link RenderType} is what lets us reference the protected {@link RenderStateShard} fields — no
     * mixin needed; it is never instantiated.
     */
    private static final class LinesNoDepth extends RenderType {

        static final RenderType GIZMO_LINES = RenderType.create(
                "somegoogly_gizmo_lines",
                DefaultVertexFormat.POSITION_COLOR_NORMAL,
                VertexFormat.Mode.LINES,
                256,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setCullState(NO_CULL)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                        .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
                        .setOutputState(MAIN_TARGET)
                        .setShaderState(RENDERTYPE_LINES_SHADER)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .createCompositeState(false)
        );

        private LinesNoDepth(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                             boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
            super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
        }
    }
}
