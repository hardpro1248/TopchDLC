package gg.topchdlc.vse.utils.player;

import gg.topchdlc.MinecraftHolder;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class ContainerUtility implements MinecraftHolder {

    private static final int PLAYER_MAIN_SLOTS = 36;

    public static void dropInventory(ScreenHandler handler) {
        if (mc.player == null || mc.interactionManager == null) return;

        for (Slot slot : handler.slots) {
            if (!isPlayerSlot(slot) || !slot.hasStack()) continue;
            mc.interactionManager.clickSlot(handler.syncId, slot.id, 1, SlotActionType.THROW, mc.player);
        }
    }

    public static void depositAll(ScreenHandler handler) {
        quickMove(handler, true);
    }

    public static void takeAll(ScreenHandler handler) {
        quickMove(handler, false);
    }

    private static void quickMove(ScreenHandler handler, boolean fromPlayer) {
        if (mc.player == null || mc.interactionManager == null) return;

        for (Slot slot : handler.slots) {
            boolean playerSlot = slot.inventory instanceof PlayerInventory;
            if (playerSlot != fromPlayer || !slot.hasStack()) continue;
            if (playerSlot && !isPlayerSlot(slot)) continue;
            mc.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
        }
    }

    private static boolean isPlayerSlot(Slot slot) {
        return slot.inventory instanceof PlayerInventory && slot.getIndex() < PLAYER_MAIN_SLOTS;
    }
}
