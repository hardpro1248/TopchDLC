package gg.topchdlc.vse.rotation.all.impl;

import gg.topchdlc.Client;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.all.FactorRotationChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.range.RangeSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import net.minecraft.util.math.MathHelper;

public class InterpolationRotation extends FactorRotationChoice {

    private static final float DEFAULT_YAW_MIN = 80f;
    private static final float DEFAULT_YAW_MAX = 85f;
    private static final float DEFAULT_PITCH_MIN = 20f;
    private static final float DEFAULT_PITCH_MAX = 25f;
    private static final float DEFAULT_DIR_MIN = 95f;
    private static final float DEFAULT_DIR_MAX = 100f;
    private static final float DEFAULT_MID = 0.35f;

    private final EnumSetting<SettingsMode> settingsMode;
    
    public InterpolationRotation() {
        super("Interpolation");

        this.settingsMode = enumSetting("Settings Mode", SettingsMode.Default);
    }

    private boolean isCustomMode() {
        return settingsMode.is(SettingsMode.Custom);
    }
    private boolean isDefaultMode() {
        return settingsMode.is(SettingsMode.Default);
    }
    final RangeSetting yaw = rangeSetting("Yaw%", DEFAULT_YAW_MIN, DEFAULT_YAW_MAX, 1, 100, 0.1f)
            .visible(this::isCustomMode);
    final RangeSetting pitch = rangeSetting("Pitch%", DEFAULT_PITCH_MIN, DEFAULT_PITCH_MAX, 1, 100, 0.1f)
            .visible(this::isCustomMode);
    final RangeSetting direction = rangeSetting("Direction%", DEFAULT_DIR_MIN, DEFAULT_DIR_MAX, 0, 100, 0.1f)
            .visible(this::isCustomMode);
    final SliderSetting mid = sliderSetting("Mid", DEFAULT_MID, 0, 1)
            .increment(0.01f)
            .visible(this::isCustomMode);
    private float sig(float x) {
        return 1f / (1f + (float)Math.exp(-0.5f * (x - 0.3f)));
    }

    private float bez(float a, float b, float x) {
        return (1f - x) * (1f - x) * a + 2f * (1f - x) * x * 1f + x * x * b;
    }

    @Override
    public Angle getFactors(Angle current, Angle target) {
        Angle delta = current.delta(target);
        Angle prev = Client.ROTATION.getPrevRotate();
        float yawValue = isDefaultMode() ? (DEFAULT_YAW_MIN + DEFAULT_YAW_MAX) / 2f : yaw.random();
        float pitchValue = isDefaultMode() ? (DEFAULT_PITCH_MIN + DEFAULT_PITCH_MAX) / 2f : pitch.random();
        float dirValue = isDefaultMode() ? (DEFAULT_DIR_MIN + DEFAULT_DIR_MAX) / 2f : direction.random();
        float midValue = isDefaultMode() ? DEFAULT_MID : mid.get();
        
        float dir = prev != null ? MathHelper.clamp(prev.angleTo(target), 0, 1) * (dirValue / 100f) : 0f;
        float yaw2 = yawValue / 100f;
        float pitch1 = pitchValue / 100f;

        float yaw1 = factor(Math.abs(delta.yaw), MathHelper.clamp(yaw2, 0, 1), dir, midValue);
        float pitch2 = factor(Math.abs(delta.pitch), MathHelper.clamp(pitch1, 0, 1), dir, midValue);

        return new Angle(yaw1 * Math.abs(delta.yaw), pitch2 * Math.abs(delta.pitch));
    }

    private float factor(float delta, float speed, float dir, float midValue) {
        float x = MathHelper.clamp(delta / 180f, 0, 1);
        return x > midValue ? bez(0.05f, 1f, 1f - x) * speed : MathHelper.clamp(speed + dir, 0, 1) * sig(x);
    }
}
