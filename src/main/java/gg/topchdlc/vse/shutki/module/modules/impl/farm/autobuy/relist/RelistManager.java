package gg.topchdlc.vse.shutki.module.modules.impl.farm.autobuy.relist;

import gg.topchdlc.vse.utils.network.NetworkUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.slot.SlotActionType;
import gg.topchdlc.vse.utils.math.TimeUtility;

import static gg.topchdlc.MinecraftHolder.mc;

/**
 * Create by daun kvass
 */
public class RelistManager {
    private static final long RELIST_INTERVAL = 60_000L;

    private final TimeUtility intervalTimer = new TimeUtility();
    private final TimeUtility stepTimer = new TimeUtility();
    private RelistState state = RelistState.IDLE;
    private Runnable onDone;
    public void configure(Runnable onDone) {
        this.onDone = onDone;
    }
    public boolean isActive() { return state != RelistState.IDLE; }
    public boolean tryStart() {
        if (!intervalTimer.reached(RELIST_INTERVAL, true)) return false;
        if (!(mc.currentScreen instanceof GenericContainerScreen s)) return false;
        String t = s.getTitle().getString().toLowerCase();
        if (!t.contains("аукцион") && !t.contains("auction")) return false;
        state = RelistState.CLICK_SLOT_46;
        stepTimer.reset();
        return true;
    }

    public void reset() {
        state = RelistState.IDLE;
        intervalTimer.reset();
        stepTimer.reset();
    }
    public void onTick() {
        if (state == RelistState.IDLE || mc.player == null) return;
        tick();
    }

    private void tick() {
        switch (state) {
            case CLICK_SLOT_46 -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    if (screen.getScreenHandler().slots.size() > 46) {
                        mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, 46, 0, SlotActionType.PICKUP, mc.player);
                    }
                    state = RelistState.WAIT_MENU_OPEN;
                    stepTimer.reset();
                } else {
                    state = RelistState.IDLE;
                }
            }
            case WAIT_MENU_OPEN -> {
                if (stepTimer.reached(800, false)) {
                    state = RelistState.CLICK_SLOT_52;
                    stepTimer.reset();
                }
            }
            case CLICK_SLOT_52 -> {
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    if (screen.getScreenHandler().slots.size() > 52) {
                        mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, 52, 0, SlotActionType.PICKUP, mc.player);
                    }
                }
                state = RelistState.CLOSE_MENU;
                stepTimer.reset();
            }
            case CLOSE_MENU -> {
                if (stepTimer.reached(400, false)) {
                    if (mc.currentScreen != null) mc.currentScreen.close();
                    state = RelistState.WAIT_MENU_CLOSE;
                    stepTimer.reset();
                }
            }
            case WAIT_MENU_CLOSE -> {
                if (mc.currentScreen == null || stepTimer.reached(1500, false)) {
                    state = RelistState.REOPEN_AH;
                    stepTimer.reset();
                }
            }
            case REOPEN_AH -> {
                if (stepTimer.reached(300, false)) {
                    NetworkUtility.sendCommand("ah");
                    state = RelistState.WAIT_AH_REOPEN;
                    stepTimer.reset();
                }
            }
            case WAIT_AH_REOPEN -> {
                if (isAhOpen()) {
                    state = RelistState.IDLE;
                    if (onDone != null) onDone.run();
                    break;
                }
                if (stepTimer.reached(5000, false)) {
                    NetworkUtility.sendCommand("ah");
                    stepTimer.reset();
                }
            }
        }
    }

    private boolean isAhOpen() {
        if (!(mc.currentScreen instanceof GenericContainerScreen s)) return false;
        String t = s.getTitle().getString().toLowerCase();
        return t.contains("аукцион") || t.contains("auction");
    }
}
