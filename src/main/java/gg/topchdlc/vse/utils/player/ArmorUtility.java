package gg.topchdlc.vse.utils.player;

import gg.topchdlc.MinecraftHolder;
import lombok.experimental.UtilityClass;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
/// сосиски
@UtilityClass
public class ArmorUtility implements MinecraftHolder {
    
    public float calculateArmorValue(ItemStack stack) {
        if (stack.isEmpty() || !isArmor(stack)) return 0;
        
        float value = getAttributeValue(stack, EntityAttributes.ARMOR);
        value += getAttributeValue(stack, EntityAttributes.ARMOR_TOUGHNESS);

        value += getEnchantLevel(stack, Enchantments.PROTECTION);
        value += getEnchantLevel(stack, Enchantments.BLAST_PROTECTION) * 0.5f;
        value += getEnchantLevel(stack, Enchantments.FIRE_PROTECTION) * 0.5f;
        value += getEnchantLevel(stack, Enchantments.PROJECTILE_PROTECTION) * 0.5f;
        value += getEnchantLevel(stack, Enchantments.UNBREAKING) * 0.1f;
        value += getEnchantLevel(stack, Enchantments.MENDING) * 0.2f;
        
        return value;
    }
    
    private boolean isArmor(ItemStack stack) {
        EquippableComponent eq = stack.get(DataComponentTypes.EQUIPPABLE);
        if (eq == null) return false;
        EquipmentSlot slot = eq.slot();
        return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
    }
    
    private float getAttributeValue(ItemStack stack, RegistryEntry<?> attribute) {
        AttributeModifiersComponent mods = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (mods == null) return 0;
        return (float) mods.modifiers().stream().filter(e -> e.attribute().equals(attribute)).mapToDouble(e -> e.modifier().value()).sum();
    }
    
    private int getEnchantLevel(ItemStack stack, RegistryKey<Enchantment> enchKey) {
        RegistryEntry<Enchantment> entry = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getEntry(enchKey.getValue()).orElseThrow();
        return EnchantmentHelper.getLevel(entry, stack);
    }
    
    public boolean hasCurseOfBinding(ItemStack stack) {
        return getEnchantLevel(stack, Enchantments.BINDING_CURSE) > 0;
    }
    
    public boolean isBroken(ItemStack stack) {
        return stack.isDamageable() && (double) stack.getDamage() / stack.getMaxDamage() > 0.98;
    }
    
    public int getBestArmorSlot(EquipmentSlot slot) {
        float best = -1;
        int bestSlot = -1;
        
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            EquippableComponent eq = stack.get(DataComponentTypes.EQUIPPABLE);
            
            if (eq == null || eq.slot() != slot) continue;
            if (isBroken(stack) || hasCurseOfBinding(stack)) continue;
            
            float value = calculateArmorValue(stack);
            if (value > best) {
                best = value;
                bestSlot = i;
            }
        }
        return bestSlot;
    }
    
    public boolean isBetter(ItemStack newArmor, ItemStack currentArmor) {
        if (currentArmor.isEmpty()) return true;
        if (!isArmor(newArmor) || !isArmor(currentArmor)) return false;
        return calculateArmorValue(newArmor) > calculateArmorValue(currentArmor);
    }
}
