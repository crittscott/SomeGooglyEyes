package com.github.crittscott.somegoogly.clientgametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;

/**
 * Client test plan section 5: getting eyes, by shears kills and Optometrist. Shots show the HUD
 * where counts matter.
 */
public final class HarvestClientTests implements FabricClientGameTest {
    /**
     * In-game: Optometrist shears on an eyed boat with the main hand, and on a chest boat with the
     * offhand.
     */
    private static void boats(Scene s) {
        s.water(-5, -3, 5, 3);
        String boat = s.boat("minecraft:oak_boat", -2, 0, 90);
        String chest = s.boat("minecraft:oak_chest_boat", 2, 0, 90);
        s.useOnEntity(boat, -2, 2.5, 2.5, Scene.SLIMY);
        s.useOnEntity(chest, 2, 2.5, 2.5, Scene.SLIMY);
        s.gamemode("survival");
        s.hud(true);
        s.useOnEntity(boat, -2, 0.5, 2.5, Scene.OPTOMETRIST);
        s.cameraAt(-2, 1, 4, -2, 0, 0);
        s.shot(
                "Main hand: the oak boat has lost its eyes, one Googly Eye lies by it, the shears' durability bar dropped, and the player is not seated.");
        s.clearInventory();
        s.slot("weapon.offhand", Scene.OPTOMETRIST);
        s.cameraOn(2, 0.5, 2.5, chest);
        s.hold(s.options().keyShift);
        s.use();
        s.release(s.options().keyShift);
        s.cameraAt(2, 1, 4, 2, 0, 0);
        s.shot(
                "Offhand while sneaking with an empty main hand: the chest boat has lost its eyes, one eye dropped, offhand shears worn, no chest screen open.");
    }

    /**
     * In-game: Optometrist and denied attempts: spectator, adventure mode at a head, out of reach.
     */
    private static void denied(Scene s) {
        String cow = s.mob("minecraft:cow", 0, 0, 0);
        s.eyes(cow);
        s.hud(true);
        s.gamemode("spectator");
        s.useOnEntity(cow, 0, 1.6, 2.5, Scene.OPTOMETRIST);
        s.cameraOn(0, 1.4, 3.5, cow);
        s.shot(
                "Spectator using Optometrist shears on an eyed cow: the cow keeps its eyes and nothing dropped.");
        s.gamemode("creative");
        s.eyedHead("minecraft:zombie_head[rotation=0]", 4, 0, 0);
        s.gamemode("adventure");
        s.useOnBlock(4, 0, 0, Scene.OPTOMETRIST);
        s.cameraAt(4, 1.2, 2.5, 4, 0.3, 0);
        s.shot(
                "Adventure mode using Optometrist shears on an eyed head: the head keeps its eyes, nothing dropped, shears unworn.");
        s.gamemode("survival");
        s.useOnEntity(cow, 0, 1.6, 7, Scene.OPTOMETRIST);
        s.cameraOn(0, 1.4, 3.5, cow);
        s.shot(
                "Optometrist shears used from beyond reach: the cow keeps its eyes, nothing dropped, shears unworn.");
    }

    /** In-game: harvest an eye from a blue-eyed glowing cow and frame it in a dark room. */
    private static void keepsAppearance(Scene s) {
        s.config("harvestOnKillPercent", "100");
        String cow = s.mob("minecraft:cow", 0, 0, 0, 0, "Health:1f");
        s.eyes(cow, "tint iris 0.1 0.2 1", "glow on");
        s.clearInventory();
        s.give("minecraft:shears");
        s.hotbar(0);
        s.cameraOn(0, 1.6, 2.2, cow);
        s.attack();
        s.waitTicks(20);
        s.camera(0, 0, 0, 0, 90);
        s.waitTicks(20);
        s.darkRoom();
        String frame = s.frame(0, 1, -3, "south", "", 0, false, false);
        s.hotbar(1);
        s.useHeldOn(frame, 0, 1, 0);
        s.cameraAt(0, 1.6, 2, 0, 1.5, -3);
        s.shot("In a dark room, the harvested eye in a frame: blue iris, glowing.");
    }

    /** In-game: Optometrist shears on an eyed cow, in survival. */
    private static void optometristMob(Scene s) {
        String cow = s.mob("minecraft:cow", 0, 0, 0);
        s.eyes(cow);
        s.gamemode("survival");
        s.hud(true);
        s.cameraOn(0, 1.4, 3.5, cow);
        s.shot("Before: an eyed cow; the hotbar is empty.");
        s.useOnEntity(cow, 0, 1.6, 2.5, Scene.OPTOMETRIST);
        s.waitTicks(5);
        s.shot(
                "After Optometrist shears: the cow has no eyes and no damage flash, one Googly Eye lies on the ground, the shears' durability bar dropped, and a shearing subtitle shows.");
    }

    /** In-game: Optometrist shears on an eyed placed head. */
    private static void optometristHead(Scene s) {
        s.eyedHead("minecraft:creeper_head[rotation=0]", 0, 0, 0);
        s.useOnBlock(0, 0, 0, Scene.OPTOMETRIST);
        s.waitTicks(5);
        s.cameraAt(0, 1.2, 2.5, 0, 0.3, 0);
        s.shot("The creeper head no longer has eyes, and one Googly Eye lies beside it.");
    }

    /** In-game: plain shears on an eyed sheep and an eyed boat. */
    private static void plainShears(Scene s) {
        String sheep = s.mob("minecraft:sheep", -3, 0, 0);
        s.eyes(sheep);
        s.useOnEntity(sheep, -3, 1.6, 2.5, "minecraft:shears");
        s.cameraOn(-3, 1.4, 3.5, sheep);
        s.shot("Plain shears on an eyed sheep: wool dropped, the sheep keeps its eyes.");
        s.water(1, -2, 5, 2);
        String boat = s.boat("minecraft:oak_boat", 3, 0, 90);
        s.useOnEntity(boat, 3, 2.5, 2.5, Scene.SLIMY);
        s.useOnEntity(boat, 3, 0.5, 2.5, "minecraft:shears");
        s.view(CameraType.THIRD_PERSON_BACK);
        s.shot(
                "Plain shears on an eyed boat: the player is seated in it and the boat keeps its eyes.");
    }

    /** In-game: remove your own eyes with Optometrist shears, then with plain shears. */
    private static void selfRemoval(Scene s) {
        s.gamemode("survival");
        s.hud(true);
        s.useOnSelf(Scene.SLIMY);
        s.useOnSelf(Scene.OPTOMETRIST);
        s.camera(0, 0, 0, 0, 0);
        s.view(CameraType.THIRD_PERSON_FRONT);
        s.shot(
                "After sneak-using Optometrist shears on the air: the player has no eyes, full health, and an eye lies nearby.");
        s.view(CameraType.FIRST_PERSON);
        s.useOnSelf(Scene.SLIMY);
        s.useOnSelf("minecraft:shears");
        s.camera(0, 0, 0, 0, 0);
        s.view(CameraType.THIRD_PERSON_FRONT);
        s.shot(
                "After sneak-using plain shears on the air: the player has no eyes and has lost health (a damage flash or missing hearts).");
    }

    /** In-game: shears kill an eyed cow, an indirect kill, and a shears kill with mob loot off. */
    private static void shearsKill(Scene s) {
        s.config("harvestOnKillPercent", "100");
        s.gamemode("survival");
        s.hud(true);
        String first = s.mob("minecraft:cow", -3, 0, 0, 0, "Health:1f");
        String second = s.mob("minecraft:cow", 0, 0, 0, 0, "Health:1f");
        String third = s.mob("minecraft:cow", 3, 0, 0, 0, "Health:1f");
        for (String cow : new String[] {first, second, third}) {
            s.gamemode("creative");
            s.eyes(cow);
        }
        s.gamemode("survival");
        s.clearInventory();
        s.give("minecraft:shears");
        s.hotbar(0);
        s.cameraOn(-3, 1.6, 2.6, first);
        s.attack();
        s.waitTicks(10);
        s.cameraAt(-3, 2, 3.5, -3, 0, 0);
        s.shot(
                "A cow killed with shears: a Googly Eye among the drops; the shears' durability bar has dropped by one use.");
        s.server("damage %s 5 minecraft:arrow", Scene.sel(second));
        s.waitTicks(10);
        s.cameraAt(0, 2, 3.5, 0, 0, 0);
        s.shot("A cow killed by an arrow-type damage, not shears: no Googly Eye among the drops.");
        s.server("gamerule doMobLoot false");
        s.cameraOn(3, 1.6, 2.6, third);
        s.attack();
        s.waitTicks(10);
        s.cameraAt(3, 2, 3.5, 3, 0, 0);
        s.shot("A cow killed with shears while doMobLoot is off: nothing dropped.");
    }

    /** In-game: with a datapack putting a stick in c:tools/shear, kill an eyed cow with a stick. */
    private static void taggedShears(Scene s) {
        s.datapack(TestPacks.SHEARS);
        s.config("harvestOnKillPercent", "100");
        String cow = s.mob("minecraft:cow", 0, 0, 0, 0, "Health:1f");
        s.eyes(cow);
        s.clearInventory();
        s.give("minecraft:stick");
        s.hotbar(0);
        s.cameraOn(0, 1.6, 2.6, cow);
        s.attack();
        s.waitTicks(10);
        s.cameraAt(0, 2, 3.5, 0, 0, 0);
        s.shot("A cow killed with a stick tagged as shears: a Googly Eye among the drops.");
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        Scene.run(
                context,
                Scene.MAIN,
                s -> {
                    s.scenario("5.1", "Shears kill drops an eye", () -> shearsKill(s));
                    s.scenario("5.2", "Tagged item counts as shears", () -> taggedShears(s));
                    s.scenario("5.3", "Harvest keeps appearance", () -> keepsAppearance(s));
                    s.scenario("5.4", "Optometrist on a mob", () -> optometristMob(s));
                    s.scenario("5.5", "Plain shears unchanged", () -> plainShears(s));
                    s.scenario("5.6", "Optometrist on a head", () -> optometristHead(s));
                    s.scenario("5.7", "Optometrist on boats, both hands", () -> boats(s));
                    s.scenario("5.8", "Removing your own eyes", () -> selfRemoval(s));
                    s.scenario("5.9", "Denied attempts change nothing", () -> denied(s));
                });
    }
}
