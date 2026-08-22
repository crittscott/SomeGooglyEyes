package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;

/**
 * Server-side coverage of the {@link EyeState} mutation API beyond the existing all-fields round-trip:
 * sparse overrides, atomic tint clearing, snapshot installation, and {@code setGlow(null)} dropping
 * the override so eyes fall back to per-eye config glow. Server mutation calls also broadcast an
 * {@code EyeStatePacket}; with no trackers in the test that is a no-op, exercised here for safety.
 */
public final class EyeStateOverrideGameTestsLogic {

    private EyeStateOverrideGameTestsLogic() {
    }

    private static Cow spawnCow(GameTestHelper helper) {
        return helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
    }

    public static void clearingTintRemovesOnlyThatField(GameTestHelper helper) {
        Cow cow = spawnCow(helper);
        EyeState.setIrisTint(cow, new EyeColor(0.1F, 0.2F, 0.3F));
        EyeState.setCorneaTint(cow, new EyeColor(0.4F, 0.5F, 0.6F));

        EyeState.clearIrisTint(cow);
        AppearanceOverride after = EyeState.readProperties(cow);
        helper.assertTrue(after.iris().isEmpty(), "iris should be cleared");
        helper.assertTrue(after.cornea().isPresent(), "cornea should be untouched when clearing iris");
        helper.succeed();
    }

    public static void glowOverrideCanBeSetAndDropped(GameTestHelper helper) {
        Cow cow = spawnCow(helper);

        EyeState.setGlow(cow, true);
        helper.assertTrue(EyeState.readProperties(cow).glow().orElse(false), "glow override should be set true");

        // null clears the override so the eye falls back to its per-eye config glow.
        EyeState.setGlow(cow, null);
        helper.assertTrue(EyeState.readProperties(cow).glow().isEmpty(), "setGlow(null) should drop the glow override");
        helper.succeed();
    }

    public static void settingOneFieldLeavesOthersAbsent(GameTestHelper helper) {
        Cow cow = spawnCow(helper);
        EyeState.setIrisTint(cow, new EyeColor(0.25F, 0.5F, 0.75F));

        AppearanceOverride override = EyeState.readProperties(cow);
        helper.assertTrue(override.iris().isPresent(), "iris override should be present");
        helper.assertTrue(override.cornea().isEmpty(), "cornea should stay absent (sparse override)");
        helper.assertTrue(override.glow().isEmpty(), "glow should stay absent (sparse override)");
        helper.assertTrue(EyeState.overridesTagOrNull(cow) != null, "an override compound should exist on the entity");
        helper.succeed();
    }

    public static void clearingEveryFieldRemovesTheCompound(GameTestHelper helper) {
        Cow cow = spawnCow(helper);
        EyeState.setIrisTint(cow, new EyeColor(0.25F, 0.5F, 0.75F));
        EyeState.clearIrisTint(cow);

        helper.assertTrue(EyeState.readProperties(cow).isEmpty(), "override should be empty once its only field is cleared");
        helper.assertTrue(EyeState.overridesTagOrNull(cow) == null, "the override compound should be removed when empty");
        helper.succeed();
    }

    public static void clearingTintsPreservesGlow(GameTestHelper helper) {
        Cow cow = spawnCow(helper);
        EyeState.setIrisTint(cow, new EyeColor(0.1F, 0.2F, 0.3F));
        EyeState.setCorneaTint(cow, new EyeColor(0.4F, 0.5F, 0.6F));
        EyeState.setGlow(cow, true);

        EyeState.clearTints(cow);
        AppearanceOverride after = EyeState.readProperties(cow);
        helper.assertTrue(after.iris().isEmpty(), "iris should be cleared");
        helper.assertTrue(after.cornea().isEmpty(), "cornea should be cleared");
        helper.assertTrue(after.glow().orElse(false), "clearing tints should preserve glow");
        helper.succeed();
    }

    public static void snapshotAppliesAllFieldsTogether(GameTestHelper helper) {
        Cow cow = spawnCow(helper);
        AppearanceOverride properties = AppearanceOverride.EMPTY
                .withIrisColor(new EyeColor(0.1F, 0.2F, 0.3F))
                .withGlow(true);

        EyeState.applySnapshot(cow, new EyeState.Snapshot(true, 0.75F, properties));
        EyeState.Snapshot after = EyeState.snapshot(cow);
        helper.assertTrue(after.hasEyes(), "snapshot should apply the has-eyes flag");
        helper.assertTrue(after.variantRoll() == 0.75F, "snapshot should apply the variant roll");
        helper.assertTrue(after.properties().equals(properties), "snapshot should apply appearance properties");
        helper.succeed();
    }
}
