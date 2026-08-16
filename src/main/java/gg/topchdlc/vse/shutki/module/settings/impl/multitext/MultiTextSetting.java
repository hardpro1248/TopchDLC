package gg.topchdlc.vse.shutki.module.settings.impl.multitext;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import gg.topchdlc.vse.shutki.module.settings.Setting;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;

import java.util.ArrayList;
import java.util.List;

public class MultiTextSetting extends Setting.Basic<List<String>, MultiTextSetting> {
    public MultiTextSetting(String name, String... defaults) {
        super(name, new ArrayList<>(List.of(defaults)));
    }

    @Override
    public SettingRenderer<?> wrap() {
        return new MultiTextRenderer(this);
    }

    public List<String> get() {
        return value;
    }

    public void add() {
        value.add("");
    }

    public void remove(int index) {
        if (index < value.size())
            value.remove(index);
    }

    @Override
    public void save(JsonObject json) {
        JsonArray array = new JsonArray();
        json.add(name, array);
        for (var element : this.value) {
            array.add(element);
        }
    }

    @Override
    public void load(JsonObject json) {
        if (json.has(name) && json.get(name) instanceof JsonArray array) {
            for (var element : array) {
                value.add(element.getAsString());
            }
        }
    }
}
