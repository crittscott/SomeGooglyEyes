package com.github.crittscott.somegoogly.client.compat.gecko;

import com.github.crittscott.somegoogly.client.compat.ClientIntegrationFailures;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.cache.model.BakedGeoModel;
import software.bernie.geckolib.event.GeoRenderEvent;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.List;

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
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List<String> enumerate(EntityRenderer<?, ?> renderer, LivingEntity living) {
        if (!(renderer instanceof GeoEntityRenderer geo)) {
            return List.of();
        }
        GeoModel model = geo.getGeoModel();
        Identifier location = model.getModelResource((GeoRenderState) geo.createRenderState(living, 0.0F));
        BakedGeoModel baked = model.getBakedModel(location);
        return baked == null ? List.of() : GeoBones.enumerate(baked);
    }

    /** Drop the GeckoLib bone cache along with the vanilla-side model caches. */
    public static void clearCaches() {
        GeoBones.clear();
    }

    /**
     * Add the googly-eye layer from a loader's GeckoLib entity compile-render-layers event, which fires once per
     * renderer. A failure is logged once and leaves that renderer without eyes rather than breaking rendering.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void compileLayers(GeoRenderEvent.Entity.CompileRenderLayers event) {
        GeoEntityRenderer<?, ?> renderer = event.getRenderer();
        try {
            event.addLayer(geoRenderer -> new GooglyGeoLayer((GeoEntityRenderer) geoRenderer));
        } catch (RuntimeException | LinkageError failure) {
            ClientIntegrationFailures.warnOnce(
                    "GeckoLib", "render-layer installation", renderer.getClass().getName(), failure);
        }
    }
}
