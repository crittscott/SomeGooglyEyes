package com.github.crittscott.somegoogly.client.fabric;

import com.github.crittscott.somegoogly.client.compat.gecko.GeckoIntegration;
import software.bernie.geckolib.event.GeoRenderEvent;

/** Adds the googly-eye layer to GeckoLib entity renderers; registered only when GeckoLib is present. */
final class FabricGeckoLayers {

    private FabricGeckoLayers() {
    }

    static void register() {
        GeoRenderEvent.Entity.CompileRenderLayers.EVENT.register(
                event -> GeckoIntegration.compileLayers(event.getRenderer(), event::addLayer));
    }
}
