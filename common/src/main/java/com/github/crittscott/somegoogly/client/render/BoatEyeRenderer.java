package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.ItemEyes;
import com.github.crittscott.somegoogly.client.ModelGooglyEye;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.vehicle.Boat;

/** Eyes in the hull model's space, including vanilla yaw, damage rocking, and bubble rotation. */
public final class BoatEyeRenderer {

    private static final ModelGooglyEye MODEL = new ModelGooglyEye();

    private BoatEyeRenderer() {
    }

    /** The caller retains the hull pose; ordinary boat rendering and water masking stay with vanilla. */
    public static void render(Boat boat, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        ItemEyes eyes = ClientEyeRuntime.boatEyes(boat);
        if (eyes == null) {
            return;
        }
        for (int part : eyes.parts) {
            for (int eye = 0; eye < eyes.helper.getEyeCount(part); eye++) {
                if (eyes.helper.getEyeScale(part, eye) > 0F) {
                    GooglyEyeRenderer.renderEye(pose, MODEL, buffers, light, OverlayTexture.NO_OVERLAY,
                            eyes.eyes[part][eye], eyes.helper, eyes.snapshot.properties(), part, eye, partialTick);
                }
            }
        }
    }
}
