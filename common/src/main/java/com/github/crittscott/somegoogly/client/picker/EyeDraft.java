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
 *
 * <p>Everything is {@code float}, matching {@link EyePlacement}/{@link EyeAppearance} and the config
 * codecs — the draft holds exactly what will be written, at the precision it will be written at.
 */
public class EyeDraft {
    public float[] position;
    public float eyeScale;
    public float irisScale;
    /** Thickness multiplier along the look axis (1 = standard googly proportions). */
    public float depth;
    public float inclination;
    public float azimuth;
    public float[] corneaColors;
    public float[] irisColors;
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
        position = p.positionArray();
        eyeScale = p.eyeScale();
        irisScale = p.irisScale();
        depth = p.depth();
        inclination = p.inclination();
        azimuth = p.azimuth();
        corneaColors = a.cornea().toArray();
        irisColors = a.iris().toArray();
        glows = a.glow();
    }

    public EyeDraft copy() {
        EyeDraft d = new EyeDraft();
        d.position = position.clone();
        d.eyeScale = eyeScale;
        d.irisScale = irisScale;
        d.depth = depth;
        d.inclination = inclination;
        d.azimuth = azimuth;
        d.corneaColors = corneaColors.clone();
        d.irisColors = irisColors.clone();
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
                eyeScale, irisScale, depth,
                inclination, azimuth,
                resolvedCrossTarget);
        EyeAppearance appearance = new EyeAppearance(
                EyeColor.of(corneaColors), EyeColor.of(irisColors), glows);
        return new EyeDefinition(placement, appearance);
    }
}
