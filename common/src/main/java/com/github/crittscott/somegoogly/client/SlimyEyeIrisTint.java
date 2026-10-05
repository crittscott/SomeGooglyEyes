package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * The Slimy Eye iris tint: the stack's stored iris color. Registered as the
 * {@code somegoogly:slimy_eye_iris} tint source type; {@code assets/somegoogly/items/slimy_eye.json}
 * places it third in its {@code tints} list so it lands on the model's {@code layer2}. Each loader puts
 * {@link #MAP_CODEC} under {@link #ID}.
 */
public record SlimyEyeIrisTint() implements ItemTintSource {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, "slimy_eye_iris");
    public static final MapCodec<SlimyEyeIrisTint> MAP_CODEC = MapCodec.unit(new SlimyEyeIrisTint());

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity) {
        return EyeItemProperties.slimyEyeIrisColor(stack);
    }

    @Override
    public MapCodec<SlimyEyeIrisTint> type() {
        return MAP_CODEC;
    }
}
