package gg.topchdlc.vse.shutki.module.modules.impl.farm.autobuy.autosell;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

/**
 * Create by daun kvass
 */
public class InventoryHelper {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public record SearchResult(int slotIndex, int count, ItemStack stack) {}

    public SearchResult findInHotbar(SellTask task) {
        if (mc.player == null) return null;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i <= 8; i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && matches(stack, task)) {
                return new SearchResult(i, stack.getCount(), stack);
            }
        }
        return null;
    }

    public SearchResult findInInventory(SellTask task) {
        if (mc.player == null) return null;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && matches(stack, task)) {
                return new SearchResult(i, stack.getCount(), stack);
            }
        }
        return null;
    }

    public SearchResult find(SellTask task) {
        SearchResult r = findInHotbar(task);
        return r != null ? r : findInInventory(task);
    }

    private boolean matches(ItemStack stack, SellTask task) {
        if (stack.isEmpty()) return false;
        if (task.itemBuy != null) return task.itemBuy.isBuy(stack);
        return stack.getItem() == task.itemType;
    }
}
