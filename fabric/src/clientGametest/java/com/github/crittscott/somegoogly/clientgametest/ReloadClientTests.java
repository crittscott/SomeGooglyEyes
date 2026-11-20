package com.github.crittscott.somegoogly.clientgametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;

/** Client test plan section 13: datapack reloads and resource reloads. */
public final class ReloadClientTests implements FabricClientGameTest {
    /** In-game: with an eyed cow and boat in view, enable datapacks that change their layouts. */
    private static void layoutChange(Scene s) {
        String cow = s.mob("minecraft:cow", -2, 0, 0);
        s.eyes(cow);
        s.water(1, -1, 4, 1);
        String boat = s.boat("minecraft:oak_boat", 2.5, 0, 90);
        s.useOnEntity(boat, 2.5, 2.5, 2.5, Scene.SLIMY);
        s.cameraAt(0, 1.6, 4, 0, 0.8, 0);
        s.shot("Bundled layouts: the cow's eyes on its face (or flank), the boat's eyes near its front.");
        s.datapack(TestPacks.LAYOUT_A);
        s.shot("After a datapack raised the cow's eyes and reloaded: the cow's eyes sit higher on its face; nothing remains where they were.");
        s.datapack(TestPacks.LAYOUT_B);
        s.shot("After a second datapack enlarged the cow's eyes and moved the boat's toward the middle: both changed, with no trace of the old positions.");
    }

    /** In-game: disable the cow definition, reload, then enable it again. */
    private static void disableAndRestore(Scene s) {
        String cow = s.mob("minecraft:cow", 0, 0, 0);
        s.eyes(cow, "tint iris 1 0 0");
        s.cameraOn(0, 1.4, 3.5, cow);
        s.shot("A cow with red-irised eyes.");
        s.datapack(TestPacks.COW_OFF);
        s.shot("Cow definition disabled by a datapack and reloaded: no eyes shown.");
        s.datapackOff(TestPacks.COW_OFF);
        s.shot("Definition enabled again: the same red-irised eyes are back.");
    }

    /** In-game: with eyed things all around, reload resource packs (F3+T). */
    private static void resourceReload(Scene s) {
        String cow = s.mob("minecraft:cow", -3, 0, 0);
        s.eyes(cow);
        s.eyedHead("minecraft:zombie_head[rotation=0]", -1, 0, 0);
        s.water(1, -1, 4, 1);
        String boat = s.boat("minecraft:oak_boat", 2.5, 0, 90);
        s.useOnEntity(boat, 2.5, 2.5, 2.5, Scene.SLIMY);
        s.frame(0, 2, -3, "south", Scene.googlyNbt("{irisColor:" + Scene.RED + "}"), 0, false, false);
        s.clearInventory();
        s.give(Scene.googly("{irisColor:" + Scene.RED + "}"), 2);
        s.hotbar(0);
        s.hud(true);
        s.camera(0, 0.5, 5, 180, 10);
        s.view(CameraType.THIRD_PERSON_BACK);
        s.shot("Before a resource reload: cow, head, boat, framed eye, held eye, and hotbar eye all show eyes.");
        s.context.runOnClient(mc -> mc.reloadResourcePacks());
        s.context.waitFor(mc -> mc.getOverlay() == null, 1200);
        s.waitTicks(10);
        s.shot("After the resource reload: the same eyes everywhere, as in the previous shot.");
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        Scene.run(context, Scene.MAIN, s -> {
            s.scenario("13.1", "Changed layouts apply on reload", () -> layoutChange(s));
            s.scenario("13.2", "Disabled and restored definition", () -> disableAndRestore(s));
            s.scenario("13.3", "Resource reload", () -> resourceReload(s));
        });
    }
}
