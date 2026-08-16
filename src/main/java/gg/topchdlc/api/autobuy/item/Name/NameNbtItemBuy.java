package gg.topchdlc.api.autobuy.item.Name;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import gg.topchdlc.api.autobuy.item.ItemBuy;

public class NameNbtItemBuy extends ItemBuy {
    private final String nameContains;
    private final String nbtKey;
    private final boolean requireNbt;
    public NameNbtItemBuy(ItemStack itemStack, String displayName, String nameContains, String nbtKey, Category category) {
        super(itemStack, displayName, category);
        this.nameContains = nameContains.toLowerCase();
        this.nbtKey = nbtKey;
        this.requireNbt = nbtKey != null && !nbtKey.isEmpty();
    }
    @Override
    public boolean isBuy(ItemStack stack) {
        if (!super.isBuy(stack)) {
            return false;
        }
        String itemName = stack.getName().getString().toLowerCase();
        if (!itemName.contains(nameContains)) {
            return false;
        }
        if (requireNbt) {
            NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (customData == null) {
                return false;
            }
            NbtCompound nbt = customData.copyNbt();
            return nbt.contains(nbtKey);
        }
        return true;
    }
    public String getNameContains() {
        return nameContains;
    }
    public String getNbtKey() {
        return nbtKey;
    }
}
