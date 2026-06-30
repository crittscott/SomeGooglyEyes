package com.github.crittscott.assets;

import net.minecraft.client.model.geom.ModelPart;

/** Mixin-backed access to {@code AgeableListModel}'s protected part groups. */
public interface AgeableListModelAccessor {
    Iterable<ModelPart> invokeHeadParts();

    Iterable<ModelPart> invokeBodyParts();
}
