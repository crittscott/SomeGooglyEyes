package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfig;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.item.ModItems;
import com.github.crittscott.somegoogly.util.LookTarget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Client action-bar verdict for whether the aimed entity can currently receive googly eyes. */
public final class EyeInspector {

    private static final double REACH = 16.0D;

    private EyeInspector() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.isPaused()
                || !player.isShiftKeyDown() || !holdingEye(player)) {
            return;
        }
        LivingEntity target = LookTarget.livingInCrosshair(player, REACH);
        if (target != null) {
            player.displayClientMessage(verdict(target), true);
        }
    }

    private static boolean holdingEye(LocalPlayer player) {
        return isEye(player.getMainHandItem()) || isEye(player.getOffhandItem());
    }

    private static boolean isEye(ItemStack stack) {
        return stack.is(ModItems.GOOGLY_EYE.get()) || stack.is(ModItems.SLIMY_EYE.get());
    }

    private static Component verdict(LivingEntity target) {
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        boolean baby = target.isBaby();
        if (RuntimeConfig.isUsable(ClientEyeConfigs.get(type, baby))) {
            return EyeState.hasEyes(target)
                    ? Component.translatable("somegoogly.inspect.already_has").withStyle(ChatFormatting.AQUA)
                    : Component.translatable("somegoogly.inspect.can_have").withStyle(ChatFormatting.GREEN);
        }
        if (RuntimeConfig.isUsable(ClientEyeConfigs.get(type, !baby))) {
            return Component.translatable(baby
                            ? "somegoogly.inspect.only_as_adult" : "somegoogly.inspect.only_as_baby")
                    .withStyle(ChatFormatting.YELLOW);
        }
        return Component.translatable("somegoogly.inspect.cannot").withStyle(ChatFormatting.RED);
    }
}
