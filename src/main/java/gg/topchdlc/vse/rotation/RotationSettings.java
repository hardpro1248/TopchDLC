package gg.topchdlc.vse.rotation;

import gg.topchdlc.Client;
import gg.topchdlc.vse.rotation.all.Rotation;
import gg.topchdlc.vse.rotation.all.RotationChoice;
import gg.topchdlc.vse.rotation.all.impl.*;
import gg.topchdlc.vse.rotation.all.impl.kitty.JitterRotation;
import gg.topchdlc.vse.rotation.all.impl.kitty.LimitRotation;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.ChoiceSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;

public class RotationSettings extends Group implements Rotation {
    public final int rotationPriority;
    
    public final ChoiceSetting<RotationChoice> mode;
    public final EnumSetting<MovementCorrection> correction;
    public final CheckBox backroation;
    public final SliderSetting timeout;
    public final CheckBox shouldLerp;
    public final CheckBox Customization;
    public final JitterRotation jitter;
    public final LimitRotation limit;

    public RotationSettings(String name, int priority) {
        super(name);
        this.rotationPriority = priority;
        
        mode = choiceSetting("Mode", 0, new SlothRotation(), DefaultRotation.INSTANCE,new Pereliv(),new HolyWorld(),new Builder(),new InterpolationRotation(),new UniversalRotation(),new SnapRotation(), new FTSnap(),new NRotation());
        correction = enumSetting("Correction", MovementCorrection.STRICT);
        backroation = checkbox("BackRatiton",false);
        timeout = sliderSetting("Timeout", 1, 1, 20);
        shouldLerp = checkbox("Smooth reset", false);
        Customization = checkbox("Customization", false);
        jitter = add(new JitterRotation());
        limit = add(new LimitRotation());
        jitter.visible(Customization::get);
        limit.visible(Customization::get);
    }

    public void rotate(Angle targetAngle) {
        boolean smoothReset = shouldLerp.get() ;
        Client.ROTATION.rotate(this, targetAngle, timeout.getInt(), smoothReset, correction.get(), this.rotationPriority);
    }

    @Override
    public Angle calculate(Angle current, Angle target) {
        target = mode.get().calculate(current, target);
        if (limit.isEnabled())
            target = limit.calculate(current, target);

        if (jitter.isEnabled())
            target = jitter.calculate(current, target);

        return target;
    }
}
