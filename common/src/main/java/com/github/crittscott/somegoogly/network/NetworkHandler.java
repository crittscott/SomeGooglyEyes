package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.resources.ResourceLocation;

/**
 * The mod's network version and its five payload ids. Sends go straight through the loader bridges,
 * {@link com.github.crittscott.somegoogly.platform.Networking} and
 * {@link com.github.crittscott.somegoogly.platform.ClientNetworking}.
 */
public final class NetworkHandler {

    /** Bumped whenever any payload becomes wire-incompatible. */
    public static final String NETWORK_VERSION = "13";

    public static final ResourceLocation EYE_STATE = versioned("eye_state");
    public static final ResourceLocation EYE_CONFIG = versioned("eye_config");
    public static final ResourceLocation EYE_BEHAVIOR = versioned("eye_behavior");
    public static final ResourceLocation PICKER_FREEZE = versioned("picker_freeze");
    public static final ResourceLocation PICKER_EXPORT = versioned("picker_export");

    private NetworkHandler() {
    }

    private static ResourceLocation versioned(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                SomeGooglyCommon.MOD_ID, "v" + NETWORK_VERSION + "/" + path);
    }
}
