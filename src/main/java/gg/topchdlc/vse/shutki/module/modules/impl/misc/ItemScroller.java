package gg.topchdlc.vse.shutki.module.modules.impl.misc;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventMouseDrag;
import gg.topchdlc.mixin.accessor.IHandledScreen;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ItemScroller extends Module {
    public static final ItemScroller INSTANCE = new ItemScroller();

    private final CheckBox shiftDrag = checkbox("Shift+LMB Drag", true);
    private final CheckBox dropSameType = checkbox("Ctrl+Shift+Q Drop", true);

    private final Set<Integer> draggedSlots = new HashSet<>();
    private int tickCooldown = 0;

    private ItemScroller() {
        super("ItemScroller", Category.Misc, "помогает быстро перемещать предметы");
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        draggedSlots.clear();
        tickCooldown = 0;
    }

    EventBus<Event> events = event -> {
        if (mc.player == null || mc.world == null) return;

        if (event instanceof EventGameTick) {
            if (tickCooldown > 0) tickCooldown--;
            return;
        }

        if (!(mc.currentScreen instanceof HandledScreen<?> screen)) return;
        if (event instanceof EventMouseDrag drag && shiftDrag.get()) {
            if (drag.button != 0 || !isShiftReallyDown()) {
                draggedSlots.clear();
                return;
            }
            Slot slot = getSlotAt(screen, drag.mouseX, drag.mouseY);
            if (slot == null || !slot.hasStack()) return;
            if (draggedSlots.contains(slot.id)) return;
            if (tickCooldown > 0) return;
            draggedSlots.add(slot.id);
            tickCooldown = 0;
            mc.interactionManager.clickSlot(
                    screen.getScreenHandler().syncId,
                    slot.id,
                    0,
                    SlotActionType.QUICK_MOVE,
                    mc.player
            );
            drag.cancel();
            return;
        }
        if (!(event instanceof EventMouseDrag)) {
            draggedSlots.clear();
        }
    };
    private boolean isShiftReallyDown() {
        long handle = mc.getWindow().getHandle();
        return InputUtil.isKeyPressed(window, 340) || InputUtil.isKeyPressed(window, 344);
    }

    private boolean isCtrlReallyDown() {
        long handle = mc.getWindow().getHandle();
        return InputUtil.isKeyPressed(window, 341) || InputUtil.isKeyPressed(window, 345);
    }

    public boolean onInventoryKeyPressed(HandledScreen<?> screen, int keyCode) {
        if (!isEnabled() || !dropSameType.get()) return false;
        if (keyCode != 81) return false;
        if (!isCtrlReallyDown() || !isShiftReallyDown()) return false;
        if (mc.player == null) return false;

        Slot focused = ((IHandledScreen) screen).getFocusedSlot();
        if (focused == null || !focused.hasStack()) return false;

        Item targetItem = focused.getStack().getItem();
        List<Integer> slotsToDrop = new ArrayList<>();
        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot.hasStack() && slot.getStack().getItem() == targetItem) {
                slotsToDrop.add(slot.id);
            }
        }

        for (int slotId : slotsToDrop) {
            mc.interactionManager.clickSlot(
                    screen.getScreenHandler().syncId,
                    slotId,
                    1,
                    SlotActionType.THROW,
                    mc.player
            );
        }
        return true;
    }

    private Slot getSlotAt(HandledScreen<?> screen, double mouseX, double mouseY) {
        Slot focused = ((IHandledScreen) screen).getFocusedSlot();
        if (focused != null) return focused;
        try {
            var xf = HandledScreen.class.getDeclaredField("x");
            var yf = HandledScreen.class.getDeclaredField("y");
            xf.setAccessible(true);
            yf.setAccessible(true);
            int ox = (int) xf.get(screen);
            int oy = (int) yf.get(screen);
            for (Slot slot : screen.getScreenHandler().slots) {
                int sx = ox + slot.x;
                int sy = oy + slot.y;
                if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                    return slot;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
