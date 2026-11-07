package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Client-local rendering preferences shared across loaders. */
public final class ClientConfig {

    // TOML file and section names, shared by every loader's client config. The section name is the one the
    // previous release's file uses, so an existing file keeps its values when Forge or NeoForge corrects it.
    public static final String FILE_NAME = SomeGooglyCommon.MOD_ID + "-client.toml";
    public static final String SECTION = "Client Settings";

    // Precedes the settings, whose initializers fill it.
    private static final List<ConfigValue<?>> SETTINGS = new ArrayList<>();

    // Declaration order is file order.
    public static final ConfigValue<Boolean> DISABLE_GOOGLY_EYES = add(ConfigValue.bool(
            SECTION, "disableGooglyEyes", false, "Disable display of all googly eyes on this client."));
    public static final ConfigValue.Parsed<Set<ResourceLocation>> DISABLED_ENTITIES = add(ConfigValue.parsedStrings(
            SECTION, "disabledEntities", List.of(), ServerConfig::validateResourceLocation,
            entries -> entries.stream().map(ResourceLocation::parse).collect(Collectors.toUnmodifiableSet()),
            "Entity ids that should not display googly eyes"));
    public static final ConfigValue.Parsed<Set<String>> DISABLED_MODS = add(ConfigValue.parsedStrings(
            SECTION, "disabledMods", List.of(), ServerConfig::validateNamespace, Set::copyOf,
            "Mod namespaces whose entities should not display googly eyes"));

    @Nullable
    private static String loggedConfig;

    private ClientConfig() {
    }

    private static <V extends ConfigValue<?>> V add(V setting) {
        SETTINGS.add(setting);
        return setting;
    }

    /** Whether the client hides eyes on this entity type, by its id or its namespace. */
    public static boolean isEntityDisabled(ResourceLocation entityType) {
        return DISABLED_MODS.parsed().contains(entityType.getNamespace())
                || DISABLED_ENTITIES.parsed().contains(entityType);
    }

    /** Record the settings that hide eyes on this client, once at load and whenever they change. */
    public static void onApplied() {
        String applied = "disableGooglyEyes=%s, disabledEntities=%d, disabledMods=%d".formatted(
                DISABLE_GOOGLY_EYES.get(), DISABLED_ENTITIES.parsed().size(), DISABLED_MODS.parsed().size());
        if (!applied.equals(loggedConfig)) {
            SomeGooglyCommon.LOGGER.info("Client configuration applied ({})", applied);
            loggedConfig = applied;
        }
    }

    /** Every setting, in the order of the client config file. */
    public static List<ConfigValue<?>> settings() {
        return Collections.unmodifiableList(SETTINGS);
    }
}
