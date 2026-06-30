package com.github.crittscott.assets;

/** Head-mode output: the located head part, its bind-pose transform, and first cube. */
public class HeadData {
    public String entityType;
    public String modelClass;
    public boolean found;
    public String path;          // e.g. "head" or "root/head"
    public float[] translation;  // bind-pose pivot [x, y, z]
    public float[] rotation;     // [xRot, yRot, zRot], radians
    public GeometryCube cube;    // first cube of the head part, if any
}
