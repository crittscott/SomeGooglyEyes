package com.github.crittscott.assets;

import net.minecraft.client.model.geom.ModelPart;

import java.util.List;
import java.util.Map;

/** Mixin-backed access to {@code ModelPart}'s private child and cube collections. */
public interface ModelPartAccessor {
    Map<String, ModelPart> getChildren();

    List<ModelPart.Cube> getCubes();
}
