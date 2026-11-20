package com.github.crittscott.somegoogly.clientgametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;

/**
 * Client settings Fabric reads only at game start (plan 9.4 and 12.2). Each group runs only in the launch started for
 * it ({@code runClientGameTestHideAll}, {@code ...HideEntities}, {@code ...HideMods}), whose run directory gets the
 * matching {@code somegoogly-client.toml} from {@code src/clientGametest/launch/<group>} before the game starts.
 */
public final class LaunchGroupClientTests implements FabricClientGameTest {
    private static final String RED = Scene.googly("{irisColor:" + Scene.RED + "}");

    /** Builds a scene of eyed things: cow, zombie, zombie head, oak and birch boats, a framed eye, a held eye. */
    private static void eyedScene(Scene s) {
        s.wideView();
        String cow = s.mob("minecraft:cow", -5, 0, 0);
        s.eyes(cow);
        String zombie = s.mob("minecraft:zombie", -2.5, 0, 0);
        s.eyes(zombie);
        s.eyedHead("minecraft:zombie_head[rotation=0]", 0, 0, 0);
        s.water(2, -1, 7, 1);
        String oak = s.boat("minecraft:oak_boat", 3, 0, 90);
        String birch = s.boat("minecraft:birch_boat", 6, 0, 90);
        s.useOnEntity(oak, 3, 2.5, 2.5, Scene.SLIMY);
        s.useOnEntity(birch, 6, 2.5, 2.5, Scene.SLIMY);
        s.frame(0, 2, -3, "south", Scene.googlyNbt("{irisColor:" + Scene.RED + "}"), 0, false, false);
        s.clearInventory();
        s.give(RED);
        s.hotbar(0);
        s.hud(true);
        s.camera(0, 1, 7, 180, 10);
        s.view(CameraType.THIRD_PERSON_BACK);
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        Scene.run(context, "hideAll", s -> {
            s.scenario("9.4", "Hide settings do not change the verdict", () -> {
                String cow = s.mob("minecraft:cow", 0, 0, 0);
                s.eyes(cow);
                TargetCheckClientTests.inspect(s, cow, 0, 0, 2.5, Scene.GOOGLY,
                        "disableGooglyEyes is on: the eyed cow shows no eyes, but the check still says it already has eyes.");
            });
            s.scenario("12.2.1", "disableGooglyEyes", () -> {
                eyedScene(s);
                s.shot("disableGooglyEyes is on: no eyes on the cow, zombie, head, or boats; the framed, held, and hotbar eyes show centered, still pupils.");
            });
        });
        Scene.run(context, "hideEntities", s -> s.scenario("12.2.2", "disabledEntities", () -> {
            eyedScene(s);
            s.shot("disabledEntities lists the zombie and the oak boat: the zombie, the zombie head, and the oak boat show no eyes; the cow and the birch boat do.");
        }));
        Scene.run(context, "hideMods", s -> s.scenario("12.2.3", "disabledMods", () -> {
            eyedScene(s);
            s.shot("disabledMods lists minecraft: no vanilla mob, head, or boat shows eyes.");
        }));
    }
}
