package gg.topchdlc.vse.shutki.module.settings.impl.range;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import gg.topchdlc.vse.shutki.module.settings.Setting;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import gg.topchdlc.vse.utils.math.MathUtility;

public class RangeSetting extends Setting.Basic<FloatRange, RangeSetting> {
    public final float minimumValue, maximumValue, increment;

    public RangeSetting(String name, float defaultMin, float defaultMax, float min, float max, float increment) {
        super(name, new FloatRange(defaultMin, defaultMax));
        this.minimumValue = min;
        this.maximumValue = max;
        this.increment = increment;
    }

    @Override
    public SettingRenderer<?> wrap() {
        return new RangeRenderer(this);
    }

    @Override
    public void save(JsonObject json) {
        JsonArray j = new JsonArray();
        json.add(name, j);
        j.add(getMin());
        j.add(getMax());
    }

    @Override
    public void load(JsonObject json) {
        if (json.has(name) && json.get(name).isJsonArray()) {
            JsonArray j = json.getAsJsonArray(name);
            set(j.get(0).getAsFloat(), j.get(1).getAsFloat());
        }
    }

    public float getMin() {
        return value.min;
    }

    public float getMax() {
        return value.max;
    }

    public FloatRange get() {
        return value;
    }

    public void set(FloatRange range) {
        value = range;
    }

    public void set(float min, float max) {
        value.set(min, max);
    }

    public void setMin(float min) {
        set(min, getMax());
    }

    public void setMax(float max) {
        set(getMin(), max);
    }

    public float random() {
        return MathUtility.random(value.min, value.max);
    }
}
