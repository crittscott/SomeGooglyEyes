package com.github.crittscott.somegoogly.event;

import com.github.crittscott.somegoogly.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.config.EyeConfigReloadListener;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.command.GooglyDebugCommand;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.state.EyeState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
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
    public void onRegisterCommands(RegisterCommandsEvent event) {
        GooglyDebugCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof LivingEntity living) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Send the full current state (has-eyes + any appearance overrides applied since spawn) so a
        // newly tracking player matches everyone else, not just the at-spawn decision.
        NetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new EyeStatePacket(living.getId(), EyeState.hasEyes(living),
                        EyeState.getVariantRoll(living), EyeState.overridesTagOrNull(living))
        );

        // Register with the behaviour scheduler (server-polite: only eyed, watched mobs) and catch the
        // player up if the mob is already mid-behaviour.
        ServerBehaviorScheduler.onStartTracking(living, player);
    }

    @SubscribeEvent
    public void onStopTracking(PlayerEvent.StopTracking event) {
        if (event.getTarget() instanceof LivingEntity living) {
            ServerBehaviorScheduler.onStopTracking(living);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ServerBehaviorScheduler.serverTick();
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        // Transient cosmetic state; drop it so a single-player JVM doesn't carry one world into the next.
        ServerBehaviorScheduler.clear();
    }

    private static void applyGooglyDecision(LivingEntity living) {
        boolean hasGooglyEyes = false;
        ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());

        if (ServerConfig.GOOGLY_EYES_ENABLED.get()) {
            // Only configured + enabled entities are eligible. A datapack `enabled:false` is an
            // authoritative hard-off that beats the percent roll. Use the age-independent check: this
            // decision is stored for life, so a baby with only an adult config must still roll (it'll
            // show eyes once grown) rather than being locked out forever.
            if (ServerEyeConfigs.canEverWearEyes(living)) {
                int percent = ServerConfig.percentFor(entityType);

                // Seeded by UUID so the same mob always rolls the same result (the decision is stored
                // anyway; this just keeps it consistent if it ever has to be recomputed).
                Random rand = new Random(Math.abs(living.getUUID().hashCode()) * 8134L);
                hasGooglyEyes = rand.nextFloat() < (percent / 100F);
            }
        }

        // Stored only; tracking clients learn the value when they start tracking the entity
        // (onStartTracking). This is the at-spawn default; the flag and appearance can later change
        // mid-life via EyeState (shears / potion / dye / redstone), which re-syncs to trackers itself.
        living.getPersistentData().putBoolean(HAS_EYES_KEY, hasGooglyEyes);

        // Pick a placement variant now and lock it for life (independent of the has-eyes roll, so a
        // later reattach/potion uses this mob's own arrangement). A separate UUID seed keeps it
        // deterministic without perturbing the has-eyes roll above. HeadInfo.chooseVariantIndex maps
        // this 0..1 roll onto whichever age config's weighted variants apply at render time.
        Random variantRand = new Random(Math.abs(living.getUUID().hashCode()) * 6271L);
        living.getPersistentData().putFloat(EyeState.VARIANT_ROLL, variantRand.nextFloat());
    }
}
