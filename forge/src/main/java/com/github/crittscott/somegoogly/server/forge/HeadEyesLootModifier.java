package com.github.crittscott.somegoogly.server.forge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Breaking an eyed head copies its eye state onto the dropped head item, where placing it restores it. Applied
 * to every loot table through {@code data/forge/loot_modifiers/global_loot_modifiers.json}; it acts only on
 * loot generated for a head block entity that has eyes.
 * Its data files live in this loader's resources.
 */
public final class HeadEyesLootModifier extends LootModifier {

    public static final MapCodec<HeadEyesLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance).apply(instance, HeadEyesLootModifier::new));
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, SomeGooglyCommon.MOD_ID);

    public HeadEyesLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    /** Register the modifier's codec with the mod's bus group; called once from the mod constructor. */
    public static void register(BusGroup modBusGroup) {
        SERIALIZERS.register("head_eyes", () -> CODEC);
        SERIALIZERS.register(modBusGroup);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(LootTable table, ObjectArrayList<ItemStack> generatedLoot,
                                                LootContext context) {
        SkullEyeState.copyToLoot(context, generatedLoot);
        return generatedLoot;
    }
}
