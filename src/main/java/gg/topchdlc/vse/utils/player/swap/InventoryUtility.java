
package gg.topchdlc.vse.utils.player.swap;

import com.google.common.collect.Lists;
import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
import gg.topchdlc.vse.shutki.module.modules.impl.helper.AutoSwap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class InventoryUtility implements MinecraftHolder {
    public static boolean skipClient = false;

    public static void swap(int from, int to) {
        if (mc.player != null) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, from, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, to, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, from, 0, SlotActionType.PICKUP, mc.player);
        }
    }

    public static int find(Item item) {
        for(int i = 0; i < 44; ++i) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }

        return -1;
    }

    public static int find(Item item, int start, int end) {
        if (mc.player == null) {
            return -1;
        } else {
            for(int i = end; i >= start; --i) {
                if (mc.player.currentScreenHandler.syncId != 0 && mc.player.currentScreenHandler.getSlot(i).getStack().getItem() == item) {
                    return i;
                }

                if (mc.player.currentScreenHandler.syncId == 0 && mc.player.getInventory().getStack(i).getItem() == item) {
                    return i;
                }
            }

            return -1;
        }
    }

    public static int find(Item item, boolean enchanted) {
        for(int i = 0; i < 44; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item && (!stack.hasEnchantments() || !enchanted)) {
                return i;
            }
        }

        return -1;
    }

    public static int wrapHotbar(int slot) {
        return slot < 9 ? slot + 36 : slot;
    }

    public static void useItem(int slot, boolean swing) {
        useItem(slot, swing, false, false);
    }

    public static void useItem(int slot, boolean swing, boolean cooldown, boolean delay) {
        if (slot != -1) {
            if (slot < 9) {
                if (mc.player.getInventory().getSelectedSlot() != slot) {
                    NetworkUtility.send(new UpdateSelectedSlotC2SPacket(slot));
                }

                NetworkUtility.sendUse(Hand.MAIN_HAND);
                if (swing) {
                    mc.player.swingHand(Hand.MAIN_HAND);
                }

                if (delay) {
                    Client.SCHEDULER.scheduleOnce(() -> {
                        if (mc.player.getInventory().getSelectedSlot() != slot) {
                            NetworkUtility.send(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().getSelectedSlot()));
                        }

                    }, 3);
                } else if (mc.player.getInventory().getSelectedSlot() != slot) {
                    NetworkUtility.send(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().getSelectedSlot()));
                }
            } else {
                int s = mc.player.getInventory().getSelectedSlot();
                if (cooldown) {
                    s = (s + 1) % 8;
                }

                final int finalS = s;
                swap(wrapHotbar(slot), wrapHotbar(finalS));
                if (cooldown) {
                    if (mc.player.getInventory().getSelectedSlot() != finalS) {
                        NetworkUtility.send(new UpdateSelectedSlotC2SPacket(finalS));
                    }

                    NetworkUtility.sendUse(Hand.MAIN_HAND);
                    if (swing) {
                        mc.player.swingHand(Hand.MAIN_HAND);
                    }

                    if (mc.player.getInventory().getSelectedSlot() != finalS) {
                        NetworkUtility.send(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().getSelectedSlot()));
                    }
                } else {
                    NetworkUtility.sendUse(Hand.MAIN_HAND);
                    if (swing) {
                        mc.player.swingHand(Hand.MAIN_HAND);
                    }
                }

                Client.SCHEDULER.scheduleOnce(() -> {
                    swap(wrapHotbar(slot), wrapHotbar(finalS));
                    NetworkUtility.send(new CloseHandledScreenC2SPacket(finalS));
                }, 3);
            }

        }
    }

    public static int findHotbar(Item item) {
        int slot = -1;

        for(int i = 0; i < 9; ++i) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                slot = i;
                break;
            }
        }

        return slot;
    }
    public static void sendOffhandSwapPacket() {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ORIGIN,
                Direction.DOWN
        ));
    }


    public static ClickSlotC2SPacket click(int syncId, int slotId, int button, SlotActionType actionType) {
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        DefaultedList<Slot> defaultedList = screenHandler.slots;
        int i = defaultedList.size();
        List<ItemStack> list = Lists.newArrayListWithCapacity(i);

        for(Slot slot : defaultedList) {
            list.add(slot.getStack().copy());
        }

        Int2ObjectMap<ItemStackHash> int2ObjectMap = new Int2ObjectOpenHashMap();


        ItemStackHash itemStackHash = ItemStackHash.fromItemStack(slotId == -999 ? screenHandler.getCursorStack() : mc.player.getInventory().getStack(slotId), mc.getNetworkHandler().getComponentHasher());
        return new ClickSlotC2SPacket(syncId, screenHandler.getRevision(), Shorts.checkedCast((long)slotId), SignedBytes.checkedCast((long)button), actionType, int2ObjectMap, itemStackHash);
    }


    private InventoryUtility() {
        throw new UnsupportedOperationException("x");
    }
}
