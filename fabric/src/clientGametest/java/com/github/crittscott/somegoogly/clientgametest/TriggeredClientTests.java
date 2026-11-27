package com.github.crittscott.somegoogly.clientgametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Client test plan section 4: triggered expressions. Each trigger below stages its mob, fires the
 * trigger the way play does, and takes one shot with the expectation it is given, so section 12 can
 * run the same trigger with its switch off and expect nothing.
 */
public final class TriggeredClientTests implements FabricClientGameTest {
    private static final String OFFERS =
            "Offers:{Recipes:[{buy:{id:\"minecraft:wheat\",count:1},sell:{id:\"minecraft:emerald\",count:1},maxUses:99}]}";
    private static final String VILLAGER_DATA =
            "VillagerData:{profession:\"minecraft:farmer\",level:2,type:\"minecraft:plains\"}";

    /**
     * Stages a mob, fires a trigger on it, and takes a shot with the given expectation; returns the
     * mob's tag.
     */
    @FunctionalInterface
    interface Fire {
        String fire(Scene s, String expect);
    }

    /** A behavior switch and the trigger it gates, for section 12.1.2. */
    record Trigger(String configKey, String name, Fire fire) {}

    /** Every server behavior switch with its trigger. */
    static final Trigger[] SWITCHED = {
        new Trigger("growOnMilk", "milking", TriggeredClientTests::milk),
        new Trigger("growOnShear", "shearing", TriggeredClientTests::shear),
        new Trigger("loveOnAffection", "feeding for love", TriggeredClientTests::love),
        new Trigger("angryEyes", "an attacking zombie", TriggeredClientTests::anger),
        new Trigger("squintOnSleep", "a sleeping fox", TriggeredClientTests::sleep),
        new Trigger("squintOnRam", "a ramming goat", TriggeredClientTests::ram),
        new Trigger("winkOnAim", "an aiming skeleton", TriggeredClientTests::aim),
        new Trigger("rattleOnSwell", "a swelling creeper", TriggeredClientTests::swell),
        new Trigger("pulseWhenCreepy", "a screaming enderman", TriggeredClientTests::creepy),
        new Trigger(
                "swirlOnTrade", "a villager trade", (s, e) -> trade(s, "minecraft:villager", e)),
        new Trigger("swirlOnHeal", "healing", TriggeredClientTests::heal)
    };

    /** In-game: an eyed skeleton draws its bow on an iron golem. */
    static String aim(Scene s, String expect) {
        String skeleton = aimingSkeleton(s);
        s.waitTicks(5);
        s.cameraOn(3, 1.8, 1, skeleton);
        s.shot(expect);
        return skeleton;
    }

    /**
     * Stages an eyed skeleton that has started drawing its bow on a still iron golem; returns its
     * tag.
     */
    private static String aimingSkeleton(Scene s) {
        // /summon with NBT skips spawn equipment, so the bow is given explicitly.
        String skeleton =
                s.liveMob(
                        "minecraft:skeleton",
                        0,
                        0,
                        0,
                        "equipment:{mainhand:{id:\"minecraft:bow\",count:1}}");
        // The scenes are lit by noon sun, which would set the skeleton burning.
        s.server("effect give %s minecraft:fire_resistance infinite 0 true", Scene.sel(skeleton));
        s.eyes(skeleton);
        s.mob("minecraft:iron_golem", 0, 0, 7, 180, "");
        if (!s.waitUntilTagged(skeleton, e -> ((LivingEntity) e).isUsingItem(), 400)) {
            throw new IllegalStateException("The skeleton did not draw its bow within 400 ticks");
        }
        return skeleton;
    }

    /** In-game: in survival, let an eyed zombie target you. */
    static String anger(Scene s, String expect) {
        String zombie = s.liveMob("minecraft:zombie", 0, 0, -6, "");
        s.eyes(zombie);
        s.server("effect give @p minecraft:resistance infinite 255 true");
        s.gamemode("survival");
        s.camera(0, 0, 2, 180, 0);
        s.waitUntilTagged(zombie, e -> ((Mob) e).getTarget() != null, 200);
        s.cameraOn(0, 0, 2, zombie);
        s.shot(expect);
        return zombie;
    }

    /** In-game: in survival, stare at an eyed enderman until it screams. */
    static String creepy(Scene s, String expect) {
        String enderman = s.liveMob("minecraft:enderman", 0, 0, -8, "");
        s.eyes(enderman);
        s.server("effect give @p minecraft:resistance infinite 255 true");
        s.gamemode("survival");
        s.cameraOn(0, 0, 0, enderman);
        s.waitUntilTagged(enderman, e -> ((EnderMan) e).isCreepy(), 200);
        s.cameraOn(0, 0, 0, enderman);
        s.burst(3, 1, expect);
        return enderman;
    }

    /** In-game: damage an eyed cow, then heal it with an instant health effect. */
    static String heal(Scene s, String expect) {
        String cow = s.mob("minecraft:cow", 0, 0, 0);
        s.eyes(cow);
        s.server("damage %s 4 minecraft:generic", Scene.sel(cow));
        s.waitTicks(15);
        s.server("effect give %s minecraft:instant_health 1 0", Scene.sel(cow));
        s.waitTicks(3);
        s.cameraOn(0, 1.2, 3.5, cow);
        s.shot(expect);
        return cow;
    }

    /** In-game: feed an eyed cow wheat. */
    static String love(Scene s, String expect) {
        String cow = s.mob("minecraft:cow", 0, 0, 0);
        s.eyes(cow);
        s.useOnEntity(cow, 0, 1.6, 2.5, "minecraft:wheat");
        s.cameraOn(0, 1.2, 3.5, cow);
        s.shot(expect);
        return cow;
    }

    /** In-game: milk an eyed cow with a bucket. */
    static String milk(Scene s, String expect) {
        String cow = s.mob("minecraft:cow", 0, 0, 0);
        s.eyes(cow);
        s.useOnEntity(cow, 0, 1.6, 2.5, "minecraft:bucket");
        s.cameraOn(0, 1.2, 3.5, cow);
        s.shot(expect);
        return cow;
    }

    /** In-game: wait for an eyed goat to charge at a sheep. */
    static String ram(Scene s, String expect) {
        String goat = s.liveMob("minecraft:goat", -3, 0, 90, "");
        s.eyes(goat);
        s.mob("minecraft:sheep", 3, 0, 0);
        s.camera(0, 2, 7, 180, 10);
        if (!s.waitUntilTagged(
                goat, e -> ((LivingEntity) e).getBrain().isActive(Activity.RAM), 6000)) {
            throw new IllegalStateException("The goat did not start a ram within 6000 ticks");
        }
        s.waitTicks(5);
        s.cameraOn(0, 2, 6, goat);
        s.shot(expect);
        return goat;
    }

    /** In-game: shear an eyed sheep. */
    static String shear(Scene s, String expect) {
        String sheep = s.mob("minecraft:sheep", 0, 0, 0);
        s.eyes(sheep);
        s.useOnEntity(sheep, 0, 1.6, 2.5, "minecraft:shears");
        s.cameraOn(0, 1.2, 3.5, sheep);
        s.shot(expect);
        return sheep;
    }

    /** In-game: an eyed fox asleep in daylight (find one sleeping under a tree by day). */
    static String sleep(Scene s, String expect) {
        // A fox falls asleep on its own only under cover, with nothing alarming nearby, after a
        // random delay; the test summons one already asleep, the state a player finds it in.
        String fox = s.mob("minecraft:fox", 0, 0, 0, 0, "Sleeping:1b");
        s.eyes(fox);
        if (!s.waitUntilTagged(fox, e -> ((LivingEntity) e).isSleeping(), 20)) {
            throw new IllegalStateException("The fox was not asleep");
        }
        s.cameraOn(0, 1.5, 3, fox);
        s.shot(expect);
        return fox;
    }

    /** In-game: ignite an eyed creeper with a long fuse and watch early and late in the fuse. */
    static String swell(Scene s, String expect) {
        String creeper = s.mob("minecraft:creeper", 0, 0, 0, 0, "Fuse:60");
        s.eyes(creeper);
        s.cameraOn(0, 1.6, 3, creeper);
        s.server("data merge entity %s {ignited:1b}", Scene.sel(creeper));
        s.waitTicks(2);
        s.burst(3, 1, "Early in the fuse. " + expect);
        s.waitTicks(40);
        s.burst(3, 1, "Late in the fuse. " + expect);
        return creeper;
    }

    /** In-game: complete a trade with an eyed villager or wandering trader. */
    static String trade(Scene s, String type, String expect) {
        String data = type.endsWith("villager") ? OFFERS + "," + VILLAGER_DATA : OFFERS;
        String trader = s.mob(type, 0, 0, 0, 0, data);
        s.eyes(trader);
        s.clearInventory();
        s.give("minecraft:wheat", 8);
        s.hotbar(0);
        s.cameraOn(0, 0, 2, trader);
        s.use();
        s.context.waitForScreen(MerchantScreen.class);
        s.context.runOnClient(mc -> {
            MerchantMenu menu = (MerchantMenu) mc.player.containerMenu;
            menu.setSelectionHint(0);
            menu.tryMoveItems(0);
            mc.getConnection().send(new ServerboundSelectTradePacket(0));
        });
        s.waitTicks(3);
        s.click(2, 0, ClickType.QUICK_MOVE);
        s.closeScreen();
        s.cameraOn(0, 1.5, 3, trader);
        s.shot(expect);
        return trader;
    }

    /** In-game: punch one eyed cow and damage another without a player. */
    private static void hit(Scene s) {
        s.config("growOnHitPercent", "100");
        String punched = s.mob("minecraft:cow", -1.5, 0, 0);
        String other = s.mob("minecraft:cow", 1.5, 0, 0);
        s.eyes(punched);
        s.eyes(other);
        s.cameraOn(-1.5, 0, 2.2, punched);
        s.attack();
        s.server("damage %s 1 minecraft:generic", Scene.sel(other));
        s.cameraAt(0, 1.5, 4, 0, 1, 0);
        s.shot(
                "Left cow, punched by the player: eyes bulging. Right cow, damaged by a non-player source: eyes normal.");
    }

    /**
     * In-game: two cows in love, villagers breeding, a golem's flower, an ocelot's trust, and a
     * tamed wolf.
     */
    private static void affection(Scene s) {
        love(s, "A cow just fed wheat: pupils twice their normal size.");
        tamed(s);
        trusting(s);
        golemFlower(s);
        villagersBreed(s);
    }

    /** In-game: an iron golem near a baby villager eventually offers it a flower. */
    private static void golemFlower(Scene s) {
        s.area();
        String golem = s.mob("minecraft:iron_golem", 0, 0, 0);
        s.eyes(golem);
        s.mob("minecraft:villager", 2, 0, 0, 0, "Age:-24000");
        // The golem's goal offers a flower on a 1-in-8000 chance per tick; the test calls the
        // method that goal calls.
        s.serverValue(sv -> {
            Scene.tagged(sv, golem).ifPresent(e -> ((IronGolem) e).offerFlower(true));
            return true;
        });
        if (!s.waitUntilTagged(golem, e -> ((IronGolem) e).getOfferFlowerTick() > 0, 20)) {
            throw new IllegalStateException("The golem did not offer a flower");
        }
        s.cameraOn(0, 3, 4, golem);
        s.shot("An iron golem holding out a poppy: pupils twice their normal size.");
    }

    /** In-game: feed a wolf bones until it is tamed. */
    private static void tamed(Scene s) {
        s.area();
        // Taming is an interaction, not AI, so the wolf stays still.
        String wolf = s.mob("minecraft:wolf", 0, 0, 0);
        s.eyes(wolf);
        s.clearInventory();
        s.give("minecraft:bone", 32);
        s.hotbar(0);
        for (int i = 0;
                i < 30
                        && !s.serverValue(sv ->
                                Scene.tagged(sv, wolf)
                                        .map(e -> ((TamableAnimal) e).isTame())
                                        .orElse(false));
                i++) {
            s.useHeldOn(wolf, 0, 1, 2);
        }
        s.cameraOn(0, 1, 2.5, wolf);
        s.shot("A wolf just tamed (red collar, hearts): pupils twice their normal size.");
    }

    /** In-game: feed an ocelot cod until it trusts the player. */
    private static void trusting(Scene s) {
        s.area();
        // Trust comes from feeding, an interaction, so the ocelot stays still.
        String ocelot = s.mob("minecraft:ocelot", 0, 0, 0);
        s.eyes(ocelot);
        s.clearInventory();
        s.give("minecraft:cod", 32);
        s.hotbar(0);
        for (int i = 0; i < 6; i++) {
            s.useHeldOn(ocelot, 0, 1, 2);
            s.cameraOn(0, 1, 2.5, ocelot);
            s.shot(
                    "Feeding an ocelot cod, attempt "
                            + (i + 1)
                            + " of 6: once hearts appear (it trusts the player), its pupils are twice their normal size.");
        }
    }

    /** In-game: two villagers with food and beds breed. */
    private static void villagersBreed(Scene s) {
        s.area();
        String bread = "Inventory:[{id:\"minecraft:bread\",count:12}]";
        String a = s.liveMob("minecraft:villager", -1, 0, 0, bread);
        String b = s.liveMob("minecraft:villager", 1, 0, 0, bread);
        s.eyes(a);
        s.eyes(b);
        for (int i = 0; i < 3; i++) {
            s.setBlock(-3 + i * 3, 0, -4, "minecraft:red_bed[facing=south,part=head]");
            s.setBlock(-3 + i * 3, 0, -3, "minecraft:red_bed[facing=south,part=foot]");
        }
        s.camera(0, 4, 8, 180, 20);
        Vec3 centre = new Vec3(s.x(0), Scene.GROUND, s.z(0));
        if (!s.waitUntil(
                sv ->
                        sv.overworld()
                                        .getEntitiesOfClass(
                                                Villager.class, AABB.ofSize(centre, 30, 10, 30))
                                        .size()
                                > 2,
                6000)) {
            throw new IllegalStateException("The villagers did not breed within 6000 ticks");
        }
        s.shot(
                "Two villagers that just bred, with their baby: the parents' pupils are twice their normal size.");
    }

    /**
     * In-game: a villager asleep in a bed at night, and a fox asleep by day that is told to blink.
     */
    private static void sleeping(Scene s) {
        String fox = sleep(s, "A sleeping fox: eyes squinting.");
        s.admin(fox, "behavior blink");
        s.cameraOn(0, 1.5, 3, fox);
        s.waitTicks(2);
        s.shot("The sleeping fox after a forced blink: still squinting, no blink.");
        s.area();
        s.server("time set midnight");
        s.setBlock(0, 0, -1, "minecraft:red_bed[facing=south,part=head]");
        s.setBlock(0, 0, 0, "minecraft:red_bed[facing=south,part=foot]");
        // The villager starts with the bed as its home, so it need not find and claim one first.
        String home =
                String.format(
                        "Brain:{memories:{\"minecraft:home\":{value:{dimension:\"minecraft:overworld\",pos:[I;%d,%d,%d]}}}}",
                        (int) Math.floor(s.x(0)), Scene.GROUND, (int) Math.floor(s.z(-1)));
        String villager = s.liveMob("minecraft:villager", 2, 0, 2, home);
        s.eyes(villager);
        s.camera(0, 3, 6, 180, 25);
        if (!s.waitUntilTagged(villager, e -> ((LivingEntity) e).isSleeping(), 6000)) {
            throw new IllegalStateException("The villager did not go to sleep within 6000 ticks");
        }
        s.cameraOn(0, 2, 2, villager);
        s.shot("A villager asleep in a bed: tan lids lowered over its eyes.");
    }

    /**
     * In-game: an eyed skeleton aims at a golem; give yourself eyes and draw a bow in F5, both
     * arms.
     */
    private static void aimWink(Scene s) {
        aim(
                s,
                "A skeleton drawing its bow (side view): the eye away from the bow arm is closed, the other open.");
        s.area();
        s.useOnSelf(Scene.SLIMY);
        s.clearInventory();
        s.give("minecraft:bow");
        s.give("minecraft:arrow", 16);
        s.hotbar(0);
        s.camera(0, 0, 0, 0, 0);
        s.view(CameraType.THIRD_PERSON_FRONT);
        s.hold(s.options().keyUse);
        s.waitTicks(10);
        s.shot(
                "Right-handed player drawing a bow, seen from the front: the eye away from the bow arm is closed.");
        s.release(s.options().keyUse);
        s.mainArm(HumanoidArm.LEFT);
        s.hold(s.options().keyUse);
        s.waitTicks(10);
        s.shot("Left-handed player drawing a bow: the other eye is now the closed one.");
        s.release(s.options().keyUse);
    }

    /**
     * In-game: an attacking zombie and a wolf you hit both glare; switching to creative calms the
     * zombie.
     */
    private static void angry(Scene s) {
        String zombie = anger(s, "A zombie targeting the player: red pupils.");
        s.gamemode("creative");
        s.waitUntilTagged(zombie, e -> ((Mob) e).getTarget() == null, 200);
        s.waitTicks(5);
        s.cameraOn(0, 1, 3, zombie);
        s.shot("After the player switched to creative the zombie lost its target: normal pupils.");
        s.area();
        String wolf = s.liveMob("minecraft:wolf", 0, 0, 0, "");
        s.eyes(wolf);
        s.server("effect give @p minecraft:resistance infinite 255 true");
        s.gamemode("survival");
        s.cameraOn(0, 0, 2, wolf);
        s.attack();
        s.waitTicks(5);
        s.cameraOn(0, 1, 3, wolf);
        s.shot("A wolf the player hit: red pupils.");
    }

    /**
     * In-game: force expressions during a swell and an aim, and two expressions back to back on a
     * cow.
     */
    private static void layering(Scene s) {
        String creeper = s.mob("minecraft:creeper", -2, 0, 0, 0, "Fuse:80");
        s.eyes(creeper);
        s.server("data merge entity %s {ignited:1b}", Scene.sel(creeper));
        s.admin(creeper, "behavior blink");
        s.cameraOn(-2, 1.6, 3, creeper);
        s.shot("A swelling creeper told to blink: rattling pupils and the blink show together.");
        s.area();
        String skeleton = aimingSkeleton(s);
        s.admin(skeleton, "behavior stare");
        s.cameraOn(3, 1.8, 1, skeleton);
        s.shot("An aiming skeleton told to stare: the wink and the stare show together.");
        s.area();
        String cow = s.mob("minecraft:cow", 0, 0, 0);
        s.eyes(cow);
        s.admin(cow, "behavior blink");
        s.admin(cow, "behavior stare");
        s.cameraOn(0, 1.2, 3.5, cow);
        s.shot("A cow given two expressions back to back: only one expression shows.");
    }

    /** In-game: hit an eyed boat in survival. */
    private static void boatHit(Scene s) {
        s.water(-3, -3, 3, 3);
        String boat = s.boat("minecraft:oak_boat", 0, 0, 90);
        s.useOnEntity(boat, 0, 2.5, 2.5, Scene.SLIMY);
        s.gamemode("survival");
        s.cameraOn(0, 0.5, 2.5, boat);
        s.attack();
        s.cameraAt(0, 1, 4, 0, 0, 0);
        s.shot("A boat just hit: it rocks, but its eyes do not bulge or react.");
    }

    /** In-game: hold an eye in first person, take damage, then heal. */
    private static void heldReactions(Scene s) {
        s.gamemode("survival");
        s.clearInventory();
        s.give(Scene.googly("{irisColor:" + Scene.RED + "}"));
        s.hotbar(0);
        s.camera(0, 0, 0, 0, 0);
        s.server("damage @p 4 minecraft:generic");
        s.waitTicks(2);
        s.shot("First person just after taking damage: the held eye shows its hurt reaction.");
        s.waitTicks(40);
        s.server("effect give @p minecraft:instant_health 1 0");
        s.waitTicks(2);
        s.shot("Just after healing: the held eye shows its heal reaction.");
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        Scene.run(
                context,
                Scene.MAIN,
                s -> {
                    s.scenario("4.1", "Player damage bulges", () -> hit(s));
                    s.scenario(
                            "4.2",
                            "Milking bulges",
                            () -> {
                                milk(s, "A cow just milked: eyes bulging.");
                                s.area();
                                String mooshroom = s.mob("minecraft:mooshroom", 0, 0, 0);
                                s.eyes(mooshroom);
                                s.useOnEntity(mooshroom, 0, 1.6, 2.5, "minecraft:bucket");
                                s.cameraOn(0, 1.2, 3.5, mooshroom);
                                s.shot("A mooshroom just milked: eyes bulging.");
                                s.area();
                                String goat = s.mob("minecraft:goat", 0, 0, 0);
                                s.eyes(goat);
                                s.useOnEntity(goat, 0, 1.6, 2.5, "minecraft:bucket");
                                s.cameraOn(0, 1.2, 3.5, goat);
                                s.shot("A goat just milked: eyes bulging.");
                            });
                    s.scenario(
                            "4.3",
                            "Shearing bulges",
                            () -> shear(s, "A sheep just sheared: eyes bulging."));
                    s.scenario("4.4", "Affection grows pupils", () -> affection(s));
                    s.scenario(
                            "4.5",
                            "Trades swirl",
                            () -> {
                                trade(
                                        s,
                                        "minecraft:villager",
                                        "A villager just traded with: eyes swirling.");
                                s.area();
                                trade(
                                        s,
                                        "minecraft:wandering_trader",
                                        "A wandering trader just traded with: eyes swirling.");
                            });
                    s.scenario(
                            "4.6",
                            "Healing swirls once per cooldown",
                            () -> {
                                String cow = heal(s, "A cow just healed: eyes swirling.");
                                s.waitTicks(20);
                                s.server("damage %s 2 minecraft:generic", Scene.sel(cow));
                                s.waitTicks(15);
                                s.server(
                                        "effect give %s minecraft:instant_health 1 0",
                                        Scene.sel(cow));
                                s.waitTicks(3);
                                s.shot("The same cow healed again within the cooldown: no swirl.");
                            });
                    s.scenario("4.7", "Sleepers squint", () -> sleeping(s));
                    s.scenario(
                            "4.8",
                            "Ramming goat squints",
                            () -> ram(s, "A goat mid-charge: eyes squinting."));
                    s.scenario("4.9", "Bow aim winks", () -> aimWink(s));
                    s.scenario(
                            "4.10",
                            "Creeper rattle",
                            () ->
                                    swell(
                                            s,
                                            "Pupils jump between consecutive shots, more late in the fuse than early."));
                    s.scenario("4.11", "Angry red pupils", () -> angry(s));
                    s.scenario(
                            "4.12",
                            "Enderman throb",
                            () ->
                                    creepy(
                                            s,
                                            "A screaming enderman: pupil size changes between consecutive shots."));
                    s.scenario("4.13", "Overlays layer on expressions", () -> layering(s));
                    s.scenario("4.14", "Boats do not react", () -> boatHit(s));
                    s.scenario("4.15", "Held eye hurt and heal", () -> heldReactions(s));
                });
    }
}
