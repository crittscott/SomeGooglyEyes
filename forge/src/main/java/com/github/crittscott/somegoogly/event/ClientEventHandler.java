package com.github.crittscott.somegoogly.event;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.ClientNetworkHandler;
import com.github.crittscott.somegoogly.client.ClientRenderLayers;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Forge adapters for the loader-neutral client runtime and render-layer installer. */
public final class ClientEventHandler {

    public void addLayers() {
        ClientRenderLayers.install(Minecraft.getInstance().getEntityRenderDispatcher());
    }

    @SubscribeEvent
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientNetworkHandler.clearPendingEyeStates();
        ClientEyeConfigs.clear();
        ClientEyeRuntime.clear();
        PickerState.resetOnDisconnect();
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            ClientNetworkHandler.onEntityLoaded(event.getEntity());
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientNetworkHandler.tick();
            ClientEyeRuntime.tick();
        }
    }
}
