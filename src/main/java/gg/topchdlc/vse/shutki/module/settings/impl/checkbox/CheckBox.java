package gg.topchdlc.vse.shutki.module.settings.impl.checkbox;

import com.google.gson.JsonObject;
import gg.topchdlc.vse.shutki.module.settings.Setting;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;

public class CheckBox extends Setting.Basic<Boolean, CheckBox> {
    public CheckBox(String name, boolean defaultValue) {
        super(name, defaultValue);
    }

    public boolean get() {
        return value;
    }

    public CheckBox set(boolean value) {
        this.value = value;
        this.onChange();
        return this;
    }

    @Override
    public SettingRenderer<?> wrap() {
        return new CheckBoxRenderer(this);
    }

    @Override
    public void save(JsonObject json) {
        String name = this.name.concat(":checkbox");

        json.addProperty(name, value);
    }

    @Override
    public void load(JsonObject json) {
        String name = this.name.concat(":checkbox");
        if (json.has(name)) {
            this.value = json.get(name).getAsBoolean();
        }
    }
}