package gg.topchdlc.vse.utils.other;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Scheduler {
    private final CopyOnWriteArrayList<SchedulerAction> actions = new CopyOnWriteArrayList<>();

    public Scheduler() {
        Client.EVENTS.register(this);
    }

    public void scheduleOnce(Runnable runnable, int ticks) {
        actions.add(new SchedulerAction(runnable, ticks, false));
    }
    
    public void scheduleForever(Runnable runnable, int interval) {
        actions.add(new SchedulerAction(runnable, interval, true));
    }

    public void cancelAll() {
        actions.clear();
    }

    EventBus<Event> ticker = event -> {
        if (event instanceof EventGameTick) {
            if (actions.isEmpty()) return;

            List<SchedulerAction> toRemove = new ArrayList<>();
            
            for (SchedulerAction action : actions) {
                action.tick--;
                if (action.tick <= 0) {
                    try {
                        action.action.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    if (!action.forever) {
                        toRemove.add(action);
                    } else {
                        action.tick = action.startTick;
                    }
                }
            }

            if (!toRemove.isEmpty()) {
                actions.removeAll(toRemove);
            }
        }
    };

    private static class SchedulerAction {
        int tick, startTick;
        Runnable action;
        boolean forever;
        
        public SchedulerAction(Runnable action, int tick, boolean forever) {
            this.action = action;
            this.tick = tick;
            this.startTick = tick;
            this.forever = forever;
        }
    }
}