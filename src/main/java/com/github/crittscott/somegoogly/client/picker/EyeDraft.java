package com.github.crittscott.somegoogly.client.picker;

import com.github.crittscott.somegoogly.eye.EyeDefinition;
import com.github.crittscott.somegoogly.eye.EyePlacement;
import com.github.crittscott.somegoogly.eye.state.EyeAppearance;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import net.minecraft.world.phys.Vec3;

/**
 * Mutable working copy of one eye while authoring in the picker. Mirrors the datapack eye fields
 * one-to-one (flat, like the JSON) so the CLI / HUD / preview can read and tweak them in place; it is
 * converted to an immutable {@link EyeDefinition} on save/export ({@link #toDefinition}). A fresh
 * draft starts as {@link EyeDefinition#DEFAULT}, so the defaults live in one place.
 */
public class EyeDraft {
    public double[] position;
    public double eyeScale;
    public double irisScale;
    /** Thickness multiplier along the look axis (1 = standard googly proportions). */
    public double depth;
    public double inclination;
    public double azimuth;
    public double[] corneaColors;
    public double[] irisColors;
    public boolean glows;
    /**
     * Cross-eye partner as a <b>flat</b> index into the current variant's eye list (0-based, {@code -1} =
     * none) — authoring space, matching what {@code /sg list eyes} shows. It's resolved to the on-disk
     * <i>within-head</i> index only at export ({@link com.github.crittscott.somegoogly.client.picker.PickerState}),
     * so this stays valid as eyes move between heads while authoring.
     */
    public int crossTarget = -1;

    public EyeDraft() {
        loadDefinition(EyeDefinition.DEFAULT);
    }

    /** Build a working draft from an immutable definition (the inverse of {@link #toDefinition}). */
    public static EyeDraft fromDefinition(EyeDefinition def) {
        EyeDraft d = new EyeDraft();
        d.loadDefinition(def);
        return d;
    }

    private void loadDefinition(EyeDefinition def) {
        EyePlacement p = def.placement();
        EyeAppearance a = def.appearance();
        Vec3 pos = p.position();
        position = new double[]{pos.x, pos.y, pos.z};
        eyeScale = p.eyeScale();
        irisScale = p.irisScale();
        depth = p.depth();
        inclination = p.inclination();
        azimuth = p.azimuth();
        corneaColors = new double[]{a.cornea().r(), a.cornea().g(), a.cornea().b()};
        irisColors = new double[]{a.iris().r(), a.iris().g(), a.iris().b()};
        glows = a.glow();
    }

    public EyeDraft copy() {
        EyeDraft d = new EyeDraft();
        d.position = new double[]{position[0], position[1], position[2]};
        d.eyeScale = eyeScale;
        d.irisScale = irisScale;
        d.depth = depth;
        d.inclination = inclination;
        d.azimuth = azimuth;
        d.corneaColors = new double[]{corneaColors[0], corneaColors[1], corneaColors[2]};
        d.irisColors = new double[]{irisColors[0], irisColors[1], irisColors[2]};
        d.glows = glows;
        d.crossTarget = crossTarget;
        return d;
    }

    /**
     * Snapshot this draft as the immutable datapack definition, baking in {@code resolvedCrossTarget} —
     * the on-disk <i>within-head</i> index the caller computed from this draft's flat {@link #crossTarget}
     * (the draft's own value is authoring-space and never written directly).
     */
    public EyeDefinition toDefinition(int resolvedCrossTarget) {
        EyePlacement placement = new EyePlacement(
                new Vec3(position[0], position[1], position[2]),
                (float) eyeScale, (float) irisScale, (float) depth,
                (float) inclination, (float) azimuth,
                resolvedCrossTarget);
        EyeAppearance appearance = new EyeAppearance(
                new EyeColor((float) corneaColors[0], (float) corneaColors[1], (float) corneaColors[2]),
                new EyeColor((float) irisColors[0], (float) irisColors[1], (float) irisColors[2]),
                glows);
        return new EyeDefinition(placement, appearance);
    }
}
