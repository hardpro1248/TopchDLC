package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;
import net.minecraft.text.Text;

public class EventAddMessage extends Event {
    private static final EventAddMessage instance = new EventAddMessage();

    public Text text;
    public Text replacement;

    public static EventAddMessage build(Text text) {
        instance.text = text;
        instance.replacement = null;
        instance.reset();
        return instance;
    }
}
