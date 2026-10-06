package com.github.crittscott.somegoogly.config.neoforge;

import net.neoforged.fml.ModList;

import java.util.Optional;

/** NeoForge implementation of the loaded-mod version lookup. */
public final class ModVersionLookupImpl {

    private ModVersionLookupImpl() {
    }

    public static Optional<String> modVersion(String modId) {
        return ModList.get()
                .getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString());
    }
}
