package gg.topchdlc.api.autobuy.item.other;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import gg.topchdlc.api.autobuy.item.ItemBuy;

import java.util.ArrayList;
import java.util.List;

public class PotionEffectItemBuy extends ItemBuy {
    private final List<PotionEffect> requiredEffects = new ArrayList<>();
    public PotionEffectItemBuy(ItemStack itemStack, String displayName, Category category) {
        super(itemStack, displayName, category);
    }
    public PotionEffectItemBuy addEffect(String effectId, Integer minAmplifier) {
        requiredEffects.add(new PotionEffect(effectId, minAmplifier));
        return this;
    }
    public PotionEffectItemBuy addEffectExact(String effectId, int amplifier) {
        requiredEffects.add(new PotionEffect(effectId, amplifier));
        return this;
    }
    public PotionEffectItemBuy addEffect(String effectId) {
        requiredEffects.add(new PotionEffect(effectId, null));
        return this;
    }

    @Override
    public boolean isBuy(ItemStack stack) {
        if (!super.isBuy(stack)) {
            return false;
        }
        net.minecraft.component.type.PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (potionContents != null) {
            java.util.List<net.minecraft.entity.effect.StatusEffectInstance> effects = potionContents.customEffects();
            if (effects.isEmpty()) {
            } else {
                for (PotionEffect required : requiredEffects) {
                    boolean found = false;
                    for (net.minecraft.entity.effect.StatusEffectInstance effect : effects) {
                        String effectId = effect.getEffectType().getKey().get().getValue().toString();
                        int amplifier = effect.getAmplifier();
                        if (effectId.equals(required.effectId) || 
                            effectId.equals(required.effectId.replace("minecraft:", "")) ||
                            ("minecraft:" + effectId).equals(required.effectId)) {
                            if (required.minAmplifier == null || amplifier >= required.minAmplifier) {
                                found = true;
                                break;
                            } else {
                            }
                        }
                    }
                    if (!found) {
                        return false;
                    }
                }
                return true;
            }
        }
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }
        NbtCompound nbt = customData.copyNbt();
        NbtElement effectsElement = null;
        if (nbt.contains("custom_potion_effects")) {
            effectsElement = nbt.get("custom_potion_effects");
        } else if (nbt.contains("CustomPotionEffects")) {
            effectsElement = nbt.get("CustomPotionEffects");
        } else if (nbt.contains("Potion")) {
            return false;
        } else {
            return false;
        }
        String effectsStr = effectsElement.toString();
        for (PotionEffect required : requiredEffects) {
            String searchPattern1 = "id:\"" + required.effectId + "\"";
            String searchPattern2 = "id:\"" + required.effectId.replace("minecraft:", "") + "\"";
            String searchPattern3 = "Id:\"" + required.effectId + "\"";
            String searchPattern4 = "Id:\"" + required.effectId.replace("minecraft:", "") + "\"";
            boolean found = effectsStr.contains(searchPattern1) || 
                           effectsStr.contains(searchPattern2) ||
                           effectsStr.contains(searchPattern3) ||
                           effectsStr.contains(searchPattern4);
            
            if (!found) {
                return false;
            }

            if (required.minAmplifier != null) {
                int idIndex = -1;
                String usedPattern = null;
                if (effectsStr.contains(searchPattern1)) {
                    idIndex = effectsStr.indexOf(searchPattern1);
                    usedPattern = searchPattern1;
                } else if (effectsStr.contains(searchPattern2)) {
                    idIndex = effectsStr.indexOf(searchPattern2);
                    usedPattern = searchPattern2;
                } else if (effectsStr.contains(searchPattern3)) {
                    idIndex = effectsStr.indexOf(searchPattern3);
                    usedPattern = searchPattern3;
                } else if (effectsStr.contains(searchPattern4)) {
                    idIndex = effectsStr.indexOf(searchPattern4);
                    usedPattern = searchPattern4;
                }
                if (idIndex == -1) {
                    return false;
                }
                String effectBlock = effectsStr.substring(idIndex, Math.min(idIndex + 200, effectsStr.length()));
                String ampPattern1 = "amplifier:";
                String ampPattern2 = "Amplifier:";
                String ampPattern3 = "Amp:";
                int ampIndex = effectBlock.indexOf(ampPattern1);
                String ampPattern = ampPattern1;
                if (ampIndex == -1) {
                    ampIndex = effectBlock.indexOf(ampPattern2);
                    ampPattern = ampPattern2;
                }
                if (ampIndex == -1) {
                    ampIndex = effectBlock.indexOf(ampPattern3);
                    ampPattern = ampPattern3;
                }
                if (ampIndex != -1) {
                    try {
                        String ampStr = effectBlock.substring(ampIndex + ampPattern.length());
                        StringBuilder numStr = new StringBuilder();
                        for (char c : ampStr.toCharArray()) {
                            if (Character.isDigit(c) || c == '-') {
                                numStr.append(c);
                            } else if (numStr.length() > 0) {
                                break;
                            }
                        }

                        if (numStr.length() > 0) {
                            int amplifier = Integer.parseInt(numStr.toString());
                            if (amplifier < required.minAmplifier) {
                                return false;
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
        return true;
    }
    private static class PotionEffect {
        private final String effectId;
        private final Integer minAmplifier;

        public PotionEffect(String effectId, Integer minAmplifier) {
            this.effectId = effectId;
            this.minAmplifier = minAmplifier;
        }
    }
    public static class Effects {
        public static final String SPEED = "minecraft:speed";
        public static final String SLOWNESS = "minecraft:slowness";
        public static final String HASTE = "minecraft:haste";
        public static final String MINING_FATIGUE = "minecraft:mining_fatigue";
        public static final String STRENGTH = "minecraft:strength";
        public static final String INSTANT_HEALTH = "minecraft:instant_health";
        public static final String INSTANT_DAMAGE = "minecraft:instant_damage";
        public static final String JUMP_BOOST = "minecraft:jump_boost";
        public static final String NAUSEA = "minecraft:nausea";
        public static final String REGENERATION = "minecraft:regeneration";
        public static final String RESISTANCE = "minecraft:resistance";
        public static final String FIRE_RESISTANCE = "minecraft:fire_resistance";
        public static final String WATER_BREATHING = "minecraft:water_breathing";
        public static final String INVISIBILITY = "minecraft:invisibility";
        public static final String BLINDNESS = "minecraft:blindness";
        public static final String NIGHT_VISION = "minecraft:night_vision";
        public static final String HUNGER = "minecraft:hunger";
        public static final String WEAKNESS = "minecraft:weakness";
        public static final String POISON = "minecraft:poison";
        public static final String WITHER = "minecraft:wither";
        public static final String HEALTH_BOOST = "minecraft:health_boost";
        public static final String ABSORPTION = "minecraft:absorption";
        public static final String SATURATION = "minecraft:saturation";
        public static final String GLOWING = "minecraft:glowing";
        public static final String LEVITATION = "minecraft:levitation";
        public static final String LUCK = "minecraft:luck";
        public static final String UNLUCK = "minecraft:unluck";
        public static final String SLOW_FALLING = "minecraft:slow_falling";
        public static final String CONDUIT_POWER = "minecraft:conduit_power";
        public static final String DOLPHINS_GRACE = "minecraft:dolphins_grace";
        public static final String BAD_OMEN = "minecraft:bad_omen";
        public static final String HERO_OF_THE_VILLAGE = "minecraft:hero_of_the_village";
    }
}
