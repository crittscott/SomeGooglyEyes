package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.compat.GeckoCompat;
import com.github.crittscott.somegoogly.client.picker.PickerInput;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.client.render.resolver.Resolvers;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/** Per-tick and disconnect housekeeping for client-side eye state, shared by every loader's client hooks. */
public final class ClientLifecycle {

    private ClientLifecycle() {
    }

    /** Record that client setup finished and which optional integrations are in play; called once per loader. */
    public static void logStartup() {
        List<String> integrations = new ArrayList<>(Resolvers.availableFamilies());
        if (GeckoCompat.isLoaded()) {
            integrations.add("GeckoLib");
        }
        SomeGooglyCommon.LOGGER.info("{} client setup finished; optional integrations found: {}",
                SomeGooglyCommon.MOD_NAME, integrations);
    }

    /** Drop all per-connection client eye state when leaving a world or server. */
    public static void onDisconnect() {
        ClientEyeConfigs.clear();
        ClientEyeRuntime.onLevelChanged();
        PickerState.resetOnDisconnect();
    }

    /** Advance the eye runtime, inspector, and picker key input. */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientEyeRuntime.tick(minecraft.level, minecraft.level != null && !minecraft.isPaused(), minecraft.gui.screen());
        EyeInspector.tick();
        PickerInput.consumePendingKeys();
    }
}
