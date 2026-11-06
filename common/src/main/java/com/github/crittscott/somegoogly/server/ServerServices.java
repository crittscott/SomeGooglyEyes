package com.github.crittscott.somegoogly.server;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.BoatEyeLayout;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.EyeSettingsPacket;
import com.github.crittscott.somegoogly.picker.PickerFreezeService;
import com.github.crittscott.somegoogly.picker.PickerGate;
import com.github.crittscott.somegoogly.platform.Networking;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * Loader-neutral server lifecycle hooks that coordinate several services. A hook that concerns only one
 * service is called on that service directly (behavior ticks, tracking, and reactions go to
 * {@link ServerBehaviorScheduler}).
 */
public final class ServerServices {

    @Nullable
    private static String loggedConfig;

    /**
     * The definitions connected clients last received, or {@code null} before the first send since server
     * start. Read and written on the server thread only.
     */
    @Nullable
    private static Map<ResourceLocation, RuntimeConfigSet> sentEyeConfigs;
    /** The settings connected clients last received, on the same terms as {@link #sentEyeConfigs}. */
    @Nullable
    private static EyeSettingsPacket sentSettings;

    private ServerServices() {
    }

    private static void applyGooglyDecision(LivingEntity living) {
        boolean hasGooglyEyes = false;
        ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        RandomSource random = living.getRandom();

        if (!(living instanceof Player) && ServerConfig.GOOGLY_EYES_ENABLED.get()
                && ServerEyeConfigs.canEverWearEyes(living)) {
            int percent = ServerConfig.percentFor(entityType);
            hasGooglyEyes = random.nextFloat() < (percent / (float) ServerConfig.PERCENT_MAX);
        }

        EyeState.initialize(living, hasGooglyEyes, random.nextFloat());
    }

    /**
     * Send every player the eye definitions and the settings, each only when it differs from what clients last
     * received. Runs once after each datapack reload and each server-config apply, on the server thread. New
     * definitions restart client eye motion, because trackers are built from them; new settings don't.
     */
    public static void broadcastEyeSyncIfChanged(MinecraftServer server) {
        if (sentEyeConfigs != ServerEyeConfigs.all()) {
            sentEyeConfigs = ServerEyeConfigs.all();
            Networking.sendToAll(server, eyeConfigPacket());
        }
        EyeSettingsPacket settings = settingsPacket();
        if (!settings.equals(sentSettings)) {
            sentSettings = settings;
            Networking.sendToAll(server, settings);
        }
    }

    private static EyeConfigSyncPacket eyeConfigPacket() {
        return new EyeConfigSyncPacket(ServerEyeConfigs.all(), ServerEyeConfigs.encoded());
    }

    /**
     * Initialize persistent eye state once for a server-side living entity, then reconcile any picker
     * freeze marker after that state is available. A player joining a level (login, respawn, dimension
     * change) gets their own snapshot: their client has just built a fresh local player, and a player is
     * never their own tracker, so the start-tracking sync doesn't cover them.
     */
    public static void onLivingEntityLoaded(LivingEntity living) {
        if (!EyeState.isInitialized(living)) {
            applyGooglyDecision(living);
        }
        if (living instanceof ServerPlayer player) {
            EyeState.sendTo(player, player);
        }
        if (living instanceof Mob mob) {
            PickerFreezeService.onMobJoin(mob);
        }
    }

    /** Release all per-player picker state when a server player leaves. */
    public static void onPlayerLeft(ServerPlayer player) {
        PickerFreezeService.onPlayerLoggedOut(player);
        PickerGate.onPlayerLeft(player.getUUID());
    }

    /** Record the settings that decide who gets eyes, once at startup and whenever they change. */
    public static void onServerConfigApplied() {
        String applied = "googlyEyesEnabled=%s, globalPercent=%d, entityOverrides=%d".formatted(
                ServerConfig.GOOGLY_EYES_ENABLED.get(), ServerConfig.GLOBAL_PERCENT.get(),
                ServerConfig.ENTITY_OVERRIDES.parsed().size());
        if (!applied.equals(loggedConfig)) {
            SomeGooglyCommon.LOGGER.info("Server configuration applied ({})", applied);
            loggedConfig = applied;
        }
    }

    /** Clear every server-lifetime service before the server instance is discarded. */
    public static void onServerStopping(MinecraftServer server) {
        ServerBehaviorScheduler.clear();
        PickerFreezeService.onServerStopping(server);
        PickerGate.onServerStopping();
        ServerEyeConfigs.onServerStopping();
        loggedConfig = null;
        sentEyeConfigs = null;
        sentSettings = null;
    }

    /**
     * Send the entity's full eye snapshot before registering the new watcher with behavior scheduling,
     * whose registration may immediately send a mid-behavior catch-up packet.
     */
    public static void onStartTracking(Entity entity, ServerPlayer player) {
        if (entity instanceof LivingEntity living) {
            EyeState.sendTo(living, player);
            ServerBehaviorScheduler.onStartTracking(living, player);
        } else if (BoatEyeLayout.supports(entity)) {
            EyeState.sendTo(entity, player);
        }
    }

    private static EyeSettingsPacket settingsPacket() {
        return new EyeSettingsPacket(ServerConfig.GOOGLY_EYES_ENABLED.get(), BehaviorSettings.fromConfig());
    }

    /** Send a joining player the current resolved eye definitions and settings. */
    public static void syncJoiningPlayer(ServerPlayer player) {
        EyeSettingsPacket settings = settingsPacket();
        if (sentEyeConfigs == null) {
            // Nobody has received a view yet, so every connected client now holds this one.
            sentEyeConfigs = ServerEyeConfigs.all();
            sentSettings = settings;
        }
        Networking.sendToPlayer(player, eyeConfigPacket());
        Networking.sendToPlayer(player, settings);
    }
}
