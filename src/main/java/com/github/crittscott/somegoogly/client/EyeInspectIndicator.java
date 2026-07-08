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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Action-bar answer to "could this mob wear googly eyes?": sneak while holding a googly eye item and
 * look at a living entity. Without this a player can't tell a mob type that's deliberately
 * unconfigured from one they just haven't seen eyed, and wastes a potion finding out.
 *
 * <p>Purely client-side: the verdict reads the synced config set ({@link ClientEyeConfigs}) through
 * the same {@link RuntimeConfig#isUsable} predicate the splash potion's server-side targeting uses,
 * and the synced per-entity eye flag ({@code EyeStatePacket} writes it onto the client entity), so it
 * reports what a thrown potion would actually do without asking the server. It deliberately ignores
 * the local {@code ClientConfig} render vetoes — those hide eyes on this client, but the potion still
 * works and other players still see the result.
 */
public class EyeInspectIndicator {

    /**
     * Well past melee pick range ({@code mc.crosshairPickEntity} stops at ~3 blocks): the point is to
     * check a mob from potion-throwing distance, before committing the throw.
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
        return player.getMainHandItem().is(ModItems.GOOGLY_EYE.get())
                || player.getOffhandItem().is(ModItems.GOOGLY_EYE.get());
    }

    private static Component verdict(LivingEntity target) {
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        boolean baby = target.isBaby();
        if (RuntimeConfig.isUsable(ClientEyeConfigs.get(type, baby))) {
            return EyeState.hasEyes(target)
                    ? Component.translatable("somegoogly.inspect.already_has").withStyle(ChatFormatting.AQUA)
                    : Component.translatable("somegoogly.inspect.can_have").withStyle(ChatFormatting.GREEN);
        }
        // Only the other age is configured: a splash right now would do nothing (the server targets by
        // current age), which is exactly the trap worth warning about.
        if (RuntimeConfig.isUsable(ClientEyeConfigs.get(type, !baby))) {
            return Component.translatable(baby ? "somegoogly.inspect.only_as_adult" : "somegoogly.inspect.only_as_baby")
                    .withStyle(ChatFormatting.YELLOW);
        }
        return Component.translatable("somegoogly.inspect.cannot").withStyle(ChatFormatting.RED);
    }
}
