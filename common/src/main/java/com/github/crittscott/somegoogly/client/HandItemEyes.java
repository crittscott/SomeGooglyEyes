package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Connects world-owned hand simulations to item draws. */
public final class HandItemEyes {

    private static final Deque<PresentationEyes.Draw> DRAWS = new ArrayDeque<>();
    private static final HandEyeAnimations SIMULATIONS = new HandEyeAnimations();

    private static PresentationEyes.Draw current = PresentationEyes.Draw.NONE;

    private HandItemEyes() {
    }

    public static void begin(PresentationEyes.Draw draw) {
        DRAWS.push(current);
        current = draw;
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
        return eyes == null ? PresentationEyes.Draw.NONE : new PresentationEyes.Draw(eyes, minecraft.getFrameTime());
    }

    public static PresentationEyes.Draw current() {
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
        if (minecraft.level == null || !ClientEyeConfigs.eyesVisible()) {
            clear();
        } else {
            SIMULATIONS.tick(minecraft.level.getGameTime(), !minecraft.isPaused(), ClientEyeConfigs.behaviors());
        }
    }
}
