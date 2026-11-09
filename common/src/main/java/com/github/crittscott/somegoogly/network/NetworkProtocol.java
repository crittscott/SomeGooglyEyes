package com.github.crittscott.somegoogly.network;

/**
 * The mod's network version. Sends go straight through the loader bridges,
 * {@link com.github.crittscott.somegoogly.platform.Networking} and
 * {@link com.github.crittscott.somegoogly.platform.ClientNetworking}.
 */
public final class NetworkProtocol {

    /** Bumped whenever any payload becomes wire-incompatible. */
    public static final String NETWORK_VERSION = "20";

    private NetworkProtocol() {
    }
}
