package com.github.crittscott.somegoogly.picker;

import com.github.crittscott.somegoogly.head.EyeDefinition;
import com.github.crittscott.somegoogly.head.EyePlacement;
import com.github.crittscott.somegoogly.state.EyeAppearance;
import com.github.crittscott.somegoogly.state.EyeColor;
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
    public double sideOffset = 0.0;
    public Double inclination = EyePlacement.DEFAULT_INCLINATION; // null = default forward
    public Double azimuth = EyePlacement.DEFAULT_AZIMUTH;
    public double[] corneaColors = {1.0, 1.0, 1.0};
    public double[] irisColors = {0.0, 0.0, 0.0};
    public boolean glows = false;
    public boolean affectedByInvisibility = true;

    public double aimInclination() {
        return inclination != null ? inclination : EyePlacement.DEFAULT_INCLINATION;
    }

    public double aimAzimuth() {
        return azimuth != null ? azimuth : EyePlacement.DEFAULT_AZIMUTH;
    }

    public EyeDraft copy() {
        EyeDraft d = new EyeDraft();
        d.position = new double[]{position[0], position[1], position[2]};
        d.eyeScale = eyeScale;
        d.irisScale = irisScale;
        d.sideOffset = sideOffset;
        d.inclination = aimInclination();
        d.azimuth = aimAzimuth();
        d.corneaColors = new double[]{corneaColors[0], corneaColors[1], corneaColors[2]};
        d.irisColors = new double[]{irisColors[0], irisColors[1], irisColors[2]};
        d.glows = glows;
        d.affectedByInvisibility = affectedByInvisibility;
        return d;
    }

    /** Snapshot this draft as the immutable datapack definition the rest of the mod uses. */
    public EyeDefinition toDefinition() {
        EyePlacement placement = new EyePlacement(
                new Vec3(position[0], position[1], position[2]),
                eyeScale, irisScale, sideOffset, aimInclination(), aimAzimuth(), affectedByInvisibility);
        EyeAppearance appearance = new EyeAppearance(
                new EyeColor((float) corneaColors[0], (float) corneaColors[1], (float) corneaColors[2]),
                new EyeColor((float) irisColors[0], (float) irisColors[1], (float) irisColors[2]),
                glows);
        return new EyeDefinition(placement, appearance);
    }
}
