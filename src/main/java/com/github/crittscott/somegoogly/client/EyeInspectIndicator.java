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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Action-bar answer to "could this mob wear googly eyes?": sneak while holding an eye or a slimey eye
 * and look at a living entity. Without this a player can't tell a mob type that's deliberately
 * unconfigured from one they just haven't seen eyed, and spends a slimey eye finding out.
 *
 * <p>Purely client-side: the verdict reads the synced config set ({@link ClientEyeConfigs}) through
 * the same {@link RuntimeConfig#isUsable} predicate {@code ServerEyeConfigs.isEligible} applies when a
 * slimey eye is used, and the synced per-entity eye flag ({@code EyeStatePacket} writes it onto the
 * client entity), so it reports what an application would actually do without asking the server. It
 * deliberately ignores the local {@code ClientConfig} render vetoes — those hide eyes on this client,
 * but the application still works and other players still see the result.
 */
public class EyeInspectIndicator {

    /**
     * Well past both melee pick range ({@code mc.crosshairPickEntity} stops at ~3 blocks) and the reach
     * a slimey eye is applied at: the point is to vet a mob from a safe distance, before walking up to
     * something that bites.
     */
    private static final double REACH = 16.0D;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.isPaused()) {
            return;
        }
        if (!player.isShiftKeyDown() || !holdingEye(player)) {
            return;
        }
        LivingEntity target = LookTarget.livingInCrosshair(player, REACH);
        if (target == null) {
            return;
        }
        // Re-sent every tick while aimed: vanilla just resets the overlay's fade timer, so the text
        // holds steady and fades on its own (~2s) once the player looks away or stops sneaking.
        player.displayClientMessage(verdict(target), true);
    }

    private static boolean holdingEye(LocalPlayer player) {
        return isEye(player.getMainHandItem()) || isEye(player.getOffhandItem());
    }

    /** Either eye item: the slimey eye is the applicator, but the plain eye is what you carry to craft it. */
    private static boolean isEye(ItemStack stack) {
        return stack.is(ModItems.GOOGLY_EYE.get()) || stack.is(ModItems.SLIMEY_EYE.get());
    }

    private static Component verdict(LivingEntity target) {
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        boolean baby = target.isBaby();
        if (RuntimeConfig.isUsable(ClientEyeConfigs.get(type, baby))) {
            return EyeState.hasEyes(target)
                    ? Component.translatable("somegoogly.inspect.already_has").withStyle(ChatFormatting.AQUA)
                    : Component.translatable("somegoogly.inspect.can_have").withStyle(ChatFormatting.GREEN);
        }
        // Only the other age is configured: applying right now would do nothing (the server checks the
        // current age), which is exactly the trap worth warning about.
        if (RuntimeConfig.isUsable(ClientEyeConfigs.get(type, !baby))) {
            return Component.translatable(baby ? "somegoogly.inspect.only_as_adult" : "somegoogly.inspect.only_as_baby")
                    .withStyle(ChatFormatting.YELLOW);
        }
        return Component.translatable("somegoogly.inspect.cannot").withStyle(ChatFormatting.RED);
    }
}
