package com.github.crittscott.somegoogly.client.render;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.ItemEyes;
import com.github.crittscott.somegoogly.client.ModelGooglyEye;
import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.vehicle.AbstractBoat;

/** Extracts boat eyes once per frame and draws them in the hull's composed pose. */
public final class BoatEyeRenderer {

    private static final ModelGooglyEye MODEL = new ModelGooglyEye();

    private BoatEyeRenderer() {
    }

    public record Data(@Nullable ItemEyes eyes, float partialTick) {
        public static final Data NONE = new Data(null, 0F);
    }

    public interface Holder {
        Data somegoogly$boatEyes();

        void somegoogly$setBoatEyes(Data data);
    }

    /** Capture the boat's current eyes before the renderer loses access to the live entity. */
    public static Data extract(AbstractBoat boat, float partialTick) {
        ItemEyes eyes = ClientEyeRuntime.boatEyes(boat);
        return eyes == null ? Data.NONE : new Data(eyes, partialTick);
    }

    /** Vanilla owns hull, cargo, paddles, and water masking; only the extracted eye data draws here. */
    public static void render(BoatRenderState state, PoseStack pose, MultiBufferSource buffers, int light) {
        Data data = ((Holder) state).somegoogly$boatEyes();
        ItemEyes eyes = data.eyes();
        if (eyes == null) {
            return;
        }
        for (int part : eyes.parts) {
            for (int eye = 0; eye < eyes.helper.getEyeCount(part); eye++) {
                if (eyes.helper.getEyeScale(part, eye) > 0F) {
                    GooglyEyeRenderer.renderEye(pose, MODEL, buffers, light, OverlayTexture.NO_OVERLAY,
                            eyes.eyes[part][eye], eyes.helper, eyes.snapshot.properties(), part, eye, data.partialTick());
                }
            }
        }
    }
}
