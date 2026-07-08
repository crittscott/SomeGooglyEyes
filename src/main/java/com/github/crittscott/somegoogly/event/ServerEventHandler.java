package com.github.crittscott.somegoogly.event;

import com.github.crittscott.somegoogly.command.GooglyAdminCommand;
import com.github.crittscott.somegoogly.config.EyeConfigReloadListener;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.picker.PickerExportService;
import com.github.crittscott.somegoogly.picker.PickerFreezeService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

public class ServerEventHandler {

    private static void applyGooglyDecision(LivingEntity living) {
        boolean hasGooglyEyes = false;
        ResourceLocation entityType = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        RandomSource random = living.getRandom();

        // Players never roll eyes at spawn — they can only receive them mid-life (the googly potion).
        if (!(living instanceof Player) && ServerConfig.GOOGLY_EYES_ENABLED.get()) {
            // Only configured + enabled entities are eligible. A datapack `enabled:false` is an
            // authoritative hard-off that beats the percent roll. Use the age-independent check: this
            // decision is stored for life, so a baby with only an adult config must still roll (it'll
            // show eyes once grown) rather than being locked out forever.
            if (ServerEyeConfigs.canEverWearEyes(living)) {
                int percent = ServerConfig.percentFor(entityType);
                hasGooglyEyes = random.nextFloat() < (percent / 100F);
            }
        }

        // Stored only; tracking clients learn the value when they start tracking the entity
        // (onStartTracking). This is the at-spawn default; the flag and appearance can later change
        // mid-life via EyeState (shears / potion / dye / redstone), which re-syncs to trackers itself.
        living.getPersistentData().putBoolean(EyeState.HAS_EYES, hasGooglyEyes);

        // Pick a placement variant now and lock it for life (independent of the has-eyes roll, so a
        // later reattach/potion uses this mob's own arrangement). HeadInfo.chooseVariantIndex maps this
        // 0..1 roll onto whichever age config's weighted variants apply at render time.
        living.getPersistentData().putFloat(EyeState.VARIANT_ROLL, random.nextFloat());
    }

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
        if (!living.getPersistentData().contains(EyeState.HAS_EYES)) {
            applyGooglyDecision(living);
        }

        // A mob rejoining with a stale picker-freeze marker (server crash mid-edit, or its chunk
        // unloaded while frozen) gets its pre-picker NoAi back; a still-live edit is re-asserted.
        if (living instanceof Mob mob) {
            PickerFreezeService.onMobJoin(mob);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        GooglyAdminCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // A disappearing picker client must never strand a frozen mob; the server releases it.
        if (event.getEntity() instanceof ServerPlayer player) {
            PickerFreezeService.onPlayerLoggedOut(player);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        // Transient cosmetic state; drop it so a single-player JVM doesn't carry one world into the next.
        ServerBehaviorScheduler.clear();
        // Restore any picker-frozen mobs synchronously before the final save, and drop per-run picker
        // state (the export cooldown is keyed to this run's tick counter).
        PickerFreezeService.onServerStopping(event.getServer());
        PickerExportService.onServerStopping();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ServerBehaviorScheduler.serverTick();
        }
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

        // Register with the behavior scheduler (server-polite: only eyed, watched mobs) and catch the
        // player up if the mob is already mid-behavior.
        ServerBehaviorScheduler.onStartTracking(living, player);
    }

    @SubscribeEvent
    public void onStopTracking(PlayerEvent.StopTracking event) {
        if (event.getTarget() instanceof LivingEntity living) {
            ServerBehaviorScheduler.onStopTracking(living);
        }
    }
}
