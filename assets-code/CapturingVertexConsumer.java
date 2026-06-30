package com.github.crittscott.assets;

import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link VertexConsumer} that records the vertices emitted by
 * {@code ModelPart.Cube.compile(...)}. Driving a cube's public {@code compile} with an
 * identity pose lets us harvest geometry, UVs, and normals without referencing the
 * package-private {@code Polygon}/{@code Vertex} types or reflecting obfuscated fields.
 *
 * <p>{@code compile} calls only the 14-argument {@link #vertex} convenience method, which we
 * override to capture everything in one shot; every other interface method is a no-op stub.
 */
public class CapturingVertexConsumer implements VertexConsumer {

    /** One captured vertex: position in model-pixel units, UV texture-normalized, plus normal. */
    public static class Vert {
        public final float x, y, z;
        public final float u, v;
        public final float nx, ny, nz;

        Vert(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
            this.x = x; this.y = y; this.z = z;
            this.u = u; this.v = v;
            this.nx = nx; this.ny = ny; this.nz = nz;
        }
    }

    public final List<Vert> vertices = new ArrayList<>();

    @Override
    public void vertex(float x, float y, float z,
                       float r, float g, float b, float a,
                       float u, float v,
                       int overlay, int light,
                       float nx, float ny, float nz) {
        // compile() divides positions by 16; multiply back to model-pixel units so they
        // line up with the cube's raw minX..maxZ bounds.
        vertices.add(new Vert(x * 16.0F, y * 16.0F, z * 16.0F, u, v, nx, ny, nz));
    }

    // --- unused abstract methods (compile() never calls these) -----------

    @Override public VertexConsumer vertex(double x, double y, double z) { return this; }
    @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
    @Override public VertexConsumer uv(float u, float v) { return this; }
    @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
    @Override public VertexConsumer uv2(int u, int v) { return this; }
    @Override public VertexConsumer normal(float x, float y, float z) { return this; }
    @Override public void endVertex() { }
    @Override public void defaultColor(int r, int g, int b, int a) { }
    @Override public void unsetDefaultColor() { }
}
