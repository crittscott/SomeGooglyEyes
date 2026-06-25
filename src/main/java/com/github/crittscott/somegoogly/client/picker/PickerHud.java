package com.github.crittscott.somegoogly.client.picker;

import com.github.crittscott.somegoogly.eye.HeadInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

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
     * Glow/invis are shown as {@code +G}/{@code -G} and {@code +Inv}/{@code -Inv} (set/unset) to keep
     * the line short; cornea/iris colors follow as {@code c#RRGGBB i#RRGGBB}.
     */
    private static void appendEye(List<Line> out, String label, String part, EyeDraft e, int colorA, int colorB) {
        out.add(new Line(String.format("%s  part=%s  pos[%.2f, %.2f, %.2f]",
                label, part, e.position[0], e.position[1], e.position[2]), colorA));
        double incl = e.inclination != null ? e.inclination : HeadInfo.DEFAULT_INCLINATION;
        double azi = e.azimuth != null ? e.azimuth : HeadInfo.DEFAULT_AZIMUTH;
        out.add(new Line(String.format("    incl %.1f°  azi %.1f°  eye %.2f  iris %.2f  %sG %sInv  c%s i%s",
                incl, azi, e.eyeScale, e.irisScale,
                e.glows ? "+" : "-", e.affectedByInvisibility ? "+" : "-",
                hex(e.corneaColors), hex(e.irisColors)), colorB));
    }

    private static int channel(double v) {
        int c = (int) Math.round(v * 255.0);
        return c < 0 ? 0 : Math.min(c, 255);
    }

    /** An RGB triple in 0–1 as {@code #RRGGBB} (8-bit, rounded), or a placeholder if absent/malformed. */
    private static String hex(double[] rgb) {
        if (rgb == null || rgb.length < 3) {
            return "#------";
        }
        return String.format("#%02X%02X%02X", channel(rgb[0]), channel(rgb[1]), channel(rgb[2]));
    }

    private static List<Line> lines() {
        List<Line> out = new ArrayList<>();
        out.add(new Line("Googly Eye Picker", YELLOW));

        if (PickerState.target() == null) {
            out.add(new Line("Look at a mob and choose it (V).", GRAY));
            return out;
        }

        out.add(new Line("Target: " + PickerState.targetType(), WHITE));

        String token = PickerState.currentPart != null ? PickerState.currentPart : "none";
        int n = PickerState.parts.size();
        int i = n == 0 ? 0 : (Math.floorMod(PickerState.partIndex, n) + 1);
        out.add(new Line("Part: " + token + "  (" + i + "/" + n + ")   [ ] cycle", WHITE));

        out.add(new Line(String.format("Variant %d/%d  (weight %.2f)",
                PickerState.variantIndex + 1, PickerState.variantCount(), PickerState.currentVariant().weight), WHITE));

        out.add(new Line("Eyes (" + PickerState.currentEyeCount() + "):", WHITE));

        // One block per saved eye in the current variant (two lines). The eye being edited via /sg
        // (selectedIndex) is drawn live from currentEye and highlighted; if nothing is selected, the
        // in-progress currentEye is appended as a trailing "new (unsaved)" block — what /sg save commits.
        java.util.List<PickerState.ListedEye> eyes = PickerState.currentEyes();
        for (int idx = 0; idx < eyes.size(); idx++) {
            PickerState.ListedEye listed = eyes.get(idx);
            if (idx == PickerState.selectedIndex) {
                appendEye(out, "▶ #" + (idx + 1), partOrNone(PickerState.currentPart), PickerState.currentEye, YELLOW, YELLOW);
            } else {
                appendEye(out, "#" + (idx + 1), partOrNone(listed.part), listed.eye, WHITE, GRAY);
            }
        }

        if (PickerState.selectedIndex < 0) {
            appendEye(out, "▶ new (unsaved)", partOrNone(PickerState.currentPart), PickerState.currentEye, YELLOW, YELLOW);
        }

        return out;
    }

    private static String partOrNone(String part) {
        return part != null ? part : "none";
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
