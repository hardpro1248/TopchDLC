package gg.topchdlc.api.events.list;

public abstract class EventStoppable {

    private boolean stopped;

    protected EventStoppable() {
    }

    public void stop() {
        stopped = true;
    }

    public boolean isStopped() {
        return stopped;
    }

}
