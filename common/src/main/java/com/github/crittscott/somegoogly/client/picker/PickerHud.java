package com.github.crittscott.somegoogly.client.picker;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.eye.EyePlacement;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

import static com.github.crittscott.somegoogly.command.MaybeFloatArgumentType.angle;
import static com.github.crittscott.somegoogly.command.MaybeFloatArgumentType.compact;

/**
 * Text HUD for the part picker: a live, read-only view of the locked mob, the selected part, and the
 * saved eye list (two lines per eye), with the eye currently being edited via {@code /sg} highlighted.
 *
 * <p>Anchored top-right and grown downward, so it stays clear of the {@code /sg} chat output in the
 * lower-left. Drawn in-world (no screen) while the picker is active; it never mutates anything — all
 * eye editing is done through the {@code /sg} CLI.
 */
public final class PickerHud {

    /** Translucent dark backdrop, leaving the target visible through the panel. */
    private static final int BACKDROP = 0xA0101010;
    private static final int LINE_GAP = 1;
    private static final int PADDING = 4;
    private static final int RIGHT_MARGIN = 6;
    private static final int TOP_MARGIN = 6;

    /** The panel's text, rebuilt at most once per client tick; {@code null} while the picker is off. */
    private static List<Component> cachedRows;
    private static int cachedRowsTick;

    private PickerHud() {
    }

    /** Add the identity/position and orientation/appearance rows for one saved or unsaved eye. */
    private static void appendEye(List<Component> out, Component label, Component part, EyeDraft eye,
                                  ChatFormatting colorA, ChatFormatting colorB) {
        out.add(Component.translatable("somegoogly.picker.hud.eye_line1", label, part,
                compact(eye.position[0]), compact(eye.position[1]), compact(eye.position[2])).withStyle(colorA));
        Component cross = eye.crossTarget != EyePlacement.NO_CROSS_TARGET
                ? Component.translatable("somegoogly.picker.cross_target_mark", eye.crossTarget + 1)
                : Component.empty();
        out.add(Component.translatable("somegoogly.picker.hud.eye_line2",
                angle(eye.inclination), angle(eye.azimuth),
                compact(eye.eyeScale), compact(eye.irisScale), compact(eye.depth),
                Component.translatable(eye.glows ? "somegoogly.picker.hud.glow_on" : "somegoogly.picker.hud.glow_off"),
                hex(eye.corneaColors), hex(eye.irisColors), cross).withStyle(colorB));
    }

    /** An RGB triple as {@code RRGGBB}; the row's translation supplies the punctuation. */
    private static String hex(float[] rgb) {
        return EyeColor.of(rgb).toHex();
    }

    /** The key's current binding, translated by the same component used in Controls. */
    private static Component keyName(KeyMapping key) {
        return key.getTranslatedKeyMessage();
    }

    private static List<Component> lines() {
        List<Component> out = new ArrayList<>();
        if (PickerState.target() == null) {
            out.add(Component.translatable("somegoogly.picker.hud.no_target", keyName(PickerInput.LOCK))
                    .withStyle(ChatFormatting.GRAY));
            return out;
        }

        String ageKey = PickerState.target().isBaby()
                ? "somegoogly.picker.hud.age_baby" : "somegoogly.picker.hud.age_adult";
        out.add(Component.translatable("somegoogly.picker.hud.target_row",
                Component.literal(PickerState.targetType().toString()).withStyle(ChatFormatting.WHITE),
                Component.translatable("somegoogly.picker.hud.variant_value", PickerState.variantIndex() + 1,
                        PickerState.variantCount(), compact(PickerState.currentVariant().weight)).withStyle(ChatFormatting.WHITE),
                Component.translatable(ageKey).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GREEN));

        Component token = partOrNone(PickerState.currentPart());
        int count = PickerState.parts().size();
        int index = count == 0 ? 0 : Math.floorMod(PickerState.partIndex(), count) + 1;
        out.add(Component.translatable("somegoogly.picker.hud.part_row",
                Component.translatable("somegoogly.picker.hud.part_value", token, index, count,
                        keyName(PickerInput.PART_PREV), keyName(PickerInput.PART_NEXT)).withStyle(ChatFormatting.WHITE))
                .withStyle(ChatFormatting.GREEN));
        out.add(Component.translatable("somegoogly.picker.hud.eyes_header",
                Component.literal(Integer.toString(PickerState.currentEyeCount())).withStyle(ChatFormatting.WHITE))
                .withStyle(ChatFormatting.GREEN));

        // Saved eyes have two rows each. The selected eye shows its live draft; an unsaved draft trails the list.
        List<PickerState.ListedEye> eyes = PickerState.currentEyes();
        for (int indexInList = 0; indexInList < eyes.size(); indexInList++) {
            PickerState.ListedEye listed = eyes.get(indexInList);
            if (indexInList == PickerState.selectedIndex()) {
                appendEye(out, Component.translatable("somegoogly.picker.hud.selected_marker", indexInList + 1),
                        token, PickerState.currentEye(), ChatFormatting.YELLOW, ChatFormatting.YELLOW);
            } else {
                appendEye(out, Component.translatable("somegoogly.picker.hud.eye_marker", indexInList + 1),
                        partOrNone(listed.part), listed.eye, ChatFormatting.WHITE, ChatFormatting.GRAY);
            }
        }
        if (PickerState.selectedIndex() == PickerState.NO_SELECTION && PickerState.currentEye() != null) {
            appendEye(out, Component.translatable("somegoogly.picker.hud.new_unsaved"), token,
                    PickerState.currentEye(), ChatFormatting.YELLOW, ChatFormatting.YELLOW);
        }
        return out;
    }

    private static Component partOrNone(String part) {
        return part != null ? Component.literal(part) : Component.translatable("somegoogly.picker.hud.none");
    }

    /** Draw the picker HUD while the picker is active. */
    public static void render(GuiGraphics graphics, int width) {
        if (!PickerState.isActive()) {
            cachedRows = null;
            return;
        }
        Font font = Minecraft.getInstance().font;
        int tick = ClientEyeRuntime.clientTicks();
        if (cachedRows == null || cachedRowsTick != tick) {
            cachedRows = lines();
            cachedRowsTick = tick;
        }
        List<Component> rows = cachedRows;
        int widest = 0;
        for (Component row : rows) {
            widest = Math.max(widest, font.width(row));
        }
        int lineHeight = font.lineHeight + LINE_GAP;
        int right = width - RIGHT_MARGIN;
        int left = right - widest;
        graphics.fill(left - PADDING, TOP_MARGIN - PADDING, right + PADDING,
                TOP_MARGIN + rows.size() * lineHeight + PADDING, BACKDROP);
        int y = TOP_MARGIN;
        for (Component row : rows) {
            graphics.drawString(font, row, left, y, ChatFormatting.WHITE.getColor());
            y += lineHeight;
        }
    }
}
