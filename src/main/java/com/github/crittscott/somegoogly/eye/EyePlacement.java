package com.github.crittscott.somegoogly.eye;

import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;

/**
 * The placement (geometry) of a single eye on a mob: offset from the attach joint, sizes, and aim.
 * Config-only — never carried on an eye item (that's appearance, see {@link AppearanceOverride}).
 *
 * <p>Aim is two angles, not a quaternion: {@code inclination} from the part's +Y axis and
 * {@code azimuth} from its +X axis (degrees). The eye's pupil faces local -Z by default; roll is
 * irrelevant (the eye is rotationally symmetric about its look axis), so two angles suffice.
 *
 * <p>{@code depth} multiplies the eye's thickness along its look axis ({@code 1.0} = the standard
 * proportions). The eye's back face sits at its attach position and the body extends forward, so a
 * deeper eye protrudes further from the model — enough to envelop a modeled eyeball that sticks out
 * of the head (e.g. the Ice and Fire cyclops). Cornea and iris stretch together, so the iris always
 * stays in front of the cornea.
 *
 * <p>{@code crossTarget} is the index of another eye <b>in the same head</b> that this eye rolls its
 * pupil toward during the cross-eye behavior (see {@code CrossEyeBehavior}); {@code -1} (the default)
 * means this eye doesn't cross. It's a within-head index into that head's {@code eyes} list.
 */
public record EyePlacement(Vec3 position, double eyeScale, double irisScale, double depth,
                           double inclination, double azimuth, int crossTarget) {

    /** Default orientation: pupil facing local -Z (straight ahead), matching the unrotated eye. */
    public static final double DEFAULT_AZIMUTH = 270.0;
    public static final double DEFAULT_INCLINATION = 90.0;
    /** No cross-eye partner: the eye stays neutral during the cross-eye behavior. */
    public static final int NO_CROSS_TARGET = -1;

    /** Standard thickness: the depth multiplier that reproduces the classic googly-eye proportions. */
    public static final double DEFAULT_DEPTH = 1.0;

    public static final EyePlacement DEFAULT = new EyePlacement(
            new Vec3(-0.13, -0.25, -0.25), 0.75, 0.6, DEFAULT_DEPTH,
            DEFAULT_INCLINATION, DEFAULT_AZIMUTH, NO_CROSS_TARGET);

    /** {@link MapCodec} (flat fields) so {@link EyeDefinition} can sit placement beside appearance. */
    public static final MapCodec<EyePlacement> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Vec3.CODEC.optionalFieldOf("position", DEFAULT.position).forGetter(EyePlacement::position),
            Codec.DOUBLE.optionalFieldOf("eyeScale", DEFAULT.eyeScale).forGetter(EyePlacement::eyeScale),
            Codec.DOUBLE.optionalFieldOf("irisScale", DEFAULT.irisScale).forGetter(EyePlacement::irisScale),
            Codec.DOUBLE.optionalFieldOf("depth", DEFAULT_DEPTH).forGetter(EyePlacement::depth),
            Codec.DOUBLE.optionalFieldOf("inclination", DEFAULT_INCLINATION).forGetter(EyePlacement::inclination),
            Codec.DOUBLE.optionalFieldOf("azimuth", DEFAULT_AZIMUTH).forGetter(EyePlacement::azimuth),
            Codec.INT.optionalFieldOf("crossTarget", NO_CROSS_TARGET).forGetter(EyePlacement::crossTarget)
    ).apply(inst, EyePlacement::new));

    public static final Codec<EyePlacement> CODEC = MAP_CODEC.codec();

    /** The offset as the {@code float[3]} the renderer/model APIs expect. */
    public float[] positionArray() {
        return new float[]{(float) position.x, (float) position.y, (float) position.z};
    }
}
