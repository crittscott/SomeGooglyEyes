package com.github.crittscott.assets;

/**
 * A single cuboid in model-pixel units (1/16 block).
 *
 * <ul>
 *   <li>{@code from}/{@code to} - raw min/max corners (no inflation).
 *   <li>{@code inflate} - per-axis grow recovered from the baked vertices (CubeDeformation).
 *   <li>{@code uvOffset} - box-UV origin in texture pixels (null if the cube has no faces).
 * </ul>
 */
public class GeometryCube {
    public float[] from;     // [minX, minY, minZ]
    public float[] to;       // [maxX, maxY, maxZ]
    public float[] inflate;  // [growX, growY, growZ]
    public float[] uvOffset; // [u, v] in texture pixels
}
