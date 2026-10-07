package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;

/**
 * The {@link EyeState} appearance-override mutations behind {@code /sg admin}: sparse overrides, atomic
 * tint clearing, snapshot installation, and {@code setGlow(null)} dropping the override so eyes fall back
 * to per-eye config glow. Each mutation also sends an {@code EyeStatePacket}, which reaches no one here
 * because the test has no tracking players.
 *
 * <p>The in-game steps below run as a creative operator (permission level 2) looking at a cow that
 * already has eyes (for example, after {@code /sg admin eyes true}).
 */
public final class EyeStateOverrideGameTestsLogic {

    private EyeStateOverrideGameTestsLogic() {
    }

    private static Cow spawnCow(GameTestHelper helper) {
        return helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
    }

    /**
     * Removing one color override leaves the other in place. No in-game form, since
     * {@code /sg admin tint clear} removes both colors at once; guards that edits to one appearance field
     * never disturb another.
     */
    public static void clearingTintRemovesOnlyThatField(GameTestHelper helper) {
        Cow cow = spawnCow(helper);
        EyeState.setIrisTint(cow, new EyeColor(0.1F, 0.2F, 0.3F));
        EyeState.setCorneaTint(cow, new EyeColor(0.4F, 0.5F, 0.6F));

        EyeState.setProperties(cow, EyeState.readProperties(cow).withIrisColor(null));
        AppearanceOverride after = EyeState.readProperties(cow);
        helper.assertTrue(after.iris().isEmpty(), "iris should be cleared");
        helper.assertTrue(after.cornea().isPresent(), "cornea should be untouched when clearing iris");
        helper.succeed();
    }

    /**
     * A glow override can be set and then dropped back to the definition's glow. In game: run
     * {@code /sg admin glow on} and the cow's eyes glow; run {@code /sg admin glow config} and they return to
     * the glow its eye definition specifies (none, for the bundled cow).
     */
    public static void glowOverrideCanBeSetAndDropped(GameTestHelper helper) {
        Cow cow = spawnCow(helper);

        EyeState.setGlow(cow, true);
        helper.assertTrue(EyeState.readProperties(cow).glow().orElse(false), "glow override should be set true");

        // null clears the override so the eye falls back to its per-eye config glow.
        EyeState.setGlow(cow, null);
        helper.assertTrue(EyeState.readProperties(cow).glow().isEmpty(), "setGlow(null) should drop the glow override");
        helper.succeed();
    }

    /**
     * Setting one color leaves every other field at the definition's value. In game:
     * {@code /sg admin tint iris 1 0 0} turns only the irises red; the corneas keep their default color and
     * the eyes do not start glowing.
     */
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

    /**
     * Clearing the last override removes the override data entirely. In game:
     * {@code /sg admin tint iris 1 0 0}, then {@code /sg admin tint clear}; the eyes look as they did before,
     * and on NeoForge or Forge {@code /data get entity <cow>} shows no {@code somegoogly:eyeOverrides} key in
     * {@code NeoForgeData} or {@code ForgeData}.
     */
    public static void clearingEveryFieldRemovesTheCompound(GameTestHelper helper) {
        Cow cow = spawnCow(helper);
        EyeState.setIrisTint(cow, new EyeColor(0.25F, 0.5F, 0.75F));
        EyeState.setProperties(cow, EyeState.readProperties(cow).withIrisColor(null));

        helper.assertTrue(EyeState.readProperties(cow).isEmpty(), "override should be empty once its only field is cleared");
        helper.assertTrue(EyeState.overridesTagOrNull(cow) == null, "the override compound should be removed when empty");
        helper.succeed();
    }

    /**
     * Clearing colors keeps a glow override. In game: {@code /sg admin tint iris 1 0 0},
     * {@code /sg admin tint cornea 0 0 1}, and {@code /sg admin glow on}; then {@code /sg admin tint clear}
     * restores the default colors while the eyes keep glowing.
     */
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

    /**
     * A full eye-state snapshot applies eyes, variant, and appearance together, as a client does when it
     * receives one. In game: give the cow a red iris and {@code /sg admin glow on}, walk far enough away that
     * it unloads from view, and come back; it reappears with the same eyes, red irises, and glow at once.
     */
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
