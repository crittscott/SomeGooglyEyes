package com.github.crittscott.somegoogly.clientgametest;

import com.github.crittscott.somegoogly.registry.ModContent;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;

/**
 * Client test plan section 7: crafting. Ingredients go into a crafting table's grid by slot clicks;
 * results whose glow matters are framed in a dark room, where glow is visible.
 */
public final class CraftingClientTests implements FabricClientGameTest {
    private static final String GLOWING_RED = Scene.googly("{irisColor:" + Scene.RED + ",glow:1b}");

    /** Puts the item in hotbar slot 0 into a new empty frame on the dark room's back wall. */
    private static void frameHeld(Scene s, int dx) {
        String frame = s.frame(dx, 1, -4, "south", "", 0, false, false);
        s.hotbar(0);
        s.useHeldOn(frame, dx, 0, -2);
    }

    /** In-game: craft a red eye, then add glowstone to it. */
    private static void stacking(Scene s) {
        s.darkRoom();
        s.craft(Scene.GOOGLY, "minecraft:red_dye");
        s.takeCraftResult();
        s.give("minecraft:glowstone_dust");
        s.craftFromHotbar(2);
        s.takeCraftResult();
        frameHeld(s, 0);
        s.cameraAt(0, 1.6, 1, 0, 1.5, -4);
        s.shot(
                "A dark room: the eye made with red dye and then glowstone has a red iris and glows.");
    }

    /** In-game: craft a Googly Eye with red dye. */
    private static void dye(Scene s) {
        s.craft(Scene.GOOGLY, "minecraft:red_dye");
        s.shot("The crafting table's result slot shows a Googly Eye with a red iris.");
    }

    /** In-game: craft with glowstone, redstone, and cobweb and frame the results in a dark room. */
    private static void glowModifiers(Scene s) {
        s.darkRoom();
        s.craft(GLOWING_RED, "minecraft:redstone");
        s.takeCraftResult();
        frameHeld(s, -2);
        s.craft(GLOWING_RED, "minecraft:cobweb");
        s.takeCraftResult();
        frameHeld(s, 0);
        s.craft(Scene.GOOGLY, "minecraft:glowstone_dust");
        s.takeCraftResult();
        frameHeld(s, 2);
        s.cameraAt(0, 1.6, 1, 0, 1.5, -4);
        s.shot(
                "A dark room. Left: red eye, not glowing (redstone). Middle: plain white eye, not glowing (cobweb cleared everything). Right: plain eye, glowing (glowstone).");
    }

    /** In-game: name a red eye, craft it with a slimeball, and select it. */
    private static void slimeKeepsName(Scene s) {
        s.craft(
                Scene.GOOGLY
                        + "[custom_name=\"Bob\",somegoogly:eye_properties={irisColor:"
                        + Scene.RED
                        + "}]",
                "minecraft:slime_ball");
        s.shot("The result slot shows a Slimy Eye with a red iris.");
        s.takeCraftResult();
        s.hud(true);
        s.hotbar(1);
        s.hotbar(0);
        s.shot(
                "The Slimy Eye selected in the hotbar: its name, Bob, shows above the hotbar, and its icon has a red iris.");
    }

    /** In-game: start a survival world and pick up a Googly Eye. */
    private static void recipeUnlock(Scene s) {
        s.newWorld(true);
        s.area();
        s.gamemode("survival");
        s.hud(true);
        s.camera(0, 0, 0, 0, 30);
        s.shot("A new survival world before picking anything up: no recipe toast.");
        s.server(
                "summon minecraft:item %s %d %s {Item:{id:\"%s\",count:1}}",
                String.valueOf(s.x(0)), Scene.GROUND, String.valueOf(s.z(0)), Scene.GOOGLY);
        s.waitTicks(10);
        s.shot(
                "Just after picking up a Googly Eye: a \"New Recipes Unlocked\" toast in the top right shows the Slimy Eye.");
    }

    /** In-game: harvest from a red cow, add glowstone, slime it, apply it, and harvest again. */
    private static void roundTrip(Scene s) {
        s.config("harvestOnKillPercent", "100");
        String first = s.mob("minecraft:cow", -2, 0, 0, 0, "Health:1f");
        s.eyes(first, "tint iris 1 0 0");
        String second = s.mob("minecraft:cow", 2, 0, 0);
        s.clearInventory();
        s.give("minecraft:shears");
        s.hotbar(0);
        s.cameraOn(-2, 1.6, 2.2, first);
        s.attack();
        s.waitTicks(15);
        s.camera(-2, 0, 0, 0, 90);
        s.waitTicks(15);
        s.server("item replace entity @p hotbar.0 from entity @p hotbar.1");
        s.server("item replace entity @p hotbar.1 with minecraft:glowstone_dust");
        s.craftFromHotbar(2);
        s.takeCraftResult();
        s.slot("hotbar.1", "minecraft:slime_ball");
        s.craftFromHotbar(2);
        s.takeCraftResult();
        s.useHeldOn(second, 2, 1.6, 2.5);
        s.cameraOn(2, 1.4, 3.5, second);
        s.shot("The second cow after the crafted Slimy Eye was applied: red irises.");
        s.clearInventory();
        s.give(Scene.OPTOMETRIST);
        s.hotbar(0);
        s.useHeldOn(second, 2, 1.6, 2.5);
        s.camera(2, 0, 0, 0, 90);
        s.waitTicks(15);
        s.server("item replace entity @p hotbar.0 from entity @p hotbar.1");
        s.darkRoom();
        frameHeld(s, 0);
        s.cameraAt(0, 1.6, 1, 0, 1.5, -4);
        s.shot(
                "A dark room: the eye harvested after the full round trip has a red iris and glows.");
    }

    /** In-game: open the Some Googly Eyes creative tab, and frame a creative Googly Eye. */
    private static void creativeTab(Scene s) {
        s.press(s.options().keyInventory);
        s.context.waitForScreen(CreativeModeInventoryScreen.class);
        s.context.runOnClient(mc ->
                ((FabricCreativeInventoryScreen) mc.screen)
                        .setSelectedItemGroup(ModContent.CREATIVE_TAB.get()));
        s.waitTicks(5);
        s.shot(
                "The Some Googly Eyes creative tab: a Googly Eye, a Slimy Eye, and an Optometrist enchanted book.");
        s.closeScreen();
        s.frame(0, 1, -3, "south", "{id:\"" + Scene.GOOGLY + "\",count:1}", 0, false, false);
        s.cameraAt(0, 1.6, 1, 0, 1.5, -3);
        s.shot("A creative Googly Eye in a frame: white cornea, black iris, no glow.");
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        Scene.run(
                context,
                Scene.MAIN,
                s -> {
                    s.scenario("7.1", "Dye sets iris", () -> dye(s));
                    s.scenario("7.2", "Glow modifiers and cobweb", () -> glowModifiers(s));
                    s.scenario("7.3", "Modifiers stack", () -> stacking(s));
                    s.scenario("7.4", "Slime keeps appearance and name", () -> slimeKeepsName(s));
                    s.scenario("7.6", "Round trip keeps appearance", () -> roundTrip(s));
                    s.scenario("7.7", "Creative tab", () -> creativeTab(s));
                    // Last: it moves the class to a new survival world.
                    s.scenario("7.5", "Slimy Eye recipe unlocks", () -> recipeUnlock(s));
                });
    }
}
