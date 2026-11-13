package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.eye.BoatEyeLayout;
import com.github.crittscott.somegoogly.item.EyeItemService;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Client action-bar verdict for whether the aimed entity or placed mob head can currently receive googly eyes. */
public final class EyeInspector {

    private EyeInspector() {
    }

    private static boolean holdingEye(LocalPlayer player) {
        return isEye(player.getMainHandItem()) || isEye(player.getOffhandItem());
    }

    private static boolean isEye(ItemStack stack) {
        return stack.is(ModContent.GOOGLY_EYE.get()) || stack.is(ModContent.SLIMY_EYE.get());
    }

    private static Component message(EyeItemService.Verdict verdict) {
        return switch (verdict) {
            case ALREADY_HAS -> Component.translatable("somegoogly.inspect.already_has").withStyle(ChatFormatting.AQUA);
            case CAN_HAVE -> Component.translatable("somegoogly.inspect.can_have").withStyle(ChatFormatting.GREEN);
            case CANNOT -> Component.translatable("somegoogly.inspect.cannot").withStyle(ChatFormatting.RED);
            case ONLY_AS_ADULT -> Component.translatable("somegoogly.inspect.only_as_adult").withStyle(ChatFormatting.YELLOW);
            case ONLY_AS_BABY -> Component.translatable("somegoogly.inspect.only_as_baby").withStyle(ChatFormatting.YELLOW);
        };
    }

    /**
     * Each client tick, while the player sneaks holding an eye item, show the verdict for the entity or placed head
     * under the crosshair, the target a right-click would use, on the action bar.
     */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.isPaused()
                || !player.isShiftKeyDown() || !holdingEye(player)) {
            return;
        }
        if (minecraft.hitResult instanceof EntityHitResult hit) {
            Entity target = hit.getEntity();
            if (target instanceof LivingEntity || BoatEyeLayout.supports(target)) {
                player.sendOverlayMessage(message(EyeItemService.verdictFor(target)));
            }
        } else if (minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            if (minecraft.level.getBlockEntity(hit.getBlockPos()) instanceof SkullBlockEntity head) {
                player.sendOverlayMessage(message(EyeItemService.verdictFor(head)));
            }
        }
    }
}
