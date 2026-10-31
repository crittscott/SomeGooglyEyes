package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.resources.Identifier;

/**
 * The mod's network version and its six payload ids. Sends go straight through the loader bridges,
 * {@link com.github.crittscott.somegoogly.platform.Networking} and
 * {@link com.github.crittscott.somegoogly.platform.ClientNetworking}.
 */
public final class NetworkHandler {

    /** Bumped whenever any payload becomes wire-incompatible. */
    public static final String NETWORK_VERSION = "17";

    public static final Identifier EYE_STATE = versioned("eye_state");
    public static final Identifier EYE_CONFIG = versioned("eye_config");
    public static final Identifier EYE_BEHAVIOR = versioned("eye_behavior");
    public static final Identifier EYE_ANGER = versioned("eye_anger");
    public static final Identifier PICKER_FREEZE = versioned("picker_freeze");
    public static final Identifier PICKER_EXPORT = versioned("picker_export");

    private NetworkHandler() {
    }

    /** A mod-namespaced id under the current network version. */
    public static Identifier versioned(String path) {
        return Identifier.fromNamespaceAndPath(
                SomeGooglyCommon.MOD_ID, "v" + NETWORK_VERSION + "/" + path);
    }
}
