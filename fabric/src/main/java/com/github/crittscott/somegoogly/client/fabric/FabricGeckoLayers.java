package com.github.crittscott.somegoogly.client.fabric;

import com.geckolib.event.entity.CompileEntityRenderLayersEvent;
import com.github.crittscott.somegoogly.client.compat.gecko.GeckoIntegration;

/**
 * Adds the googly-eye layer to GeckoLib entity renderers; registered only when GeckoLib is present.
 */
final class FabricGeckoLayers {

    private FabricGeckoLayers() {}

    static void register() {
        CompileEntityRenderLayersEvent.EVENT.register(GeckoIntegration::compileLayers);
    }
}
