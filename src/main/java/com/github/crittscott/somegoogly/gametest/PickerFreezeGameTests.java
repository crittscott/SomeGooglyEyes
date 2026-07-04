package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.picker.PickerFreezeService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * The server-owned freeze lifecycle ({@link PickerFreezeService}): NoAi capture and restore, the
 * one-editor-per-mob contention rule, releasing the previous mob when an editor switches, and the
 * persistent-marker crash recovery (a hard crash drops the live records but not the mob's marker tag,
 * which the next join restores from). Each test drives the service directly on the server thread —
 * the same calls the {@code PickerFreezePacket} handler and lifecycle events make — with a fresh
 * random "player" UUID, and releases everything it froze before succeeding.
 */
@GameTestHolder(SomeGoogly.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PickerFreezeGameTests {

    private static final String TEMPLATE = "empty";

    private PickerFreezeGameTests() {
    }

    /** A cow with normal AI as the freeze baseline ({@code spawnWithNoFreeWill} forces NoAi on). */
    private static Cow spawnCow(GameTestHelper helper, BlockPos pos) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, pos);
        cow.setNoAi(false);
        return cow;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void freezeCapturesAndUnfreezeRestores(GameTestHelper helper) {
        Cow cow = spawnCow(helper, new BlockPos(2, 2, 2));
        UUID editor = UUID.randomUUID();

        String error = PickerFreezeService.freeze(helper.getLevel(), editor, cow.getUUID());
        helper.assertTrue(error == null, "freezing an unclaimed mob should succeed");
        helper.assertTrue(cow.isNoAi(), "a frozen mob should have NoAi forced on");
        helper.assertTrue(cow.getPersistentData().contains(PickerFreezeService.PREV_NO_AI_TAG),
                "freezing should write the recovery marker");
        helper.assertTrue(!cow.getPersistentData().getBoolean(PickerFreezeService.PREV_NO_AI_TAG),
                "the marker should hold the pre-freeze NoAi value (false)");

        PickerFreezeService.unfreeze(helper.getLevel().getServer(), editor);
        helper.assertTrue(!cow.isNoAi(), "unfreezing should restore the pre-freeze NoAi value");
        helper.assertTrue(!cow.getPersistentData().contains(PickerFreezeService.PREV_NO_AI_TAG),
                "unfreezing should strip the recovery marker");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void freezePreservesAlreadyForcedNoAi(GameTestHelper helper) {
        // spawnWithNoFreeWill leaves NoAi on — the "mob was already NoAi before the picker" case.
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        UUID editor = UUID.randomUUID();

        PickerFreezeService.freeze(helper.getLevel(), editor, cow.getUUID());
        PickerFreezeService.unfreeze(helper.getLevel().getServer(), editor);
        helper.assertTrue(cow.isNoAi(), "a mob that was NoAi before freezing should stay NoAi after release");
        helper.assertTrue(!cow.getPersistentData().contains(PickerFreezeService.PREV_NO_AI_TAG),
                "the marker should still be stripped on release");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void freezeRefusesSecondEditor(GameTestHelper helper) {
        Cow cow = spawnCow(helper, new BlockPos(2, 2, 2));
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        helper.assertTrue(PickerFreezeService.freeze(helper.getLevel(), first, cow.getUUID()) == null,
                "the first editor should freeze the mob");
        String refusal = PickerFreezeService.freeze(helper.getLevel(), second, cow.getUUID());
        helper.assertTrue(refusal != null, "a second editor must be refused (a second capture would read"
                + " the forced NoAi as the original)");
        helper.assertTrue(cow.isNoAi(), "the refused attempt must not disturb the freeze");

        PickerFreezeService.unfreeze(helper.getLevel().getServer(), first);
        helper.assertTrue(!cow.isNoAi(), "the first editor's release should still restore the mob");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void switchingMobsReleasesThePreviousOne(GameTestHelper helper) {
        Cow first = spawnCow(helper, new BlockPos(2, 2, 2));
        Cow second = spawnCow(helper, new BlockPos(4, 2, 2));
        UUID editor = UUID.randomUUID();

        PickerFreezeService.freeze(helper.getLevel(), editor, first.getUUID());
        PickerFreezeService.freeze(helper.getLevel(), editor, second.getUUID());
        helper.assertTrue(!first.isNoAi(), "choosing a new mob should release the previous one");
        helper.assertTrue(!first.getPersistentData().contains(PickerFreezeService.PREV_NO_AI_TAG),
                "the previous mob's marker should be stripped");
        helper.assertTrue(second.isNoAi(), "the newly chosen mob should be frozen");

        PickerFreezeService.unfreeze(helper.getLevel().getServer(), editor);
        helper.assertTrue(!second.isNoAi(), "release should restore the second mob");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void staleMarkerIsRestoredOnJoin(GameTestHelper helper) {
        Cow cow = spawnCow(helper, new BlockPos(2, 2, 2));
        UUID editor = UUID.randomUUID();

        PickerFreezeService.freeze(helper.getLevel(), editor, cow.getUUID());
        // A hard crash drops the live records without restoring; the marker survives with the mob.
        PickerFreezeService.clear();
        helper.assertTrue(cow.isNoAi(), "precondition: the mob is still stuck NoAi after the 'crash'");

        PickerFreezeService.onMobJoin(cow);
        helper.assertTrue(!cow.isNoAi(), "a stale marker must restore the pre-freeze NoAi on next join");
        helper.assertTrue(!cow.getPersistentData().contains(PickerFreezeService.PREV_NO_AI_TAG),
                "the stale marker must be stripped after recovery");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void joinDuringLiveEditReassertsTheFreeze(GameTestHelper helper) {
        Cow cow = spawnCow(helper, new BlockPos(2, 2, 2));
        UUID editor = UUID.randomUUID();

        PickerFreezeService.freeze(helper.getLevel(), editor, cow.getUUID());
        // The mob's chunk reloading mid-edit re-fires the join event while the record is still live.
        PickerFreezeService.onMobJoin(cow);
        helper.assertTrue(cow.isNoAi(), "a join during a live edit must keep the mob frozen");
        helper.assertTrue(cow.getPersistentData().contains(PickerFreezeService.PREV_NO_AI_TAG),
                "the marker must survive a live-edit join (it's still needed for crash recovery)");

        PickerFreezeService.unfreeze(helper.getLevel().getServer(), editor);
        helper.assertTrue(!cow.isNoAi(), "release after the reload should still restore the mob");
        helper.succeed();
    }
}
