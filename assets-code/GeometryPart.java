package com.github.crittscott.assets;

import java.util.ArrayList;
import java.util.List;

/** One bone of the geometry export: transform + cubes + child bones. */
public class GeometryPart {
    public String name;
    public float[] translation; // bind-pose pivot [x, y, z], model-pixel units
    public float[] rotation;    // [xRot, yRot, zRot], radians
    public float[] scale;       // [xScale, yScale, zScale]
    public List<GeometryCube> cubes = new ArrayList<>();
    public List<GeometryPart> children = new ArrayList<>();
}
