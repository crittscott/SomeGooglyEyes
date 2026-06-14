package com.github.crittscott.somegoogly.config;

import net.minecraft.SharedConstants;
import net.minecraftforge.fml.ModList;

import java.util.Optional;

/** Resolves the loaded version string for a config namespace. */
public final class ModVersionLookup {

    private ModVersionLookup() {
    }

    public static Optional<String> versionForNamespace(String namespace) {
        if ("minecraft".equals(namespace)) {
            return Optional.of(SharedConstants.getCurrentVersion().getName());
        }
        return ModList.get()
                .getModContainerById(namespace)
                .map(container -> container.getModInfo().getVersion().toString());
    }
}
