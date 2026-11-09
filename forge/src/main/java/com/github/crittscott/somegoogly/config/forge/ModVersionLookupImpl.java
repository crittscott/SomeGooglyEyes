package com.github.crittscott.somegoogly.config.forge;

import java.util.Optional;
import net.minecraftforge.fml.ModList;

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
