package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.picker.PickerInput;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.config.ModVersionLookup;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Per-tick and disconnect housekeeping for client-side eye state, shared by every loader's client hooks. */
public final class ClientLifecycle {

    /** The mods with a dedicated client integration, which a player's log should show as found or not. */
    private static final List<String> INTEGRATIONS = List.of("citadel", "geckolib", "llibrary", "uranus");

    private ClientLifecycle() {
    }

    /** Record that client setup finished and which integrations are in play; the last step of each loader's setup. */
    public static void logStartup() {
        SomeGooglyCommon.LOGGER.info("{} client setup finished; optional integrations found: {}",
                SomeGooglyCommon.MOD_NAME,
                INTEGRATIONS.stream().filter(id -> ModVersionLookup.modVersion(id).isPresent()).toList());
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
        GuiItemEyes.tick();
        HandItemEyes.tick();
        ClientEyeRuntime.tick(minecraft.level, minecraft.level != null && !minecraft.isPaused());
        if (!minecraft.isPaused()) {
            ClientEyeRuntime.tickHeldEye(minecraft.player);
        }
        EyeInspector.tick();
        PickerInput.consumePendingKeys();
    }
}
