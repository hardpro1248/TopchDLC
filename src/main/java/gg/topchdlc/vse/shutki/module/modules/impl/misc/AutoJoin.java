package gg.topchdlc.vse.shutki.module.modules.impl.misc;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.math.TimeUtility;
import gg.topchdlc.vse.utils.player.swap.InventoryUtility;

/**
 * Create by daun kvass
 */
public class AutoJoin extends Module {
    public static final AutoJoin INSTANCE = new AutoJoin();

    public enum Server { SpookyTime, ReallyWorld }

    private enum State {
        USE_COMPASS,
        WAIT_MENU,
        CLICK_ANCHOR,
        WAIT_RETRY,
        WAIT_JOINED,

        RW_WAIT_MAIN_MENU,
        RW_CLICK_CRAFTING,
        RW_WAIT_GRIF_MENU,
        RW_CLICK_GRIF,
        RW_WAIT_GRIF_PAGE,
        RW_GRIF_CLICKED,
        RW_FIND_HEAD,
        RW_CLICK_NEXT_PAGE,
        RW_WAIT_NEXT_PAGE,
    }

    private State state = State.USE_COMPASS;
    private final TimeUtility timer = new TimeUtility();
    private int rwPageAttempts = 0;

    private final EnumSetting<Server> server = enumSetting("Server", Server.SpookyTime);
    private final SliderSetting grifNumber = sliderSetting("Гриф №", 1, 1, 80).increment(1)
            .visible(() -> server.is(Server.ReallyWorld));

    private AutoJoin() {
        super("Auto Join", Category.Misc, "само заходит на дуэли или гриф");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        resetState();
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        resetState();
    }

    private void resetState() {
        state = State.USE_COMPASS;
        rwPageAttempts = 0;
        timer.reset();
    }


    private void retry() {
        resetState();
    }

    EventBus<Event> events = event -> {
        if (!(event instanceof EventGameTick)) return;
        if (nullCheck()) return;

        if (server.is(Server.SpookyTime)) {
            tickSpookyTime();
        } else {
            tickReallyWorld();
        }
    };

    private void tickSpookyTime() {
        switch (state) {
            case USE_COMPASS -> {
                int slot = InventoryUtility.findHotbar(Items.COMPASS);
                if (slot == -1) return;
                InventoryUtility.useItem(slot, false);
                state = State.WAIT_MENU;
                timer.reset();
            }
            case WAIT_MENU -> {
                if (!(mc.currentScreen instanceof HandledScreen<?> screen)) {
                    if (timer.reached(300L, false)) retry();
                    return;
                }
                String title = screen.getTitle().getString();
                if (!title.contains("Выберите режим")) {
                    toggle();
                    return;
                }
                state = State.CLICK_ANCHOR;
            }
            case CLICK_ANCHOR -> {
                if (!(mc.currentScreen instanceof HandledScreen<?> screen)) {
                    retry();
                    return;
                }
                String title = screen.getTitle().getString();
                if (!title.contains("Выберите режим")) {
                    toggle();
                    return;
                }
                var handler = screen.getScreenHandler();
                for (Slot slot : handler.slots) {
                    if (slot.getStack().getItem() == Items.NETHERITE_SWORD) {
                        mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                        state = State.WAIT_RETRY;
                        timer.reset();
                        return;
                    }
                }
                if (timer.reached(500L, false)) retry();
            }
            case WAIT_RETRY -> {
                if (mc.currentScreen instanceof HandledScreen<?> screen) {
                    String title = screen.getTitle().getString();
                    if (!title.contains("Выберите режим")) {
                        toggle();
                        return;
                    }
                }
                if (timer.reached(200L, false)) retry();
            }
        }
    }

    private void tickReallyWorld() {
        switch (state) {
            case USE_COMPASS -> {
                int slot = InventoryUtility.findHotbar(Items.COMPASS);
                if (slot == -1) return;
                InventoryUtility.useItem(slot, false);
                state = State.RW_WAIT_MAIN_MENU;
                timer.reset();
            }

            case RW_WAIT_MAIN_MENU -> {
                if (!(mc.currentScreen instanceof HandledScreen<?>)) {
                    if (timer.reached(300L, false)) retry();
                    return;
                }
                if (timer.reached(120L, false)) state = State.RW_CLICK_CRAFTING;
            }

            case RW_CLICK_CRAFTING -> {
                if (!(mc.currentScreen instanceof HandledScreen<?> screen)) {
                    if (timer.reached(300L, false)) retry();
                    return;
                }
                var handler = screen.getScreenHandler();
                for (Slot slot : handler.slots) {
                    if (slot.getStack().getItem() == Items.CRAFTING_TABLE) {
                        mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                        state = State.RW_WAIT_GRIF_MENU;
                        timer.reset();
                        return;
                    }
                }
                if (timer.reached(500L, false)) retry();
            }

            case RW_WAIT_GRIF_MENU -> {
                if (!(mc.currentScreen instanceof HandledScreen<?>)) {
                    if (timer.reached(300L, false)) retry();
                    return;
                }
                if (timer.reached(150L, false)) {
                    state = State.RW_CLICK_GRIF;
                }
            }

            case RW_CLICK_GRIF -> {
                if (!(mc.currentScreen instanceof HandledScreen<?> screen)) {
                    if (timer.reached(300L, false)) retry();
                    return;
                }
                String menuTitle = screen.getTitle().getString().toUpperCase();
                if (!menuTitle.contains("ГРИФ") && !menuTitle.contains("GRIEF")) {
                    rwPageAttempts = 0;
                    state = State.RW_FIND_HEAD;
                    timer.reset();
                    return;
                }
                int targetNum = (int) grifNumber.get();
                var handler = screen.getScreenHandler();
                for (Slot slot : handler.slots) {
                    if (containsGrifNumber(slot.getStack().getName().getString(), targetNum)) {
                        mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                        state = State.RW_GRIF_CLICKED;
                        timer.reset();
                        return;
                    }
                }
                if (handler.slots.size() > 44) {
                    mc.interactionManager.clickSlot(handler.syncId, 44, 0, SlotActionType.PICKUP, mc.player);
                    state = State.RW_WAIT_GRIF_PAGE;
                    timer.reset();
                } else {
                    retry();
                }
            }

            case RW_GRIF_CLICKED -> {
                if (!(mc.currentScreen instanceof HandledScreen<?> screen)) {
                    if (timer.reached(300L, false)) retry();
                    return;
                }
                String title = screen.getTitle().getString().toUpperCase();
                if (!title.contains("ГРИФ") && !title.contains("GRIEF")) {
                    rwPageAttempts = 0;
                    state = State.RW_FIND_HEAD;
                    timer.reset();
                    return;
                }
                if (timer.reached(200L, false)) {
                    int targetNum = (int) grifNumber.get();
                    var handler = screen.getScreenHandler();
                    for (Slot slot : handler.slots) {
                        if (containsGrifNumber(slot.getStack().getName().getString(), targetNum)) {
                            mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                            timer.reset();
                            return;
                        }
                    }
                    retry();
                }
            }

            case RW_WAIT_GRIF_PAGE -> {
                if (!(mc.currentScreen instanceof HandledScreen<?>)) {
                    if (timer.reached(300L, false)) retry();
                    return;
                }
                if (timer.reached(800L, false)) state = State.RW_CLICK_GRIF;
            }

            case RW_FIND_HEAD -> {
                if (!(mc.currentScreen instanceof HandledScreen<?> screen)) {
                    if (timer.reached(300L, false)) retry();
                    return;
                }
                int targetNum = (int) grifNumber.get();
                var handler = screen.getScreenHandler();
                for (Slot slot : handler.slots) {
                    if (containsNumber(slot.getStack().getName().getString(), targetNum)) {
                        mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                        toggle();
                        return;
                    }
                }
                if (rwPageAttempts < 2 && handler.slots.size() > 44) {
                    state = State.RW_CLICK_NEXT_PAGE;
                } else {
                    rwPageAttempts = 0;
                    retry();
                }
            }

            case RW_CLICK_NEXT_PAGE -> {
                if (!(mc.currentScreen instanceof HandledScreen<?> screen)) {
                    if (timer.reached(300L, false)) retry();
                    return;
                }
                var handler = screen.getScreenHandler();
                if (handler.slots.size() > 44) {
                    mc.interactionManager.clickSlot(handler.syncId, 44, 0, SlotActionType.PICKUP, mc.player);
                    rwPageAttempts++;
                    state = State.RW_WAIT_NEXT_PAGE;
                    timer.reset();
                } else {
                    retry();
                }
            }

            case RW_WAIT_NEXT_PAGE -> {
                if (!(mc.currentScreen instanceof HandledScreen<?>)) {
                    if (timer.reached(300L, false)) retry();
                    return;
                }
                if (timer.reached(150L, false)) state = State.RW_FIND_HEAD;
            }
        }
    }

    private boolean containsGrifNumber(String name, int targetNum) {
        String upper = name.toUpperCase();
        if (!upper.contains("ГРИФ") && !upper.contains("GRIF")) return false;
        return containsNumber(name, targetNum);
    }

    private boolean containsNumber(String name, int targetNum) {
        String numStr = String.valueOf(targetNum);
        if (name.contains("#" + numStr) || name.contains("№" + numStr)) return true;
        return name.matches(".*(?<![0-9])" + numStr + "(?![0-9]).*");
    }
}
