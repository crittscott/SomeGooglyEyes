package com.github.crittscott.somegoogly.picker;

import com.github.crittscott.somegoogly.head.HeadInfo.EyeConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.List;

/** Text HUD for the part picker. Drawn in-world (no screen) while the picker is active. */
public final class PickerHud {

    private static final int WHITE = 0xFFFFFFFF;
    private static final int YELLOW = 0xFFFFE060;
    private static final int GREY = 0xFFB0B0B0;

    private PickerHud() {
    }

    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("somegoogly_picker", (IGuiOverlay) PickerHud::render);
    }

    private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        if (!PickerState.active) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int x = 6;
        int y = 6;
        for (Line line : lines()) {
            graphics.drawString(font, line.text, x, y, line.color);
            y += 10;
        }
    }

    private record Line(String text, int color) {
    }

    private static List<Line> lines() {
        List<Line> out = new ArrayList<>();
        out.add(new Line("Googly Eye Picker", YELLOW));

        if (PickerState.target() == null) {
            out.add(new Line("Look at a hierarchical mob and press Lock (V).", GREY));
            return out;
        }

        out.add(new Line("Target: " + PickerState.targetType(), WHITE));

        String token = PickerState.selectedToken();
        int n = PickerState.parts.size();
        int i = n == 0 ? 0 : (Math.floorMod(PickerState.partIndex, n) + 1);
        out.add(new Line("Part: " + token + "  (" + i + "/" + n + ")   [ ] to cycle", WHITE));

        out.add(new Line(String.format("Field: %s = %.3f   step %.3f   - / = adjust, ; ' field, \\ step",
                PickerState.field.label, PickerState.fieldValue(), PickerState.step()), WHITE));

        EyeConfig d = PickerState.draft;
        out.add(new Line(String.format("Draft: pos[%.2f, %.2f, %.2f]  eye %.2f  iris %.2f  glow %b  invis %b",
                d.position[0], d.position[1], d.position[2], d.eyeScale, d.irisScale, d.glows, d.affectedByInvisibility), GREY));

        out.add(new Line("Committed: " + PickerState.committedCount() + " eyes / " + PickerState.heads.size() + " heads", WHITE));
        out.add(new Line("Enter commit · M mirror · Backspace undo · G glow · I invis · P export", GREY));
        return out;
    }
}
