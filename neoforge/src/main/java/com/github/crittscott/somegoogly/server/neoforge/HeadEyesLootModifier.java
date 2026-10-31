package com.github.crittscott.somegoogly.server.neoforge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Breaking an eyed head copies its eye state onto the dropped head item, where placing it restores it. Applied
 * to every loot table through {@code data/neoforge/loot_modifiers/global_loot_modifiers.json}; it acts only on
 * loot generated for a head block entity that has eyes.
 * Its data files live in this loader's resources.
 */
public final class HeadEyesLootModifier extends LootModifier {

    public static final MapCodec<HeadEyesLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance).apply(instance, HeadEyesLootModifier::new));
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, SomeGooglyCommon.MOD_ID);

    public HeadEyesLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    /** Register the modifier's codec on the mod bus; called once from the mod constructor. */
    public static void register(IEventBus modBus) {
        SERIALIZERS.register("head_eyes", () -> CODEC);
        SERIALIZERS.register(modBus);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        SkullEyeState.copyToLoot(context, generatedLoot);
        return generatedLoot;
    }
}
