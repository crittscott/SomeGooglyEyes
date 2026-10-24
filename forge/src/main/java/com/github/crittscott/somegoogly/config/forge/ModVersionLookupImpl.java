package com.github.crittscott.somegoogly.config.forge;

import net.minecraftforge.fml.ModList;

import java.util.Optional;

/** Forge implementation of the loaded-mod version lookup. */
public final class ModVersionLookupImpl {

    private ModVersionLookupImpl() {
    }

    public static Optional<String> modVersion(String modId) {
        return ModList.get()
                .getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString());
    }
}
