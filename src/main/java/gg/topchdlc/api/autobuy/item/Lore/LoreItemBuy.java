package gg.topchdlc.api.autobuy.item.Lore;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import gg.topchdlc.api.autobuy.item.ItemBuy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public class LoreItemBuy extends ItemBuy {
    private final List<String> requiredLoreLines = new ArrayList<>();
    private boolean requireAll = true;

    public LoreItemBuy(ItemStack itemStack, String displayName, String searchName, Category category) {
        super(itemStack, displayName, searchName, category);
    }

    public LoreItemBuy(ItemStack itemStack, String searchName, Category category) {
        super(itemStack, searchName, category);
    }

    public LoreItemBuy(String displayName, String searchName, Category category, String skin) {
        super(Items.PLAYER_HEAD.getDefaultStack(), displayName, searchName, category);
        if (skin != null && !skin.isEmpty()) {
            applySkin(skin);
        }
        this.itemStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(displayName).styled(s -> s.withItalic(false).withColor(0x00FFFF)));
    }

    public LoreItemBuy addLoreLine(String loreLine) {
        this.requiredLoreLines.add(loreLine.toLowerCase());

        var currentLore = this.itemStack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT);
        List<Text> lines = new ArrayList<>(currentLore.lines());
        lines.add(Text.literal(loreLine).styled(s -> s.withColor(0xAAAAAA).withItalic(false)));
        this.itemStack.set(DataComponentTypes.LORE, new LoreComponent(lines));

        return this;
    }

    public LoreItemBuy addLoreLines(String... loreLines) {
        for (String line : loreLines) {
            this.addLoreLine(line);
        }
        return this;
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

    public LoreItemBuy setRequireAll(boolean requireAll) {
        this.requireAll = requireAll;
        return this;
    }

    @Override
    public boolean isBuy(ItemStack stack) {
        if (stack == null || stack.getItem() != this.itemStack.getItem()) {
            return false;
        }
        if (requiredLoreLines.isEmpty()) {
            return true;
        }
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
        if (requireAll) {
            for (String requiredLine : requiredLoreLines) {
                if (!fullLoreText.contains(requiredLine)) {
                    return false;
                }
            }
            return true;
        } else {
            for (String requiredLine : requiredLoreLines) {
                if (fullLoreText.contains(requiredLine)) {
                    return true;
                }
            }
            return false;
        }
    }
    public List<String> getRequiredLoreLines() {
        return new ArrayList<>(requiredLoreLines);
    }
    public boolean isRequireAll() {
        return requireAll;
    }
}