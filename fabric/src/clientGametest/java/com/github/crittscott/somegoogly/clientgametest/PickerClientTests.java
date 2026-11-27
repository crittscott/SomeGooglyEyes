package com.github.crittscott.somegoogly.clientgametest;

import com.github.crittscott.somegoogly.client.picker.PickerInput;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;

/**
 * Client test plan section 14: the creative picker and {@code /sg} commands. Shots show the HUD,
 * where the picker's overlay and command feedback appear.
 */
public final class PickerClientTests implements FabricClientGameTest {
    /** Turns the picker on and chooses a still cow in front of the camera; returns its tag. */
    private static String chooseCow(Scene s) {
        String cow = s.mob("minecraft:cow", 0, 0, 0);
        s.hud(true);
        s.press(PickerInput.TOGGLE);
        s.cameraOn(0, 0, 3, cow);
        s.press(PickerInput.LOCK);
        return cow;
    }

    /** In-game: toggle the picker with K beside an eyeless cow. */
    private static void toggle(Scene s) {
        String cow = s.mob("minecraft:cow", 0, 0, 0);
        s.hud(true);
        s.cameraOn(0, 0, 3, cow);
        s.shot("Picker off, spawn chance 0: the cow has no eyes.");
        s.press(PickerInput.TOGGLE);
        s.shot("Picker on: the cow shows its configured eyes and the picker HUD is visible.");
        s.press(PickerInput.TOGGLE);
        s.shot("Picker off again: no eyes, no picker HUD.");
    }

    /**
     * In-game: choose a wandering cow with V, release it, then choose with /sg choose and rejoin.
     */
    private static void choosing(Scene s) {
        String cow = s.liveMob("minecraft:cow", 0, 0, 0, "");
        s.hud(true);
        s.press(PickerInput.TOGGLE);
        s.cameraOn(0, 0, 3, cow);
        s.press(PickerInput.LOCK);
        s.shot("The cow chosen with V: the HUD names it and the axis gizmo is drawn on it.");
        s.waitTicks(100);
        s.shot("100 ticks later: the chosen cow has not moved.");
        s.press(PickerInput.LOCK);
        s.waitTicks(200);
        s.camera(0, 4, 8, 180, 25);
        s.shot("Released for 200 ticks: the cow has wandered away from where it stood.");
        s.cameraOn(0, 4, 6, cow);
        s.player("sg choose");
        s.reopen();
        s.waitTicks(200);
        s.camera(0, 4, 8, 180, 25);
        s.shot(
                "Chosen with /sg choose, then the world was left and reopened: the cow is free and has moved.");
    }

    /** In-game: select parts with the bracket keys and commands. */
    private static void parts(Scene s) {
        chooseCow(s);
        s.press(PickerInput.PART_NEXT);
        s.shot("After ]: the HUD shows the next model part selected.");
        s.press(PickerInput.PART_NEXT);
        s.shot("After ] again: the part after that.");
        s.press(PickerInput.PART_PREV);
        s.shot("After [: back one part.");
        s.player("sg part head");
        s.shot("After /sg part head: the HUD shows the head part.");
        s.player("sg list parts");
        s.shot("/sg list parts: chat lists the cow's model parts, head among them.");
    }

    /** In-game: create an eye and edit it with move, rot, posrot, and properties. */
    private static void editing(Scene s) {
        chooseCow(s);
        s.player("sg part head");
        s.player("sg create 0 0 -0.4");
        s.shot("/sg create: a preview eye on the front of the cow's head, pupil centered.");
        s.player("sg move 0.2 ~ ~");
        s.shot("/sg move 0.2 ~ ~: the eye moved sideways only.");
        s.player("sg rot 60 ~");
        s.shot("/sg rot 60 ~: the eye tilted; its azimuth unchanged.");
        s.player("sg posrot ~ 0.1 ~ ~ 300");
        s.shot("/sg posrot ~ 0.1 ~ ~ 300: the eye moved up and turned.");
        s.player("sg properties eyescale 1.5");
        s.player("sg properties iriscolor 1 0 0");
        s.player("sg properties glow true");
        s.shot(
                "After properties eyescale 1.5, iriscolor red, glow true: a larger eye with a red iris, glowing.");
    }

    /** In-game: save, dupe, select, delete, and list eyes; discard an unsaved edit. */
    private static void managing(Scene s) {
        chooseCow(s);
        s.player("sg part head");
        s.player("sg create -0.15 0 -0.4");
        s.player("sg save");
        s.shot("/sg save: chat confirms; the eye stays.");
        s.player("sg dupe 1");
        s.player("sg move 0.15 ~ ~");
        s.player("sg save");
        s.shot("/sg dupe 1, moved, saved: two eyes side by side.");
        s.player("sg list eyes");
        s.shot("/sg list eyes: chat lists two eyes.");
        s.player("sg select 1");
        s.player("sg move ~ 0.2 ~");
        s.player("sg select 2");
        s.player("sg select 1");
        s.shot(
                "Eye 1 moved but not saved, then another eye selected and eye 1 again: eye 1 is back in its saved place.");
        s.player("sg delete 2");
        s.shot("/sg delete 2: only one eye remains.");
    }

    /** In-game: create, switch, weigh, and delete variants. */
    private static void variants(Scene s) {
        chooseCow(s);
        s.player("sg part head");
        s.player("sg create 0 0 -0.4");
        s.player("sg save");
        s.player("sg variant new");
        s.shot("/sg variant new: the HUD shows a second, empty variant.");
        s.player("sg variant 1");
        s.shot("/sg variant 1: back to the first variant with its eye.");
        s.player("sg variant weight 2");
        s.player("sg list variants");
        s.shot(
                "/sg variant weight 2 and /sg list variants: chat shows variant 1 with weight 2 and variant 2.");
        s.player("sg variant del 2");
        s.player("sg list variants");
        s.shot("/sg variant del 2: chat lists one variant.");
    }

    /** In-game: edit an adult cow and a baby cow in one session. */
    private static void adultAndBaby(Scene s) {
        String adult = s.mob("minecraft:cow", -2, 0, 0);
        String baby = s.mob("minecraft:cow", 2, 0, 0, 0, "Age:-24000");
        s.hud(true);
        s.press(PickerInput.TOGGLE);
        s.cameraOn(-2, 0, 3, adult);
        s.press(PickerInput.LOCK);
        s.player("sg part head");
        s.player("sg create 0 0.2 -0.4");
        s.player("sg save");
        s.press(PickerInput.LOCK);
        s.cameraOn(2, 0, 3, baby);
        s.press(PickerInput.LOCK);
        s.player("sg part head");
        s.player("sg create 0 -0.1 -0.3");
        s.player("sg save");
        s.press(PickerInput.LOCK);
        s.cameraAt(0, 1.5, 4, 0, 0.8, 0);
        s.shot(
                "Both edits kept: the adult cow shows the eye saved for adults, the baby the eye saved for babies.");
    }

    /** In-game: export, export again at once, and export a definition too large to send. */
    private static void export(Scene s) {
        String cow = chooseCow(s);
        s.player("sg part head");
        s.player("sg create 0 0.25 -0.4");
        s.player("sg properties eyescale 2");
        s.player("sg save");
        s.player("sg export");
        s.waitTicks(20);
        // Still choosing the cow: turning the picker off clears the choice, and export then asks
        // for one instead.
        s.player("sg export");
        s.shot("A second export straight away: chat refuses it until 10 seconds have passed.");
        s.press(PickerInput.LOCK);
        s.press(PickerInput.TOGGLE);
        String other = s.mob("minecraft:cow", 3, 0, 0);
        s.eyes(other);
        s.cameraOn(3, 1.4, 3.5, other);
        s.shot(
                "After /sg export: chat confirms, and another cow given eyes shows the exported large eye high on its head.");
        s.press(PickerInput.TOGGLE);
        s.cameraOn(0, 0, 3, cow);
        s.press(PickerInput.LOCK);
        for (int i = 0; i < 200; i++) {
            s.player("sg dupe 1");
            s.player("sg save");
        }
        s.waitTicks(220);
        s.player("sg export");
        s.shot(
                "Export of a definition with hundreds of eyes: chat refuses it as too large to send.");
    }

    /** In-game: run /sg exportall and look at the world. */
    private static void exportAll(Scene s) {
        String cow = chooseCow(s);
        s.player("sg exportall");
        s.waitTicks(20);
        s.cameraOn(0, 1.4, 3.5, cow);
        s.shot(
                "/sg exportall: chat reports the export folder, and the world is unchanged. (Check the files under somegoogly-export by hand.)");
    }

    /** In-game: spawn suggestions with pig excluded; spawn a cow; move and turn it. */
    private static void spawn(Scene s) {
        s.config("spawnExcludedEntities", Scene.tomlList("minecraft:pig"));
        s.hud(true);
        s.context.setScreen(() -> new ChatScreen("/sg spawn minecraft:p"));
        s.waitTicks(10);
        s.shot(
                "Typing /sg spawn minecraft:p: the suggestions list types like parrot and panda, but not pig.");
        s.closeScreen();
        s.cameraAt(0, 0, 4, 0, 0, 0);
        s.player("sg spawn minecraft:cow");
        s.waitTicks(40);
        s.cameraAt(0, 1.5, 5, 0, 0.5, 0);
        s.shot(
                "/sg spawn minecraft:cow: a cow stands on the targeted block and has not moved after 40 ticks.");
        s.press(PickerInput.TOGGLE);
        s.cameraAt(0, 1, 3, 0, 0.8, 0);
        s.press(PickerInput.LOCK);
        s.player("sg mob move 1 0 0");
        s.player("sg mob rot 90");
        s.cameraAt(0, 1.5, 5, 0, 0.5, 0);
        s.shot(
                "After /sg mob move 1 0 0 and /sg mob rot 90: the cow moved one block sideways and turned.");
    }

    /** In-game: /sg spawnall refused while disabled, then a namespace run. */
    private static void spawnAll(Scene s) {
        s.wideView();
        s.hud(true);
        s.camera(0, 2, 0, 0, 30);
        s.player("sg spawnall minecraft");
        s.shot("/sg spawnall with allowSpawnAll off: chat refuses.");
        s.config("allowSpawnAll", "true");
        s.player("sg spawnall minecraft");
        s.waitTicks(100);
        s.shot("/sg spawnall minecraft: chat reports skipped types and why.");
        s.camera(0, 25, 0, 0, 60);
        s.shot("Looking down at the audit grid of Minecraft mobs it built.");
    }

    /** In-game: every /sg admin command on a cow. */
    private static void admin(Scene s) {
        String cow = s.mob("minecraft:cow", 0, 0, 0);
        s.camera(0, 0, 3, 180, 0);
        String[][] steps = {
            {"eyes true", "/sg admin eyes true: the cow has eyes."},
            {"tint iris 1 0 0", "tint iris 1 0 0: red irises."},
            {"tint cornea 0 0 1", "tint cornea 0 0 1: blue corneas around the red irises."},
            {"tint clear", "tint clear: back to the definition's white corneas and black irises."},
            {"glow on", "glow on: eyes glowing."},
            {"glow off", "glow off: eyes not glowing."},
            {"glow config", "glow config: the definition's glow setting (not glowing for a cow)."},
            {"behavior blink", "behavior blink: the cow is blinking."},
            {"behavior random", "behavior random: some expression is playing."},
            {"eyes false", "eyes false: the cow has no eyes."}
        };
        for (String[] step : steps) {
            s.admin(cow, step[0]);
            s.cameraOn(0, 1.4, 3.5, cow);
            s.shot(step[1]);
        }
    }

    /** In-game: rebind the picker toggle from K to J in Controls and press J. */
    private static void rebind(Scene s) {
        String cow = s.mob("minecraft:cow", 0, 0, 0);
        s.hud(true);
        s.cameraOn(0, 0, 3, cow);
        s.context.runOnClient(mc -> {
            PickerInput.TOGGLE.setKey(InputConstants.getKey("key.keyboard.j"));
            KeyMapping.resetMapping();
        });
        s.press(PickerInput.TOGGLE);
        s.shot(
                "Toggle rebound to J and J pressed: the picker HUD is shown and the cow shows its configured eyes.");
        s.press(PickerInput.TOGGLE);
        s.context.runOnClient(mc -> {
            PickerInput.TOGGLE.setKey(PickerInput.TOGGLE.getDefaultKey());
            KeyMapping.resetMapping();
        });
    }

    /**
     * In-game: as a survival player with cheats, then in a world without cheats, try a command and
     * the K key.
     */
    private static void refused(Scene s) {
        s.hud(true);
        s.gamemode("survival");
        s.player("sg list parts");
        s.press(PickerInput.TOGGLE);
        s.shot("Survival player: chat refuses /sg list parts and the K key; no picker HUD.");
        s.newWorld(false);
        s.area();
        s.hud(true);
        s.player("sg list parts");
        s.press(PickerInput.TOGGLE);
        s.shot("World without cheats: chat refuses /sg list parts and the K key; no picker HUD.");
    }

    /**
     * Turns the picker off if a scenario left it on, so the next scenario's first K press turns it
     * on.
     */
    private static void pickerOff(Scene s) {
        if (s.context.computeOnClient(mc -> PickerState.isActive())) {
            s.press(PickerInput.TOGGLE);
        }
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        Scene.run(
                context,
                Scene.MAIN,
                s -> {
                    s.scenario(
                            "14.2",
                            "Picker toggle",
                            () -> {
                                toggle(s);
                                pickerOff(s);
                            });
                    s.scenario(
                            "14.3",
                            "Choosing freezes and releases",
                            () -> {
                                choosing(s);
                                pickerOff(s);
                            });
                    s.scenario(
                            "14.4",
                            "Part selection",
                            () -> {
                                parts(s);
                                pickerOff(s);
                            });
                    s.scenario(
                            "14.5",
                            "Creating and editing an eye",
                            () -> {
                                editing(s);
                                pickerOff(s);
                            });
                    s.scenario(
                            "14.6",
                            "Saving and managing eyes",
                            () -> {
                                managing(s);
                                pickerOff(s);
                            });
                    s.scenario(
                            "14.7",
                            "Variants",
                            () -> {
                                variants(s);
                                pickerOff(s);
                            });
                    s.scenario(
                            "14.8",
                            "Adult and baby edits",
                            () -> {
                                adultAndBaby(s);
                                pickerOff(s);
                            });
                    s.scenario(
                            "14.9",
                            "Export",
                            () -> {
                                export(s);
                                pickerOff(s);
                            });
                    s.scenario(
                            "14.10",
                            "Export all",
                            () -> {
                                exportAll(s);
                                pickerOff(s);
                            });
                    s.scenario(
                            "14.11",
                            "Spawn and mob pose",
                            () -> {
                                spawn(s);
                                pickerOff(s);
                            });
                    s.scenario(
                            "14.12",
                            "Spawn all",
                            () -> {
                                spawnAll(s);
                                pickerOff(s);
                            });
                    s.scenario(
                            "14.13",
                            "Admin commands",
                            () -> {
                                admin(s);
                                pickerOff(s);
                            });
                    s.scenario(
                            "14.14",
                            "Rebound keys",
                            () -> {
                                rebind(s);
                                pickerOff(s);
                            });
                    // Last: it moves the class to a world without cheats.
                    s.scenario(
                            "14.1",
                            "Refused without permission",
                            () -> {
                                refused(s);
                                pickerOff(s);
                            });
                });
    }
}
