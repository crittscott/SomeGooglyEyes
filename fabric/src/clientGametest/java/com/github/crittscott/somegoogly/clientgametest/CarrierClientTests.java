package com.github.crittscott.somegoogly.clientgametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;

import java.util.List;

/**
 * Client test plan section 1: eyes on mobs, players, placed heads, and boats.
 *
 * <p>Mobs face south (yaw 0) and the camera usually stands south of them, looking back.
 */
public final class CarrierClientTests implements FabricClientGameTest {
    /**
     * Property naming a GeckoLib-rendered mob for 1.1.8, which needs a GeckoLib-based mob mod in
     * the run.
     */
    static final String GECKOLIB_MOB_PROPERTY = "somegoogly.clientgametest.geckolibMob";

    private static final List<String> BOAT_WOODS =
            List.of(
                    "oak",
                    "spruce",
                    "birch",
                    "jungle",
                    "acacia",
                    "cherry",
                    "dark_oak",
                    "mangrove",
                    "pale_oak");
    private static final List<String> HEADS =
            List.of(
                    "skeleton_skull",
                    "wither_skeleton_skull",
                    "zombie_head",
                    "creeper_head",
                    "piglin_head",
                    "player_head");
    private static final List<String> WALL_HEADS =
            List.of(
                    "skeleton_wall_skull",
                    "wither_skeleton_wall_skull",
                    "zombie_wall_head",
                    "creeper_wall_head",
                    "piglin_wall_head",
                    "player_wall_head");

    /** In-game: give a cow eyes with a red iris and look at it from the front and from an angle. */
    private static void adultMob(Scene s) {
        String cow = s.mob("minecraft:cow", 0, 0, 0);
        s.eyes(cow, "tint iris 1 0 0");
        s.cameraOn(0, 1.2, 4, cow);
        s.shot(
                "A cow facing the camera with two eyes on its face (or, for the rare body variant, on its flank), red irises and white corneas.");
        s.cameraOn(3, 1.6, 3, cow);
        s.shot("The same cow from a three-quarter angle; the eyes sit on the model, red irises.");
    }

    /**
     * In-game: with spawn chance 100, summon an adult cow, a baby cow, and a baby armadillo;
     * compare.
     */
    private static void babies(Scene s) {
        s.config("globalPercent", "100");
        s.mob("minecraft:cow", -2.5, 0, 0, 0, "");
        s.mob("minecraft:cow", 0, 0, 0, 0, "Age:-24000");
        s.mob("minecraft:armadillo", 2.5, 0, 0, 0, "Age:-24000");
        s.cameraAt(0, 1.5, 5, 0, 0.6, 0);
        s.shot(
                "Left: adult cow with eyes. Middle: baby cow with eyes in the baby layout. Right: baby armadillo with no eyes.");
    }

    /**
     * Places every boat variant on water in two rows, boats behind and chest boats in front;
     * returns their tags.
     */
    static String[] boatRows(Scene s) {
        s.water(-12, -6, 12, 6);
        String[] tags = new String[20];
        for (int i = 0; i < BOAT_WOODS.size(); i++) {
            tags[i] = s.boat("minecraft:" + BOAT_WOODS.get(i) + "_boat", -10 + i * 2.3, -3, 90);
            tags[10 + i] =
                    s.boat("minecraft:" + BOAT_WOODS.get(i) + "_chest_boat", -10 + i * 2.3, 3, 90);
        }
        tags[9] = s.boat("minecraft:bamboo_raft", -10 + 9 * 2.3, -3, 90);
        tags[19] = s.boat("minecraft:bamboo_chest_raft", -10 + 9 * 2.3, 3, 90);
        return tags;
    }

    /**
     * In-game: place every boat, chest boat, and raft with spawn chance 100; none starts with eyes.
     */
    private static void boatsStartEyeless(Scene s) {
        s.wideView();
        s.config("globalPercent", "100");
        boatRows(s);
        s.cameraAt(0, 1.5, 9, 0, 0, 3);
        s.shot("The chest boats and chest raft from the side: none has eyes.");
        s.cameraAt(0, 1.5, -9, 0, 0, -3);
        s.shot("The boats and raft from the side: none has eyes.");
    }

    /**
     * In-game: apply a Slimy Eye to every boat variant; look from both sides and the front, then
     * with passengers.
     */
    private static void boatsEyed(Scene s) {
        s.wideView();
        String[] tags = boatRows(s);
        for (int i = 0; i < tags.length; i++) {
            double dx = -10 + (i % 10) * 2.3;
            double dz = i < 10 ? -3 : 3;
            s.useOnEntity(tags[i], dx, 2.5, dz + (i < 10 ? -2.5 : 2.5), Scene.SLIMY);
        }
        s.cameraAt(0, 1.5, -9, 0, 0, -3);
        s.shot(
                "Boats and raft from their north side: on every hull, one eye on this side near the bow (right end), facing outward.");
        s.cameraAt(0, 1.5, 9, 0, 0, 3);
        s.shot(
                "Chest boats and chest raft from their south side: on every hull, one eye on this side near the bow (left end), facing outward.");
        s.cameraAt(0, 1.5, -1, 0, 0, -3);
        s.shot(
                "Boats and raft from their south side, between the rows: on every hull, one eye on this side too, near the bow (left end).");
        for (String tag : tags) {
            String villager = s.mob("minecraft:villager", 0, 3, 0, 0, "");
            s.server("ride %s mount %s", Scene.sel(villager), Scene.sel(tag));
        }
        s.waitTicks(5);
        s.cameraAt(0, 1.5, 9, 0, 0, 3);
        s.shot(
                "Chest boats and chest raft with a villager seated in each: eyes stay in the same places on every hull.");
        s.cameraAt(0, 1.5, -9, 0, 0, -3);
        s.shot(
                "Boats and raft with a villager seated in each: eyes stay in the same places on every hull.");
    }

    /**
     * In-game: compare an eyed and an eyeless boat while walking away and turning so they reach the
     * screen edge.
     */
    private static void boatCulling(Scene s) {
        s.water(-4, -2, 4, 2);
        String eyed = s.boat("minecraft:oak_boat", -1.5, 0, 90);
        s.boat("minecraft:oak_boat", 1.5, 0, 90);
        s.useOnEntity(eyed, -1.5, 2.5, 2.5, Scene.SLIMY);
        for (int distance : new int[] {8, 32, 64, 128}) {
            s.cameraAt(0, 2, distance, 0, 0, 0);
            s.shot(
                    "From "
                            + distance
                            + " blocks: the left boat has eyes and the right does not; both boats are equally visible or both gone.");
        }
        s.camera(0, 2, 12, 150, 5);
        s.shot(
                "Turned so the boats sit at the right edge of the screen: both boats are shown or both are cut off together, never just the eyes.");
    }

    /**
     * In-game: place each eligible vanilla head on the floor and on a wall and apply a Slimy Eye to
     * each.
     */
    private static void eligibleHeads(Scene s) {
        s.wideView();
        for (int i = 0; i < HEADS.size(); i++) {
            int dx = -5 + i * 2;
            s.eyedHead("minecraft:" + HEADS.get(i) + "[rotation=0]", dx, 0, 0);
            s.setBlock(dx, 1, -3, "minecraft:stone");
            s.eyedHead("minecraft:" + WALL_HEADS.get(i) + "[facing=south]", dx, 1, -2);
        }
        s.cameraAt(0, 1.2, 5, 0, 0.6, -1);
        s.shot(
                "Front: skeleton, wither skeleton, zombie, creeper, piglin, and player heads on the floor, and the same six on the wall behind. Every head has eyes laid out like its mob's.");
        s.cameraAt(4, 2, 4, 0, 0.5, -1);
        s.shot("The same heads from an angle: eyes sit on each head's face.");
    }

    /** In-game: use a Slimy Eye on a dragon head in survival; nothing happens. */
    private static void dragonHead(Scene s) {
        s.gamemode("survival");
        s.hud(true);
        s.setBlock(0, 0, 0, "minecraft:dragon_head[rotation=0]");
        s.useOnBlock(0, 0, 0, Scene.SLIMY);
        s.cameraAt(0, 1.2, 3, 0, 0.5, 0);
        s.shot("A dragon head with no eyes; the Slimy Eye is still in the first hotbar slot.");
    }

    /** In-game: with a GeckoLib-based mob mod installed, give one of its mobs eyes. */
    private static void geckoLibMob(Scene s) {
        String type = System.getProperty(GECKOLIB_MOB_PROPERTY);
        if (type == null) {
            throw new IllegalStateException(
                    "No GeckoLib mob in this run; set -D"
                            + GECKOLIB_MOB_PROPERTY
                            + "=<entity id> with its mod installed");
        }
        String mob = s.mob(type, 0, 0, 0);
        s.eyes(mob);
        s.cameraOn(0, 1.5, 4, mob);
        s.shot("A GeckoLib-rendered " + type + " with eyes placed on its model.");
    }

    /** In-game: give a cow with glow on and one with glow off eyes in a dark room at midnight. */
    private static void glowInDark(Scene s) {
        s.darkRoom();
        String glowing = s.mob("minecraft:cow", -1.5, 0, 0);
        String plain = s.mob("minecraft:cow", 1.5, 0, 0);
        s.eyes(glowing, "glow on");
        s.eyes(plain, "glow off");
        s.cameraAt(0, 1.5, 4, 0, 1, 0);
        s.shot(
                "A dark room: the left cow's eyes glow brightly; the right cow's eyes are as dark as the cow.");
    }

    /** In-game: give a cow eyes, then make it invisible. */
    private static void invisibleMob(Scene s) {
        String cow = s.mob("minecraft:cow", 0, 0, 0);
        s.eyes(cow);
        s.server("effect give %s minecraft:invisibility infinite 0 true", Scene.sel(cow));
        s.cameraAt(0, 1.2, 4, 0, 1, 0);
        s.shot("An empty platform: no cow and no floating eyes.");
    }

    /** In-game: apply a Slimy Eye to yourself and look in first person, F5 back, and F5 front. */
    private static void ownEyes(Scene s) {
        s.useOnSelf(Scene.SLIMY);
        s.camera(0, 0, 0, 0, 0);
        s.shot("First person looking ahead: no eyes anywhere in view.");
        s.view(CameraType.THIRD_PERSON_BACK);
        s.shot("Third person from behind: the back of the player's head; no eyes visible.");
        s.view(CameraType.THIRD_PERSON_FRONT);
        s.shot("Third person from the front: the player's face has eyes.");
    }

    /** In-game: shear an eyed sheep, then let its wool regrow. */
    private static void shearedSheep(Scene s) {
        String sheep = s.mob("minecraft:sheep", 0, 0, 0);
        s.eyes(sheep);
        s.cameraOn(3, 1.4, 2, sheep);
        s.shot("A woolly sheep with eyes; any body eyes sit on the wool.");
        s.useOnEntity(sheep, 0, 1.5, 2.5, "minecraft:shears");
        s.cameraOn(3, 1.4, 2, sheep);
        s.shot(
                "The sheared sheep: body eyes sit on the bare body, not floating where the wool was.");
        s.server("data merge entity %s {Sheared:0b}", Scene.sel(sheep));
        s.waitTicks(5);
        s.shot("Wool back: body eyes sit on the wool again.");
    }

    /** In-game: give a cow eyes, note its arrangement, save and quit, reopen, and compare. */
    private static void variantPersists(Scene s) {
        String cow = s.mob("minecraft:cow", 0, 0, 0);
        s.eyes(cow);
        s.cameraAt(3, 1.6, 3, 0, 0.8, 0);
        s.shot("A cow with eyes in one of its arrangements (face or flank).");
        s.reopen();
        s.cameraAt(3, 1.6, 3, 0, 0.8, 0);
        s.shot(
                "After save and reopen: the same cow with eyes in the same arrangement as the previous shot.");
    }

    /** In-game: put an eyed head on your head and look at yourself in F5 front. */
    private static void wornHead(Scene s) {
        s.eyedHeadItem("minecraft:zombie_head[rotation=0]");
        s.server("item replace entity @p armor.head from entity @p hotbar.0");
        s.camera(0, 0, 0, 0, 0);
        s.view(CameraType.THIRD_PERSON_FRONT);
        s.shot("The player wearing a zombie head, seen from the front: the worn head has no eyes.");
    }

    /** In-game: summon an armadillo baby that is about to grow up, with spawn chance 100. */
    private static void growsUp(Scene s) {
        s.config("globalPercent", "100");
        String baby = s.liveMob("minecraft:armadillo", 0, 0, 0, "Age:-60");
        s.cameraOn(0, 2, 4, baby);
        s.shot("A baby armadillo with no eyes.");
        s.waitTicks(100);
        s.cameraOn(0, 2, 4, baby);
        s.shot("The armadillo has grown up and now has eyes.");
    }

    /** In-game: close-ups of an eyed oak chest boat and bamboo raft with passengers. */
    private static void boatCloseUps(Scene s) {
        s.water(-6, -3, 6, 3);
        String chest = s.boat("minecraft:oak_chest_boat", -2.5, 0, 90);
        String raft = s.boat("minecraft:bamboo_raft", 2.5, 0, 90);
        s.useOnEntity(chest, -2.5, 2.5, 2.5, Scene.SLIMY);
        s.useOnEntity(raft, 2.5, 2.5, 2.5, Scene.SLIMY);
        for (String boat : new String[] {chest, raft}) {
            String villager = s.mob("minecraft:villager", 0, 3, 0, 0, "");
            s.server("ride %s mount %s", Scene.sel(villager), Scene.sel(boat));
        }
        s.waitTicks(5);
        s.cameraAt(-3, 0.8, 2.2, -3.1, 0.1, 0.5);
        s.shot(
                "Close-up of the oak chest boat's near eye: flush with the hull, no gap or clipping; paddles, chest, and passenger look normal; no water inside the hull.");
        s.cameraAt(2, 0.8, 2.2, 1.9, 0.1, 0.5);
        s.shot(
                "Close-up of the bamboo raft's near eye: placed for the raft's shape, flush, no gap or clipping; passenger normal.");
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        Scene.run(
                context,
                Scene.MAIN,
                s -> {
                    s.scenario("1.1.1", "Eyed adult mob", () -> adultMob(s));
                    s.scenario("1.1.2", "Baby layouts", () -> babies(s));
                    s.scenario("1.1.3", "Baby grows into eyes", () -> growsUp(s));
                    s.scenario("1.1.4", "Variant survives reload", () -> variantPersists(s));
                    s.scenario("1.1.5", "Sheared sheep body eyes", () -> shearedSheep(s));
                    s.scenario("1.1.6", "Glow in darkness", () -> glowInDark(s));
                    s.scenario("1.1.7", "Invisible mob", () -> invisibleMob(s));
                    s.scenario("1.1.8", "GeckoLib mob", () -> geckoLibMob(s));
                    s.scenario("1.2.1", "Own eyes by view", () -> ownEyes(s));
                    s.scenario("1.3.1", "Eligible heads", () -> eligibleHeads(s));
                    s.scenario("1.3.2", "Dragon head refuses", () -> dragonHead(s));
                    s.scenario("1.3.3", "Worn head", () -> wornHead(s));
                    s.scenario("1.4.1", "Boats start eyeless", () -> boatsStartEyeless(s));
                    s.scenario("1.4.2", "Eyed boats", () -> boatsEyed(s));
                    s.scenario("1.4.3", "Boat close-ups", () -> boatCloseUps(s));
                    s.scenario("1.4.4", "Boat culling", () -> boatCulling(s));
                });
    }
}
