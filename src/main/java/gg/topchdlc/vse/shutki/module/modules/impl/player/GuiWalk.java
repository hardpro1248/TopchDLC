package gg.topchdlc.vse.shutki.module.modules.impl.player;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventClickSlot;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventInput;
import gg.topchdlc.mixin.accessor.IKeyBinding;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.impl.helper.AutoSwap;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.slot.SlotActionType;
import gg.topchdlc.vse.utils.player.swap.InventoryUtility;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class GuiWalk extends Module {
    public static final GuiWalk INSTANCE = new GuiWalk();

    private GuiWalk() {
        super("Gui walk", Category.PLAYER, "Позволяет ходить с открытым инвентарем");
    }

    public enum Mode { Normal, SpookyTime }

    public EnumSetting<Mode> mode = enumSetting("Mode", Mode.Normal);
    public CheckBox disableSneak = checkbox("Disable Sneak", true);

    private record SlotClick(int syncId, int slotId, int button, SlotActionType actionType) {}
    private final Deque<SlotClick> clickQueue = new ArrayDeque<>();
    private boolean wasInInventory = false;
    private enum FlushPhase { READY, SLOWING_DOWN, WAITING_STOP, SENDING, SPEEDING_UP, FINISHED }
    private FlushPhase flushPhase = FlushPhase.READY;
    private long actionStart = 0;
    private List<SlotClick> pendingClicks = new ArrayList<>();
    private int sendIndex = 0;
    private long lastSendTime = 0;
    private boolean keysOverridden = false;

    public boolean handle(KeyBinding key) {
        if (mc.currentScreen == null) return false;
        if (!isEnabled() || mc.currentScreen instanceof ChatScreen) return false;
        if (disableSneak.get() && key == mc.options.sneakKey) return false;
        return InputUtil.isKeyPressed(window,
                ((IKeyBinding) key).client$boundKey().getCode());
    }

    EventBus<Event> events = event -> {
        if (mc.player == null) return;

        if (event instanceof EventClickSlot e && mode.is(Mode.SpookyTime)) {
            if (AutoSwap.INSTANCE.getPendingPickSlot() >= 0) {
                return;
            }
            if(mc.currentScreen instanceof HandledScreen<?> screen) {
                String title = screen.getTitle().getString();
                if (title.contains("Выберите режим")) {
                    return;
                }
            }

            if (mc.currentScreen instanceof HandledScreen) {
                if (isMoving() || flushPhase != FlushPhase.READY) {
                    clickQueue.add(new SlotClick(e.syncId, e.slotId, e.button, e.actionType));
                    InventoryUtility.skipClient = false;
                    mc.player.currentScreenHandler.onSlotClick(e.slotId, e.button, e.actionType, mc.player);
                    e.cancel();
                    return;
                }
            }
        }

        if (event instanceof EventGameTick && mode.is(Mode.SpookyTime)) {
            boolean inInventory = mc.currentScreen instanceof HandledScreen;

            if (wasInInventory && !inInventory && !clickQueue.isEmpty() && flushPhase == FlushPhase.READY) {
                beginFlush();
            }
            wasInInventory = inInventory;
            if (inInventory && !clickQueue.isEmpty() && flushPhase == FlushPhase.READY && !isMoving()) {
                beginFlush();
            }

            if (flushPhase != FlushPhase.READY) {
                processFlush();
            }
        }

        if (event instanceof EventInput e) {
            if (keysOverridden) {
                e.setForward(0);
                e.setStrafe(0);
                e.setJump(false);
                e.setSneak(false);
                e.setSprint(false);
            } else if (mc.currentScreen != null
                    && !(mc.currentScreen instanceof ChatScreen)
                    && !(mc.currentScreen instanceof SignEditScreen)) {
                updateMovementKeysInGui();
            }
        }
    };

    private boolean isMoving() {
        if (mc.player == null) return false;
        return InputUtil.isKeyPressed(window, mc.options.forwardKey.getDefaultKey().getCode()) ||
                InputUtil.isKeyPressed(window, mc.options.backKey.getDefaultKey().getCode()) ||
                InputUtil.isKeyPressed(window, mc.options.leftKey.getDefaultKey().getCode()) ||
                InputUtil.isKeyPressed(window, mc.options.rightKey.getDefaultKey().getCode());
    }

    private void beginFlush() {
        if (mc.player == null || flushPhase != FlushPhase.READY || clickQueue.isEmpty()) return;
        pendingClicks = new ArrayList<>(clickQueue);
        clickQueue.clear();
        sendIndex = 0;
        flushPhase = FlushPhase.SLOWING_DOWN;
        actionStart = System.currentTimeMillis();
        keysOverridden = false;
    }

    private void processFlush() {
        if (mc.player == null) { resetFlush(); return; }
        long elapsed = System.currentTimeMillis() - actionStart;

        switch (flushPhase) {
            case SLOWING_DOWN -> {
                if (mc.player.isSprinting()) mc.player.setSprinting(false);
                if (!keysOverridden) keysOverridden = true;
                if (elapsed > 0) flushPhase = FlushPhase.WAITING_STOP;
            }
            case WAITING_STOP -> {
                double vx = Math.abs(mc.player.getVelocity().x);
                double vz = Math.abs(mc.player.getVelocity().z);
                if ((vx < 0.001 && vz < 0.001) || elapsed > 100) {
                    flushPhase = FlushPhase.SENDING;
                    actionStart = System.currentTimeMillis();
                    lastSendTime = 0;
                }
            }
            case SENDING -> {
                long now = System.currentTimeMillis();
                if (sendIndex < pendingClicks.size() && (now - lastSendTime) >= 25) {
                    SlotClick c = pendingClicks.get(sendIndex);
                    mc.interactionManager.clickSlot(c.syncId(), c.slotId(), c.button(), c.actionType(), mc.player);
                    sendIndex++;
                    lastSendTime = now;
                }
                if (sendIndex >= pendingClicks.size()) {
                    flushPhase = FlushPhase.SPEEDING_UP;
                    actionStart = System.currentTimeMillis();
                }
            }
            case SPEEDING_UP -> {
                long sp = System.currentTimeMillis() - actionStart;
                if (sp > 50 && keysOverridden) keysOverridden = false;
                if (sp > 100) flushPhase = FlushPhase.FINISHED;
            }
            case FINISHED -> resetFlush();
        }
    }

    private void resetFlush() {
        keysOverridden = false;
        flushPhase = FlushPhase.READY;
        pendingClicks.clear();
        sendIndex = 0;
    }

    private void updateMovementKeysInGui() {
        if (mc.player == null) return;
        mc.options.forwardKey.setPressed(InputUtil.isKeyPressed(window, mc.options.forwardKey.getDefaultKey().getCode()));
        mc.options.backKey.setPressed(InputUtil.isKeyPressed(window, mc.options.backKey.getDefaultKey().getCode()));
        mc.options.leftKey.setPressed(InputUtil.isKeyPressed(window, mc.options.leftKey.getDefaultKey().getCode()));
        mc.options.rightKey.setPressed(InputUtil.isKeyPressed(window, mc.options.rightKey.getDefaultKey().getCode()));
        mc.options.jumpKey.setPressed(InputUtil.isKeyPressed(window, mc.options.jumpKey.getDefaultKey().getCode()));
        mc.options.sneakKey.setPressed(disableSneak.get() ? false :
                InputUtil.isKeyPressed(window, mc.options.sneakKey.getDefaultKey().getCode()));
        mc.options.sprintKey.setPressed(InputUtil.isKeyPressed(window, mc.options.sprintKey.getDefaultKey().getCode()));
    }

    @Override
    protected void onDisable() {
        clickQueue.clear();
        resetFlush();
        wasInInventory = false;
        if (mc.player == null) return;
        mc.options.forwardKey.setPressed(InputUtil.isKeyPressed(window, mc.options.forwardKey.getDefaultKey().getCode()));
        mc.options.backKey.setPressed(InputUtil.isKeyPressed(window, mc.options.backKey.getDefaultKey().getCode()));
        mc.options.leftKey.setPressed(InputUtil.isKeyPressed(window, mc.options.leftKey.getDefaultKey().getCode()));
        mc.options.rightKey.setPressed(InputUtil.isKeyPressed(window, mc.options.rightKey.getDefaultKey().getCode()));
        mc.options.jumpKey.setPressed(InputUtil.isKeyPressed(window, mc.options.jumpKey.getDefaultKey().getCode()));
        mc.options.sprintKey.setPressed(InputUtil.isKeyPressed(window, mc.options.sprintKey.getDefaultKey().getCode()));
    }
}