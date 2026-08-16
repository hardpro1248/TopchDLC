package gg.topchdlc.api.autobuy.item.donate;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import gg.topchdlc.api.autobuy.item.ItemBuy;

import java.util.ArrayList;
import java.util.List;

public class DonateItemBuy extends ItemBuy {
    
    private final List<String> requiredLoreLines = new ArrayList<>();
    private boolean requireAll = false;

    public DonateItemBuy(ItemStack itemStack, String searchName, Category category) {
        super(itemStack, searchName, category);
    }
    
    public DonateItemBuy(ItemStack itemStack, String displayName, String searchName, Category category) {
        super(itemStack, displayName, searchName, category);
    }
    public DonateItemBuy addLoreLine(String loreLine) {
        requiredLoreLines.add(loreLine.toLowerCase());

        var currentLore = this.itemStack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT);
       List<Text> lines = new ArrayList<>(currentLore.lines());
        lines.add(Text.literal(loreLine).styled(s -> s.withColor(0xAAAAAA).withItalic(false)));

        this.itemStack.set(DataComponentTypes.LORE, new LoreComponent(lines));
        return this;
    }
    public DonateItemBuy addLoreLines(String... loreLines) {
        for (String line : loreLines) {
            requiredLoreLines.add(line.toLowerCase());
        }
        return this;
    }

    public DonateItemBuy setRequireAll(boolean requireAll) {
        this.requireAll = requireAll;
        return this;
    }
    @Override
    public boolean isBuy(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        boolean itemTypeMatches = stack.getItem() == this.itemStack.getItem();
        if (requiredLoreLines.isEmpty()) {
            return itemTypeMatches;
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
