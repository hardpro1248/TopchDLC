package gg.topchdlc.vse.utils.player.swap;

import lombok.experimental.UtilityClass;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.shutki.module.modules.impl.player.Sprint;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Create by daun kvass
 */
@UtilityClass
public class SwapUtility implements MinecraftHolder {
    public static int searchItem(Item item, int start, int end) {
        for (int i = start; i < end; i++) {
            if (mc.player.getInventory().getStack(i).getItem().equals(item)) {
                return i;
            }
        }
        return -1;
    }
    private static void sendOffhandSwapPacket() {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ORIGIN,
                Direction.DOWN
        ));
    }

    private static boolean isHotbarSlot(int slot) {
        return (slot >= 36 && slot <= 44) || (slot >= 0 && slot <= 8);
    }

    private static int convertToHotbarIndex(int slot) {
        if (slot >= 36 && slot <= 44) return slot - 36;
        return slot;
    }

    public static int serchSlot(int slot, int start, int end) {
        for (int i = start; i < end; i++) {
            if (mc.player.getInventory().getStack(i).getItem().equals(slot)) {
                return i;
            }
        }
        return -1;
    }

    public static void vanilaswap(int slot) {
        if (mc.player == null || mc.getNetworkHandler() == null || mc.interactionManager == null) return;
        int syncId = mc.player.currentScreenHandler.syncId;
        mc.interactionManager.clickSlot(syncId, slot, 40, SlotActionType.SWAP, mc.player);

    }

    public static void grimSwapArmorInv(int inventorySlot, int armorSlot) {
        if (mc.player == null || mc.getNetworkHandler() == null || mc.interactionManager == null) return;

        int syncId = mc.player.currentScreenHandler.syncId;

        boolean wasSprinting = NetworkUtility.serverSprinting();
        NetworkUtility.sendInputPacket(false, false, false, false, false, false, false);
        if (wasSprinting) {
            NetworkUtility.send(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }
        mc.interactionManager.clickSlot(syncId, inventorySlot, 0, SlotActionType.SWAP, mc.player);
        mc.interactionManager.clickSlot(syncId, armorSlot, 0, SlotActionType.SWAP, mc.player);
        mc.interactionManager.clickSlot(syncId, inventorySlot, 0, SlotActionType.SWAP, mc.player);
        NetworkUtility.sendSilentPacket(new CloseHandledScreenC2SPacket(0));
        NetworkUtility.send(new PlayerInputC2SPacket(mc.player.input.playerInput));
        if (wasSprinting) {
            NetworkUtility.send(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        }

    }

    public static void grimSwapArmor(int slot, int armor) {
        if (mc.player == null || mc.getNetworkHandler() == null || mc.interactionManager == null) return;
        int islot = serchSlot(slot, 9, 45);
        int slotHotbar = serchSlot(slot, 0, 8);
        int syncId = mc.player.currentScreenHandler.syncId;
        if (islot != -1) {
            boolean wasSprinting = NetworkUtility.serverSprinting();
            NetworkUtility.sendInputPacket(false, false, false, false, false, false, false);
            if (wasSprinting) {
                NetworkUtility.send(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            }
            mc.interactionManager.clickSlot(syncId, slot, 0, SlotActionType.SWAP, mc.player);
            mc.interactionManager.clickSlot(syncId, armor, 0, SlotActionType.SWAP, mc.player);
            mc.interactionManager.clickSlot(syncId, slot, 0, SlotActionType.SWAP, mc.player);
            NetworkUtility.sendSilentPacket(new CloseHandledScreenC2SPacket(0));
            // NetworkUtility.send(new PlayerInputC2SPacket(mc.player.input.playerInput));
            if (wasSprinting) {
                NetworkUtility.send(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            }

        } else {
            boolean wasSprinting = NetworkUtility.serverSprinting();
            NetworkUtility.sendInputPacket(false, false, false, false, false, false, false);
            if (wasSprinting) {
                NetworkUtility.send(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            }
            mc.interactionManager.clickSlot(0, 6, slot, SlotActionType.SWAP, mc.player);
            NetworkUtility.sendSilentPacket(new CloseHandledScreenC2SPacket(0));
            NetworkUtility.send(new PlayerInputC2SPacket(mc.player.input.playerInput));
            if (wasSprinting) {
                NetworkUtility.send(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            }
        }
    }

    public void safeSwapToOffhand(int slot) {
        if (slot == -1 || mc.player == null || mc.interactionManager == null) return;

        if (mc.player.isTouchingWater() || mc.player.isSubmergedInWater() || mc.player.isGliding()) {
            boolean wasForwardW = InputUtil.isKeyPressed(window, mc.options.forwardKey.getDefaultKey().getCode());
            boolean wasBackW = InputUtil.isKeyPressed(window, mc.options.backKey.getDefaultKey().getCode());
            boolean wasLeftW = InputUtil.isKeyPressed(window, mc.options.leftKey.getDefaultKey().getCode());
            boolean wasRightW = InputUtil.isKeyPressed(window, mc.options.rightKey.getDefaultKey().getCode());
            Client.SCHEDULER.scheduleOnce(() -> {
                if (mc.player == null) return;
                mc.options.forwardKey.setPressed(false);
                mc.options.backKey.setPressed(false);
                mc.options.leftKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
                mc.player.setSprinting(false);
                Client.SCHEDULER.scheduleOnce(() -> {
                    if (mc.player == null) return;
                    int syncId = mc.player.currentScreenHandler.syncId;
                    mc.interactionManager.clickSlot(syncId, slot, 40, SlotActionType.SWAP, mc.player);
                    Client.SCHEDULER.scheduleOnce(() -> {
                        if (mc.player == null) return;
                        long h = mc.getWindow().getHandle();
                        mc.options.forwardKey.setPressed(wasForwardW && InputUtil.isKeyPressed(window, mc.options.forwardKey.getDefaultKey().getCode()));
                        mc.options.backKey.setPressed(wasBackW && InputUtil.isKeyPressed(window, mc.options.backKey.getDefaultKey().getCode()));
                        mc.options.leftKey.setPressed(wasLeftW && InputUtil.isKeyPressed(window, mc.options.leftKey.getDefaultKey().getCode()));
                        mc.options.rightKey.setPressed(wasRightW && InputUtil.isKeyPressed(window, mc.options.rightKey.getDefaultKey().getCode()));
                    }, 1);
                }, 1);
            }, 1);
            return;
        }
        boolean wasForward = InputUtil.isKeyPressed(window, mc.options.forwardKey.getDefaultKey().getCode());
        boolean wasBack = InputUtil.isKeyPressed(window, mc.options.backKey.getDefaultKey().getCode());
        boolean wasLeft = InputUtil.isKeyPressed(window, mc.options.leftKey.getDefaultKey().getCode());
        boolean wasRight = InputUtil.isKeyPressed(window, mc.options.rightKey.getDefaultKey().getCode());
        boolean wasJump = InputUtil.isKeyPressed(window, mc.options.jumpKey.getDefaultKey().getCode());
        boolean wasSneak = InputUtil.isKeyPressed(window, mc.options.sneakKey.getDefaultKey().getCode());
        Client.SCHEDULER.scheduleOnce(() -> {
            if (mc.player == null) return;

            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
            mc.options.sneakKey.setPressed(false);
            Client.SCHEDULER.scheduleOnce(() -> {
                if (mc.player == null) return;

                int syncId = mc.player.currentScreenHandler.syncId;
                mc.interactionManager.clickSlot(syncId, slot, 40, SlotActionType.SWAP, mc.player);
                Client.SCHEDULER.scheduleOnce(() -> {
                    if (mc.player == null) return;
                    long h = mc.getWindow().getHandle();
                    mc.options.forwardKey.setPressed(wasForward && InputUtil.isKeyPressed(window, mc.options.forwardKey.getDefaultKey().getCode()));
                    mc.options.backKey.setPressed(wasBack && InputUtil.isKeyPressed(window, mc.options.backKey.getDefaultKey().getCode()));
                    mc.options.leftKey.setPressed(wasLeft && InputUtil.isKeyPressed(window, mc.options.leftKey.getDefaultKey().getCode()));
                    mc.options.rightKey.setPressed(wasRight && InputUtil.isKeyPressed(window, mc.options.rightKey.getDefaultKey().getCode()));
                    mc.options.jumpKey.setPressed(wasJump && InputUtil.isKeyPressed(window, mc.options.jumpKey.getDefaultKey().getCode()));
                    mc.options.sneakKey.setPressed(wasSneak && InputUtil.isKeyPressed(window, mc.options.sneakKey.getDefaultKey().getCode()));
                }, 1);
            }, 1);
        }, 1);
    }
   public static void matrixswaptooffhend(int slot) {
       if (mc.player == null || mc.interactionManager == null || mc.getNetworkHandler() == null) return;
       if (slot == -1 || slot == 45) return;

       int currentHotbarSlot = mc.player.getInventory().getSelectedSlot();

       if (isHotbarSlot(slot)) {
           int targetHotbarIndex = convertToHotbarIndex(slot);

           if (targetHotbarIndex == currentHotbarSlot) {
               sendOffhandSwapPacket();
           } else {
               mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(targetHotbarIndex));
               mc.player.getInventory().setSelectedSlot(targetHotbarIndex);

               sendOffhandSwapPacket();

               mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(currentHotbarSlot));
               mc.player.getInventory().setSelectedSlot(currentHotbarSlot);
           }
           return;
       }

       boolean wasForward = InputUtil.isKeyPressed(window, mc.options.forwardKey.getDefaultKey().getCode());
       boolean wasBack = InputUtil.isKeyPressed(window, mc.options.backKey.getDefaultKey().getCode());
       boolean wasLeft = InputUtil.isKeyPressed(window, mc.options.leftKey.getDefaultKey().getCode());
       boolean wasRight = InputUtil.isKeyPressed(window, mc.options.rightKey.getDefaultKey().getCode());
       boolean wasJump = InputUtil.isKeyPressed(window, mc.options.jumpKey.getDefaultKey().getCode());
       boolean wasSneak = InputUtil.isKeyPressed(window, mc.options.sneakKey.getDefaultKey().getCode());
       Client.SCHEDULER.scheduleOnce(() -> {
           if (mc.player == null) return;

           mc.options.forwardKey.setPressed(false);
           mc.options.backKey.setPressed(false);
           mc.options.leftKey.setPressed(false);
           mc.options.rightKey.setPressed(false);
           mc.options.jumpKey.setPressed(false);
           mc.options.sneakKey.setPressed(false);
           Client.SCHEDULER.scheduleOnce(() -> {
               if (mc.player == null) return;
               int syncId = mc.player.currentScreenHandler.syncId;
               mc.interactionManager.clickSlot(syncId, slot, 40, SlotActionType.SWAP, mc.player);
               Client.SCHEDULER.scheduleOnce(() -> {
                   if (mc.player == null) return;
                   long h = mc.getWindow().getHandle();
                   mc.options.forwardKey.setPressed(wasForward && InputUtil.isKeyPressed(window, mc.options.forwardKey.getDefaultKey().getCode()));
                   mc.options.backKey.setPressed(wasBack && InputUtil.isKeyPressed(window, mc.options.backKey.getDefaultKey().getCode()));
                   mc.options.leftKey.setPressed(wasLeft && InputUtil.isKeyPressed(window, mc.options.leftKey.getDefaultKey().getCode()));
                   mc.options.rightKey.setPressed(wasRight && InputUtil.isKeyPressed(window, mc.options.rightKey.getDefaultKey().getCode()));
                   mc.options.jumpKey.setPressed(wasJump && InputUtil.isKeyPressed(window, mc.options.jumpKey.getDefaultKey().getCode()));
                   mc.options.sneakKey.setPressed(wasSneak && InputUtil.isKeyPressed(window, mc.options.sneakKey.getDefaultKey().getCode()));
               }, 1);
           }, 1);
       }, 1);
   }




    public void GrimPacketSwapOffHand(int slot){
        if(slot == -1||mc.player == null || mc.interactionManager == null) return;
        NetworkUtility.sendInputPacket(false,false,false,false,false,false,false);
        int syncId = mc.player.currentScreenHandler.syncId;
        NetworkUtility.send(new ClientCommandC2SPacket(mc.player,ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        mc.interactionManager.clickSlot(syncId, slot, 40, SlotActionType.SWAP, mc.player);
        NetworkUtility.sendSilentPacket(new CloseHandledScreenC2SPacket(0));
        NetworkUtility.send(new ClientCommandC2SPacket(mc.player,ClientCommandC2SPacket.Mode.START_SPRINTING));
    }

    public void safeSwapElytraViaHotbar(int hotbarSlot, int armorSlot) {
        if (hotbarSlot == -1 || armorSlot == -1 || mc.player == null || mc.interactionManager == null) return;
        boolean wasForward = InputUtil.isKeyPressed(window, mc.options.forwardKey.getDefaultKey().getCode());
        boolean wasBack = InputUtil.isKeyPressed(window, mc.options.backKey.getDefaultKey().getCode());
        boolean wasLeft = InputUtil.isKeyPressed(window, mc.options.leftKey.getDefaultKey().getCode());
        boolean wasRight = InputUtil.isKeyPressed(window, mc.options.rightKey.getDefaultKey().getCode());
        boolean wasJump = InputUtil.isKeyPressed(window, mc.options.jumpKey.getDefaultKey().getCode());
        boolean wasSneak = InputUtil.isKeyPressed(window, mc.options.sneakKey.getDefaultKey().getCode());
        Client.SCHEDULER.scheduleOnce(() -> {
            if (mc.player == null) return;
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
            mc.options.sneakKey.setPressed(false);
            Client.SCHEDULER.scheduleOnce(() -> {
                if (mc.player == null) return;
                Client.SCHEDULER.scheduleOnce(() -> {
                    if (mc.player == null) return;
                    Client.SCHEDULER.scheduleOnce(() -> {
                        if (mc.player == null) return;
                        int syncId = mc.player.currentScreenHandler.syncId;
                        mc.interactionManager.clickSlot(syncId, armorSlot, hotbarSlot, SlotActionType.SWAP, mc.player);
                        Client.SCHEDULER.scheduleOnce(() -> {
                            if (mc.player == null) return;
                            Client.SCHEDULER.scheduleOnce(() -> {
                                if (mc.player == null) return;
                                Client.SCHEDULER.scheduleOnce(() -> {
                                    if (mc.player == null) return;
                                    long h = mc.getWindow().getHandle();
                                    mc.options.forwardKey.setPressed(wasForward && InputUtil.isKeyPressed(window, mc.options.forwardKey.getDefaultKey().getCode()));
                                    mc.options.backKey.setPressed(wasBack && InputUtil.isKeyPressed(window, mc.options.backKey.getDefaultKey().getCode()));
                                    mc.options.leftKey.setPressed(wasLeft && InputUtil.isKeyPressed(window, mc.options.leftKey.getDefaultKey().getCode()));
                                    mc.options.rightKey.setPressed(wasRight && InputUtil.isKeyPressed(window, mc.options.rightKey.getDefaultKey().getCode()));
                                    mc.options.jumpKey.setPressed(wasJump && InputUtil.isKeyPressed(window, mc.options.jumpKey.getDefaultKey().getCode()));
                                    mc.options.sneakKey.setPressed(wasSneak && InputUtil.isKeyPressed(window, mc.options.sneakKey.getDefaultKey().getCode()));
                                }, 1);
                            }, 1);
                        }, 1);
                    }, 1);
                }, 1);
            }, 1);
        }, 1);
    }
    public void safeSwapElytraFull(int inventorySlot, int hotbarSlot, int armorSlot) {
        if (inventorySlot == -1 || hotbarSlot == -1 || armorSlot == -1 || mc.player == null || mc.interactionManager == null)
            return;
        int hotbarScreenSlot = 36 + hotbarSlot;
        boolean wasForward = InputUtil.isKeyPressed(window, mc.options.forwardKey.getDefaultKey().getCode());
        boolean wasBack = InputUtil.isKeyPressed(window, mc.options.backKey.getDefaultKey().getCode());
        boolean wasLeft = InputUtil.isKeyPressed(window, mc.options.leftKey.getDefaultKey().getCode());
        boolean wasRight = InputUtil.isKeyPressed(window, mc.options.rightKey.getDefaultKey().getCode());
        boolean wasJump = InputUtil.isKeyPressed(window, mc.options.jumpKey.getDefaultKey().getCode());
        boolean wasSneak = InputUtil.isKeyPressed(window, mc.options.sneakKey.getDefaultKey().getCode());
        Client.SCHEDULER.scheduleOnce(() -> {
            if (mc.player == null) return;
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
            mc.options.sneakKey.setPressed(false);
            int syncId = mc.player.currentScreenHandler.syncId;
            Client.SCHEDULER.scheduleOnce(() -> {
                if (mc.player == null) return;
                Client.SCHEDULER.scheduleOnce(() -> {
                    if (mc.player == null) return;
                    mc.interactionManager.clickSlot(syncId, inventorySlot, 0, SlotActionType.SWAP, mc.player);
                    Client.SCHEDULER.scheduleOnce(() -> {
                        if (mc.player == null) return;
                        mc.interactionManager.clickSlot(syncId, hotbarScreenSlot, 0, SlotActionType.SWAP, mc.player);
                        Client.SCHEDULER.scheduleOnce(() -> {
                            if (mc.player == null) return;
                            mc.interactionManager.clickSlot(syncId, inventorySlot, 0, SlotActionType.SWAP, mc.player);
                            Client.SCHEDULER.scheduleOnce(() -> {
                                if (mc.player == null) return;
                                Client.SCHEDULER.scheduleOnce(() -> {
                                    if (mc.player == null) return;
                                    mc.interactionManager.clickSlot(syncId, armorSlot, hotbarSlot, SlotActionType.SWAP, mc.player);
                                    Client.SCHEDULER.scheduleOnce(() -> {
                                        if (mc.player == null) return;
                                        Client.SCHEDULER.scheduleOnce(() -> {
                                            if (mc.player == null) return;
                                            mc.interactionManager.clickSlot(syncId, hotbarScreenSlot, 0, SlotActionType.SWAP, mc.player);
                                            Client.SCHEDULER.scheduleOnce(() -> {
                                                if (mc.player == null) return;
                                                mc.interactionManager.clickSlot(syncId, inventorySlot, 0, SlotActionType.SWAP, mc.player);
                                                Client.SCHEDULER.scheduleOnce(() -> {
                                                    if (mc.player == null) return;
                                                    mc.interactionManager.clickSlot(syncId, hotbarScreenSlot, 0, SlotActionType.SWAP, mc.player);
                                                    Client.SCHEDULER.scheduleOnce(() -> {
                                                        if (mc.player == null) return;
                                                        Client.SCHEDULER.scheduleOnce(() -> {
                                                            if (mc.player == null) return;
                                                            Client.SCHEDULER.scheduleOnce(() -> {
                                                                if (mc.player == null) return;
                                                                Client.SCHEDULER.scheduleOnce(() -> {
                                                                    if (mc.player == null) return;
                                                                    long h = mc.getWindow().getHandle();
                                                                    mc.options.forwardKey.setPressed(wasForward && InputUtil.isKeyPressed(window, mc.options.forwardKey.getDefaultKey().getCode()));
                                                                    mc.options.backKey.setPressed(wasBack && InputUtil.isKeyPressed(window, mc.options.backKey.getDefaultKey().getCode()));
                                                                    mc.options.leftKey.setPressed(wasLeft && InputUtil.isKeyPressed(window, mc.options.leftKey.getDefaultKey().getCode()));
                                                                    mc.options.rightKey.setPressed(wasRight && InputUtil.isKeyPressed(window, mc.options.rightKey.getDefaultKey().getCode()));
                                                                    mc.options.jumpKey.setPressed(wasJump && InputUtil.isKeyPressed(window, mc.options.jumpKey.getDefaultKey().getCode()));
                                                                    mc.options.sneakKey.setPressed(wasSneak && InputUtil.isKeyPressed(window, mc.options.sneakKey.getDefaultKey().getCode()));
                                                                }, 1);
                                                            }, 1);
                                                        }, 1);
                                                    }, 1);
                                                }, 1);
                                            }, 1);
                                        }, 1);
                                    }, 1);
                                }, 1);
                            }, 1);
                        }, 1);
                    }, 1);
                }, 1);
            }, 1);
        }, 1);
    }

    public void safeUseItem(int fireworkSlot) {
        if (fireworkSlot == -1 || mc.player == null || mc.interactionManager == null) return;
        if (Client.IS_PANIC) return;
        ItemStack offhand = mc.player.getOffHandStack();
         int main =mc.player.getInventory().getSelectedSlot();
        int hotbarSlot = serchSlot(fireworkSlot,0,9);
        boolean wasForward = InputUtil.isKeyPressed(window, mc.options.forwardKey.getDefaultKey().getCode());
        boolean wasBack = InputUtil.isKeyPressed(window, mc.options.backKey.getDefaultKey().getCode());
        boolean wasLeft = InputUtil.isKeyPressed(window, mc.options.leftKey.getDefaultKey().getCode());
        boolean wasRight = InputUtil.isKeyPressed(window, mc.options.rightKey.getDefaultKey().getCode());
        boolean wasJump = InputUtil.isKeyPressed(window, mc.options.jumpKey.getDefaultKey().getCode());
        boolean wasSneak = InputUtil.isKeyPressed(window, mc.options.sneakKey.getDefaultKey().getCode());

        if(offhand.getItem() == Items.TOTEM_OF_UNDYING){
                Client.SCHEDULER.scheduleOnce(() -> {
                    if (mc.player == null || Client.IS_PANIC) return;
                    mc.options.forwardKey.setPressed(false);
                    mc.options.backKey.setPressed(false);
                    mc.options.leftKey.setPressed(false);
                    mc.options.rightKey.setPressed(false);
                    mc.options.jumpKey.setPressed(false);
                    mc.options.sneakKey.setPressed(false);
                    int syncId = mc.player.currentScreenHandler.syncId;
                    Client.SCHEDULER.scheduleOnce(() -> {
                        if (mc.player == null || Client.IS_PANIC) return;
                        Client.SCHEDULER.scheduleOnce(() -> {
                            if (mc.player == null || Client.IS_PANIC) return;
                            mc.interactionManager.clickSlot(syncId, fireworkSlot, main, SlotActionType.SWAP, mc.player);
                            Client.SCHEDULER.scheduleOnce(() -> {
                                if (mc.player == null || Client.IS_PANIC) return;
                                Client.SCHEDULER.scheduleOnce(() -> {
                                    if (mc.player == null || Client.IS_PANIC) return;
                                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                                    Client.SCHEDULER.scheduleOnce(() -> {
                                        if (mc.player == null || Client.IS_PANIC) return;
                                        Client.SCHEDULER.scheduleOnce(() -> {
                                            if (mc.player == null || Client.IS_PANIC) return;
                                            mc.interactionManager.clickSlot(syncId, fireworkSlot, main, SlotActionType.SWAP, mc.player);
                                            Client.SCHEDULER.scheduleOnce(() -> {
                                                if (mc.player == null || Client.IS_PANIC) return;
                                                Client.SCHEDULER.scheduleOnce(() -> {
                                                    if (mc.player == null || Client.IS_PANIC) return;
                                                    long h = mc.getWindow().getHandle();
                                                    mc.options.forwardKey.setPressed(wasForward && InputUtil.isKeyPressed(window, mc.options.forwardKey.getDefaultKey().getCode()));
                                                    mc.options.backKey.setPressed(wasBack && InputUtil.isKeyPressed(window, mc.options.backKey.getDefaultKey().getCode()));
                                                    mc.options.leftKey.setPressed(wasLeft && InputUtil.isKeyPressed(window, mc.options.leftKey.getDefaultKey().getCode()));
                                                    mc.options.rightKey.setPressed(wasRight && InputUtil.isKeyPressed(window, mc.options.rightKey.getDefaultKey().getCode()));
                                                    mc.options.jumpKey.setPressed(wasJump && InputUtil.isKeyPressed(window, mc.options.jumpKey.getDefaultKey().getCode()));
                                                    mc.options.sneakKey.setPressed(wasSneak && InputUtil.isKeyPressed(window, mc.options.sneakKey.getDefaultKey().getCode()));
                                                }, 1);
                                            }, 1);
                                        }, 1);
                                    }, 1);
                                }, 1);
                            }, 1);
                        }, 1);
                    }, 1);
                }, 1);

        }else {
            Client.SCHEDULER.scheduleOnce(() -> {
                if (mc.player == null || Client.IS_PANIC) return;
                mc.options.forwardKey.setPressed(false);
                mc.options.backKey.setPressed(false);
                mc.options.leftKey.setPressed(false);
                mc.options.rightKey.setPressed(false);
                mc.options.jumpKey.setPressed(false);
                mc.options.sneakKey.setPressed(false);
                int syncId = mc.player.currentScreenHandler.syncId;
                Client.SCHEDULER.scheduleOnce(() -> {
                    if (mc.player == null || Client.IS_PANIC) return;
                    Client.SCHEDULER.scheduleOnce(() -> {
                        if (mc.player == null || Client.IS_PANIC) return;
                        mc.interactionManager.clickSlot(syncId, fireworkSlot, 40, SlotActionType.SWAP, mc.player);
                        Client.SCHEDULER.scheduleOnce(() -> {
                            if (mc.player == null || Client.IS_PANIC) return;
                            Client.SCHEDULER.scheduleOnce(() -> {
                                if (mc.player == null || Client.IS_PANIC) return;
                                mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
                                Client.SCHEDULER.scheduleOnce(() -> {
                                    if (mc.player == null || Client.IS_PANIC) return;
                                    Client.SCHEDULER.scheduleOnce(() -> {
                                        if (mc.player == null || Client.IS_PANIC) return;
                                        mc.interactionManager.clickSlot(syncId, fireworkSlot, 40, SlotActionType.SWAP, mc.player);
                                        Client.SCHEDULER.scheduleOnce(() -> {
                                            if (mc.player == null || Client.IS_PANIC) return;
                                            Client.SCHEDULER.scheduleOnce(() -> {
                                                if (mc.player == null || Client.IS_PANIC) return;
                                                long h = mc.getWindow().getHandle();
                                                mc.options.forwardKey.setPressed(wasForward && InputUtil.isKeyPressed(window, mc.options.forwardKey.getDefaultKey().getCode()));
                                                mc.options.backKey.setPressed(wasBack && InputUtil.isKeyPressed(window, mc.options.backKey.getDefaultKey().getCode()));
                                                mc.options.leftKey.setPressed(wasLeft && InputUtil.isKeyPressed(window, mc.options.leftKey.getDefaultKey().getCode()));
                                                mc.options.rightKey.setPressed(wasRight && InputUtil.isKeyPressed(window, mc.options.rightKey.getDefaultKey().getCode()));
                                                mc.options.jumpKey.setPressed(wasJump && InputUtil.isKeyPressed(window, mc.options.jumpKey.getDefaultKey().getCode()));
                                                mc.options.sneakKey.setPressed(wasSneak && InputUtil.isKeyPressed(window, mc.options.sneakKey.getDefaultKey().getCode()));
                                            }, 1);
                                        }, 1);
                                    }, 1);
                                }, 1);
                            }, 1);
                        }, 1);
                    }, 1);
                }, 1);
            }, 1);
        }
    }

    private boolean canSprint() {
        if (mc.player == null) return false;
        return mc.player.getHungerManager().getFoodLevel() > 6 && !mc.player.isSneaking();
    }
    public void legitUseItemFromHotbar(int itemSlot, int currentSlot) {
        if (itemSlot == -1 || mc.player == null || mc.interactionManager == null) return;
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(itemSlot));
        Client.SCHEDULER.scheduleOnce(() -> {
            if (mc.player == null) return;
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            Client.SCHEDULER.scheduleOnce(() -> {
                if (mc.player == null) return;
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(currentSlot));
            }, 1);
        }, 1);
    }
    public void legitUseItemFromInventory(int inventorySlot, int currentSlot) {
        if (inventorySlot == -1 || mc.player == null || mc.interactionManager == null) return;
        int targetHotbarSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (i == currentSlot) continue;
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                targetHotbarSlot = i;
                break;
            }
        }
        if (targetHotbarSlot == -1) targetHotbarSlot = currentSlot == 0 ? 1 : currentSlot - 1;
        final int finalTargetSlot = targetHotbarSlot;
        int syncId = mc.player.currentScreenHandler.syncId;
        mc.interactionManager.clickSlot(syncId, inventorySlot, finalTargetSlot, SlotActionType.SWAP, mc.player);
        Client.SCHEDULER.scheduleOnce(() -> {
            if (mc.player == null) return;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(finalTargetSlot));
            Client.SCHEDULER.scheduleOnce(() -> {
                if (mc.player == null) return;
                mc.interactionManager.interactItem(mc.player,  Hand.MAIN_HAND);
                Client.SCHEDULER.scheduleOnce(() -> {
                    if (mc.player == null) return;
                    mc.interactionManager.clickSlot(syncId, inventorySlot, finalTargetSlot, SlotActionType.SWAP, mc.player);
                    Client.SCHEDULER.scheduleOnce(() -> {
                        if (mc.player == null) return;
                        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(currentSlot));
                    }, 1);
                }, 1);
            }, 1);
        }, 1);
    }
}
