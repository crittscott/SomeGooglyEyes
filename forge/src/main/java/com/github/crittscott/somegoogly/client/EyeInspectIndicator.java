package com.github.crittscott.somegoogly.client;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Forge tick adapter for the shared eye-inspection action-bar indicator. */
public final class EyeInspectIndicator {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            EyeInspector.tick();
        }
    }
}
