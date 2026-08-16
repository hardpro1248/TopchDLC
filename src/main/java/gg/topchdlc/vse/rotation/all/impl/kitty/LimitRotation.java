package gg.topchdlc.vse.rotation.all.impl.kitty;

import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.all.FactorRotationChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;

public class LimitRotation extends FactorRotationChoice {
    public LimitRotation() {
        super("Limit");
    }

    public final SliderSetting yawSpeed = sliderSetting("Yaw speed", 180, 0, 180).increment(.1f);
    public final SliderSetting pitchSpeed = sliderSetting("Pitch speed", 180, 0, 180).increment(.1f);

    @Override
    public Angle getFactors(Angle current, Angle target) {
        return new Angle(yawSpeed.get(), pitchSpeed.get());
    }
}
