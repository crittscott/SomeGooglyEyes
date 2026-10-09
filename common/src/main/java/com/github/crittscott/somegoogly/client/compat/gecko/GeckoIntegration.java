package com.github.crittscott.somegoogly.client.compat.gecko;

import com.github.crittscott.somegoogly.client.compat.ClientIntegrationFailures;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.List;
import java.util.function.Consumer;

/**
 * GeckoLib-referencing entry points, reached only via {@code GeckoCompat} or from loader listeners registered
 * after its presence check (so this class — and the GeckoLib classes it touches — load only when GeckoLib is
 * present).
 */
public final class GeckoIntegration {

    private GeckoIntegration() {
    }

    /**
     * The bone names of {@code living}'s GeckoLib model, or an empty list for a non-GeckoLib renderer or unbaked
     * model.
     */
    @SuppressWarnings({"rawtypes", "unchecked", "removal"})
    public static List<String> enumerate(EntityRenderer<?, ?> renderer, LivingEntity living) {
        if (!(renderer instanceof GeoEntityRenderer geo)) {
            return List.of();
        }
        GeoModel model = geo.getGeoModel();
        ResourceLocation location = model.getModelResource((GeoAnimatable) living, geo);
        BakedGeoModel baked = model.getBakedModel(location);
        return baked == null ? List.of() : GeoBones.enumerate(baked);
    }

    /**
     * Add the googly-eye layer from a loader's GeckoLib entity compile-render-layers event, which fires once per
     * renderer. A failure is logged once and leaves that renderer without eyes rather than breaking rendering.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void compileLayers(GeoEntityRenderer<?> renderer, Consumer<GeoRenderLayer> addLayer) {
        try {
            addLayer.accept(new GooglyGeoLayer(renderer));
        } catch (Throwable failure) {
            ClientIntegrationFailures.warnOnce(
                    "GeckoLib", "render-layer installation", renderer.getClass().getName(), failure);
        }
    }
}
