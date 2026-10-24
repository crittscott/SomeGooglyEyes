package com.github.crittscott.somegoogly.client.neoforge;

import com.github.crittscott.somegoogly.client.compat.gecko.GeckoIntegration;
import net.neoforged.bus.api.IEventBus;
import software.bernie.geckolib.event.GeoRenderEvent;

/** Adds the googly-eye layer to GeckoLib entity renderers; registered only when GeckoLib is present. */
final class NeoForgeGeckoLayers {

    private NeoForgeGeckoLayers() {
    }

    static void register(IEventBus gameBus) {
        gameBus.addListener(NeoForgeGeckoLayers::compileLayers);
    }

    private static void compileLayers(GeoRenderEvent.Entity.CompileRenderLayers event) {
        GeckoIntegration.compileLayers(event.getRenderer(), event::addLayer);
    }
}
