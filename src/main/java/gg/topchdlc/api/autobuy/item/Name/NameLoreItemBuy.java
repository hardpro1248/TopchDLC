package gg.topchdlc.api.autobuy.item.Name;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import gg.topchdlc.api.autobuy.item.ItemBuy;

import java.util.ArrayList;
import java.util.List;

public class NameLoreItemBuy extends ItemBuy {
    private final String nameContains;
    private final List<String> loreContains;

    public NameLoreItemBuy(ItemStack itemStack, String displayName, String nameContains, List<String> loreContains, Category category) {
        super(itemStack, displayName, category);
        this.nameContains = nameContains.toLowerCase();

        this.loreContains = new ArrayList<>();
        for (String lore : loreContains) {
            this.loreContains.add(lore.toLowerCase());
        }

        List<Text> lines = new ArrayList<>();
        for (String line : loreContains) {
            lines.add(Text.literal(line).styled(s -> s.withColor(0xAAAAAA).withItalic(false)));
        }
        this.itemStack.set(DataComponentTypes.LORE, new LoreComponent(lines));
    }

    public NameLoreItemBuy(ItemStack itemStack, String displayName, String nameContains, String loreContains, Category category) {
        this(itemStack, displayName, nameContains, List.of(loreContains), category);
    }

    @Override
    public boolean isBuy(ItemStack stack) {
        if (!super.isBuy(stack)) {
            return false;
        }
        String itemName = stack.getName().getString().toLowerCase();
        if (!itemName.contains(nameContains)) {
            return false;
        }
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null || lore.lines().isEmpty()) {
            return false;
        }
        StringBuilder fullLore = new StringBuilder();
        for (Text line : lore.lines()) {
            fullLore.append(line.getString().toLowerCase()).append(" ");
        }
        String fullLoreText = fullLore.toString();
        for (String required : loreContains) {
            if (!fullLoreText.contains(required)) {
                return false;
            }
        }

        return true;
    }

    public String getNameContains() {
        return nameContains;
    }

    public List<String> getLoreContains() {
        return loreContains;
    }
}