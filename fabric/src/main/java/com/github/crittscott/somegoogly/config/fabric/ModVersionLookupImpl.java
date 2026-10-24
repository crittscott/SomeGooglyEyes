package com.github.crittscott.somegoogly.config.fabric;

import net.fabricmc.loader.api.FabricLoader;

import java.util.Optional;

/** Fabric implementation of the loaded-mod version lookup. */
public final class ModVersionLookupImpl {

    private ModVersionLookupImpl() {
    }

    public static Optional<String> modVersion(String modId) {
        return FabricLoader.getInstance()
                .getModContainer(modId)
                .map(container -> container.getMetadata().getVersion().getFriendlyString());
    }
}
