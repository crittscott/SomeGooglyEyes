package com.github.crittscott.somegoogly.clientgametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Client test plan section 12.1: server settings. Client settings (12.2) need their own launches; see {@link LaunchGroupClientTests}. */
public final class ConfigClientTests implements FabricClientGameTest {
    private static final String RED_NBT = Scene.googlyNbt("{irisColor:" + Scene.RED + "}");

    /** Punches a new eyed cow and shoots the result with the given expectation. */
    private static void punch(Scene s, double dx, String expect) {
        String cow = s.mob("minecraft:cow", dx, 0, 0);
        s.eyes(cow);
        s.cameraOn(dx, 0, 2.2, cow);
        s.attack();
        s.cameraOn(dx, 1.4, 3.5, cow);
        s.shot(expect);
    }

    /** In-game: turn the master switch off with eyed things around, try every mod action, then turn it back on. */
    private static void masterSwitch(Scene s) {
        String cow = s.mob("minecraft:cow", -4, 0, 0);
        s.eyes(cow, "tint iris 1 0 0");
        s.eyedHead("minecraft:zombie_head[rotation=0]", -1, 0, 0);
        s.water(1, -1, 5, 1);
        String boat = s.boat("minecraft:oak_boat", 3, 0, 90);
        s.useOnEntity(boat, 3, 2.5, 2.5, Scene.SLIMY);
        s.frame(0, 2, -3, "south", RED_NBT, 0, false, false);
        s.config("googlyEyesEnabled", "false", "growOnHitPercent", "100", "harvestOnKillPercent", "100");
        s.clearInventory();
        s.slot("hotbar.0", Scene.googly("{irisColor:" + Scene.RED + "}"));
        s.hud(true);
        s.camera(0, 0.5, 6, 180, 10);
        s.shot("Master switch off: the cow, head, and boat show no eyes; the framed eye and hotbar eye show centered, still pupils.");
        String eyeless = s.mob("minecraft:cow", -4, 0, -5);
        s.useOnEntity(eyeless, -4, 1.6, -2.5, Scene.SLIMY);
        s.cameraOn(-4, 1.4, -1.5, eyeless);
        s.shot("A Slimy Eye used on an eyeless cow with the switch off: it gets no eyes.");
        s.cameraOn(-4, 0, 2.2, cow);
        s.attack();
        s.cameraOn(-4, 1.4, 3.5, cow);
        s.shot("Punching the eyed cow at hit chance 100 with the switch off: no eyes and no bulge.");
        String victim = s.mob("minecraft:cow", 4, 0, -5, 0, "Health:1f");
        s.eyes(victim);
        s.clearInventory();
        s.give("minecraft:shears");
        s.hotbar(0);
        s.cameraOn(4, 1.6, -2.8, victim);
        s.attack();
        s.waitTicks(10);
        s.cameraAt(4, 2, -2, 4, 0, -5);
        s.shot("A cow killed with shears at harvest chance 100 with the switch off: no Googly Eye dropped.");
        s.config("googlyEyesEnabled", "true");
        s.camera(0, 0.5, 6, 180, 10);
        s.shot("Master switch back on: the cow's red-irised eyes, the head's eyes, and the boat's eyes are back as before.");
    }

    /** In-game: for each behavior switch, turn it off and fire its trigger; then show an unrelated behavior still works. */
    private static void behaviorSwitches(Scene s) {
        boolean first = true;
        for (TriggeredClientTests.Trigger trigger : TriggeredClientTests.SWITCHED) {
            if (!first) {
                s.area();
            }
            first = false;
            s.restoreConfig();
            s.config(trigger.configKey(), "false", "growOnHitPercent", "100");
            trigger.fire().fire(s, trigger.configKey() + " is off: " + trigger.name() + " shows no reaction in the eyes.");
            s.gamemode("creative");
            s.area();
            punch(s, 0, trigger.configKey() + " is off, and a punched cow at hit chance 100 still bulges: other behaviors are unaffected.");
        }
    }

    /** In-game: ambient off, then an empty pool: no idle expressions, but pupils still move. */
    private static void ambientOff(Scene s) {
        String cow = s.mob("minecraft:cow", -2, 0, 0);
        s.eyes(cow);
        s.frame(0, 2, -3, "south", RED_NBT, 0, false, false);
        s.eyedHead("minecraft:zombie_head[rotation=0]", 2, 0, 0);
        s.config("ambientBehaviors", "false", "ambientMinTicks", "10", "ambientMaxTicks", "10");
        s.cameraAt(0, 1.5, 4, 0, 1, 0);
        s.burst(4, 10, "Ambient behaviors off: the cow, framed eye, and head never blink or glance.");
        for (int i = 0; i < 4; i++) {
            s.server("execute as %s at @s run tp @s ~0.4 ~ ~", Scene.sel(cow));
            s.waitTicks(1);
        }
        s.shot("The cow just moved sideways with ambient behaviors off: its pupils still lag the motion.");
        s.config("ambientBehaviors", "true", "ambientBehaviorPool", "[]");
        s.burst(4, 10, "An empty pool: the cow, framed eye, and head never blink or glance.");
    }

    /** In-game: a cross-eye-only pool, then blink-only, with a paired head and a framed loose eye. */
    private static void crossEyeOnly(Scene s) {
        s.eyedHead("minecraft:zombie_head[rotation=0]", -1, 0, 0);
        s.frame(1, 1, -2, "south", RED_NBT, 0, false, false);
        s.cameraAt(0, 1.2, 3, 0, 0.6, -1);
        s.ambient(20, "cross_eye");
        s.burst(5, 4, "Cross-eye-only pool: the zombie head's paired eyes cross in at least one shot; the framed loose eye never changes expression.");
        s.ambient(20, "blink");
        s.burst(5, 4, "Blink-only pool: both the head and the framed eye blink in at least one shot.");
    }

    /** In-game: a fixed interval, hit chance 0, harvest chance 0, and a short heal cooldown. */
    private static void tunables(Scene s) {
        String cow = s.mob("minecraft:cow", -3, 0, 0);
        s.eyes(cow);
        s.ambient(40, "stare");
        s.cameraOn(-3, 1.4, 3.5, cow);
        s.burst(3, 40, "Interval fixed at 40 ticks with a stare-only pool: shots 40 ticks apart all catch the same phase of the stare cycle.");
        s.config("growOnHitPercent", "0", "harvestOnKillPercent", "0", "swirlHealCooldownTicks", "20");
        punch(s, 0, "Hit chance 0: the punched cow does not bulge.");
        s.area();
        String victim = s.mob("minecraft:cow", 0, 0, 0, 0, "Health:1f");
        s.eyes(victim);
        s.clearInventory();
        s.give("minecraft:shears");
        s.hotbar(0);
        s.cameraOn(0, 1.6, 2.2, victim);
        s.attack();
        s.waitTicks(10);
        s.cameraAt(0, 2, 3, 0, 0, 0);
        s.shot("Harvest chance 0: a cow killed with shears dropped no Googly Eye.");
        s.area();
        String healed = s.mob("minecraft:cow", 0, 0, 0);
        s.eyes(healed);
        s.cameraOn(0, 1.4, 3.5, healed);
        for (int i = 1; i <= 2; i++) {
            s.server("damage %s 2 minecraft:generic", Scene.sel(healed));
            s.waitTicks(12);
            s.server("effect give %s minecraft:instant_health 1 0", Scene.sel(healed));
            s.waitTicks(3);
            s.shot("Heal cooldown 20 ticks, heal " + i + " of 2 (30 ticks apart): eyes swirling.");
        }
    }

    /** In-game: edit a setting without reloading, then reload, then edit and reopen the world. */
    private static void appliesOnReload(Scene s) {
        s.config("growOnHitPercent", "100");
        s.writeConfig("growOnHitPercent", "0");
        punch(s, -3, "File says hit chance 0 but nothing reloaded yet: the punched cow still bulges.");
        s.reload();
        punch(s, 0, "After /reload: a punched cow no longer bulges.");
        s.writeConfig("growOnHitPercent", "100");
        s.reopen();
        punch(s, 3, "File set back to 100 and the world reopened: a punched cow bulges.");
    }

    /** In-game: start the world with a malformed config file, then with one missing a key. */
    private static void malformedFile(Scene s) {
        String original = s.readConfig();
        s.writeConfigText("this is = = not [[ valid toml\n");
        s.reopen();
        s.area();
        String cow = s.mob("minecraft:cow", 0, 0, 0);
        s.eyes(cow);
        s.cameraOn(0, 1.4, 3.5, cow);
        s.shot("The world opened with a malformed server config: it loaded normally and an eyed cow shows its eyes. (Check the .bak beside the config file by hand.)");
        s.writeConfigText(original.replaceAll("(?m)^\\s*ambientBehaviors\\s*=.*\\R", "")
                .replaceAll("(?m)^(\\s*)growOnHitPercent\\s*=.*$", "$1growOnHitPercent = 100"));
        s.reopen();
        s.area();
        punch(s, 0, "Reopened with ambientBehaviors missing and hit chance 100: the existing value is kept and the punched cow bulges. (Check that the file gained ambientBehaviors by hand.)");
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        Scene.run(context, Scene.MAIN, s -> {
            s.scenario("12.1.1", "Master switch", () -> masterSwitch(s));
            s.scenario("12.1.2", "Behavior switches", () -> behaviorSwitches(s));
            s.scenario("12.1.3", "Ambient off and empty pool", () -> ambientOff(s));
            s.scenario("12.1.4", "Cross-eye-only pool", () -> crossEyeOnly(s));
            s.scenario("12.1.5", "Tunable values", () -> tunables(s));
            s.scenario("12.1.6", "Edits apply on reload and start", () -> appliesOnReload(s));
            s.scenario("12.1.7", "Malformed and partial files", () -> malformedFile(s));
        });
    }
}
