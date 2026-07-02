package com.github.crittscott.somegoogly.eye;

import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;

/**
 * The placement (geometry) of a single eye on a mob: offset from the attach joint, sizes, aim, and
 * whether invisibility hides it. Config-only — never carried on an eye item (that's appearance, see
 * {@link AppearanceOverride}).
 *
 * <p>Aim is two angles, not a quaternion: {@code inclination} from the part's +Y axis and
 * {@code azimuth} from its +X axis (degrees). The eye's pupil faces local -Z by default; roll is
 * irrelevant (the eye is rotationally symmetric about its look axis), so two angles suffice.
 *
 * <p>{@code crossTarget} is the index of another eye <b>in the same head</b> that this eye rolls its
 * pupil toward during the cross-eye behavior (see {@code CrossEyeBehavior}); {@code -1} (the default)
 * means this eye doesn't cross. It's a within-head index into that head's {@code eyes} list.
 */
public record EyePlacement(Vec3 position, double eyeScale, double irisScale,
                           double inclination, double azimuth, boolean affectedByInvisibility,
                           int crossTarget) {

    /** Default orientation: pupil facing local -Z (straight ahead), matching the unrotated eye. */
    public static final double DEFAULT_AZIMUTH = 270.0;
    public static final double DEFAULT_INCLINATION = 90.0;
    /** No cross-eye partner: the eye stays neutral during the cross-eye behavior. */
    public static final int NO_CROSS_TARGET = -1;

    public static final EyePlacement DEFAULT = new EyePlacement(
            new Vec3(-0.13, -0.25, -0.25), 0.75, 0.6,
            DEFAULT_INCLINATION, DEFAULT_AZIMUTH, true, NO_CROSS_TARGET);

    /** {@link MapCodec} (flat fields) so {@link EyeDefinition} can sit placement beside appearance. */
    public static final MapCodec<EyePlacement> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Vec3.CODEC.optionalFieldOf("position", DEFAULT.position).forGetter(EyePlacement::position),
            Codec.DOUBLE.optionalFieldOf("eyeScale", 0.75).forGetter(EyePlacement::eyeScale),
            Codec.DOUBLE.optionalFieldOf("irisScale", 0.6).forGetter(EyePlacement::irisScale),
            Codec.DOUBLE.optionalFieldOf("inclination", DEFAULT_INCLINATION).forGetter(EyePlacement::inclination),
            Codec.DOUBLE.optionalFieldOf("azimuth", DEFAULT_AZIMUTH).forGetter(EyePlacement::azimuth),
            Codec.BOOL.optionalFieldOf("affectedByInvisibility", true).forGetter(EyePlacement::affectedByInvisibility),
            Codec.INT.optionalFieldOf("crossTarget", NO_CROSS_TARGET).forGetter(EyePlacement::crossTarget)
    ).apply(inst, EyePlacement::new));

    public static final Codec<EyePlacement> CODEC = MAP_CODEC.codec();

    /** The offset as the {@code float[3]} the renderer/model APIs expect. */
    public float[] positionArray() {
        return new float[]{(float) position.x, (float) position.y, (float) position.z};
    }
}
