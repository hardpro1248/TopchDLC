package gg.topchdlc.vse.rotation.all.impl.kitty;

import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.all.RotationChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.range.RangeSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import gg.topchdlc.vse.utils.math.MathUtility;

public class JitterRotation extends RotationChoice {
    public JitterRotation() {
        super("Jitter");
        toggleable(false);
    }

    public final RangeSetting yawJitter = rangeSetting("Yaw", 0, 5, 0, 180, 0.1f);
    public final RangeSetting pitchJitter = rangeSetting("Pitch", 0, 5, 0, 90, 0.1f);

    public final SliderSetting multiplier = sliderSetting("Multiplier", 1, 0, 10).increment(0.1f);
    public final CheckBox multiplyNonDistance = checkbox("Multiply Dist", true);

    @Override
    public Angle calculate(Angle current, Angle target) {
        float jyaw = yawJitter.getMax() <= 0f ? 0f : MathUtility.random(yawJitter.getMin(), yawJitter.getMax());
        jyaw = MathUtility.random(-jyaw, jyaw);

        float jpitch = pitchJitter.getMax() <= 0f ? 0f : MathUtility.random(pitchJitter.getMin(), pitchJitter.getMax());
        jpitch = MathUtility.random(-jpitch, jpitch);

        if (multiplyNonDistance.get() && TargetsUtility.getTarget() != null) {
            float dist = 1f / TargetsUtility.getTarget().distanceTo(mc.player);
            jyaw *= dist;
            jpitch *= dist;
        }

        jyaw *= multiplier.get();
        jpitch *= multiplier.get();

        return new Angle(target.getYaw() + jyaw, target.getPitch() + jpitch);
    }
}
