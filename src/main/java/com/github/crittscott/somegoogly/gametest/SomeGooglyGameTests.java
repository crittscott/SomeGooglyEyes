package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(SomeGoogly.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SomeGooglyGameTests {
    private static final String TEMPLATE = "empty";

    private SomeGooglyGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void configuredCowHasServerGeometry(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(cow.getType());
        HeadInfo headInfo = HeadInfo.serverHelper(type, cow);

        helper.assertTrue(ServerEyeConfigs.isEligible(cow), "Expected cow to be eligible for eyes");
        helper.assertTrue(headInfo.hasConfig(), "Expected cow to have selected server eye config");
        helper.assertTrue(headInfo.getHeadCount() > 0, "Expected cow config to have at least one head");
        helper.assertTrue(headInfo.getEyeCount(0) > 0, "Expected cow config to have at least one eye");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void spawnInitializesEyePersistentData(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));

        helper.succeedWhen(() -> {
            CompoundTag data = cow.getPersistentData();
            helper.assertTrue(data.contains(EyeState.HAS_EYES), "Expected spawned mob to have has-eyes flag");
            helper.assertTrue(data.contains(EyeState.VARIANT_ROLL), "Expected spawned mob to have variant roll");
            float roll = data.getFloat(EyeState.VARIANT_ROLL);
            helper.assertTrue(roll >= 0.0F && roll < 1.0F, "Expected variant roll in [0, 1)");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void eyeStateAppearanceOverridesRoundTrip(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        EyeColor iris = new EyeColor(0.25F, 0.5F, 0.75F);
        EyeColor cornea = new EyeColor(0.9F, 0.8F, 0.7F);

        EyeState.setHasEyes(cow, true);
        EyeState.setIrisTint(cow, iris);
        EyeState.setCorneaTint(cow, cornea);
        EyeState.setGlow(cow, true);

        AppearanceOverride overrides = EyeState.readProperties(cow);
        helper.assertTrue(EyeState.hasEyes(cow), "Expected EyeState has-eyes flag to be true");
        helper.assertTrue(overrides.iris().isPresent() && overrides.iris().get().equals(iris),
                "Expected iris override to round-trip");
        helper.assertTrue(overrides.cornea().isPresent() && overrides.cornea().get().equals(cornea),
                "Expected cornea override to round-trip");
        helper.assertTrue(overrides.glow().isPresent() && overrides.glow().get(),
                "Expected glow override to round-trip");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void googlyEyeItemStoresAppearanceOverride(GameTestHelper helper) {
        AppearanceOverride appearance = AppearanceOverride.EMPTY
                .withIrisColor(new EyeColor(0.1F, 0.2F, 0.3F))
                .withCorneaColor(new EyeColor(0.4F, 0.5F, 0.6F))
                .withGlow(true);

        ItemStack stack = GooglyEyeItem.create(appearance, 3);
        AppearanceOverride roundTrip = GooglyEyeItem.getProperties(stack);

        helper.assertTrue(stack.getCount() == 3, "Expected created eye stack count to be preserved");
        helper.assertTrue(roundTrip.equals(appearance), "Expected eye item appearance NBT to round-trip");
        helper.succeed();
    }
}
