package com.github.crittscott.somegoogly.event;

import com.github.crittscott.somegoogly.config.EyeConfigReloadListener;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.head.HeadInfo.RuntimeConfig;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.GooglyEyePacket;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Random;

public class ServerEventHandler {
    private static final String HAS_EYES_KEY = "somegoogly:hasGooglyEyes";

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new EyeConfigReloadListener());
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        EyeConfigSyncPacket packet = new EyeConfigSyncPacket(ServerEyeConfigs.all());
        if (event.getPlayer() != null) {
            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(event::getPlayer), packet);
        } else {
            NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
        }
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }

        // Decide once, at first spawn. The result is stored in persistent data (saved with the
        // entity), so on later world loads / dimension changes / growing up we keep the existing
        // decision instead of re-rolling. A mob keeps its eyes (or lack of them) for life, even if
        // the spawn-chance config is changed afterwards — only newly spawned mobs see the new chance.
        // (Whether babies and adults should differ is a separate, deferred question; for now a mob's
        // having-eyes answer is fixed at spawn and the client just swaps in the age-appropriate
        // geometry as the mob grows.)
        if (!living.getPersistentData().contains(HAS_EYES_KEY)) {
            applyGooglyDecision(living);
        }
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof LivingEntity living)) {
            return;
        }

        boolean hasGooglyEyes = living.getPersistentData().getBoolean(HAS_EYES_KEY);
        NetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()),
                new GooglyEyePacket(living.getId(), hasGooglyEyes)
        );
    }

    private static void applyGooglyDecision(LivingEntity living) {
        boolean hasGooglyEyes = false;
        ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());

        if (ServerConfig.GOOGLY_EYES_ENABLED.get()) {
            // Only configured + enabled entities are eligible. A datapack `enabled:false` is an
            // authoritative hard-off that beats the percent roll.
            RuntimeConfig config = ServerEyeConfigs.get(entityType, living);
            if (config != null && config.isEnabled() && config.heads != null && !config.heads.isEmpty()) {
                int percent = ServerConfig.percentFor(entityType);

                // Seeded by UUID so the same mob always rolls the same result (the decision is stored
                // anyway; this just keeps it consistent if it ever has to be recomputed).
                Random rand = new Random(Math.abs(living.getUUID().hashCode()) * 8134L);
                hasGooglyEyes = rand.nextFloat() < (percent / 100F);
            }
        }

        // Stored only; tracking clients learn the value when they start tracking the entity
        // (onStartTracking). Since the decision never changes after spawn, no mid-life sync is needed.
        living.getPersistentData().putBoolean(HAS_EYES_KEY, hasGooglyEyes);
    }
}
