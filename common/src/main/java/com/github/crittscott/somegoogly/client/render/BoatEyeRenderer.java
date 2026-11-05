package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.PresentationEyes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.vehicle.Boat;

/** Eyes in the hull model's space, including vanilla yaw, damage rocking, and bubble rotation. */
public final class BoatEyeRenderer {

    private BoatEyeRenderer() {
    }

    /** The caller retains the hull pose; ordinary boat rendering and water masking stay with vanilla. */
    public static void render(Boat boat, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        PresentationEyes eyes = ClientEyeRuntime.boatEyes(boat);
        if (eyes == null) {
            return;
        }
        for (int part : eyes.parts) {
            GooglyEyeRenderer.renderHead(pose, buffers, light, OverlayTexture.NO_OVERLAY, eyes.eyes[part], eyes.helper,
                    eyes.snapshot.properties(), part, partialTick);
        }
    }
}
