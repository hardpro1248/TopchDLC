package gg.topchdlc.api.events;

import lombok.Getter;

@Getter
public abstract class Event {
    boolean cancelled = false;
    public void cancel() {
        cancelled = true;
    }
    public void reset() {
        cancelled = false;
    }

}
