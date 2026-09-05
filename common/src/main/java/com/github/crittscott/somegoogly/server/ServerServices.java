package com.github.crittscott.somegoogly.server;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.eye.state.EyeStateSync;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.picker.PickerExportService;
import com.github.crittscott.somegoogly.picker.PickerFreezeService;
import com.github.crittscott.somegoogly.picker.PickerGate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/** Loader-neutral implementation behind each loader's server lifecycle hooks. */
public final class ServerServices {

    private ServerServices() {
    }

    /**
     * Initialize persistent eye state once for a server-side living entity, then reconcile any picker
     * freeze marker after that state is available.
     */
    public static void onLivingEntityLoaded(LivingEntity living) {
        if (!EyeState.isInitialized(living)) {
            applyGooglyDecision(living);
        }
        if (living instanceof Mob mob) {
            PickerFreezeService.onMobJoin(mob);
        }
    }

    /** Begin protocol negotiation for a newly joined server player. */
    public static void onPlayerJoined(ServerPlayer player) {
        NetworkHandler.beginHandshake(player);
    }

    /** Release all per-player networking and picker state when a server player leaves. */
    public static void onPlayerLeft(ServerPlayer player) {
        NetworkHandler.playerLeft(player);
        PickerFreezeService.onPlayerLoggedOut(player);
        PickerExportService.onPlayerLeft(player.getUUID());
        PickerGate.onPlayerLeft(player.getUUID());
    }

    /** Clear every server-lifetime service before the server instance is discarded. */
    public static void onServerStopping(MinecraftServer server) {
        ServerBehaviorScheduler.clear();
        PickerFreezeService.onServerStopping(server);
        PickerExportService.onServerStopping();
        PickerGate.onServerStopping();
        ServerEyeConfigs.onServerStopping();
        NetworkHandler.serverStopped();
    }

    /** Advance handshake timeouts and behavior scheduling once at the end of a server tick. */
    public static void onServerTick(MinecraftServer server) {
        NetworkHandler.tickHandshake(server);
        ServerBehaviorScheduler.serverTick();
    }

    /**
     * Send the entity's full eye snapshot before registering the new watcher with behavior scheduling,
     * whose registration may immediately send a mid-behavior catch-up packet.
     */
    public static void onStartTracking(LivingEntity living, ServerPlayer player) {
        EyeStateSync.sendTo(living, player);
        ServerBehaviorScheduler.onStartTracking(living, player);
    }

    /** Remove one watcher from the entity's server-side behavior schedule. */
    public static void onStopTracking(LivingEntity living) {
        ServerBehaviorScheduler.onStopTracking(living);
    }

    /** Send current resolved eye definitions after login or reload, once the handshake is ready. */
    public static void syncEyeConfigs(ServerPlayer player) {
        if (NetworkHandler.ready(player)) {
            NetworkHandler.sendConfig(player);
        }
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
}
