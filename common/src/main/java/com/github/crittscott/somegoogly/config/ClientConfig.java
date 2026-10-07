package com.github.crittscott.somegoogly.config;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Client-local rendering preferences shared across loaders. */
public final class ClientConfig {

    // TOML file and section names, shared by every loader's client config.
    public static final String FILE_NAME = SomeGooglyCommon.MOD_ID + "-client.toml";
    public static final String SECTION = "client";

    public static final String DISABLE_GOOGLY_EYES_KEY = "disableGooglyEyes";
    public static final boolean DISABLE_GOOGLY_EYES_DEFAULT = false;
    public static final String DISABLED_ENTITIES_KEY = "disabledEntities";
    public static final List<String> DISABLED_ENTITIES_DEFAULT = List.of();
    public static final String DISABLED_MODS_KEY = "disabledMods";
    public static final List<String> DISABLED_MODS_DEFAULT = List.of();

    public static final String DISABLE_GOOGLY_EYES_COMMENT = "Disable display of all googly eyes on this client.";
    public static final String DISABLED_ENTITIES_COMMENT = "Entity ids that should not display googly eyes";
    public static final String DISABLED_MODS_COMMENT = "Mod namespaces whose entities should not display googly eyes";

    public static final ConfigValue<Boolean> DISABLE_GOOGLY_EYES = ConfigValue.bool(DISABLE_GOOGLY_EYES_DEFAULT);
    public static final ConfigValue.Parsed<Set<ResourceLocation>> DISABLED_ENTITIES = ConfigValue.parsedStrings(
            DISABLED_ENTITIES_DEFAULT, ServerConfig::validateResourceLocation,
            entries -> entries.stream().map(ResourceLocation::parse).collect(Collectors.toUnmodifiableSet()));
    public static final ConfigValue.Parsed<Set<String>> DISABLED_MODS = ConfigValue.parsedStrings(
            DISABLED_MODS_DEFAULT, ServerConfig::validateNamespace, Set::copyOf);

    private ClientConfig() {
    }

    /** Whether the client hides eyes on this entity type, by its id or its namespace. */
    public static boolean isEntityDisabled(ResourceLocation entityType) {
        return DISABLED_MODS.parsed().contains(entityType.getNamespace())
                || DISABLED_ENTITIES.parsed().contains(entityType);
    }

    /** Restore built-in defaults before a loader applies values from disk. */
    public static void resetDefaults() {
        DISABLE_GOOGLY_EYES.reset();
        DISABLED_ENTITIES.reset();
        DISABLED_MODS.reset();
    }
}
