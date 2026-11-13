package com.github.crittscott.somegoogly.mixin.neoforge.twilight;

import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Pseudo;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import org.jspecify.annotations.Nullable;

/**
 * Gives Twilight Forest's boss trophies the saved, synced, and item-carried eye state vanilla heads get from
 * {@code SkullBlockEntityDataMixin}, making each trophy a {@link SkullEyeState.Holder}. Twilight Forest is an
 * optional mod with no API for this, and its trophy block entity is not a vanilla skull, so that Mixin does not
 * reach it.
 */
@Pseudo
@Mixin(targets = "twilightforest.block.entity.TrophyBlockEntity", remap = false)
abstract class TwilightTrophyDataMixin extends BlockEntity implements SkullEyeState.Holder {
    protected TwilightTrophyDataMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }


    @Unique
    @Nullable
    private CompoundTag somegoogly$data;

    @Override
    @Nullable
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
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        EyeState.Snapshot snapshot = input.get(ModContent.HEAD_EYES.get());
        if (snapshot == null) {
            somegoogly$data = null;
        } else {
            somegoogly$data = new CompoundTag();
            snapshot.write(somegoogly$data);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (somegoogly$data != null) {
            EyeState.Snapshot snapshot = EyeState.Snapshot.read(somegoogly$data);
            if (snapshot.hasEyes()) {
                components.set(ModContent.HEAD_EYES.get(), snapshot);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        somegoogly$data = SkullEyeState.loadedData(input.read(SkullEyeState.DATA_KEY, CompoundTag.CODEC).orElse(null));
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard(SkullEyeState.DATA_KEY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (somegoogly$data != null && somegoogly$data.getBooleanOr(EyeState.HAS_EYES, false)) {
            output.store(SkullEyeState.DATA_KEY, CompoundTag.CODEC, somegoogly$data.copy());
        }
    }
}
