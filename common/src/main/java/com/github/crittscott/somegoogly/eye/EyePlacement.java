package com.github.crittscott.somegoogly.eye;

import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

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
 * pupil toward during the cross-eye behavior (see {@code EyeBehavior#CROSS_EYE}); {@link #NO_CROSS_TARGET}
 * means this eye doesn't cross. It's a within-head index into that head's {@code eyes} list.
 *
 * <p>Sizes and angles are {@code float}, and {@code position} serializes at float precision, so authored
 * values such as {@code 0.22} are written back to a datapack unchanged.
 */
public record EyePlacement(Vec3 position, float eyeScale, float irisScale, float depth,
                           float inclination, float azimuth, int crossTarget) {

    /** Default orientation: pupil facing local -Z (straight ahead), matching the unrotated eye. */
    public static final float DEFAULT_AZIMUTH = 270F;
    public static final float DEFAULT_INCLINATION = 90F;
    /** No cross-eye partner: the eye stays neutral during the cross-eye behavior. */
    public static final int NO_CROSS_TARGET = -1;

    /** Standard thickness: the depth multiplier that reproduces the classic googly-eye proportions. */
    public static final float DEFAULT_DEPTH = 1F;

    public static final EyePlacement DEFAULT = new EyePlacement(
            new Vec3(-0.13, -0.25, -0.25), 0.75F, 0.6F, DEFAULT_DEPTH,
            DEFAULT_INCLINATION, DEFAULT_AZIMUTH, NO_CROSS_TARGET);

    /** {@code [x, y, z]} at float precision, mirroring how {@code EyeColor} writes its channels. */
    private static final Codec<Vec3> POSITION_CODEC = Codec.FLOAT.listOf().comapFlatMap(
            list -> list.size() == 3
                    ? DataResult.success(new Vec3(list.get(0), list.get(1), list.get(2)))
                    : DataResult.error(() -> "Expected 3 position components, got " + list.size()),
            v -> List.of((float) v.x, (float) v.y, (float) v.z));

    /**
     * {@link MapCodec} (flat fields) so {@link EyeDefinition} can sit placement beside appearance.
     *
     * <p>Every field is required. A config has exactly one shape on disk, so absent-vs-default is a
     * distinction nothing reads — and making the fields optional would mean {@code encode} silently
     * elides any value equal to its default, leaving files whose meaning drifts with the code.
     */
    public static final MapCodec<EyePlacement> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            POSITION_CODEC.fieldOf("position").forGetter(EyePlacement::position),
            Codec.FLOAT.fieldOf("eyeScale").forGetter(EyePlacement::eyeScale),
            Codec.FLOAT.fieldOf("irisScale").forGetter(EyePlacement::irisScale),
            Codec.FLOAT.fieldOf("depth").forGetter(EyePlacement::depth),
            Codec.FLOAT.fieldOf("inclination").forGetter(EyePlacement::inclination),
            Codec.FLOAT.fieldOf("azimuth").forGetter(EyePlacement::azimuth),
            Codec.INT.fieldOf("crossTarget").forGetter(EyePlacement::crossTarget)
    ).apply(inst, EyePlacement::new));

    /** The offset as the {@code float[3]} the renderer/model APIs expect. */
    public float[] positionArray() {
        return new float[]{(float) position.x, (float) position.y, (float) position.z};
    }

    /**
     * The rotation from the attachment part's frame into this eye's local frame, whose -Z is the pupil
     * axis: azimuth turns about the part's vertical axis, then inclination tilts about the horizontal.
     */
    public Quaternionf orientation() {
        return orientation(inclination, azimuth);
    }

    /** {@link #orientation()} for raw authored angles, in degrees. */
    public static Quaternionf orientation(double inclination, double azimuth) {
        return new Quaternionf()
                .rotationY((float) Math.toRadians(-(azimuth + 90.0)))
                .rotateX((float) Math.toRadians(90.0 - inclination));
    }

    /** Project a head-frame direction into this eye's local pupil-plane right/up basis. */
    public float[] projectToPupilPlane(double dx, double dy, double dz) {
        Vector3f local = orientation().conjugate().transform(new Vector3f((float) dx, (float) dy, (float) dz));
        return new float[]{local.x, local.y};
    }
}
