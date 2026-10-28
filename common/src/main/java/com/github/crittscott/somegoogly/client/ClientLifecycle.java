package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.client.picker.PickerInput;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import net.minecraft.client.Minecraft;

/** Per-tick and disconnect housekeeping for client-side eye state, shared by every loader's client hooks. */
public final class ClientLifecycle {

    private ClientLifecycle() {
    }

    /** Drop all per-connection client eye state when leaving a world or server. */
    public static void onDisconnect() {
        ClientEyeConfigs.clear();
        ClientEyeRuntime.onLevelChanged();
        GooglyEyeItemRenderer.reset();
        PickerState.resetOnDisconnect();
    }

    /** Advance the pupil/behavior runtime, held-eye pupil, inspector, and picker key input. */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientEyeRuntime.tick(minecraft.level != null && !minecraft.isPaused());
        GooglyEyeItemRenderer.tick();
        EyeInspector.tick();
        PickerInput.consumePendingKeys();
    }
}
