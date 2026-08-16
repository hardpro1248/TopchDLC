package gg.topchdlc.api.events;

@FunctionalInterface
public interface EventBus<T extends Event> {
    void onEvent(T event);
}
