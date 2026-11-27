package com.github.crittscott.somegoogly.client.neoforge;

import com.geckolib.event.entity.CompileEntityRenderLayersEvent;
import com.github.crittscott.somegoogly.client.compat.gecko.GeckoIntegration;

import net.neoforged.bus.api.IEventBus;

/**
 * Adds the googly-eye layer to GeckoLib entity renderers; registered only when GeckoLib is present.
 */
final class NeoForgeGeckoLayers {

    private NeoForgeGeckoLayers() {}

    static void register(IEventBus gameBus) {
        gameBus.addListener(CompileEntityRenderLayersEvent.class, GeckoIntegration::compileLayers);
    }
}
