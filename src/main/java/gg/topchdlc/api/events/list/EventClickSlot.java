package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;
import lombok.ToString;
import net.minecraft.screen.slot.SlotActionType;

@ToString
public class EventClickSlot extends Event {
    private static final EventClickSlot instance = new EventClickSlot();
    public int syncId, slotId, button;
    public SlotActionType actionType;
    public static boolean postedFromScreen = false;

    public static EventClickSlot build(int syncId, int slotId, int button, SlotActionType actionType) {
        instance.syncId = syncId;
        instance.slotId = slotId;
        instance.button = button;
        instance.actionType = actionType;
        instance.reset();
        return instance;
    }
}
