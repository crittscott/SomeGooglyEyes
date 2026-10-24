package com.github.crittscott.somegoogly.server;

import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.picker.PickerFreezeService;
import com.github.crittscott.somegoogly.picker.PickerGate;
import com.github.crittscott.somegoogly.platform.Networking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Loader-neutral server lifecycle hooks that coordinate several services. A hook that concerns only one
 * service is called on that service directly (behavior ticks, tracking, and reactions go to
 * {@link ServerBehaviorScheduler}).
 */
public final class ServerServices {

    /**
     * The definitions and master switch connected clients last received, or {@code null} before the first
     * send since server start. Read and written on the server thread only.
     */
    @Nullable
    private static Map<ResourceLocation, RuntimeConfigSet> sentEyeConfigs;
    private static boolean sentGooglyEyesEnabled;

    private ServerServices() {
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

    /** Clear every server-lifetime service before the server instance is discarded. */
    public static void onServerStopping(MinecraftServer server) {
        ServerBehaviorScheduler.clear();
        PickerFreezeService.onServerStopping(server);
        PickerGate.onServerStopping();
        ServerEyeConfigs.onServerStopping();
        sentEyeConfigs = null;
    }

    /**
     * Send the entity's full eye snapshot before registering the new watcher with behavior scheduling,
     * whose registration may immediately send a mid-behavior catch-up packet.
     */
    public static void onStartTracking(LivingEntity living, ServerPlayer player) {
        EyeState.sendTo(living, player);
        ServerBehaviorScheduler.onStartTracking(living, player);
    }

    /** Send the current resolved eye definitions and master switch to a joining player. */
    public static void syncEyeConfigs(ServerPlayer player) {
        if (sentEyeConfigs == null) {
            // Nobody has received a view yet, so every connected client now holds this one.
            recordSentEyeConfigs();
        }
        Networking.sendToPlayer(player, eyeConfigPacket());
    }

    /**
     * Send the current resolved eye definitions and master switch to every player, but only when either
     * differs from what clients last received. Runs once after each datapack reload and each server-config
     * apply, on the server thread; an unchanged reload sends nothing, so clients keep their eye motion.
     */
    public static void broadcastEyeConfigsIfChanged(MinecraftServer server) {
        if (sentEyeConfigs == ServerEyeConfigs.all()
                && sentGooglyEyesEnabled == ServerConfig.GOOGLY_EYES_ENABLED.get()) {
            return;
        }
        recordSentEyeConfigs();
        EyeConfigSyncPacket packet = eyeConfigPacket();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Networking.sendToPlayer(player, packet);
        }
    }

    private static void recordSentEyeConfigs() {
        sentEyeConfigs = ServerEyeConfigs.all();
        sentGooglyEyesEnabled = ServerConfig.GOOGLY_EYES_ENABLED.get();
    }

    private static EyeConfigSyncPacket eyeConfigPacket() {
        return new EyeConfigSyncPacket(ServerEyeConfigs.all(), ServerEyeConfigs.encoded(),
                ServerConfig.GOOGLY_EYES_ENABLED.get());
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
