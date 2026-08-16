package gg.topchdlc.vse.utils.math;

public class TimeUtility {
    private long lastTime = 0L;

    public void reset() {
        lastTime = System.currentTimeMillis();
    }
    public long getTime() {
        return System.currentTimeMillis() - lastTime;
    }
    public boolean reached(long time) {
        if (time == 0L) return true;
        return System.currentTimeMillis() - lastTime > time;
    }
    public boolean reached(long time, boolean reset) {
        boolean elapsed = System.currentTimeMillis() - lastTime > time;
        if (elapsed && reset) reset();
        return elapsed;
    }
    public void setTime(long time) {
        lastTime = System.currentTimeMillis();
    }
}
