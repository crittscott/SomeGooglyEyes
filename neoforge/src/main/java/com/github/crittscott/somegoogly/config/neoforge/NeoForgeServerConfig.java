package com.github.crittscott.somegoogly.config.neoforge;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.server.ServerServices;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** NeoForge-native world server configuration backed by the shared runtime values. */
public final class NeoForgeServerConfig {

    /** Copies the loaded file's values into {@link ServerConfig}. */
    private static final Runnable COPY_VALUES;
    /** Native schema used to load and correct this loader's world server file. */
    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        COPY_VALUES = NeoForgeConfig.define(builder, ServerConfig.settings());
        SPEC = builder.build();
    }

    private NeoForgeServerConfig() {
    }

    /** Register the SERVER config spec and copy its values into {@link ServerConfig} on load and reload. */
    public static void register(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SPEC, ServerConfig.FILE_NAME);
        modBus.addListener(NeoForgeServerConfig::onConfigChanged);
    }

    private static void applyValues() {
        COPY_VALUES.run();
        ServerServices.onServerConfigApplied();
    }

    private static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        if (event instanceof ModConfigEvent.Unloading) {
            ServerConfig.resetDefaults();
            return;
        }
        // A reload arrives off the server thread from the file watcher, and can find clients connected, so the
        // whole change is handed to the server thread: no tick sees it half copied.
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (event instanceof ModConfigEvent.Reloading && server != null) {
            server.execute(() -> {
                applyValues();
                ServerServices.broadcastEyeSyncIfChanged(server);
            });
        } else {
            applyValues();
        }
    }
}
