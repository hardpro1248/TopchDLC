package gg.topchdlc.api.scripts.api.bindings.event;

import gg.topchdlc.api.scripts.Script;
import org.graalvm.polyglot.Value;

public class EventProvider {
    private final Script script;
    public EventProvider(Script script) {
        this.script = script;
    }

    public EventProvider set(Value provider) {
        script.setEventProvider(provider.as(IEventProvider.class));
        return this;
    }
}
