package gg.topchdlc.vse.utils.attack;

import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.range.RangeSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.mixin.accessor.ILivingEntity;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.math.TimeUtility;

public class AttackUtility extends Group {
    final TimeUtility time = new TimeUtility();
    static final float BASE_TIME = 1.5f, MIN_COOLDOWN = 0.944f, MIN_COOLDOWN_ELYTRA = 0.99f;
    int falloff;

    private void reset() {
        falloff = 0;
    }

    final CheckBox hurtTime = checkbox("Hurt time", false).desc("Attack relative to target hurt time");
    final SliderSetting maxHurtTime = sliderSetting("Max hurt time", 6, 0, 10).visible(hurtTime::get).desc("Target hurt time to attack");
    final EnumSetting<Mode> mode = enumSetting("Mode", Mode.Modern).desc("Mode of clicking");
    final SliderSetting ticks = sliderSetting("Min attack ticks", 9, 0, 20).increment(1).visible(() -> mode.is(Mode.Modern)).desc("Minimal required ticks since last attack");
    final RangeSetting ppt = rangeSetting("Packets", 1, 1, 0, 100, 1).visible(() -> mode.is(Mode.Old)).onChanged(ignored -> this.reset()).desc("How many attack packets to send");
    final EnumSetting<Distribution> distribution = enumSetting("Distribution", Distribution.Linear).visible(() -> mode.is(Mode.Old)).onChanged(ignored -> this.reset()).desc("Clicks distribution");

    public AttackUtility() {
        super("Attack settings");
    }

    @Override
    public String getLangKey() {
        return "topchdlc.AttackUtility";
    }

    public int getClicks() {
        long delay = 0;
        if (!time.reached(delay, true)) return 0;
        if (hurtTime.get() && TargetsUtility.getTarget() != null && TargetsUtility.getTarget().hurtTime > maxHurtTime.getInt()) return 0;

        return switch (mode.get()) {
            case Modern -> (mc.player.getAttackCooldownProgress(BASE_TIME) > (mc.player.isGliding() ? MIN_COOLDOWN_ELYTRA : MIN_COOLDOWN) && ((ILivingEntity)mc.player).client$lastAttackedTicks() > ticks.getInt()) ? 1 : 0;
            case Old -> {
                int min = (int)ppt.getMin();
                int max = (int)ppt.getMax();
                if (max == 0) yield 0;

                yield switch (distribution.get()) {
                    case Linear -> MathUtility.random(min, max);
                    case Falloff -> falloff <= 0 ? falloff = MathUtility.random(min, max) : --falloff;
                };
            }
        };
    }

    public float getProgress() {
        return switch (mode.get()) {
            case Modern -> {
                int attackTicks = ((ILivingEntity)mc.player).client$lastAttackedTicks();
                if (ticks.get() > 0) yield (float) attackTicks / ticks.get();
                else
                    yield mc.player.getAttackCooldownProgress(BASE_TIME) - (1 - (mc.player.isGliding() ? MIN_COOLDOWN_ELYTRA : MIN_COOLDOWN));
            }
            case Old -> ppt.getMax() > 0 ? 1 : 0;
        };
    }

    enum Mode implements EnumChoice {
        Modern, Old;

        @Override
        public String getLangClassName() {
            return "AttackUtility.Mode";
        }
    }

    enum Distribution implements EnumChoice {
        Linear, Falloff;

        @Override
        public String getLangClassName() {
            return "AttackUtility.Distribution";
        }
    }
}
