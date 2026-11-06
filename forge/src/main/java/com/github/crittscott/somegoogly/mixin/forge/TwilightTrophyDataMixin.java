package com.github.crittscott.somegoogly.mixin.forge;

import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nullable;

@Pseudo
@Mixin(targets = "twilightforest.block.entity.TrophyBlockEntity", remap = false)
abstract class TwilightTrophyDataMixin extends BlockEntity implements SkullEyeState.Holder {
    @Unique
    @Nullable
    private CompoundTag somegoogly$data;

    protected TwilightTrophyDataMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public CompoundTag somegoogly$find() {
        return somegoogly$data;
    }

    @Override
    public CompoundTag somegoogly$get() {
        if (somegoogly$data == null) {
            somegoogly$data = new CompoundTag();
        }
        return somegoogly$data;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        somegoogly$data = SkullEyeState.loadedData(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        somegoogly$saveEyes(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        somegoogly$saveEyes(tag);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
