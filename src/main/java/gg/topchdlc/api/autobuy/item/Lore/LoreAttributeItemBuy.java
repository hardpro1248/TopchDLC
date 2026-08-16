package gg.topchdlc.api.autobuy.item.Lore;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import gg.topchdlc.api.autobuy.item.ItemBuy;
import net.minecraft.util.Identifier;

import java.util.*;

public class LoreAttributeItemBuy extends ItemBuy {
    private final List<String> requiredLoreLines = new ArrayList<>();
    private boolean requireAllLore = true;
    private final Map<String, AttributeRequirement> requiredAttributes = new HashMap<>();
    public LoreAttributeItemBuy(ItemStack itemStack, String displayName, String searchName, Category category) {
        super(itemStack, displayName, searchName, category);
    }
    public LoreAttributeItemBuy(ItemStack itemStack, String searchName, Category category) {
        super(itemStack, searchName, category);
    }
    public LoreAttributeItemBuy(String displayName, String searchName, Category category, String skin) {
        super(Items.PLAYER_HEAD.getDefaultStack(), displayName, searchName, category);
        if (skin != null && !skin.isEmpty()) {
            applySkin(skin);
        }
    }
    private void applySkin(String skin) {
        ComponentChanges.Builder builder = ComponentChanges.builder();

        Property textureProperty = new Property("textures", skin);

        Multimap<String, Property> backingMap = ImmutableMultimap.of("textures", textureProperty);

        PropertyMap properties = new PropertyMap(backingMap);

        GameProfile profile = new GameProfile(UUID.randomUUID(), "", properties);

        ProfileComponent resolvableProfile = ProfileComponent.ofStatic(profile);
        builder.add(DataComponentTypes.PROFILE, resolvableProfile);

        ItemStackArgument input = new ItemStackArgument(this.itemStack.getRegistryEntry(), builder.build());

        try {
            this.itemStack = input.createStack(1, false);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ignored) {
        }
    }
    public LoreAttributeItemBuy addLoreLine(String loreLine) {
        requiredLoreLines.add(loreLine.toLowerCase());
        return this;
    }
    public LoreAttributeItemBuy addLoreLines(String... loreLines) {
        for (String line : loreLines) {
            requiredLoreLines.add(line.toLowerCase());
        }
        return this;
    }
    public LoreAttributeItemBuy setRequireAllLore(boolean requireAll) {
        this.requireAllLore = requireAll;
        return this;
    }
    public LoreAttributeItemBuy addAttribute(String attributeName, Double minValue, Double maxValue, Double exactValue, Integer operation, String slot) {
        requiredAttributes.put(attributeName, new AttributeRequirement(minValue, maxValue, exactValue, operation, slot));

       Identifier attrId = Identifier.of(attributeName);
        var attributeEntry = Registries.ATTRIBUTE.getEntry(attrId);

        if (attributeEntry.isPresent()) {
            double displayVal = (exactValue != null) ? exactValue : (minValue != null ? minValue : 0);

            int opId = (operation != null ? operation : 0);
            EntityAttributeModifier.Operation op =
                   EntityAttributeModifier.Operation.values()[Math.min(2, Math.max(0, opId))];

            EntityAttributeModifier modifier = new EntityAttributeModifier(
                    attrId,
                    displayVal,
                    op
            );

            var currentModifiers = this.itemStack.getOrDefault(
                   DataComponentTypes.ATTRIBUTE_MODIFIERS,
                   AttributeModifiersComponent.DEFAULT
            );

           AttributeModifierSlot modifierSlot = AttributeModifierSlot.ANY;
            if (slot != null) {
                if (slot.equalsIgnoreCase("mainhand")) modifierSlot = AttributeModifierSlot.MAINHAND;
                else if (slot.equalsIgnoreCase("offhand")) modifierSlot =AttributeModifierSlot.OFFHAND;
                else if (slot.equalsIgnoreCase("feet")) modifierSlot =AttributeModifierSlot.FEET;
                else if (slot.equalsIgnoreCase("legs")) modifierSlot = AttributeModifierSlot.LEGS;
                else if (slot.equalsIgnoreCase("chest")) modifierSlot = AttributeModifierSlot.CHEST;
                else if (slot.equalsIgnoreCase("head")) modifierSlot = AttributeModifierSlot.HEAD;
            }

            this.itemStack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,
                    currentModifiers.with(attributeEntry.get(), modifier, modifierSlot));
        }

        return this;
    }
    public LoreAttributeItemBuy addAttributeExact(String attributeName, double value) {
        return addAttribute(attributeName, null, null, value, null, null);
    }
    public LoreAttributeItemBuy addAttributeExact(String attributeName, double value, int operation, String slot) {
        return addAttribute(attributeName, null, null, value, operation, slot);
    }
    public LoreAttributeItemBuy addAttributeMin(String attributeName, double minValue) {
        return addAttribute(attributeName, minValue, null, null, null, null);
    }
    public LoreAttributeItemBuy addAttributeMax(String attributeName, double maxValue) {
        return addAttribute(attributeName, null, maxValue, null, null, null);
    }
    public LoreAttributeItemBuy addAttributeRange(String attributeName, double minValue, double maxValue) {
        return addAttribute(attributeName, minValue, maxValue, null, null, null);
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
        if (!requiredLoreLines.isEmpty()) {
            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            if (lore == null || lore.lines().isEmpty()) {
                return false;
            }
            StringBuilder fullLore = new StringBuilder();
            for (Text line : lore.lines()) {
                String lineText = line.getString().toLowerCase();
                fullLore.append(lineText).append(" ");
            }
            String fullLoreText = fullLore.toString();
            if (requireAllLore) {
                for (String requiredLine : requiredLoreLines) {
                    if (!fullLoreText.contains(requiredLine)) {
                        return false;
                    }
                }
            } else {
                boolean foundAny = false;
                for (String requiredLine : requiredLoreLines) {
                    if (fullLoreText.contains(requiredLine)) {
                        foundAny = true;
                        break;
                    }
                }
                if (!foundAny) {
                    return false;
                }
            }
        }

        if (!requiredAttributes.isEmpty()) {
            AttributeModifiersComponent modifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            if (modifiers == null) {
                return false;
            }
            for (Map.Entry<String, AttributeRequirement> reqEntry : requiredAttributes.entrySet()) {
                String reqAttrName = reqEntry.getKey();
                AttributeRequirement requirement = reqEntry.getValue();
                boolean found = false;
                for (AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
                    RegistryEntry<EntityAttribute> attribute = entry.attribute();
                    String attributeName = getAttributeName(attribute);
                    double value = entry.modifier().value();
                    int operation = entry.modifier().operation().getId();
                    String slot = entry.slot().asString();
                    if (!attributeName.equals(reqAttrName)) {
                        continue;
                    }
                    if (requirement.matches(value, operation, slot)) {
                        found = true;
                        break;
                    } else {
                    }
                }
                if (!found) {
                    return false;
                }
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
    public static class AttributeRequirement {
        private final Double minValue;
        private final Double maxValue;
        private final Double exactValue;
        private final Integer operation;
        private final String slot;
        
        public AttributeRequirement(Double minValue, Double maxValue, Double exactValue, Integer operation, String slot) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.exactValue = exactValue;
            this.operation = operation;
            this.slot = slot;
        }
        
        public boolean matches(double value, int itemOperation, String itemSlot) {
            if (operation != null && operation != itemOperation) {
                return false;
            }
            if (slot != null && !slot.equals(itemSlot)) {
                return false;
            }
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
    }
    public List<String> getRequiredLoreLines() {
        return new ArrayList<>(requiredLoreLines);
    }
    public Map<String, AttributeRequirement> getRequiredAttributes() {
        return requiredAttributes;
    }
}
