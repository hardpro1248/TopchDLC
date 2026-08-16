package gg.topchdlc.vse.utils.math;

import lombok.Getter;

public class TickUtility {
    @Getter
    private int tick = 0;

    public void reset() {
        tick = 0;
    }

    public boolean reached(int ticks) {
        return tick > ticks;
    }

    public boolean reached(int ticks, boolean reset) {
        boolean elapsed = reached(ticks);
        if (elapsed && reset) reset();
        return elapsed;
    }

    public void tick() {
        tick++;
    }
}
