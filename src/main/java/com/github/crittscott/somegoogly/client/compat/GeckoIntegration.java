package com.github.crittscott.somegoogly.client.compat;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.List;

/**
 * GeckoLib-referencing entry points, reached only via {@link GeckoCompat} (so this class — and the
 * GeckoLib classes it touches — load only when GeckoLib is present).
 */
final class GeckoIntegration {

    private GeckoIntegration() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static boolean tryAddLayer(EntityRenderer<?> renderer) {
        if (!(renderer instanceof GeoEntityRenderer geo)) {
            return false;
        }
        geo.addRenderLayer(new GooglyGeoLayer(geo));
        return true;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static List<String> enumerate(EntityRenderer<?> renderer, LivingEntity living) {
        if (!(renderer instanceof GeoEntityRenderer geo)) {
            return List.of();
        }
        GeoModel model = geo.getGeoModel();
        ResourceLocation location = model.getModelResource((GeoAnimatable) living);
        BakedGeoModel baked = model.getBakedModel(location);
        return baked == null ? List.of() : GeoBones.enumerate(baked);
    }
}
