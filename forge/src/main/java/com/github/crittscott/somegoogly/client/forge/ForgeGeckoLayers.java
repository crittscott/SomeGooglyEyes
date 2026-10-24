package com.github.crittscott.somegoogly.client.forge;

import com.github.crittscott.somegoogly.client.compat.gecko.GeckoIntegration;
import net.minecraftforge.eventbus.api.IEventBus;
import software.bernie.geckolib.event.GeoRenderEvent;

/** Adds the googly-eye layer to GeckoLib entity renderers; registered only when GeckoLib is present. */
final class ForgeGeckoLayers {

    private ForgeGeckoLayers() {
    }

    static void register(IEventBus gameBus) {
        gameBus.addListener(ForgeGeckoLayers::compileLayers);
    }

    private static void compileLayers(GeoRenderEvent.Entity.CompileRenderLayers event) {
        GeckoIntegration.compileLayers(event.getRenderer(), event::addLayer);
    }
}
