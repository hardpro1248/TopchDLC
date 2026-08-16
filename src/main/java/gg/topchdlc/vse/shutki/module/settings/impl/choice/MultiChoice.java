package gg.topchdlc.vse.shutki.module.settings.impl.choice;

import gg.topchdlc.api.events.Event;

import java.util.function.BiFunction;

public class MultiChoice<C extends Choice> {
    final C[] choices;

    public MultiChoice(C[] choices) {
        this.choices = choices;
    }

    public C[] get() {
        return choices;
    }

    public void onEnabled() {
        for (C choice : choices) {
            if (choice.isEnabled())
                choice.onEnabled();
        }
    }

    public void onDisabled() {
        for (C choice : choices) {
            if (choice.isEnabled()) choice.onDisabled();
        }
    }

    public void onEvent(Event event) {
        for (C choice : choices) {
            if (choice.isEnabled()) choice.onEvent(event);
        }
    }

    public <T> T reduce(T value, BiFunction<C, T, T> reducer) {
        for (C choice : choices) {
            if (choice.isEnabled()) value = reducer.apply(choice, value);
        }
        return value;
    }
}
