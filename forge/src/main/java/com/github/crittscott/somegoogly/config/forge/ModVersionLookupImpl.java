package com.github.crittscott.somegoogly.config.forge;

import net.minecraft.SharedConstants;
import net.minecraftforge.fml.ModList;

import java.util.Optional;

/** Forge implementation of the loaded-mod version lookup. */
public final class ModVersionLookupImpl {

    private ModVersionLookupImpl() {
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
