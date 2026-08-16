package gg.topchdlc.vse.shutki.module.modules.impl.player;

import com.google.common.eventbus.Subscribe;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import lombok.AllArgsConstructor;
import lombok.Getter;
import gg.topchdlc.api.events.list.EventAttack;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.multienum.MultiEnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.sounds.MySoundEvents;
import gg.topchdlc.vse.utils.client.sounds.SoundUtility;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import net.minecraft.sound.SoundEvent;

import java.util.concurrent.ThreadLocalRandom;

public class HitSound extends Module {
    public final static HitSound INSTANCE = new HitSound();

    public EnumSetting<SoundMode> soundMode = enumSetting("Звук", SoundMode.HIT1);
    public SliderSetting volume = sliderSetting("Volume", 1.0f, 0.0f, 10.0f).increment(0.1f);
    public MultiEnumSetting<Sounds> soundMultiEnumSettings = multiEnumSetting("Check?", Sounds.class);

    private final SoundEvent[] hit3Variants = new SoundEvent[] {
            MySoundEvents.hit3,
            MySoundEvents.hit3V2,
            MySoundEvents.hit3V3,
            MySoundEvents.hit3V4
    };

    @AllArgsConstructor
    @Getter
    public enum SoundMode implements EnumChoice {
        HIT1("Crystal"),
        HIT2("UwU"),
        HIT3("Tap Tap");

        private final String renderName;
    }

    @AllArgsConstructor
    @Getter
    public enum Sounds implements EnumChoice {
        muteoriginal("Muteoriginal", true),
        aura("Only With Target", true);

        private final String renderName;
        private final boolean defaultEnabled;
    }

    private HitSound() {
        super("HitSound", Category.Misc, "Кастомные звуки ударов");
    }

    public boolean shouldApply() {
        if (!this.isEnabled()) return false;
        if (soundMultiEnumSettings.get(Sounds.aura)) {
            return TargetsUtility.getTarget() != null;
        }

        return true;
    }

    @Subscribe
    public void onAttack(EventAttack event) {
        if (this.shouldApply()) {
            SoundEvent soundToPlay = switch (soundMode.get()) {
                case HIT1 -> MySoundEvents.hit;
                case HIT2 -> MySoundEvents.hit2;
                case HIT3 -> hit3Variants[ThreadLocalRandom.current().nextInt(hit3Variants.length)];
            };

            SoundUtility.playSound(soundToPlay, volume.get());
        }
    }
}