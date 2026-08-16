package gg.topchdlc.api.autobuy;

import net.minecraft.item.ItemStack;

public class PurchaseHistoryItem {
    public ItemStack itemStack;
    public int price;
    public long timestamp;
    public String auctionName;
    public PurchaseHistoryItem(ItemStack itemStack, int price, long l) {
        this.itemStack = itemStack;
        this.price = price;
        this.timestamp = System.currentTimeMillis();
        this.auctionName = itemStack.getName().getString();
    }
}
