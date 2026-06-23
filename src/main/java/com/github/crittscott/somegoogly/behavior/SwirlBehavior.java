package com.github.crittscott.somegoogly.behavior;

import com.github.crittscott.somegoogly.head.HeadInfo;

/**
 * Pupils orbit rapidly, the orbit radius shrinking to zero so they spiral into the center as the effect
 * ends. Fully overrides the iris channel ({@code irisWeight = 1}). The target position is stored
 * directly in the instance's x/y so a tick's worth of rotation interpolates smoothly.
 */
final class SwirlBehavior extends AbstractEyeBehavior {

    private static final float RADIUS = 0.8f;
    private static final float SPEED = 0.6f; // radians per tick

    SwirlBehavior() {
        super("swirl", 70);
    }

    @Override
    public void onStart(BehaviorInstance i) {
        i.prevWeight = i.weight = 1f;
    }

    @Override
    public void tick(BehaviorInstance i) {
        i.prevX = i.x;
        i.prevY = i.y;
        double angle = SPEED * i.age;
        float radius = RADIUS * (1f - (float) i.age / i.duration); // shrink to 0
        i.x = (float) Math.cos(angle) * radius;
        i.y = (float) Math.sin(angle) * radius;
    }

    @Override
    public void contribute(BehaviorInstance i, HeadInfo helper, int head, int eye, float pt, EyeRenderContribution out) {
        out.irisTargetX = Curves.lerp(i.prevX, i.x, pt);
        out.irisTargetY = Curves.lerp(i.prevY, i.y, pt);
        out.irisWeight = 1f;
    }
}
