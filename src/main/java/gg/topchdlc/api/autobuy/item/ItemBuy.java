package gg.topchdlc.api.autobuy.item;

import lombok.Generated;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import gg.topchdlc.MinecraftHolder;
import net.minecraft.text.Text;


public class ItemBuy implements MinecraftHolder {
   protected ItemStack itemStack;
   protected final String displayName;
   protected final String searchName;
   protected final ItemBuy.Category category;

    public ItemBuy(ItemStack itemStack, String searchName, ItemBuy.Category category) {
        this.itemStack = itemStack.copy();
        this.displayName = searchName;
        this.searchName = searchName;
        this.category = category;

        this.itemStack.set(DataComponentTypes.CUSTOM_NAME,
               Text.literal(searchName).styled(s -> s.withItalic(false).withColor(0x00FFFF)));
    }

    public ItemBuy(ItemStack itemStack, String displayName, String searchName, ItemBuy.Category category) {
        this.itemStack = itemStack.copy();
        this.displayName = displayName;
        this.searchName = searchName;
        this.category = category;

        if (this.displayName != null && !this.displayName.isEmpty()) {
            this.itemStack.set(DataComponentTypes.CUSTOM_NAME,
                   Text.literal(this.displayName)
                            .styled(style -> style.withItalic(false).withColor(0x00FFFF)));
        }
    }
   public boolean isBuy(ItemStack stack) {
      return stack != null && stack.getItem() == this.itemStack.getItem();
   }

   @Generated
   public ItemStack getItemStack() {
      return this.itemStack;
   }

   @Generated
   public String getDisplayName() {
      return this.displayName;
   }

   @Generated
   public String getSearchName() {
      return this.searchName;
   }

   @Generated
   public ItemBuy.Category getCategory() {
      return this.category;
   }

   public static enum Category {
      FUNTIME,
      HOLLYWORLD,
      ANY;

   
      private static ItemBuy.Category[] $values() {
         return new ItemBuy.Category[]{FUNTIME, HOLLYWORLD, ANY};
      }
   }
}
