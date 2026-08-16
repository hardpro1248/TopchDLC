package gg.topchdlc.vse.shutki.module.settings.impl.slider;

import com.google.gson.JsonObject;
import gg.topchdlc.vse.shutki.module.settings.Setting;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import lombok.Getter;
import net.minecraft.util.math.MathHelper;

import java.util.function.Function;

public class SliderSetting extends Setting.Basic<Float, SliderSetting> {
    public SliderSetting(String name, float defaultValue, float min, float max) {
        super(name, defaultValue);
        setValue(defaultValue);
        this.min = min;
        this.max = max;
    }

    @Getter
    private float min = -999, max = -999;
    @Getter
    private float increment = 1f;
    private Function<Float, Float> onChange;

    public void setValue(float value) {
        if (onChange != null)
            value = onChange.apply(value);
        if (min == -999 && max == -999)
            this.value = value;
        else
            this.value = MathHelper.clamp(value, min, max);
        onChange();
    }

    public SliderSetting increment(float increment) {
        this.increment = increment;
        return this;
    }

    public SliderSetting onChange(Function<Float, Float> onChange) {
        this.onChange = onChange;
        return this;
    }

    public SliderSetting set(float value) {
        setValue(value);
        return this;
    }

    public float get() {
        return value;
    }

    public int getInt() {
        return value.intValue();
    }

    public long getLong() {
        return value.longValue();
    }

    public float getSquared() {
        return value * value;
    }

    public float[] getWithMax(SliderSetting max) {
        float vMin = Math.min(get(), max.get());
        float vMax = Math.max(get(), max.get());

        this.setValue(vMin);
        max.setValue(vMax);

        return new float[]{vMin, vMax};
    }

    @Override
    public SettingRenderer<?> wrap() {
        return new SliderRenderer(this);
    }

    @Override
    public void save(JsonObject json) {
        String name = this.name.concat(":slider");

        json.addProperty(name, value);
    }

    @Override
    public void load(JsonObject json) {
        String name = this.name.concat(":slider");

        if (json.has(name)) {
            this.setValue(json.get(name).getAsFloat());
        }
    }
}