package gg.topchdlc.api.autobuy.item.other;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import gg.topchdlc.api.autobuy.item.ItemBuy;

import java.util.HashMap;
import java.util.Map;

public class AttributeItemBuy extends ItemBuy {
    private final Map<String, AttributeRequirement> requiredAttributes = new HashMap<>();
    
    public AttributeItemBuy(ItemStack itemStack, String displayName, String searchName, Category category) {
        super(itemStack, displayName, searchName, category);
    }
    
    public AttributeItemBuy(ItemStack itemStack, String searchName, Category category) {
        super(itemStack, searchName, category);
    }
    public AttributeItemBuy addAttribute(String attributeName, Double minValue, Double maxValue, Double exactValue) {
        requiredAttributes.put(attributeName, new AttributeRequirement(minValue, maxValue, exactValue));
        return this;
    }
    public AttributeItemBuy addAttributeExact(String attributeName, double value) {
        return addAttribute(attributeName, null, null, value);
    }
    public AttributeItemBuy addAttributeMin(String attributeName, double minValue) {
        return addAttribute(attributeName, minValue, null, null);
    }
    public AttributeItemBuy addAttributeMax(String attributeName, double maxValue) {
        return addAttribute(attributeName, null, maxValue, null);
    }
    public AttributeItemBuy addAttributeRange(String attributeName, double minValue, double maxValue) {
        return addAttribute(attributeName, minValue, maxValue, null);
    }
    
    @Override
    public boolean isBuy(ItemStack stack) {
        if (stack == null || stack.getItem() != this.itemStack.getItem()) {
            return false;
        }
        String stackName = stack.getName().getString();
        String searchNameLower = searchName.toLowerCase();
        String stackNameLower = stackName.toLowerCase();
        if (!stackNameLower.contains(searchNameLower)) {
            return false;
        }
        AttributeModifiersComponent modifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) {
            return requiredAttributes.isEmpty();
        }
        Map<String, Double> itemAttributes = new HashMap<>();
        for (AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
            RegistryEntry<EntityAttribute> attribute = entry.attribute();
            String attributeName = getAttributeName(attribute);
            double value = entry.modifier().value();
            itemAttributes.merge(attributeName, value, Double::sum);
        }
        for (Map.Entry<String, AttributeRequirement> reqEntry : requiredAttributes.entrySet()) {
            String reqAttrName = reqEntry.getKey();
            AttributeRequirement requirement = reqEntry.getValue();
            Double itemValue = itemAttributes.get(reqAttrName);
            if (itemValue == null) {
                return false;
            }
            if (!requirement.matches(itemValue)) {
                return false;
            }
        }
        return true;
    }
    private String getAttributeName(RegistryEntry<EntityAttribute> attribute) {
        if (attribute.getKey().isPresent()) {
            return attribute.getKey().get().getValue().toString();
        }
        return attribute.value().getTranslationKey();
    }
    public static class Attributes {
        public static final String MAX_HEALTH = "minecraft:generic.max_health";
        public static final String ARMOR = "minecraft:generic.armor";
        public static final String ARMOR_TOUGHNESS = "minecraft:generic.armor_toughness";
        public static final String ATTACK_DAMAGE = "minecraft:generic.attack_damage";
        public static final String ATTACK_SPEED = "minecraft:generic.attack_speed";
        public static final String MOVEMENT_SPEED = "minecraft:generic.movement_speed";
        public static final String KNOCKBACK_RESISTANCE = "minecraft:generic.knockback_resistance";
        public static final String LUCK = "minecraft:generic.luck";
    }
    private static class AttributeRequirement {
        private final Double minValue;
        private final Double maxValue;
        private final Double exactValue;
        
        public AttributeRequirement(Double minValue, Double maxValue, Double exactValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.exactValue = exactValue;
        }
        
        public boolean matches(double value) {
            if (exactValue != null) {
                return Math.abs(value - exactValue) < 0.001;
            }
            if (minValue != null && value < minValue) {
                return false;
            }
            if (maxValue != null && value > maxValue) {
                return false;
            }
            return true;
        }
        
        @Override
        public String toString() {
            if (exactValue != null) {
                return "= " + exactValue;
            }
            if (minValue != null && maxValue != null) {
                return minValue + " to " + maxValue;
            }
            if (minValue != null) {
                return ">= " + minValue;
            }
            if (maxValue != null) {
                return "<= " + maxValue;
            }
            return "any";
        }
    }
    
    public Map<String, AttributeRequirement> getRequiredAttributes() {
        return requiredAttributes;
    }
}
