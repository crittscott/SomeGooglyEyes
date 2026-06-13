package com.github.crittscott.somegoogly.event;

import com.github.crittscott.somegoogly.config.EyeConfigReloadListener;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.head.HeadInfo.EntityConfig;
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

        if (!ServerConfig.GOOGLY_EYES_ENABLED.get()) {
            return;
        }

        ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());

        // Only configured + enabled entities are eligible. A datapack `enabled:false` is an
        // authoritative hard-off that beats the percent roll. Write false explicitly so a mob that
        // previously had eyes saved in NBT loses them when a pack disables it.
        EntityConfig config = ServerEyeConfigs.get(entityType);
        if (config == null || !config.isEnabled()) {
            living.getPersistentData().putBoolean("somegoogly:hasGooglyEyes", false);
            return;
        }

        ServerConfig.parseOverrides();
        int percent = ServerConfig.entityOverrideParsed.getOrDefault(entityType, ServerConfig.GLOBAL_PERCENT.get());

        Random rand = new Random(Math.abs(living.getUUID().hashCode()) * 8134L);
        boolean hasGooglyEyes = rand.nextFloat() < (percent / 100F);

        living.getPersistentData().putBoolean("somegoogly:hasGooglyEyes", hasGooglyEyes);
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof LivingEntity living)) {
            return;
        }

        boolean hasGooglyEyes = living.getPersistentData().getBoolean("somegoogly:hasGooglyEyes");
        if (hasGooglyEyes) {
            NetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()),
                    new GooglyEyePacket(living.getId(), hasGooglyEyes)
            );
        }
    }
}
