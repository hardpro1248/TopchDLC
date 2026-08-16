package gg.topchdlc.api.autobuy.item.other;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import gg.topchdlc.api.autobuy.item.ItemBuy;

public class NbtItemBuy extends ItemBuy {
   private final Map<String, String> nbtKeyValue;

   public NbtItemBuy(ItemStack itemStack, String displayName, String searchName, ItemBuy.Category maxSumBuy) {
      super(itemStack, displayName, searchName, maxSumBuy);
      this.nbtKeyValue = new HashMap();
   }
   public NbtItemBuy(ItemStack itemStack, String searchName, ItemBuy.Category maxSumBuy, Map<String, String> nbtKeyValue) {
      super(itemStack, searchName, maxSumBuy);
      this.nbtKeyValue = nbtKeyValue;
   }
   public NbtItemBuy(ItemStack itemStack, String searchName, ItemBuy.Category maxSumBuy, String key, String value) {
      super(itemStack, searchName, maxSumBuy);
      this.nbtKeyValue = new HashMap();
      this.nbtKeyValue.put(key, value);
   }
   public boolean isBuy(ItemStack stack) {
      if (!super.isBuy(stack)) {
         return false;
      } else {
         String stackName = stack.getName().getString();
         String searchNameLower = searchName.toLowerCase();
         String stackNameLower = stackName.toLowerCase();
         if (!stackNameLower.contains(searchNameLower)) {
            return false;
         }
         NbtComponent customData = (NbtComponent)stack.get(DataComponentTypes.CUSTOM_DATA);
         if (customData == null) {
            return false;
         } else {
            NbtCompound nbt = customData.copyNbt();
            Iterator var4 = this.nbtKeyValue.entrySet().iterator();
            String key;
            String value;
            do {
               if (!var4.hasNext()) {
                  return true;
               }
               Entry<String, String> entry = (Entry)var4.next();
               key = (String)entry.getKey();
               value = (String)entry.getValue();
            } while(nbt.contains(key) && nbt.get(key).toString().replaceAll(",?UUID:\\[I;[-0-9]+,[-0-9]+,[-0-9]+,[-0-9]+]", "").contains(value.replaceAll(",?UUID:\\[I;[-0-9]+,[-0-9]+,[-0-9]+,[-0-9]+]", "")));
            return false;
         }
      }
   }
}
