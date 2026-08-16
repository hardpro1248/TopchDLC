package gg.topchdlc.vse.shutki.module.modules.impl.misc.nottification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum SpecialPotionType {
    // KILLER(13369344, "§4§l[★] §c§lЗелье Ассасин", Arrays.asList(
    //        new PotionEffectData(StatusEffects.RESISTANCE.value(), 3 * 60, 0),
   //         new PotionEffectData(StatusEffects.STRENGTH.value(), 90, 3)
   // ), Arrays.asList(12582912, 15007744, 14024704)),
    SVYATAYA_VODA(16777215, "§f§l[★] §f§lСвятая вода", Arrays.asList(
            new PotionEffectData(StatusEffects.REGENERATION.value(), 60, 1),
            new PotionEffectData(StatusEffects.INVISIBILITY.value(), 10 * 60, 1),
            new PotionEffectData(StatusEffects.INSTANT_HEALTH.value(), 1, 1)
    ), Arrays.asList(16777215, 16777200, 15790335)),
    ZELIE_GNEVA(16737280, "§4§l[★] §c§lЗелье Гнева", Arrays.asList(
            new PotionEffectData(StatusEffects.STRENGTH.value(), 30, 4),
            new PotionEffectData(StatusEffects.SLOWNESS.value(), 30, 3)
    ), Arrays.asList(16737280, 16746887, 14423100)),
    ZELIE_PALLADINA(16766720, "§e§l[★] §6§lЗелье Палладина", Arrays.asList(
            new PotionEffectData(StatusEffects.RESISTANCE.value(), 10 * 60, 0),
            new PotionEffectData(StatusEffects.FIRE_RESISTANCE.value(), 10 * 60, 0),
            new PotionEffectData(StatusEffects.HEALTH_BOOST.value(), 60, 2),
            new PotionEffectData(StatusEffects.INVISIBILITY.value(), 15 * 60, 2)
    ), Arrays.asList(16766720, 16753920, 16759567)),
    ZELIE_ASSASSINA(9109504, "§4§l[★] §8§lЗелье Ассасина", Arrays.asList(
            new PotionEffectData(StatusEffects.STRENGTH.value(), 60, 3),
            new PotionEffectData(StatusEffects.SPEED.value(), 5 * 60, 2),
            new PotionEffectData(StatusEffects.HASTE.value(), 60, 0),
            new PotionEffectData(StatusEffects.INSTANT_DAMAGE.value(), 1, 1)
    ), Arrays.asList(9109504, 8388608, 9116186)),
    ZELIE_RADIACII(16776960, "§e§l[★] §a§lЗелье Радиации", Arrays.asList(
            new PotionEffectData(StatusEffects.POISON.value(), 60, 1),
            new PotionEffectData(StatusEffects.WITHER.value(), 60, 1),
            new PotionEffectData(StatusEffects.SLOWNESS.value(), 90, 2),
            new PotionEffectData(StatusEffects.HUNGER.value(), 60, 4),
            new PotionEffectData(StatusEffects.GLOWING.value(), 2 * 60, 0)
    ), Arrays.asList(16776960, 16777011, 16777062)),
    SNOTVORNOE(9662683, "§5§l[★] §d§lСнотворное", Arrays.asList(
            new PotionEffectData(StatusEffects.WEAKNESS.value(), 90, 1),
            new PotionEffectData(StatusEffects.MINING_FATIGUE.value(), 10, 1),
            new PotionEffectData(StatusEffects.WITHER.value(), 90, 2),
            new PotionEffectData(StatusEffects.BLINDNESS.value(), 10, 0)
    ), Arrays.asList(9662683, 9055202, 9699539)),
    HLOPUSHKA(16716947, "§d§l[★] §5§lХлопушка", Arrays.asList(
            new PotionEffectData(StatusEffects.SLOWNESS.value(), 10, 8),
            new PotionEffectData(StatusEffects.SPEED.value(), 20, 4),
            new PotionEffectData(StatusEffects.BLINDNESS.value(), 5, 0),
            new PotionEffectData(StatusEffects.GLOWING.value(), 3 * 60, 0)
    ), Arrays.asList(16716947, 16738740, 14381203));

    private final Integer baseColor;
    private final String displayName;
    private final List<PotionEffectData> effects;
    private final List<Integer> colorVariations;

    @Getter
    @RequiredArgsConstructor
    public static class PotionEffectData {
        private final StatusEffect effect;
        private final Integer durationSeconds;
        private final Integer amplifier;

        public Integer getDurationTicks() {
            return durationSeconds * 20;
        }
    }
}