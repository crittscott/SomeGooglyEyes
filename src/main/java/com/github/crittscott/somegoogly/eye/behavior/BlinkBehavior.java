package com.github.crittscott.somegoogly.eye.behavior;

/**
 * Squashes between 1 and N of the mob's eyes shut and open again. The participating set is chosen once
 * from the seed, so a "wink" (one eye) and a full blink are the same behavior at different counts.
 * Non-physical overlay: drives the squash channel only — the pupil keeps wobbling underneath.
 */
final class BlinkBehavior extends AbstractEyeBehavior {

    private static final float SQUASH = 0.95f; // vertical squash at full close (→ ~5% height)

    BlinkBehavior() {
        super("blink", 8);
    }

    @Override
    public void influence(BehaviorInstance i, int head, int eye, EyeInfluence out) {
        if (i.mask != null && head < i.mask.length && eye < i.mask[head].length && i.mask[head][eye]) {
            out.squashY = 1f - SQUASH * Curves.sinPulse((float) i.age / i.duration);
        }
    }

    @Override
    public void onStart(BehaviorInstance i) {
        int heads = i.helper.getHeadCount();
        i.mask = new boolean[heads][];
        int total = 0;
        for (int h = 0; h < heads; h++) {
            int eyes = i.helper.getEyeCount(h);
            i.mask[h] = new boolean[eyes];
            total += eyes;
        }
        if (total <= 0) {
            return;
        }
        // Pick k ∈ [1, total] eyes at random to participate (k = 1 is a wink).
        int k = 1 + i.rand.nextInt(total);
        for (int picked = 0; picked < k; ) {
            int target = i.rand.nextInt(total);
            int seen = 0;
            outer:
            for (int h = 0; h < heads; h++) {
                for (int e = 0; e < i.mask[h].length; e++) {
                    if (seen++ == target) {
                        if (!i.mask[h][e]) {
                            i.mask[h][e] = true;
                            picked++;
                        }
                        break outer;
                    }
                }
            }
        }
    }
}
