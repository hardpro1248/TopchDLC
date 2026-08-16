package gg.topchdlc.vse.shutki.module.settings.impl.text;

import com.google.gson.JsonObject;
import gg.topchdlc.vse.shutki.module.settings.Setting;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Getter
@Setter
public class TextSetting extends Setting.Basic<String, TextSetting> {
    public TextSetting(String name, String defaultValue) {
        super(name, defaultValue);
    }

    public String getText() {
        return value;
    }

    private String hideMask = null;

    public TextSetting setText(String value) {
        this.value = value;
        if (onChanged != null)
            onChanged.accept(value);
        return this;
    }

    @Override
    public SettingRenderer<?> wrap() {
        return new TextSettingRenderer(this);
    }

    @Override
    public void save(JsonObject json) {
        String name = this.name.concat(":text");

        json.addProperty(name, value);
    }

    @Override
    public void load(JsonObject json) {
        String name = this.name.concat(":text");

        if (json.has(name)) {
            this.value = json.get(name).getAsString();
        }
    }
}
