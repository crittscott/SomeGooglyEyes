package com.github.crittscott.somegoogly.client.picker;

import com.github.crittscott.somegoogly.eye.state.EyeColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.List;

/**
 * Text HUD for the part picker: a live, read-only view of the locked mob, the selected part, and the
 * saved eye list (two lines per eye), with the eye currently being edited via {@code /sg} highlighted.
 *
 * <p>Anchored top-right and grown downward, so it stays clear of the {@code /sg} chat output in the
 * lower-left. Drawn in-world (no screen) while the picker is active; it never mutates anything — all
 * eye editing is done through the {@code /sg} CLI.
 */
public final class PickerHud {

    // Translucent dark backdrop so the text stays legible over a busy scene while the mob shows
    // through. ARGB: raise the leading alpha byte (0xA0) toward 0xFF for a more opaque panel.
    private static final int BACKDROP = 0xA0101010;
    private static final int GRAY = 0xFFB0B0B0;
    private static final int LINE_HEIGHT = 10;
    private static final int PADDING = 4;
    private static final int RIGHT_MARGIN = 6;
    private static final int TOP_MARGIN = 6;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int YELLOW = 0xFFFFE060;

    private PickerHud() {
    }

    private record Line(String text, int color) {
    }

    /**
     * Add an eye as two lines: identity/part/position, then orientation/scales/flags (indented).
     * Glow is shown as {@code +G}/{@code -G} (set/unset) to keep the line short; cornea/iris colors
     * follow as {@code c#RRGGBB i#RRGGBB}.
     */
    private static void appendEye(List<Line> out, String label, String part, EyeDraft e, int colorA, int colorB) {
        out.add(new Line(I18n.get("somegoogly.picker.hud.eye_line1", label, part,
                String.format("%.2f", e.position[0]), String.format("%.2f", e.position[1]), String.format("%.2f", e.position[2])), colorA));
        String cross = e.crossTarget >= 0 ? "  X→" + (e.crossTarget + 1) : "";
        out.add(new Line(I18n.get("somegoogly.picker.hud.eye_line2",
                String.format("%.0f", e.inclination), String.format("%.0f", e.azimuth),
                String.format("%.2f", e.eyeScale), String.format("%.2f", e.irisScale), String.format("%.2f", e.depth),
                e.glows ? "+" : "-",
                hex(e.corneaColors), hex(e.irisColors), cross), colorB));
    }

    /** An RGB triple in 0–1 as {@code #RRGGBB} (8-bit, rounded). */
    private static String hex(float[] rgb) {
        return String.format("#%06X", EyeColor.of(rgb).toRgb24());
    }

    private static List<Line> lines() {
        List<Line> out = new ArrayList<>();
        out.add(new Line(I18n.get("somegoogly.picker.hud.title"), YELLOW));

        if (PickerState.target() == null) {
            out.add(new Line(I18n.get("somegoogly.picker.hud.no_target"), GRAY));
            return out;
        }

        out.add(new Line(I18n.get("somegoogly.picker.hud.target", PickerState.targetType()), WHITE));

        String token = partOrNone(PickerState.currentPart());
        int n = PickerState.parts().size();
        int i = n == 0 ? 0 : (Math.floorMod(PickerState.partIndex(), n) + 1);
        out.add(new Line(I18n.get("somegoogly.picker.hud.part", token, i, n), WHITE));

        out.add(new Line(I18n.get("somegoogly.picker.hud.variant", PickerState.variantIndex() + 1,
                PickerState.variantCount(), String.format("%.2f", PickerState.currentVariant().weight)), WHITE));

        out.add(new Line(I18n.get("somegoogly.picker.hud.eyes_header", PickerState.currentEyeCount()), WHITE));

        // One block per saved eye in the current variant (two lines). The eye being edited via /sg
        // (selectedIndex) is drawn live from currentEye and highlighted; if nothing is selected, the
        // in-progress currentEye is appended as a trailing "new (unsaved)" block — what /sg save commits.
        java.util.List<PickerState.ListedEye> eyes = PickerState.currentEyes();
        for (int idx = 0; idx < eyes.size(); idx++) {
            PickerState.ListedEye listed = eyes.get(idx);
            if (idx == PickerState.selectedIndex()) {
                appendEye(out, I18n.get("somegoogly.picker.hud.selected_marker", idx + 1), token, PickerState.currentEye(), YELLOW, YELLOW);
            } else {
                appendEye(out, I18n.get("somegoogly.picker.hud.eye_marker", idx + 1), partOrNone(listed.part), listed.eye, WHITE, GRAY);
            }
        }

        if (PickerState.selectedIndex() < 0 && PickerState.currentEye() != null) {
            appendEye(out, I18n.get("somegoogly.picker.hud.new_unsaved"), token, PickerState.currentEye(), YELLOW, YELLOW);
        }

        return out;
    }

    private static String partOrNone(String part) {
        return part != null ? part : I18n.get("somegoogly.picker.hud.none");
    }

    public static void render(GuiGraphics graphics, int width, int height) {
        if (!PickerState.isActive()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        List<Line> lines = lines();

        int widest = 0;
        for (Line line : lines) {
            widest = Math.max(widest, font.width(line.text));
        }

        // Anchor the panel against the right edge and grow downward from the top.
        int right = width - RIGHT_MARGIN;
        int left = right - widest;
        graphics.fill(
                left - PADDING,
                TOP_MARGIN - PADDING,
                right + PADDING,
                TOP_MARGIN + lines.size() * LINE_HEIGHT + PADDING,
                BACKDROP);

        int y = TOP_MARGIN;
        for (Line line : lines) {
            graphics.drawString(font, line.text, left, y, line.color);
            y += LINE_HEIGHT;
        }
    }
}
