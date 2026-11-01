package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.config.ClientConfig;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import javax.annotation.Nullable;

/** Connects world-owned hand simulations to immediate and extracted item draws. */
public final class HandItemEyes {

    private static final Deque<Context> DRAWS = new ArrayDeque<>();
    public static final Context EMPTY = new Context(null, 0F);
    private static final HandEyeAnimations SIMULATIONS = new HandEyeAnimations();

    private static Context current = EMPTY;

    private HandItemEyes() {
    }

    public record Context(@Nullable ItemEyes eyes, float partialTick) {
    }

    public interface Holder {
        Context somegoogly$handEyes();

        void somegoogly$setHandEyes(Context context);
    }

    public static void begin(Context context) {
        DRAWS.push(current);
        current = context;
    }

    public static void clear() {
        SIMULATIONS.clear();
    }

    /** Physical left/right display contexts map to logical hands using the holder's dominant arm. */
    public static Context context(@Nullable LivingEntity holder, ItemStack stack, ItemDisplayContext display) {
        if (holder == null || (display != ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                && display != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)) {
            return EMPTY;
        }
        Minecraft minecraft = Minecraft.getInstance();
        HumanoidArm arm = display == ItemDisplayContext.THIRD_PERSON_LEFT_HAND ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
        InteractionHand hand = arm == holder.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        boolean enabled = minecraft.level != null && !holder.isRemoved()
                && ClientEyeConfigs.googlyEyesEnabled() && !ClientConfig.DISABLE_GOOGLY_EYES.get();
        ItemEyes eyes = SIMULATIONS.get(holder.getUUID(), hand, stack, enabled);
        return eyes == null ? EMPTY : new Context(eyes, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
    }

    public static Context current() {
        return current;
    }

    public static void end() {
        current = DRAWS.pop();
    }

    public static void remove(UUID holder) {
        SIMULATIONS.remove(holder);
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !ClientEyeConfigs.googlyEyesEnabled()
                || ClientConfig.DISABLE_GOOGLY_EYES.get()) {
            clear();
        } else {
            SIMULATIONS.tick(minecraft.level.getGameTime(), !minecraft.isPaused(), ClientEyeConfigs.behaviors());
        }
    }
}
