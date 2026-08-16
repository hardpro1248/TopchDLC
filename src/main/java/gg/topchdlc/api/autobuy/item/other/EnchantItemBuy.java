package gg.topchdlc.api.autobuy.item.other;

import java.util.ArrayList;

import net.minecraft.item.ItemStack;
import gg.topchdlc.api.autobuy.enchantes.Enchant;
import gg.topchdlc.api.autobuy.item.ItemBuy;


public class EnchantItemBuy extends ItemBuy {
   protected final ArrayList<Enchant> enchants = new ArrayList();

   public EnchantItemBuy(ItemStack itemStack, String displayName, String searchName, ItemBuy.Category maxSumBuy) {
      super(itemStack, displayName, searchName, maxSumBuy);
   }

   public EnchantItemBuy(ItemStack itemStack, String searchName, ItemBuy.Category maxSumBuy) {
      super(itemStack, searchName, maxSumBuy);
   }

   public boolean isBuy(ItemStack stack) {
      if (!super.isBuy(stack)) {
         return false;
      } else {
         for (Enchant enchant : this.enchants) {
            if (!enchant.isEnchanted(stack)) {
               return false;
            }
         }
         return true;
      }
   }

   public void addEnchant(Enchant enchant) {
      this.enchants.add(enchant);
   }

   public ArrayList<Enchant> getEnchants() {
      return this.enchants;
   }
}
