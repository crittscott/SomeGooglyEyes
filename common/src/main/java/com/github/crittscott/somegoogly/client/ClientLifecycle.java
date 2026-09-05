package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.client.picker.PickerInput;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;

/** Per-tick and disconnect housekeeping for client-side eye state, shared by every loader's client hooks. */
public final class ClientLifecycle {

    private ClientLifecycle() {
    }

    /** Advance the pupil/behavior runtime, inspector, and picker key input. */
    public static void tick() {
        ClientEyeRuntime.tick();
        EyeInspector.tick();
        PickerInput.consumePendingKeys();
    }

    /** Drop all per-connection client eye state when leaving a world or server. */
    public static void onDisconnect() {
        ClientNetworkHandler.clearPendingEyeStates();
        ClientEyeConfigs.clear();
        ClientEyeRuntime.clear();
        PickerState.resetOnDisconnect();
    }
}
