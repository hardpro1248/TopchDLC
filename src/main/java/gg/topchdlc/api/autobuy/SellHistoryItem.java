package gg.topchdlc.api.autobuy;

import net.minecraft.item.ItemStack;

public class SellHistoryItem {
    public ItemStack itemStack;
    public int buyPrice;
    public int sellPrice;
    public long timestamp;
    public String itemName;

    public SellHistoryItem(ItemStack itemStack, int buyPrice, int sellPrice) {
        this.itemStack = itemStack;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.timestamp = System.currentTimeMillis();
        this.itemName = itemStack.getName().getString();
    }

    public int getProfit() {
        return sellPrice - buyPrice;
    }
}
