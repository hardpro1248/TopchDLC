package gg.topchdlc.api.autobuy.item.Lore;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import gg.topchdlc.api.autobuy.enchantes.Enchant;
import gg.topchdlc.api.autobuy.item.ItemBuy;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;


public class LoreEnchantItemBuy extends ItemBuy {
    protected final List<String> requiredLoreLines = new ArrayList<>();
    protected final ArrayList<Enchant> enchants = new ArrayList<>();
    public LoreEnchantItemBuy(ItemStack itemStack, String displayName, String searchName, Category category) {
        super(itemStack, displayName, searchName, category);
    }
    public LoreEnchantItemBuy(ItemStack itemStack, String searchName, Category category) {
        super(itemStack, searchName, category);
    }
    @Override
    public boolean isBuy(ItemStack stack) {
        if (!super.isBuy(stack)) {
            return false;
        }
        List<String> lore = getLoreLines(stack);
        for (String requiredLine : requiredLoreLines) {
            boolean found = false;
            for (String loreLine : lore) {
                if (loreLine.toLowerCase().contains(requiredLine.toLowerCase())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        for (Enchant enchant : this.enchants) {
            if (!enchant.isEnchanted(stack)) {
                return false;
            }
        }
        return true;
    }

    public LoreEnchantItemBuy addLoreLine(String line) {
        this.requiredLoreLines.add(line);

        var currentLore = this.itemStack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT);
        List<Text> lines = new ArrayList<>(currentLore.lines());
        lines.add(Text.literal(line).styled(s -> s.withColor(0xAAAAAA).withItalic(false)));
        this.itemStack.set(DataComponentTypes.LORE, new LoreComponent(lines));

        return this;
    }

    public LoreEnchantItemBuy addEnchant(Enchant enchant) {
        this.enchants.add(enchant);

        this.itemStack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        var currentLore = this.itemStack.getOrDefault(DataComponentTypes.LORE,LoreComponent.DEFAULT);
        List<Text> lines = new ArrayList<>(currentLore.lines());

        String enchantName = enchant.getName();
        String line = enchantName + " " + enchant.getMinLevel();

        lines.add(0, Text.literal(line).styled(s -> s.withColor(0xAAAAAA).withItalic(false)));

        this.itemStack.set(DataComponentTypes.LORE, new LoreComponent(lines));

        return this;
    }

    public LoreEnchantItemBuy addLoreLines(String... lines) {
        for (String line : lines) {
            this.requiredLoreLines.add(line);
        }
        return this;
    }
    public ArrayList<Enchant> getEnchants() {
        return this.enchants;
    }

    private List<String> getLoreLines(ItemStack stack) {
        List<String> loreLines = new ArrayList<>();
        var loreComponent = stack.get(DataComponentTypes.LORE);
        if (loreComponent != null) {
            for (Text line : loreComponent.lines()) {
                loreLines.add(line.getString());
            }
        }
        return loreLines;
    }
}
