package com.github.crittscott.assets.mixins;

import com.github.crittscott.assets.AgeableListModelAccessor;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AgeableListModel.class)
public abstract class AgeableListModelMixin implements AgeableListModelAccessor {

    @Invoker("headParts")
    public abstract Iterable<ModelPart> invokeHeadParts();

    @Invoker("bodyParts")
    public abstract Iterable<ModelPart> invokeBodyParts();
}
