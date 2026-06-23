package com.github.crittscott.somegoogly.client.picker;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.util.OptionalDouble;

/**
 * A lines render type identical to vanilla {@link RenderType#lines()} but with depth-testing OFF, so
 * the gizmo (and its origin, buried inside the mob mesh) is always visible. Subclassing
 * {@link RenderType} is what lets us reference the protected {@link RenderStateShard} fields — no
 * mixin needed.
 */
public final class GizmoRenderType extends RenderType {

    public static final RenderType GIZMO_LINES = RenderType.create(
            "somegoogly_gizmo_lines",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_LINES_SHADER)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(MAIN_TARGET)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setCullState(NO_CULL)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .createCompositeState(false)
    );

    // Never instantiated; exists only so the static field above can see the protected shards.
    private GizmoRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                            boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }
}
