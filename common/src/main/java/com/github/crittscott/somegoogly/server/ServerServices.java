package com.github.crittscott.somegoogly.server;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.eye.state.EyeStateSync;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.picker.PickerExportService;
import com.github.crittscott.somegoogly.picker.PickerFreezeService;
import com.github.crittscott.somegoogly.picker.PickerRequestLimiter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/** Loader-neutral implementation behind Forge and Fabric server lifecycle hooks. */
public final class ServerServices {

    private ServerServices() {
    }

    public static void onLivingEntityLoaded(LivingEntity living) {
        if (!EyeState.isInitialized(living)) {
            applyGooglyDecision(living);
        }
        if (living instanceof Mob mob) {
            PickerFreezeService.onMobJoin(mob);
        }
    }

    public static void onPlayerJoined(ServerPlayer player) {
        NetworkHandler.beginHandshake(player);
    }

    public static void onPlayerLeft(ServerPlayer player) {
        NetworkHandler.playerLeft(player);
        PickerFreezeService.onPlayerLoggedOut(player);
        PickerExportService.onPlayerLeft(player.getUUID());
        PickerRequestLimiter.onPlayerLeft(player.getUUID());
    }

    public static void onServerStopping(MinecraftServer server) {
        ServerBehaviorScheduler.clear();
        PickerFreezeService.onServerStopping(server);
        PickerExportService.onServerStopping();
        PickerRequestLimiter.onServerStopping();
        NetworkHandler.serverStopped();
    }

    public static void onServerTick(MinecraftServer server) {
        NetworkHandler.tickHandshake(server);
        ServerBehaviorScheduler.serverTick();
    }

    public static void onStartTracking(LivingEntity living, ServerPlayer player) {
        EyeStateSync.sendTo(living, player);
        ServerBehaviorScheduler.onStartTracking(living, player);
    }

    public static void onStopTracking(LivingEntity living) {
        ServerBehaviorScheduler.onStopTracking(living);
    }

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
            hasGooglyEyes = random.nextFloat() < (percent / 100F);
        }

        EyeState.initialize(living, hasGooglyEyes, random.nextFloat());
    }
}
