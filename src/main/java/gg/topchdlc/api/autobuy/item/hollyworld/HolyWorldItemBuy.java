package gg.topchdlc.api.autobuy.item.hollyworld;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import gg.topchdlc.api.autobuy.item.ItemBuy;


public class HolyWorldItemBuy extends ItemBuy {
    
    private final String nbtTypeKey;
    private final String nbtValueKey;
    private final String nbtValue;

    public HolyWorldItemBuy(ItemStack itemStack, String searchName, Category category) {
        super(itemStack, searchName, category);
        this.nbtTypeKey = null;
        this.nbtValueKey = null;
        this.nbtValue = null;
    }

    public HolyWorldItemBuy(ItemStack itemStack, String searchName, Category category, 
                            String nbtTypeKey, String nbtValueKey, String nbtValue) {
        super(itemStack, searchName, category);
        this.nbtTypeKey = nbtTypeKey;
        this.nbtValueKey = nbtValueKey;
        this.nbtValue = nbtValue;
    }
    
    @Override
    public boolean isBuy(ItemStack stack) {
        if (stack == null || stack.getItem() != this.itemStack.getItem()) {
            return false;
        }
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent == null) {
            return false;
        }
        NbtCompound nbt = nbtComponent.copyNbt();
        if (!nbt.contains("HolyWorldItem") || !nbt.getBoolean("HolyWorldItem").orElse(false)) {
            return false;
        }
        if (nbtTypeKey != null) {
            if (!nbt.contains(nbtTypeKey) || !nbt.getBoolean(nbtTypeKey).orElse(false)) {
                return false;
            }
        }
        if (nbtValueKey != null && nbtValue != null) {
            if (!nbt.contains(nbtValueKey)) {
                return false;
            }
            String stackValue = nbt.getString(nbtValueKey).orElse("");
            if (!nbtValue.equalsIgnoreCase(stackValue)) {
                return false;
            }
        }
        
        return true;
    }
}
