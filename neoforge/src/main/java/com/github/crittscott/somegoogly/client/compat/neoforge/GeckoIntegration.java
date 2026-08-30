package com.github.crittscott.somegoogly.client.compat.neoforge;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.List;

/** GeckoLib-typed entry points, reached only through the soft-loaded platform gate. */
final class GeckoIntegration {

    private GeckoIntegration() {
    }

    @SuppressWarnings({"rawtypes", "unchecked", "removal"})
    static List<String> enumerate(EntityRenderer<?> renderer, LivingEntity living) {
        if (!(renderer instanceof GeoEntityRenderer geo)) {
            return List.of();
        }
        GeoModel model = geo.getGeoModel();
        ResourceLocation location = model.getModelResource((GeoAnimatable) living);
        BakedGeoModel baked = model.getBakedModel(location);
        return baked == null ? List.of() : GeoBones.enumerate(baked);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static boolean tryAddLayer(EntityRenderer<?> renderer) {
        if (!(renderer instanceof GeoEntityRenderer geo)) {
            return false;
        }
        geo.addRenderLayer(new GooglyGeoLayer(geo));
        return true;
    }
}
