package gg.topchdlc.api.autobuy.item.hollyworld;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

import java.util.UUID;

public class HolyWorldData {

    public static ItemStack createSphere(String name, String texture, String sphereName, String requiredEffects) {
        ItemStack stack = Items.PLAYER_HEAD.getDefaultStack();

        if (texture != null && !texture.isEmpty()) {
            ComponentChanges.Builder builder = ComponentChanges.builder();

            Multimap<String, Property> map = HashMultimap.create();
            map.put("textures", new Property("textures", texture));
            PropertyMap propertyMap = new PropertyMap(map);
            UUID uuid = UUID.nameUUIDFromBytes(texture.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            GameProfile profile = new GameProfile(uuid, "", propertyMap);

            ProfileComponent resolvableProfile = ProfileComponent.ofStatic(profile);

            builder.add(DataComponentTypes.PROFILE, resolvableProfile);
            ItemStackArgument input = new ItemStackArgument(stack.getRegistryEntry(), builder.build());
            try {
                stack = input.createStack(1, false);
            } catch (CommandSyntaxException ignored) {
            }
        }

        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("HolyWorldItem", true);
        nbt.putBoolean("HolyWorldSphere", true);
        nbt.putInt("HideFlags", 127);
        nbt.putBoolean("Unbreakable", true);

        if (sphereName != null && !sphereName.isEmpty()) {
            nbt.putString("sphereName", sphereName);
        }

        if (requiredEffects != null && !requiredEffects.isEmpty()) {
            nbt.putString("requiredEffects", requiredEffects);
        }

        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(name).styled(s -> s.withItalic(false)));
        return stack;
    }

    public static ItemStack createBackpack(String name, Item material, String backpackType) {
        ItemStack stack = material.getDefaultStack();

        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("HolyWorldItem", true);
        nbt.putBoolean("HolyWorldBackpack", true);
        nbt.putString("backpackType", backpackType);
        nbt.putInt("HideFlags", 127);
        nbt.putBoolean("Unbreakable", true);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(name).styled(s -> s.withItalic(false)));
        return stack;
    }

    public static ItemStack createPyrotechnic(String name, Item material, String pyrotechnicType) {
        ItemStack stack = material.getDefaultStack();
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("HolyWorldItem", true);
        nbt.putBoolean("HolyWorldPyrotechnic", true);
        nbt.putString("pyrotechnicType", pyrotechnicType);
        nbt.putInt("HideFlags", 127);
        nbt.putBoolean("Unbreakable", true);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(name).styled(s -> s.withItalic(false)));
        return stack;
    }


    public static ItemStack createKringe(String name, Item material, String kringeType) {
        ItemStack stack = material.getDefaultStack();
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("HolyWorldItem", true);
        nbt.putBoolean("HolyWorldKringe", true);
        nbt.putString("kringeType", kringeType);
        nbt.putInt("HideFlags", 127);
        nbt.putBoolean("Unbreakable", true);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(name).styled(s -> s.withItalic(false)));
        return stack;
    }
    public static ItemStack createRune(String name, Item material, String runeId) {
        ItemStack stack = material.getDefaultStack();
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("HolyWorldItem", true);
        nbt.putBoolean("HolyWorldRune", true);
        nbt.putString("runeId", runeId);
        nbt.putInt("HideFlags", 127);
        nbt.putBoolean("Unbreakable", true);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(name).styled(s -> s.withItalic(false)));
        return stack;
    }

    public static ItemStack createKringeEffect(String name, Item material, String effectType) {
        ItemStack stack = material.getDefaultStack();
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("HolyWorldItem", true);
        nbt.putBoolean("HolyWorldKringeEffect", true);
        nbt.putString("effectType", effectType);
        nbt.putInt("HideFlags", 127);
        nbt.putBoolean("Unbreakable", true);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(name).styled(s -> s.withItalic(false)));
        return stack;
    }

    public static ItemStack createExpBottle(String name, int expValue) {
        ItemStack stack = Items.EXPERIENCE_BOTTLE.getDefaultStack();
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("HolyWorldItem", true);
        nbt.putBoolean("HolyWorldExpBottle", true);
        nbt.putInt("holy-exp-bottle-value", expValue);
        nbt.putInt("HideFlags", 127);
        nbt.putBoolean("Unbreakable", true);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(name).styled(s -> s.withItalic(false)));
        return stack;
    }

    public static ItemStack createSphereShard(String name, String texture) {
        ItemStack stack = Items.PLAYER_HEAD.getDefaultStack();
        if (texture != null && !texture.isEmpty()) {
            ComponentChanges.Builder builder = ComponentChanges.builder();

            Multimap<String, Property> map = HashMultimap.create();
            map.put("textures", new Property("textures", texture));
            PropertyMap propertyMap = new PropertyMap(map);

            GameProfile profile = new GameProfile(UUID.fromString("9afca6b1-556f-3cf9-b349-3886d7d2c53b"), "", propertyMap);

            ProfileComponent resolvableProfile = ProfileComponent.ofStatic(profile);

            builder.add(DataComponentTypes.PROFILE, resolvableProfile);
            ItemStackArgument input = new ItemStackArgument(stack.getRegistryEntry(), builder.build());
            try {
                stack = input.createStack(1, false);
            } catch (CommandSyntaxException ignored) {
            }
        }
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("HolyWorldItem", true);
        nbt.putBoolean("HolyWorldSphereShard", true);
        nbt.putInt("HideFlags", 127);
        nbt.putBoolean("Unbreakable", true);
        nbt.putInt("sphereEffect", 1);
        NbtCompound publicBukkitValues = new NbtCompound();
        publicBukkitValues.putByte("magicspheres:burned-sphere-shard", (byte)1);
        nbt.put("PublicBukkitValues", publicBukkitValues);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(name).styled(s -> s.withItalic(false)));
        return stack;
    }

    public static ItemStack createSimple(Item material) {
        ItemStack stack = material.getDefaultStack();
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("HolyWorldItem", true);
        nbt.putInt("HideFlags", 127);
        nbt.putBoolean("Unbreakable", true);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

        return stack;
    }
}