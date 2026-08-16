package gg.topchdlc.vse.shutki.module.settings.impl.choice;

import com.google.gson.JsonObject;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.vse.shutki.module.settings.Setting;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.animations.impl.SmoothStepAnimation;
import lombok.Getter;

import java.util.List;

public class ChoiceSetting<C extends Choice> extends Setting.Basic<C, ChoiceSetting<C>> implements EventBus<Event> {
    @Getter
    private final List<C> choices;
    @Getter
    private boolean expanded = true;
    public SmoothStepAnimation expandAnim = new SmoothStepAnimation(150, 1);

    public ChoiceSetting<C> expanded(boolean expanded) {
        this.expanded = expanded;
        expandAnim.setDirection(expanded ? Direction.FORWARDS : Direction.BACKWARDS);
        return this;
    }

    @SafeVarargs
    public ChoiceSetting(String name, int defaultIndex, C... choices) {
        super(name, choices[defaultIndex]);
        this.choices = List.of(choices);
    }

    @Override
    public void onEvent(Event event) {
        value.onEvent(event);
    }

    public void onEnabled() {
        value.onEnabled();
    }

    public void onDisabled() {
        value.onDisabled();
    }

    public C get() {
        return value;
    }

    public void select(int index) {
        select(choices.get(index));
    }

    public void select(C choice) {
        value.onDisabled();
        value = choice;
        value.onEnabled();
    }

    @Override
    public SettingRenderer<?> wrap() {
        return new ChoiceRenderer<>(this);
    }

    private static final String ACTIVE = "_active";
    static final String EXPANDED = "_expanded";

    @Override
    public void save(JsonObject json) {
        JsonObject j = new JsonObject();
        json.add(name, j);
        j.addProperty(ACTIVE, value.name);
        j.addProperty(EXPANDED, expanded);
        for (C choice : choices) {
            choice.save(j);
        }
    }

    @Override
    public void load(JsonObject json) {
        if (json.has(name) && json.get(name).isJsonObject()) {
            JsonObject j = json.getAsJsonObject(name);
            if (j.has(EXPANDED) && j.get(EXPANDED).isJsonPrimitive()) expanded(j.get(EXPANDED).getAsBoolean());

            String active = j.get(ACTIVE).getAsString();
            value = choices.stream().filter(choice -> choice.name.equalsIgnoreCase(active)).findAny().orElse(choices.getFirst());
            for (C choice : choices) {
                choice.load(j);
            }
        }
    }
}
