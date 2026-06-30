package com.github.crittscott.assets.mixins;

import com.github.crittscott.assets.ModelPartAccessor;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Map;

@Mixin(ModelPart.class)
public class ModelPartMixin implements ModelPartAccessor {

    @Shadow
    private Map<String, ModelPart> children;

    @Shadow
    private List<ModelPart.Cube> cubes;

    @Override
    public Map<String, ModelPart> getChildren() {
        return this.children;
    }

    @Override
    public List<ModelPart.Cube> getCubes() {
        return this.cubes;
    }
}
