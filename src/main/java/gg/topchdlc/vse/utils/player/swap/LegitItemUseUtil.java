package gg.topchdlc.vse.utils.player.swap;

import net.minecraft.client.MinecraftClient;

import static gg.topchdlc.MinecraftHolder.window;
import static net.minecraft.client.util.InputUtil.isKeyPressed;

public class LegitItemUseUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public enum UsePhase {
        READY, SLOWING_DOWN, WAITING_STOP, USE_ITEM, SPEEDING_UP, FINISHED
    }

    private UsePhase usePhase = UsePhase.READY;
    private long actionStartTime = 0;
    private int pendingSlot = -1;
    private boolean playerFullyStopped = false;
    private boolean wasForward, wasBack, wasLeft, wasRight, wasJump;
    private boolean keysOverridden = false;

    public boolean isBusy() {
        return usePhase != UsePhase.READY;
    }

    public UsePhase getUsePhase() {
        return usePhase;
    }

    public void startLegitUse(int slot) {
        if (mc.player == null || mc.getWindow() == null) return;

        this.pendingSlot = slot;

        this.wasForward = isKeyPressed(window, mc.options.forwardKey.getDefaultKey().getCode());
        this.wasBack = isKeyPressed(window, mc.options.backKey.getDefaultKey().getCode());
        this.wasLeft = isKeyPressed(window, mc.options.leftKey.getDefaultKey().getCode());
        this.wasRight = isKeyPressed(window, mc.options.rightKey.getDefaultKey().getCode());
        this.wasJump = isKeyPressed(window, mc.options.jumpKey.getDefaultKey().getCode());

        this.usePhase = UsePhase.SLOWING_DOWN;
        this.actionStartTime = System.currentTimeMillis();
        this.playerFullyStopped = false;
        this.keysOverridden = false;
    }

    public void onTick() {
        if (usePhase == UsePhase.READY) return;

        if (mc.player == null || mc.currentScreen != null) {
            reset();
            return;
        }

        long elapsed = System.currentTimeMillis() - actionStartTime;

        switch (usePhase) {
            case SLOWING_DOWN -> {
                if (mc.player.isSprinting()) mc.player.setSprinting(false);
                if (!keysOverridden) {
                    mc.options.forwardKey.setPressed(false);
                    mc.options.backKey.setPressed(false);
                    mc.options.leftKey.setPressed(false);
                    mc.options.rightKey.setPressed(false);
                    mc.options.jumpKey.setPressed(false);
                    keysOverridden = true;
                }
                if (elapsed > 1) usePhase = UsePhase.WAITING_STOP;
            }
            case WAITING_STOP -> {
                if (mc.player.getVelocity().horizontalLength() < 0.001 || elapsed > 15) {
                    playerFullyStopped = true;
                    usePhase = UsePhase.USE_ITEM;
                }
            }
            case USE_ITEM -> {
                if (playerFullyStopped && pendingSlot != -1) {
                    useItemWithReturn(pendingSlot);
                    usePhase = UsePhase.SPEEDING_UP;
                    actionStartTime = System.currentTimeMillis();
                }
            }
            case SPEEDING_UP -> {
                long se = System.currentTimeMillis() - actionStartTime;
                if (se > 100 && keysOverridden) restoreKeyStates();
                if (se > 180) usePhase = UsePhase.FINISHED;
            }
            case FINISHED -> reset();
        }
    }

    public void restoreKeyStates() {
        if (!keysOverridden || mc.getWindow() == null) return;

        mc.options.forwardKey.setPressed(wasForward && isKeyPressed(window, mc.options.forwardKey.getDefaultKey().getCode()));
        mc.options.backKey.setPressed(wasBack && isKeyPressed(window, mc.options.backKey.getDefaultKey().getCode()));
        mc.options.leftKey.setPressed(wasLeft && isKeyPressed(window, mc.options.leftKey.getDefaultKey().getCode()));
        mc.options.rightKey.setPressed(wasRight && isKeyPressed(window, mc.options.rightKey.getDefaultKey().getCode()));
        mc.options.jumpKey.setPressed(wasJump && isKeyPressed(window, mc.options.jumpKey.getDefaultKey().getCode()));

        keysOverridden = false;
    }

    public void reset() {
        restoreKeyStates();
        usePhase = UsePhase.READY;
        pendingSlot = -1;
        playerFullyStopped = false;
    }

    private void useItemWithReturn(int slot) {
        if (mc.player == null) return;
        int currentSlot = mc.player.getInventory().getSelectedSlot();
        if (slot < 9) {
            SwapUtility.legitUseItemFromHotbar(slot, currentSlot);
        } else {
            SwapUtility.legitUseItemFromInventory(slot, currentSlot);
        }
    }
}