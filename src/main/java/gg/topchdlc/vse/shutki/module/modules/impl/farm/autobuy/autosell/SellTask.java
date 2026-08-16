package gg.topchdlc.vse.shutki.module.modules.impl.farm.autobuy.autosell;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import gg.topchdlc.api.autobuy.item.ItemBuy;

/**
 * Create by daun kvass
 */
public class SellTask {
    public static final int MAX_CHECK_ATTEMPTS = 15;

    public final Item itemType;
    public final String itemId;
    public final int buyPrice;
    public final int buyPricePerUnit;
    public final int originalCount;
    public final String displayName;
    public final String searchName;
    public final ItemBuy itemBuy;
    public int checkAttempts = 0;

    public SellTask(ItemStack item, int totalBuyPrice, String displayName, String searchName, ItemBuy itemBuy) {
        this.itemType = item.getItem();
        this.itemId = Registries.ITEM.getId(item.getItem()).toString();
        this.buyPrice = totalBuyPrice;
        this.originalCount = item.getCount();
        this.buyPricePerUnit = originalCount > 0 ? totalBuyPrice / originalCount : totalBuyPrice;
        this.displayName = displayName;
        this.searchName = searchName;
        this.itemBuy = itemBuy;
    }
}
