package gg.topchdlc.api.autobuy.item.Name;

import net.minecraft.item.ItemStack;
import gg.topchdlc.api.autobuy.item.ItemBuy;

public class NameItemBuy extends ItemBuy {
    private final String nameContains;
    
    public NameItemBuy(ItemStack itemStack, String displayName, String nameContains, Category category) {
        super(itemStack, displayName, category);
        this.nameContains = nameContains.toLowerCase();
    }
    @Override
    public boolean isBuy(ItemStack stack) {
        if (!super.isBuy(stack)) {
            return false;
        }
        String itemName = stack.getName().getString().toLowerCase();
        return itemName.contains(nameContains);
    }
    
    public String getNameContains() {
        return nameContains;
    }
}
