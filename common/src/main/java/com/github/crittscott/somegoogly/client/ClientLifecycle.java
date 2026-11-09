package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.compat.GeckoCompat;
import com.github.crittscott.somegoogly.client.picker.PickerInput;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.client.render.resolver.Resolvers;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

/** Per-tick and disconnect housekeeping for client-side eye state, shared by every loader's client hooks. */
public final class ClientLifecycle {

    private ClientLifecycle() {
    }

    /**
     * Record that client setup finished and which optional model libraries the eyes attach through, as decided
     * by the same checks that enable them; called from each loader's client-setup milestone.
     */
    public static void logStartup() {
        List<String> libraries = new ArrayList<>(Resolvers.availableFamilies());
        if (GeckoCompat.isLoaded()) {
            libraries.add("GeckoLib");
        }
        SomeGooglyCommon.LOGGER.info("{} client setup finished; optional model libraries found: {}",
                SomeGooglyCommon.MOD_NAME, libraries);
    }

    /** Drop all per-connection client eye state when leaving a world or server. */
    public static void onDisconnect() {
        ClientEyeConfigs.clear();
        ClientEyeRuntime.onLevelChanged();
        PickerState.resetOnDisconnect();
    }

    /** Advance the pupil/behavior runtime, held-eye pupil, inspector, and picker key input. */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientEyeRuntime.tick(minecraft.level, minecraft.level != null && !minecraft.isPaused(), minecraft.screen);
        EyeInspector.tick();
        PickerInput.consumePendingKeys();
    }
}
