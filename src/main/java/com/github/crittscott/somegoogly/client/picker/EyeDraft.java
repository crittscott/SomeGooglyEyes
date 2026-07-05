package com.github.crittscott.somegoogly.client.picker;

import com.github.crittscott.somegoogly.eye.EyeDefinition;
import com.github.crittscott.somegoogly.eye.EyePlacement;
import com.github.crittscott.somegoogly.eye.state.EyeAppearance;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import net.minecraft.world.phys.Vec3;

/**
 * Mutable working copy of one eye while authoring in the picker. Mirrors the datapack eye fields
 * one-to-one (flat, like the JSON) so the CLI / HUD / preview can read and tweak them in place; it is
 * converted to an immutable {@link EyeDefinition} on save/export ({@link #toDefinition()}).
 */
public class EyeDraft {
    public double[] position = {-0.13, -0.25, -0.25};
    public double eyeScale = 0.75;
    public double irisScale = 0.6;
    /** Thickness multiplier along the look axis (1 = standard googly proportions). */
    public double depth = EyePlacement.DEFAULT_DEPTH;
    public Double inclination = EyePlacement.DEFAULT_INCLINATION; // null = default forward
    public Double azimuth = EyePlacement.DEFAULT_AZIMUTH;
    public double[] corneaColors = {1.0, 1.0, 1.0};
    public double[] irisColors = {0.0, 0.0, 0.0};
    public boolean glows = false;
    /**
     * Cross-eye partner as a <b>flat</b> index into the current variant's eye list (0-based, {@code -1} =
     * none) — authoring space, matching what {@code /sg list eyes} shows. It's resolved to the on-disk
     * <i>within-head</i> index only at export ({@link com.github.crittscott.somegoogly.client.picker.PickerState}),
     * so this stays valid as eyes move between heads while authoring.
     */
    public int crossTarget = -1;

    public double aimAzimuth() {
        return azimuth != null ? azimuth : EyePlacement.DEFAULT_AZIMUTH;
    }

    public double aimInclination() {
        return inclination != null ? inclination : EyePlacement.DEFAULT_INCLINATION;
    }

    /** Build a working draft from an immutable definition (the inverse of {@link #toDefinition()}). */
    public static EyeDraft fromDefinition(EyeDefinition def) {
        EyePlacement p = def.placement();
        EyeAppearance a = def.appearance();
        EyeColor cornea = a.cornea();
        EyeColor iris = a.iris();
        EyeDraft d = new EyeDraft();
        Vec3 pos = p.position();
        d.position = new double[]{pos.x, pos.y, pos.z};
        d.eyeScale = p.eyeScale();
        d.irisScale = p.irisScale();
        d.depth = p.depth();
        d.inclination = p.inclination();
        d.azimuth = p.azimuth();
        d.corneaColors = new double[]{cornea.r(), cornea.g(), cornea.b()};
        d.irisColors = new double[]{iris.r(), iris.g(), iris.b()};
        d.glows = a.glow();
        return d;
    }

    public EyeDraft copy() {
        EyeDraft d = new EyeDraft();
        d.position = new double[]{position[0], position[1], position[2]};
        d.eyeScale = eyeScale;
        d.irisScale = irisScale;
        d.depth = depth;
        d.inclination = aimInclination();
        d.azimuth = aimAzimuth();
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
                eyeScale, irisScale, depth, aimInclination(), aimAzimuth(),
                resolvedCrossTarget);
        EyeAppearance appearance = new EyeAppearance(
                new EyeColor((float) corneaColors[0], (float) corneaColors[1], (float) corneaColors[2]),
                new EyeColor((float) irisColors[0], (float) irisColors[1], (float) irisColors[2]),
                glows);
        return new EyeDefinition(placement, appearance);
    }
}
