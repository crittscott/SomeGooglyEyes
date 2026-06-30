package com.github.crittscott.assets;

import java.util.ArrayList;
import java.util.List;

/** Geometry-mode output: cube geometry + transforms in a bone hierarchy. */
public class GeometryData {
    public String entityType;
    public String modelClass;
    public int[] textureSize; // inferred [width, height] from the captured UVs
    public String note = "Units are model-pixels (1/16 block) in the model's internal space. "
            + "Vanilla entity models are built inverted; the renderer applies a 180-degree flip. "
            + "Cube boxes are raw min/max; 'inflate' is the per-axis grow (CubeDeformation); "
            + "'uvOffset' is the box-UV origin in texture pixels; 'textureSize' is inferred from the UVs.";
    public List<GeometryPart> roots = new ArrayList<>();
}
