package com.github.crittscott.somegoogly.neoforge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.neoforge.NeoForgeClient;
import com.github.crittscott.somegoogly.config.EyeConfigReloadListener;
import com.github.crittscott.somegoogly.config.neoforge.NeoForgeClientConfig;
import com.github.crittscott.somegoogly.config.neoforge.NeoForgeServerConfig;
import com.github.crittscott.somegoogly.registry.neoforge.NeoForgeContentRegistrar;
import com.github.crittscott.somegoogly.network.neoforge.NeoForgeNetworkTransport;
import com.github.crittscott.somegoogly.server.neoforge.NeoForgeServerEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/** NeoForge bootstrap for common registration and server datapack resources. */
@Mod(SomeGooglyCommon.MOD_ID)
public final class SomeGooglyNeoForge {

    public SomeGooglyNeoForge(IEventBus modBus, ModContainer modContainer, Dist dist) {
        NeoForgeContentRegistrar registrar = new NeoForgeContentRegistrar();
        SomeGooglyCommon.init(registrar);
        registrar.register(modBus);
        NeoForgeNetworkTransport.register(modBus);
        NeoForgeServerConfig.register(NeoForge.EVENT_BUS);
        if (dist == Dist.CLIENT) {
            NeoForgeClientConfig.register(modBus, modContainer);
            NeoForgeClient.register(modBus, NeoForge.EVENT_BUS);
        }
        NeoForgeServerEvents.register(NeoForge.EVENT_BUS);
        NeoForge.EVENT_BUS.addListener(SomeGooglyNeoForge::addReloadListeners);
    }

    private static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new EyeConfigReloadListener());
    }
}
