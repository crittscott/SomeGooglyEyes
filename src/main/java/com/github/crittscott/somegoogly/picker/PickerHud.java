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

    // Translucent dark backdrop so the text stays legible over a busy scene while the mob shows
    // through. ARGB: raise the leading alpha byte (0xA0) toward 0xFF for a more opaque panel.
    private static final int BACKDROP = 0xA0101010;
    private static final int LINE_HEIGHT = 10;
    private static final int PADDING = 4;
    private static final int ORIGIN_X = 6;
    private static final int ORIGIN_Y = 6;

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
        List<Line> lines = lines();

        int widest = 0;
        for (Line line : lines) {
            widest = Math.max(widest, font.width(line.text));
        }
        graphics.fill(
                ORIGIN_X - PADDING,
                ORIGIN_Y - PADDING,
                ORIGIN_X + widest + PADDING,
                ORIGIN_Y + lines.size() * LINE_HEIGHT + PADDING,
                BACKDROP);

        int y = ORIGIN_Y;
        for (Line line : lines) {
            graphics.drawString(font, line.text, ORIGIN_X, y, line.color);
            y += LINE_HEIGHT;
        }
    }

    private record Line(String text, int color) {
    }

    private static List<Line> lines() {
        List<Line> out = new ArrayList<>();
        out.add(new Line("Googly Eye Picker", YELLOW));

        if (PickerState.target() == null) {
            out.add(new Line("Look at a mob and choose it (V).", GREY));
            return out;
        }

        out.add(new Line("Target: " + PickerState.targetType(), WHITE));

        String token = PickerState.currentPart != null ? PickerState.currentPart : "none";
        int n = PickerState.parts.size();
        int i = n == 0 ? 0 : (Math.floorMod(PickerState.partIndex, n) + 1);
        out.add(new Line("Part: " + token + "  (" + i + "/" + n + ")   [ ] to cycle", WHITE));

        if (PickerState.field.isAngle()) {
            out.add(new Line(String.format("Field: %s = %.1f°   step %.1f°   - / = adjust, ; ' field, \\ step",
                    PickerState.field.label, PickerState.fieldValue(), PickerState.step()), WHITE));
        } else {
            out.add(new Line(String.format("Field: %s = %.3f   step %.3f   - / = adjust, ; ' field, \\ step",
                    PickerState.field.label, PickerState.fieldValue(), PickerState.step()), WHITE));
        }

        EyeConfig d = PickerState.currentEye;
        out.add(new Line(String.format("Eye: pos[%.2f, %.2f, %.2f]  eye %.2f  iris %.2f  glow %b  invis %b",
                d.position[0], d.position[1], d.position[2], d.eyeScale, d.irisScale, d.glows, d.affectedByInvisibility), GREY));

        String sel = PickerState.selectedIndex >= 0 ? " · editing #" + (PickerState.selectedIndex + 1) : " · new eye";
        out.add(new Line("Saved: " + PickerState.committedCount() + " eyes" + sel, WHITE));
        out.add(new Line("Enter save · M mirror · Backspace undo · G glow · I invis · P export", GREY));
        return out;
    }
}
