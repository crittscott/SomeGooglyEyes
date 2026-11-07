package com.github.crittscott.somegoogly.config.forge;

import com.github.crittscott.somegoogly.config.ConfigValue;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.server.ServerServices;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

/** Forge-native world server configuration backed by the shared runtime values. */
public final class ForgeServerConfig {

    /** Copies the loaded file's values into {@link ServerConfig}. */
    private static final Runnable COPY_VALUES;
    /** Native schema used to load and correct this loader's world server file. */
    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        COPY_VALUES = define(builder, ServerConfig.settings());
        SPEC = builder.build();
    }

    private ForgeServerConfig() {
    }

    /**
     * Declare {@code settings} on {@code builder}, section by section, returning the step that copies the loaded
     * file's values into them.
     */
    static Runnable define(ForgeConfigSpec.Builder builder, List<ConfigValue<?>> settings) {
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
                ForgeConfigSpec.BooleanValue spec = builder.define(flag.key(), flag.defaultValue().booleanValue());
                copies.add(() -> flag.set(spec.get()));
            } else if (setting instanceof ConfigValue.Ranged ranged) {
                ForgeConfigSpec.IntValue spec = builder.defineInRange(ranged.key(),
                        ranged.defaultValue().intValue(), ranged.min(), ranged.max());
                copies.add(() -> ranged.set(spec.get()));
            } else if (setting instanceof ConfigValue.Parsed<?> list) {
                ForgeConfigSpec.ConfigValue<List<? extends String>> spec = builder.defineList(list.key(),
                        list.defaultValue(), value -> value instanceof String string && list.valid().test(string));
                copies.add(() -> list.set(new ArrayList<>(spec.get())));
            }
        }
        builder.pop();
        return () -> copies.forEach(Runnable::run);
    }

    /** Register the SERVER config spec and copy its values into {@link ServerConfig} on load and reload. */
    public static void register(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.SERVER, SPEC, ServerConfig.FILE_NAME);
        BusGroup modBusGroup = context.getModBusGroup();
        ModConfigEvent.Loading.getBus(modBusGroup).addListener(ForgeServerConfig::onConfigChanged);
        ModConfigEvent.Reloading.getBus(modBusGroup).addListener(ForgeServerConfig::onConfigChanged);
        ModConfigEvent.Unloading.getBus(modBusGroup).addListener(ForgeServerConfig::onConfigChanged);
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
