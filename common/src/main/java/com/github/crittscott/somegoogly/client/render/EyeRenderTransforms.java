package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.eye.EyePlacement;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/** Rendering transforms that orient an eye's local pupil axis from its authored placement angles. */
public final class EyeRenderTransforms {

    private EyeRenderTransforms() {
    }

    public static void applyRotation(PoseStack poseStack, double inclination, double azimuth) {
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (-(azimuth + 90.0))));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (90.0 - inclination)));
    }

    public static void applyRotation(PoseStack poseStack, EyePlacement placement) {
        applyRotation(poseStack, placement.inclination(), placement.azimuth());
    }
}
