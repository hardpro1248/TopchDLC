package gg.topchdlc.api.autobuy.item.donate;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import gg.topchdlc.api.autobuy.item.ItemBuy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PotionItemBuy extends ItemBuy {
    
    public PotionItemBuy(String displayName, String searchName, int color, List<StatusEffectInstance> effects, Category category) {
        super(createPotionStack(color, effects), displayName, searchName, category);
    }
    
    private static ItemStack createPotionStack(int color, List<StatusEffectInstance> effects) {
        ItemStack stack = Items.SPLASH_POTION.getDefaultStack();
        stack.set(DataComponentTypes.POTION_CONTENTS, createPotionContents(color, effects));
        return stack;
    }
    
    private static PotionContentsComponent createPotionContents(int color, List<StatusEffectInstance> effects) {
        List<RegistryEntry<net.minecraft.entity.effect.StatusEffect>> effectEntries = new ArrayList<>();
        for (StatusEffectInstance effect : effects) {
            effectEntries.add(effect.getEffectType());
        }
        return new PotionContentsComponent(Optional.empty(), Optional.of(color), effects, Optional.empty());
    }
    
    @Override
    public boolean isBuy(ItemStack stack) {
        if (!stack.isOf(Items.SPLASH_POTION) && !stack.isOf(Items.POTION)) return false;
        PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (potionContents == null) return false;
        PotionContentsComponent expectedContents = this.itemStack.get(DataComponentTypes.POTION_CONTENTS);
        if (expectedContents == null) return false;
        Optional<Integer> stackColor = potionContents.customColor();
        Optional<Integer> expectedColor = expectedContents.customColor();
        if (!stackColor.equals(expectedColor)) {
            return false;
        }
        List<StatusEffectInstance> stackEffects = potionContents.customEffects();
        List<StatusEffectInstance> expectedEffects = expectedContents.customEffects();
        if (stackEffects.size() != expectedEffects.size()) {
            return false;
        }
        for (StatusEffectInstance expectedEffect : expectedEffects) {
            boolean found = false;
            for (StatusEffectInstance stackEffect : stackEffects) {
                if (stackEffect.getEffectType().equals(expectedEffect.getEffectType()) &&
                    stackEffect.getAmplifier() == expectedEffect.getAmplifier() &&
                    stackEffect.getDuration() == expectedEffect.getDuration()) {
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
}
