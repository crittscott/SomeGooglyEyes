package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.RandomSupport;

/** World-owned simulations of eyes in third-person hands, and their connection to item draws. */
public final class HandItemEyes {

    private static final Simulations SIMULATIONS = new Simulations();

    private HandItemEyes() {
    }

    /** Disposable ambient simulations, independent for each holder's main hand and offhand. */
    public static final class Simulations {

        private static final long OFFHAND_SEED = RandomSupport.GOLDEN_RATIO_64;

        private final Map<Key, Entry> entries = new HashMap<>();
        private long ticks;

        private record Key(UUID holder, InteractionHand hand) {
        }

        private static final class Entry {
            final PresentationEyes eyes;
            final LivingEntity holder;
            long lastShown;

            Entry(PresentationEyes eyes, LivingEntity holder) {
                this.eyes = eyes;
                this.holder = holder;
            }
        }

        public void clear() {
            entries.clear();
        }

        /** Stack copies and counts retain state; eye layout and appearance changes replace it. */
        @Nullable
        public PresentationEyes get(LivingEntity holder, InteractionHand hand, ItemStack stack) {
            Key key = new Key(holder.getUUID(), hand);
            if (holder.isRemoved()) {
                entries.remove(key);
                return null;
            }
            Entry entry = entries.get(key);
            if (entry == null || entry.holder != holder || !entry.eyes.matches(stack)) {
                UUID uuid = holder.getUUID();
                UUID seed = hand == InteractionHand.MAIN_HAND ? uuid
                        : new UUID(uuid.getMostSignificantBits() ^ OFFHAND_SEED, uuid.getLeastSignificantBits());
                PresentationEyes eyes = PresentationEyes.create(seed, stack);
                if (eyes == null) {
                    entries.remove(key);
                    return null;
                }
                entry = new Entry(eyes, holder);
                entries.put(key, entry);
            }
            entry.lastShown = ticks;
            return entry.eyes;
        }

        /** Entity removal immediately releases both hands, including holders not currently visible. */
        public void remove(UUID holder) {
            entries.keySet().removeIf(key -> key.holder().equals(holder));
        }

        /** World time determines expressions; pause stops simulation and recently unseen hands expire. */
        public void tick(long gameTime, boolean simulate, BehaviorSettings settings) {
            if (!simulate) {
                return;
            }
            ticks++;
            ClientEyeRuntime.sweep(entries, ticks, entry -> entry.lastShown,
                    (key, entry) -> entry.holder.isRemoved(),
                    (key, entry) -> entry.eyes.tick(gameTime, settings, entry.holder));
        }
    }

    public static void clear() {
        SIMULATIONS.clear();
    }

    /** Physical left/right display contexts map to logical hands using the holder's dominant arm. */
    public static PresentationEyes.Draw context(@Nullable LivingEntity holder, ItemStack stack,
                                                ItemDisplayContext display) {
        if (holder == null || (display != ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                && display != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) || !PresentationEyes.supports(stack)) {
            return PresentationEyes.Draw.NONE;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !ClientEyeConfigs.eyesVisible()) {
            return PresentationEyes.Draw.NONE;
        }
        HumanoidArm arm = display == ItemDisplayContext.THIRD_PERSON_LEFT_HAND ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
        InteractionHand hand = arm == holder.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        PresentationEyes eyes = SIMULATIONS.get(holder, hand, stack);
        return eyes == null ? PresentationEyes.Draw.NONE : new PresentationEyes.Draw(eyes, minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }

    public static void remove(UUID holder) {
        SIMULATIONS.remove(holder);
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !ClientEyeConfigs.eyesVisible()) {
            clear();
        } else {
            SIMULATIONS.tick(minecraft.level.getGameTime(), !minecraft.isPaused(), ClientEyeConfigs.behaviors());
        }
    }
}
