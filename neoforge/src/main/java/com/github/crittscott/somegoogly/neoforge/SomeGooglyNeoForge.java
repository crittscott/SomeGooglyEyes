package com.github.crittscott.somegoogly.neoforge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.neoforge.NeoForgeClient;
import com.github.crittscott.somegoogly.config.EyeConfigReloadListener;
import com.github.crittscott.somegoogly.config.ModVersionLookup;
import com.github.crittscott.somegoogly.config.neoforge.NeoForgeServerConfig;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.github.crittscott.somegoogly.registry.ModContent;
import com.github.crittscott.somegoogly.network.neoforge.NeoForgeNetworkTransport;
import com.github.crittscott.somegoogly.platform.neoforge.RegistrationImpl;
import com.github.crittscott.somegoogly.server.neoforge.HeadEyesLootModifier;
import com.github.crittscott.somegoogly.server.neoforge.NeoForgeServerEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

import java.util.ArrayList;
import java.util.List;

/** NeoForge bootstrap for common registration and server datapack resources. */
@Mod(SomeGooglyCommon.MOD_ID)
public final class SomeGooglyNeoForge {

    public SomeGooglyNeoForge(IEventBus modBus, ModContainer modContainer, Dist dist) {
        ModContent.init();
        RegistrationImpl.attach(modBus);
        HeadEyesLootModifier.register(modBus);
        NeoForgeNetworkTransport.register(modBus);
        NeoForgeServerConfig.register(modBus, modContainer);
        if (dist == Dist.CLIENT) {
            NeoForgeClient.register(modBus, NeoForge.EVENT_BUS, modContainer);
        }
        NeoForgeServerEvents.register(NeoForge.EVENT_BUS);
        NeoForge.EVENT_BUS.addListener(SomeGooglyNeoForge::addReloadListeners);

        SomeGooglyCommon.LOGGER.info("{} initialized on NeoForge; optional head integrations found: {}",
                SomeGooglyCommon.MOD_NAME, headIntegrations());
    }

    private static void addReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(EyeConfigReloadListener.ID, new EyeConfigReloadListener());
    }

    /** The installed mods whose heads can carry eyes, with their versions. */
    private static List<String> headIntegrations() {
        List<String> found = new ArrayList<>();
        ModVersionLookup.modVersion(SkullEyeState.HEADS_NAMESPACE).ifPresent(version -> found.add("Heads " + version));
        ModVersionLookup.modVersion(SkullEyeState.TWILIGHT_NAMESPACE)
                .ifPresent(version -> found.add("Twilight Forest " + version));
        return found;
    }
}
