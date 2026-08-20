package com.github.crittscott.somegoogly.config.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;

import java.util.Optional;

/** Fabric implementation of the loaded-mod version lookup. */
public final class ModVersionLookupImpl {

    private ModVersionLookupImpl() {
    }

    public static Optional<String> versionForNamespace(String namespace) {
        if ("minecraft".equals(namespace)) {
            return Optional.of(SharedConstants.getCurrentVersion().getName());
        }
        return FabricLoader.getInstance()
                .getModContainer(namespace)
                .map(container -> container.getMetadata().getVersion().getFriendlyString());
    }
}
