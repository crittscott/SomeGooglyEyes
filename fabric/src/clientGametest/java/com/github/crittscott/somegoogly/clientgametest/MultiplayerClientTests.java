package com.github.crittscott.somegoogly.clientgametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.PauseScreen;

/** Client test plan section 11: a dedicated server with this client connected as an operator. */
public final class MultiplayerClientTests implements FabricClientGameTest {
    /**
     * In-game: apply a tinted Slimy Eye on a server, reconnect, then leave tracking range and
     * return.
     */
    private static void joinAndTrack(Scene s) {
        String cow = s.mob("minecraft:cow", 0, 0, 0);
        s.useOnEntity(cow, 0, 1.6, 2.5, ApplyClientTests.slimy("{irisColor:" + Scene.GREEN + "}"));
        s.reconnect();
        s.cameraOn(0, 1.4, 3.5, cow);
        s.waitTicks(10);
        s.shot("After reconnecting: the cow has green-irised eyes.");
        s.camera(0, 0, 300, 0, 0);
        s.waitTicks(60);
        s.cameraOn(0, 1.4, 3.5, cow);
        s.waitTicks(10);
        s.shot("After leaving tracking range and returning: the cow still has green-irised eyes.");
    }

    /** In-game: while watching an eyed cow, turn the server's master switch off and on. */
    private static void masterSwitch(Scene s) {
        String cow = s.mob("minecraft:cow", 0, 0, 0);
        s.eyes(cow);
        s.cameraOn(0, 1.4, 3.5, cow);
        s.config("googlyEyesEnabled", "false");
        s.shot(
                "Master switch turned off on the server: the cow shows no eyes, without reconnecting.");
        s.config("googlyEyesEnabled", "true");
        s.shot("Master switch back on: the cow's eyes are back.");
    }

    /** In-game: watch a wandering cow, open the pause menu for a few seconds, and close it. */
    private static void noPause(Scene s) {
        String cow = s.liveMob("minecraft:cow", 0, 0, 0, "");
        s.camera(0, 3, 8, 180, 15);
        s.shot("A cow wandering on the platform.");
        s.context.setScreen(() -> new PauseScreen(true));
        s.waitTicks(100);
        s.closeScreen();
        s.shot("After five seconds in the pause menu: the cow has moved; the server kept running.");
        s.server("kill %s", Scene.sel(cow));
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        Scene.runDedicated(
                context,
                s -> {
                    s.scenario(
                            "11.1", "Joining and tracking see saved eyes", () -> joinAndTrack(s));
                    s.scenario("11.2", "Master switch reaches clients", () -> masterSwitch(s));
                    s.scenario("11.3", "Menus do not pause the server", () -> noPause(s));
                });
    }
}
