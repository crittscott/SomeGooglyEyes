package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.github.crittscott.somegoogly.registry.ModContent;
import com.github.crittscott.somegoogly.util.LookTarget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Client action-bar verdict for whether the aimed entity or placed mob head can currently receive googly eyes. */
public final class EyeInspector {

    private static final double REACH = 16.0D;

    private EyeInspector() {
    }

    /**
     * Each client tick, while the player sneaks holding an eye item, show the verdict for the aimed entity, or for
     * the aimed placed head when it is nearer, on the action bar.
     */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.isPaused()
                || !player.isShiftKeyDown() || !holdingEye(player)) {
            return;
        }
        LivingEntity target = LookTarget.livingInCrosshair(player, REACH);
        if (player.pick(REACH, 1.0F, false) instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && minecraft.level.getBlockEntity(hit.getBlockPos()) instanceof SkullBlockEntity skull
                && (target == null || player.getEyePosition().distanceToSqr(hit.getLocation())
                        < player.getEyePosition().distanceToSqr(target.position()))) {
            player.displayClientMessage(verdict(skull), true);
        } else if (target != null) {
            player.displayClientMessage(verdict(target), true);
        }
    }

    private static Component alreadyHas() {
        return Component.translatable("somegoogly.inspect.already_has").withStyle(ChatFormatting.AQUA);
    }

    private static Component canHave() {
        return Component.translatable("somegoogly.inspect.can_have").withStyle(ChatFormatting.GREEN);
    }

    private static Component cannot() {
        return Component.translatable("somegoogly.inspect.cannot").withStyle(ChatFormatting.RED);
    }

    private static boolean holdingEye(LocalPlayer player) {
        return isEye(player.getMainHandItem()) || isEye(player.getOffhandItem());
    }

    private static boolean isEye(ItemStack stack) {
        return stack.is(ModContent.GOOGLY_EYE.get()) || stack.is(ModContent.SLIMY_EYE.get());
    }

    private static Component verdict(LivingEntity target) {
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        boolean baby = target.isBaby();
        if (RuntimeConfig.isUsable(ClientEyeConfigs.get(type, baby))) {
            return EyeState.hasEyes(target) ? alreadyHas() : canHave();
        }
        if (RuntimeConfig.isUsable(ClientEyeConfigs.get(type, !baby))) {
            return Component.translatable(baby
                            ? "somegoogly.inspect.only_as_adult" : "somegoogly.inspect.only_as_baby")
                    .withStyle(ChatFormatting.YELLOW);
        }
        return cannot();
    }

    /** A placed head can have eyes when its mob has an adult definition, which it borrows, with eyes it shows. */
    private static Component verdict(SkullBlockEntity skull) {
        ResourceLocation entity = SkullEyeState.entityFor(skull.getBlockState().getBlock());
        if (entity == null || !SkullEyeState.canHaveEyes(entity, ClientEyeConfigs.get(entity, false))) {
            return cannot();
        }
        return SkullEyeState.hasEyes(skull) ? alreadyHas() : canHave();
    }
}
