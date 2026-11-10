package com.github.crittscott.somegoogly.config.forge;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.server.ServerServices;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Forge-native world server configuration backed by the shared runtime values. */
public final class ForgeServerConfig {

    /** Copies the loaded file's values into {@link ServerConfig}. */
    private static final Runnable COPY_VALUES;
    /** Native schema used to load and correct this loader's world server file. */
    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        COPY_VALUES = ForgeConfig.define(builder, ServerConfig.settings());
        SPEC = builder.build();
    }

    private ForgeServerConfig() {
    }

    /** Register the SERVER config spec and copy its values into {@link ServerConfig} on load and reload. */
    public static void register(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.SERVER, SPEC, ServerConfig.FILE_NAME);
        context.getModEventBus().addListener(ForgeServerConfig::onConfigChanged);
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
