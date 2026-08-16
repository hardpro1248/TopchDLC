package gg.topchdlc.vse.utils.animations;

import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.math.easings.Easing;
import net.minecraft.util.math.MathHelper;

public class TweenAnimation {
    public TweenAnimation(long durationNanos, long delayNanos, float from, float to, Easing easing) {
        this.durationNanos = durationNanos;
        this.delayNanos = delayNanos;
        this.from = from;
        this.to = to;
        this.easing = easing;
    }

    public long startNanos = 0, durationNanos, delayNanos;
    public float from, to;
    public Easing easing;

    public float get() {
        long nanos = System.nanoTime();
        nanos = MathHelper.clamp(nanos - startNanos - delayNanos, 0, durationNanos);
        if (nanos > durationNanos) {
            return to;
        } else {
            float frac = easing.easef((float)nanos / (float)durationNanos);
            return MathUtility.linear(from, to, frac);
        }
    }

    public void reset() {
        startNanos = System.nanoTime();
    }

    public float get(boolean reset) {
        float value = get();
        if (reset && value == to) {
            reset();
        }
        return value;
    }

    public static TweenAnimation millis(long duration, long delay, float from, float to, Easing easing) {
        return new TweenAnimation(duration * MillisToNanos, delay * MillisToNanos, from, to, easing);
    }

    public static final long MillisToNanos = 1_000_000L;
}
