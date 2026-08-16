package gg.topchdlc.vse.shutki.module.modules.impl.farm.autobuy;

import net.minecraft.item.ItemStack;
import gg.topchdlc.api.autobuy.item.ItemBuy;

/**
 * Create by daun kvass
 */
public class PendingPurchase {
    public final ItemStack stack;
    public final int price;
    public final long clickTime;
    public final ItemBuy itemBuy;

    public PendingPurchase(ItemStack stack, int price, ItemBuy itemBuy) {
        this.stack = stack;
        this.price = price;
        this.clickTime = System.currentTimeMillis();
        this.itemBuy = itemBuy;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - clickTime > 10_000;
    }
}
