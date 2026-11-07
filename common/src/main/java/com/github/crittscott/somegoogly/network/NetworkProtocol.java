package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.resources.Identifier;

/**
 * The mod's network version and the namespace of its payload ids. Sends go straight through the loader bridges,
 * {@link com.github.crittscott.somegoogly.platform.Networking} and
 * {@link com.github.crittscott.somegoogly.platform.ClientNetworking}.
 */
public final class NetworkProtocol {

    /** Bumped whenever any payload becomes wire-incompatible. */
    public static final String NETWORK_VERSION = "18";

    private NetworkProtocol() {
    }

    /** A mod-namespaced payload id. */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, path);
    }
}
