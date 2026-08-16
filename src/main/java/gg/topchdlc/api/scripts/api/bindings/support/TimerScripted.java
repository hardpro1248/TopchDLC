package gg.topchdlc.api.scripts.api.bindings.support;

public class TimerScripted {
    private long lastTime = 0L;

    public void reset() {
        lastTime = System.currentTimeMillis();
    }
    public long getTime() {
        return System.currentTimeMillis() - lastTime;
    }
    public boolean reached(double time, boolean reset) {
        boolean elapsed = System.currentTimeMillis() - lastTime > time;
        if (elapsed && reset) reset();
        return elapsed;
    }
}
