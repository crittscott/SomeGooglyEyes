package com.github.crittscott.somegoogly.gametest;

import net.minecraft.network.chat.contents.TranslatableContents;
import com.github.crittscott.somegoogly.network.fabric.FabricNetworkTransport;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

/**
 * Fabric GameTest entry points for the shared core and picker-spawn assertions. Listed under the
 * {@code somegoogly_gametest} dev-mod's {@code fabric-gametest}
 * entrypoint for annotation-based discovery.
 */
public final class SomeGooglyGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(structure = TEMPLATE, maxTicks = 20)
    public void commandSpawnFinalizesBeforeApplyingPickerState(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.commandSpawnFinalizesBeforeApplyingPickerState(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 20)
    public void spawnCommandsRefuseDragonPlayersAndExcludedTypes(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.spawnCommandsRefuseDragonPlayersAndExcludedTypes(helper);
    }

    /**
     * Exercises Fabric's persistent-data attachment save/load rather than only the shared in-memory boundary.
     * In game: see {@link SomeGooglyGameTestsLogic#entityPersistentDataSurvivesSaveLoad}.
     */
    @GameTest(structure = TEMPLATE, maxTicks = 20)
    public void fabricEntityPersistentDataSurvivesSaveLoad(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.entityPersistentDataSurvivesSaveLoad(helper, "Fabric");
    }

    @GameTest(structure = TEMPLATE, maxTicks = 20)
    public void conversionKeepsEyeState(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.conversionKeepsEyeState(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 20)
    public void googlyEyeItemStoresAppearanceOverride(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.googlyEyeItemStoresAppearanceOverride(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 20)
    public void optometristAcceptsOnlyShears(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.optometristAcceptsOnlyShears(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 20)
    public void deathHarvestRejectsNonqualifyingKills(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.deathHarvestRejectsNonqualifyingKills(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void realShearsKillsHarvestAnEye(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.realShearsKillsHarvestAnEye(helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void requiredClientRefusalCarriesItsText(GameTestHelper helper) {
        TranslatableContents refusal = (TranslatableContents) FabricNetworkTransport.requiredClientMessage().getContents();
        helper.assertTrue(refusal.getFallback() != null && !refusal.getFallback().equals(refusal.getKey()),
                Component.literal("the refusal should carry the English text for a client without our language file"));
        helper.succeed();
    }

    /** Fabric's copy event; see the shared test for the End-exit and death steps. */
    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void nativePlayerCopiesKeepEyesOnlyWhileAlive(GameTestHelper helper) {
        LoaderInteractionGameTestsLogic.playerCopiesKeepEyesOnlyWhileAlive(helper, FakePlayer.get(helper.getLevel()),
                (original, replacement, alive) -> ServerPlayerEvents.COPY_FROM.invoker()
                        .copyFromPlayer(original, replacement, alive));
    }

    /**
     * Fabric's registered use callbacks; see the shared test for the in-game steps. Protection-listener ordering is
     * checked by hand, so no refusal is exercised here.
     */
    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void nativeUseHooksHonorResults(GameTestHelper helper) {
        LoaderInteractionGameTestsLogic.useHooksHonorResults(helper, FakePlayer.get(helper.getLevel()),
                (player, target, hit, refused) -> {
                    if (hit != null) {
                        return UseBlockCallback.EVENT.invoker().interact(player, helper.getLevel(), InteractionHand.MAIN_HAND, hit);
                    }
                    if (target != null) {
                        return UseEntityCallback.EVENT.invoker().interact(player, helper.getLevel(), InteractionHand.MAIN_HAND,
                                target, target instanceof ArmorStand ? new EntityHitResult(target, target.position()) : null);
                    }
                    return UseItemCallback.EVENT.invoker().interact(player, helper.getLevel(), InteractionHand.MAIN_HAND);
                }, false);
    }
}
