package gg.topchdlc.api.autobuy.enchantes.custom;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import gg.topchdlc.api.autobuy.enchantes.Enchant;


public class EnchantCustom extends Enchant {
   public EnchantCustom(String name, String checked, int minLevel) {
      super(name, checked, minLevel);
   }

   public boolean isEnchanted(ItemStack stack) {
      NbtComponent customData = (NbtComponent)stack.get(DataComponentTypes.CUSTOM_DATA);
      if (customData != null && customData.copyNbt().contains("custom-enchantments")) {
         NbtList customEnchants = customData.copyNbt().getList("custom-enchantments").orElse(null);
         if (customEnchants == null) return false;

         for(int i = 0; i < customEnchants.size(); ++i) {
            NbtCompound ench = customEnchants.getCompound(i).orElse(null);
            if (ench == null) continue;
            String type = ench.getString("type").orElse("");
            int level = ench.getInt("level").orElse(0);
            if (type.equals(this.checked)) {
               return level >= this.minLevel;
            }
         }
      }

      return false;
   }
}
