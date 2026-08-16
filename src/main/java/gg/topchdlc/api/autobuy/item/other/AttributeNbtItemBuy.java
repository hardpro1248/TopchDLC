package gg.topchdlc.api.autobuy.item.other;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import gg.topchdlc.api.autobuy.item.ItemBuy;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public class AttributeNbtItemBuy extends ItemBuy {
    
    private final List<AttributeRequirement> requiredAttributes = new ArrayList<>();

    public AttributeNbtItemBuy(ItemStack itemStack, String displayName, String searchName, Category category) {
        super(itemStack, displayName, searchName, category);
    }
    public AttributeNbtItemBuy(String displayName, String searchName, Category category, String skin) {
        super(Items.PLAYER_HEAD.getDefaultStack(), displayName, searchName, category);
        if (skin != null && !skin.isEmpty()) {
            applySkin(skin);
        }
        this.itemStack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(displayName).styled(s -> s.withItalic(false)));
    }
    private void applySkin(String skin) {
        ComponentChanges.Builder builder = ComponentChanges.builder();

        Property textureProperty = new Property("textures", skin);

        Multimap<String, Property> backingMap = ImmutableMultimap.of("textures", textureProperty);

        PropertyMap properties = new PropertyMap(backingMap);
        UUID uuid = UUID.nameUUIDFromBytes(skin.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        GameProfile profile = new GameProfile(uuid, "", properties);

        ProfileComponent resolvableProfile = ProfileComponent.ofStatic(profile);
        builder.add(DataComponentTypes.PROFILE, resolvableProfile);

        ItemStackArgument input = new ItemStackArgument(this.itemStack.getRegistryEntry(), builder.build());

        try {
            this.itemStack = input.createStack(1, false);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ignored) {
        }
    }
    public AttributeNbtItemBuy addAttribute(String attributeName, double amount, int operation, String slot) {
        requiredAttributes.add(new AttributeRequirement(attributeName, amount, operation, slot));
        return this;
    }
    
    @Override
    public boolean isBuy(ItemStack stack) {
        if (stack == null || stack.getItem() != this.itemStack.getItem()) {
            return false;
        }
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }
        NbtCompound nbt = customData.copyNbt();
        if (!nbt.contains("AttributeModifiers")) {
            return false;
        }
        NbtList attributeList = nbt.getList("AttributeModifiers").orElse(null);
        if (attributeList == null || attributeList.isEmpty()) {
            return false;
        }
        for (AttributeRequirement requirement : requiredAttributes) {
            boolean found = false;
            for (int i = 0; i < attributeList.size(); i++) {
                NbtCompound attrNbt = attributeList.getCompound(i).orElse(null);
                if (attrNbt == null) {
                    continue;
                }
                String attrName = attrNbt.getString("AttributeName").orElse("");
                double amount = attrNbt.getDouble("Amount").orElse(0.0);
                int operation = attrNbt.getInt("Operation").orElse(-1);
                String slot = attrNbt.getString("Slot").orElse("");
                if (requirement.matches(attrName, amount, operation, slot)) {
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                return false;
            }
        }
        
        return true;
    }
    private static class AttributeRequirement {
        private final String attributeName;
        private final double amount;
        private final int operation;
        private final String slot;
        public AttributeRequirement(String attributeName, double amount, int operation, String slot) {
            this.attributeName = attributeName;
            this.amount = amount;
            this.operation = operation;
            this.slot = slot;
        }
        public boolean matches(String attrName, double attrAmount, int attrOperation, String attrSlot) {
            if (!this.attributeName.equals(attrName)) {
                return false;
            }
            
            if (Math.abs(this.amount - attrAmount) > 0.001) {
                return false;
            }
            
            if (this.operation != attrOperation) {
                return false;
            }
            
            if (!this.slot.equals(attrSlot)) {
                return false;
            }
            
            return true;
        }
    }
}
