package com.github.crittscott.somegoogly.client.forge;

import com.geckolib.event.entity.CompileEntityRenderLayersEvent;
import com.github.crittscott.somegoogly.client.compat.gecko.GeckoIntegration;

/**
 * Adds the googly-eye layer to GeckoLib entity renderers; registered only when GeckoLib is present.
 */
final class ForgeGeckoLayers {

    private ForgeGeckoLayers() {}

    static void register() {
        CompileEntityRenderLayersEvent.BUS.addListener(GeckoIntegration::compileLayers);
    }
}
