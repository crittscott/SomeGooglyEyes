package com.github.crittscott.somegoogly.config.neoforge;

import com.github.crittscott.somegoogly.config.ConfigValue;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.server.ServerServices;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

/** NeoForge-native world server configuration backed by the shared runtime values. */
public final class NeoForgeServerConfig {

    /** Copies the loaded file's values into {@link ServerConfig}. */
    private static final Runnable COPY_VALUES;
    /** Native schema used to load and correct this loader's world server file. */
    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        COPY_VALUES = define(builder, ServerConfig.settings());
        SPEC = builder.build();
    }

    private NeoForgeServerConfig() {
    }

    /**
     * Declare {@code settings} on {@code builder}, section by section, returning the step that copies the loaded
     * file's values into them.
     */
    static Runnable define(ModConfigSpec.Builder builder, List<ConfigValue<?>> settings) {
        List<Runnable> copies = new ArrayList<>();
        String section = null;
        for (ConfigValue<?> setting : settings) {
            if (!setting.section().equals(section)) {
                if (section != null) {
                    builder.pop();
                }
                section = setting.section();
                builder.push(section);
            }
            builder.comment(setting.comment().split("\n"));
            if (setting instanceof ConfigValue.Flag flag) {
                ModConfigSpec.BooleanValue spec = builder.define(flag.key(), flag.defaultValue().booleanValue());
                copies.add(() -> flag.set(spec.get()));
            } else if (setting instanceof ConfigValue.Ranged ranged) {
                ModConfigSpec.IntValue spec = builder.defineInRange(ranged.key(),
                        ranged.defaultValue().intValue(), ranged.min(), ranged.max());
                copies.add(() -> ranged.set(spec.get()));
            } else if (setting instanceof ConfigValue.Parsed<?> list) {
                ModConfigSpec.ConfigValue<List<? extends String>> spec = builder.defineList(list.key(),
                        list.defaultValue(), () -> "", value -> value instanceof String string && list.valid().test(string));
                copies.add(() -> list.set(new ArrayList<>(spec.get())));
            }
        }
        builder.pop();
        return () -> copies.forEach(Runnable::run);
    }

    /** Register the SERVER config spec and copy its values into {@link ServerConfig} on load and reload. */
    public static void register(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SPEC, ServerConfig.FILE_NAME);
        modBus.addListener(NeoForgeServerConfig::onConfigChanged);
    }

    private static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        if (event instanceof ModConfigEvent.Unloading) {
            ServerConfig.resetDefaults();
            return;
        }
        COPY_VALUES.run();
        ServerServices.onServerConfigApplied();
        // Only a reload can find clients connected; it arrives off the server thread from the file watcher.
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (event instanceof ModConfigEvent.Reloading && server != null) {
            server.execute(() -> ServerServices.broadcastEyeSyncIfChanged(server));
        }
    }
}
