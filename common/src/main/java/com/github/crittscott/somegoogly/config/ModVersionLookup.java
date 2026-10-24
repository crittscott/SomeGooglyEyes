package com.github.crittscott.somegoogly.config;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** Resolves the loaded version string for a config namespace. */
public final class ModVersionLookup {

    private ModVersionLookup() {
    }

    /** The running Minecraft version for {@code minecraft}, otherwise the version of the loaded mod with that id. */
    public static Optional<String> versionForNamespace(String namespace) {
        if (ResourceLocation.DEFAULT_NAMESPACE.equals(namespace)) {
            return Optional.of(SharedConstants.getCurrentVersion().getName());
        }
        return modVersion(namespace);
    }

    /** The version of the loaded mod {@code modId}, or empty when it is not loaded. */
    @ExpectPlatform
    public static Optional<String> modVersion(String modId) {
        throw new AssertionError();
    }
}
