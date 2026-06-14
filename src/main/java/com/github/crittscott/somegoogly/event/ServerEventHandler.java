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
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Random;

public class ServerEventHandler {
    private static final String HAS_EYES_KEY = "somegoogly:hasGooglyEyes";
    private static final String AGE_STATE_KEY = "somegoogly:eyeAgeState";
    private static final byte AGE_ADULT = 1;
    private static final byte AGE_BABY = 2;

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

        applyGooglyDecision(living, false);
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        LivingEntity living = event.getEntity();
        byte currentAge = ageState(living);
        if (living.getPersistentData().getByte(AGE_STATE_KEY) != currentAge) {
            applyGooglyDecision(living, true);
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

    private static void applyGooglyDecision(LivingEntity living, boolean notifyTrackingClients) {
        boolean hasGooglyEyes = false;
        ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());

        if (ServerConfig.GOOGLY_EYES_ENABLED.get()) {
            // Only configured + enabled entries for this exact age are eligible. A datapack
            // `enabled:false` is an authoritative hard-off that beats the percent roll.
            RuntimeConfig config = ServerEyeConfigs.get(entityType, living);
            if (config != null && config.isEnabled() && config.heads != null && !config.heads.isEmpty()) {
                ServerConfig.parseOverrides();
                int percent = ServerConfig.entityOverrideParsed.getOrDefault(entityType, ServerConfig.GLOBAL_PERCENT.get());

                Random rand = new Random(Math.abs(living.getUUID().hashCode()) * 8134L + ageState(living));
                hasGooglyEyes = rand.nextFloat() < (percent / 100F);
            }
        }

        living.getPersistentData().putBoolean(HAS_EYES_KEY, hasGooglyEyes);
        living.getPersistentData().putByte(AGE_STATE_KEY, ageState(living));

        if (notifyTrackingClients) {
            NetworkHandler.INSTANCE.send(
                    PacketDistributor.TRACKING_ENTITY.with(() -> living),
                    new GooglyEyePacket(living.getId(), hasGooglyEyes)
            );
        }
    }

    private static byte ageState(LivingEntity living) {
        return living.isBaby() ? AGE_BABY : AGE_ADULT;
    }
}
