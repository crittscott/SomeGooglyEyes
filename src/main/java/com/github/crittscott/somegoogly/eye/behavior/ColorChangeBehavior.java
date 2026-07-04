package com.github.crittscott.somegoogly.eye.behavior;

import net.minecraft.util.Mth;

/**
 * Blends the cornea toward a color and back. The color is chosen from the seed (a random hue), so this
 * is the general form of what the old hardwired anger-tint did. Non-physical overlay: drives the
 * cornea-tint channel.
 */
final class ColorChangeBehavior extends AbstractEyeBehavior {

    ColorChangeBehavior() {
        super("color_change", 50);
    }

    @Override
    public void influence(BehaviorInstance i, int head, int eye, EyeInfluence out) {
        out.corneaTint = i.tintColor;
        // Ease in, hold the color, ease out.
        out.tintAmount = Curves.trapezoid((float) i.age / i.duration, 0.2f, 0.2f);
    }

    @Override
    public void onStart(BehaviorInstance i) {
        // A vivid random hue (full saturation/value) so the change reads clearly.
        int rgb = Mth.hsvToRgb(i.rand.nextFloat(), 1f, 1f);
        i.tintColor = new float[]{
                ((rgb >> 16) & 0xFF) / 255f,
                ((rgb >> 8) & 0xFF) / 255f,
                (rgb & 0xFF) / 255f
        };
    }
}
