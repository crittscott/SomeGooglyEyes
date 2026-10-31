package com.github.crittscott.somegoogly.server.forge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

/**
 * Breaking an eyed head copies its eye state onto the dropped head item, where placing it restores it. Applied
 * to every loot table through {@code data/forge/loot_modifiers/global_loot_modifiers.json}; it acts only on loot
 * generated for a head block entity that has eyes. Its data files live in this loader's resources.
 */
public final class HeadEyesLootModifier extends LootModifier {

    public static final Supplier<Codec<HeadEyesLootModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(instance -> codecStart(instance).apply(instance, HeadEyesLootModifier::new)));
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, SomeGooglyCommon.MOD_ID);

    public HeadEyesLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        SkullEyeState.copyToLoot(context, generatedLoot);
        return generatedLoot;
    }

    /** Register the modifier's codec on the mod bus; called once from the mod constructor. */
    public static void register(IEventBus modBus) {
        SERIALIZERS.register("head_eyes", CODEC);
        SERIALIZERS.register(modBus);
    }
}
