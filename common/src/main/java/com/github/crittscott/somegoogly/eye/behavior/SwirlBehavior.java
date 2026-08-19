package com.github.crittscott.somegoogly.eye.behavior;

/**
 * Pupils orbit rapidly, the orbit radius shrinking to zero so they spiral into the center as the effect
 * ends. Drives the pupil with a firm spring to a moving anchor; because the anchor itself spirals to
 * center, the pupil ends near center and the release is soft (no drop from the rim).
 */
final class SwirlBehavior extends AbstractEyeBehavior {

    private static final float RADIUS = 0.8f;
    private static final float SPEED = 0.6f;     // radians per tick
    private static final float STIFFNESS = 3.0f; // firm so the pupil tracks the fast orbit

    SwirlBehavior() {
        super("swirl", 70);
    }

    @Override
    public void influence(BehaviorInstance i, int head, int eye, EyeInfluence out) {
        float t = (float) i.age / i.duration;
        double angle = SPEED * i.age;
        float radius = RADIUS * (1f - t); // shrink to 0
        out.anchorX = (float) Math.cos(angle) * radius;
        out.anchorY = (float) Math.sin(angle) * radius;
        out.stiffness = STIFFNESS;
    }
}
