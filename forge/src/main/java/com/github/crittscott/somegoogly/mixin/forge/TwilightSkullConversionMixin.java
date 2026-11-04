package com.github.crittscott.somegoogly.mixin.forge;

import com.github.crittscott.somegoogly.eye.state.EyeState.Snapshot;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayDeque;
import java.util.Deque;

@Pseudo
@Mixin(targets = {"twilightforest.events.EntityEvents", "twilightforest.dispenser.SkullCandleDispenseBehavior"},
        remap = false)
abstract class TwilightSkullConversionMixin {
    @Unique
    private static final ThreadLocal<Deque<Snapshot>> somegoogly$conversion =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Redirect(method = {"makeFloorSkull", "makeWallSkull"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            remap = true), require = 2)
    private static boolean somegoogly$rememberEyes(Level level, BlockPos pos, BlockState state) {
        Snapshot snapshot = level.isClientSide() ? Snapshot.NONE : SkullEyeState.snapshot(level.getBlockEntity(pos));
        boolean replaced = level.setBlockAndUpdate(pos, state);
        somegoogly$conversion.get().push(replaced ? snapshot : Snapshot.NONE);
        return replaced;
    }

    @Redirect(method = {"makeFloorSkull", "makeWallSkull"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
            remap = true), require = 2)
    private static void somegoogly$transferEyes(Level level, BlockEntity candle) {
        Snapshot snapshot = somegoogly$conversion.get().pop();
        level.setBlockEntity(candle);
        if (snapshot.hasEyes() && level.getBlockEntity(candle.getBlockPos()) == candle) {
            SkullEyeState.set(candle, snapshot);
        }
    }
}
