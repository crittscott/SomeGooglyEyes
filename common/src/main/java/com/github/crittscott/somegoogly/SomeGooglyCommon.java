package com.github.crittscott.somegoogly;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod identity shared by every platform. Kept separate from the platform-specific mod entry point
 * class so common code never needs to depend on it.
 */
public final class SomeGooglyCommon {
    public static final String MOD_ID = "somegoogly";
    public static final String MOD_NAME = "Some Googly Eyes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private SomeGooglyCommon() {
    }

    /** An internal resource identifier in this mod's namespace. */
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

}
