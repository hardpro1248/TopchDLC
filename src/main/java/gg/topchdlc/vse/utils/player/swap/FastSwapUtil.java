package gg.topchdlc.vse.utils.player.swap;

import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import lombok.experimental.UtilityClass;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

@UtilityClass
public class FastSwapUtil implements MinecraftHolder {
    private static volatile boolean busy = false;

    public boolean isBusy() {
        return busy;
    }

    public void reset() {
        busy = false;
    }

    public void silentUseMatrix(Item item) {
        int slot = findItemSlot(item);
        if (slot != -1) {
            silentUseMatrix(slot);
        }
    }

    public void silentUseMatrix(int slot) {
        useItemPacketSilent(slot);
    }

    public void useItemPacketSilent(int slot) {
        if (slot == -1 || mc.player == null || mc.interactionManager == null) return;
        if (busy) return;

        busy = true;

        try {
            int currentSlot = mc.player.getInventory().getSelectedSlot();
            if (slot >= 0 && slot < 9) {
                if (slot == currentSlot) {
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                } else {
                    NetworkUtility.send(new UpdateSelectedSlotC2SPacket(slot));
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    resetClientHandSwing();
                    NetworkUtility.send(new UpdateSelectedSlotC2SPacket(currentSlot));
                }
                return;
            }

            if (slot >= 9 && slot < 36) {
                int syncId = mc.player.currentScreenHandler.syncId;
                boolean wasSprinting = mc.player.isSprinting();
                ItemStack offhandStack = mc.player.getOffHandStack();
                boolean hasTotemInOffhand = offhandStack.getItem() == Items.TOTEM_OF_UNDYING;
                NetworkUtility.sendInputPacket(false, false, false, false, false, false, false);
                if (wasSprinting) {
                    mc.player.setSprinting(false);
                }
                int targetButton = hasTotemInOffhand ? currentSlot : 40;
                Hand targetHand = hasTotemInOffhand ? Hand.MAIN_HAND : Hand.OFF_HAND;
                mc.interactionManager.clickSlot(syncId, slot, targetButton, SlotActionType.SWAP, mc.player);
                mc.interactionManager.interactItem(mc.player, targetHand);
                resetClientHandSwing();
                mc.interactionManager.clickSlot(syncId, slot, targetButton, SlotActionType.SWAP, mc.player);
                NetworkUtility.sendSilentPacket(new CloseHandledScreenC2SPacket(syncId));
                if (wasSprinting) {
                    mc.player.setSprinting(true);
                }
            }
        } finally {
            busy = false;
        }
    }



    public void swapElytra(int slot, int armorSlot) {
        if (slot == -1 || armorSlot == -1 || mc.player == null || mc.interactionManager == null) return;
        if (busy) return;

        busy = true;

        boolean wasForward = InputUtil.isKeyPressed(window, mc.options.forwardKey.getDefaultKey().getCode());
        boolean wasBack    = InputUtil.isKeyPressed(window, mc.options.backKey.getDefaultKey().getCode());
        boolean wasLeft    = InputUtil.isKeyPressed(window, mc.options.leftKey.getDefaultKey().getCode());
        boolean wasRight   = InputUtil.isKeyPressed(window, mc.options.rightKey.getDefaultKey().getCode());
        boolean wasJump    = InputUtil.isKeyPressed(window, mc.options.jumpKey.getDefaultKey().getCode());
        boolean wasSneak   = InputUtil.isKeyPressed(window, mc.options.sneakKey.getDefaultKey().getCode());

        Client.SCHEDULER.scheduleOnce(() -> {
            if (mc.player == null) {
                busy = false;
                return;
            }

            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
            mc.options.sneakKey.setPressed(false);

            Client.SCHEDULER.scheduleOnce(() -> {
                if (mc.player == null) {
                    busy = false;
                    return;
                }

                int syncId = mc.player.currentScreenHandler.syncId;

                int containerSlot = (slot < 9) ? slot + 36 : slot;

                if (containerSlot >= 36 && containerSlot <= 44) {
                    int button = containerSlot - 36;
                    mc.interactionManager.clickSlot(syncId, armorSlot, button, SlotActionType.SWAP, mc.player);
                } else {
                    mc.interactionManager.clickSlot(syncId, containerSlot, 0, SlotActionType.SWAP, mc.player);
                    mc.interactionManager.clickSlot(syncId, armorSlot, 0, SlotActionType.SWAP, mc.player);
                    mc.interactionManager.clickSlot(syncId, containerSlot, 0, SlotActionType.SWAP, mc.player);
                }

                resetClientHandSwing();

                Client.SCHEDULER.scheduleOnce(() -> {
                    if (mc.player != null) {
                        mc.options.forwardKey.setPressed(wasForward && InputUtil.isKeyPressed(window, mc.options.forwardKey.getDefaultKey().getCode()));
                        mc.options.backKey.setPressed(wasBack && InputUtil.isKeyPressed(window, mc.options.backKey.getDefaultKey().getCode()));
                        mc.options.leftKey.setPressed(wasLeft && InputUtil.isKeyPressed(window, mc.options.leftKey.getDefaultKey().getCode()));
                        mc.options.rightKey.setPressed(wasRight && InputUtil.isKeyPressed(window, mc.options.rightKey.getDefaultKey().getCode()));
                        mc.options.jumpKey.setPressed(wasJump && InputUtil.isKeyPressed(window, mc.options.jumpKey.getDefaultKey().getCode()));
                        mc.options.sneakKey.setPressed(wasSneak && InputUtil.isKeyPressed(window, mc.options.sneakKey.getDefaultKey().getCode()));
                    }
                    busy = false;
                }, 1);
            }, 1);
        }, 1);
    }

    public int findItemSlot(Item item) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private void resetClientHandSwing() {
        if (mc.player != null) {
            mc.player.handSwinging = false;
            mc.player.handSwingTicks = 0;
            mc.player.handSwingProgress = 0.0f;
        }
    }
}