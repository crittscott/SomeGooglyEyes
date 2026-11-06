package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.resources.ResourceLocation;

/**
 * The mod's network version and its seven payload ids. Sends go straight through the loader bridges,
 * {@link com.github.crittscott.somegoogly.platform.Networking} and
 * {@link com.github.crittscott.somegoogly.platform.ClientNetworking}.
 */
public final class NetworkHandler {

    /** Bumped whenever any payload becomes wire-incompatible. */
    public static final String NETWORK_VERSION = "18";

    public static final ResourceLocation EYE_STATE = id("eye_state");
    public static final ResourceLocation EYE_CONFIG = id("eye_config");
    public static final ResourceLocation EYE_SETTINGS = id("eye_settings");
    public static final ResourceLocation EYE_BEHAVIOR = id("eye_behavior");
    public static final ResourceLocation EYE_ANGER = id("eye_anger");
    public static final ResourceLocation PICKER_FREEZE = id("picker_freeze");
    public static final ResourceLocation PICKER_EXPORT = id("picker_export");

    private NetworkHandler() {
    }

    /** A mod-namespaced channel id. */
    public static ResourceLocation id(String path) {
        return new ResourceLocation(SomeGooglyCommon.MOD_ID, path);
    }
}
