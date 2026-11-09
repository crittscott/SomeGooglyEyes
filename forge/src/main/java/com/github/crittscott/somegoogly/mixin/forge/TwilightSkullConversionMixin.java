package com.github.crittscott.somegoogly.mixin.forge;

import com.github.crittscott.somegoogly.eye.state.EyeState.Snapshot;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Carries a placed head's eyes onto the skull candle that replaces it when a candle is put on it, by hand or by
 * dispenser. Twilight Forest is an optional mod with no API or event for this conversion, and it replaces the
 * block and installs a new block entity directly, so nothing else sees the old head's state. Each redirect still
 * makes the original call.
 */
@Pseudo
@Mixin(targets = {"twilightforest.events.EntityEvents", "twilightforest.dispenser.SkullCandleDispenseBehavior"},
        remap = false)
abstract class TwilightSkullConversionMixin {
    @Unique
    private static final ThreadLocal<Deque<Snapshot>> somegoogly$conversion =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Redirect(method = {"makeFloorSkull", "makeWallSkull"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            remap = true))
    private static boolean somegoogly$rememberEyes(Level level, BlockPos pos, BlockState state) {
        Snapshot snapshot = level.isClientSide() ? Snapshot.NONE : SkullEyeState.snapshot(level.getBlockEntity(pos));
        boolean replaced = level.setBlockAndUpdate(pos, state);
        somegoogly$conversion.get().push(replaced ? snapshot : Snapshot.NONE);
        return replaced;
    }

    @Redirect(method = {"makeFloorSkull", "makeWallSkull"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
            remap = true))
    private static void somegoogly$transferEyes(Level level, BlockEntity candle) {
        // Empty if a Twilight Forest update left only this redirect applying.
        Snapshot snapshot = somegoogly$conversion.get().poll();
        level.setBlockEntity(candle);
        if (snapshot != null && snapshot.hasEyes() && level.getBlockEntity(candle.getBlockPos()) == candle) {
            SkullEyeState.set(candle, snapshot);
        }
    }
}
