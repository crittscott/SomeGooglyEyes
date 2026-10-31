package com.github.crittscott.somegoogly.client.forge;

import com.github.crittscott.somegoogly.client.compat.gecko.GeckoIntegration;
import software.bernie.geckolib.event.GeoRenderEvent;

/** Adds the googly-eye layer to GeckoLib entity renderers; registered only when GeckoLib is present. */
final class ForgeGeckoLayers {

    private ForgeGeckoLayers() {
    }

    static void register() {
        GeoRenderEvent.Entity.CompileRenderLayers.BUS.addListener(ForgeGeckoLayers::compileLayers);
    }

    private static void compileLayers(GeoRenderEvent.Entity.CompileRenderLayers event) {
        GeckoIntegration.compileLayers(event.getRenderer(), event::addLayer);
    }
}
