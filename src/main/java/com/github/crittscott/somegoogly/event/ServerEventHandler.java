package com.github.crittscott.somegoogly.event;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.network.GooglyEyePacket;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.Random;

public class ServerEventHandler {
    private static final Marker MARK = MarkerManager.getMarker(ServerEventHandler.class.getSimpleName());
    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }

        boolean globalEnabled = ServerConfig.GOOGLY_EYES_ENABLED.get();

        if (!globalEnabled) {
            return;
        }

        ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());

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