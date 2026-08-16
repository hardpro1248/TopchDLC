package gg.topchdlc.vse.shutki.module.settings.impl.binder;

import com.google.gson.JsonObject;
import gg.topchdlc.vse.shutki.module.settings.Setting;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BinderSetting extends Setting.Basic<Map<Identifier, Integer>, BinderSetting> {
    public BinderSetting(String name, Map<Identifier, Integer> defaultValue) {
        super(name, new HashMap<>(defaultValue));
    }

    public BinderSetting bind(Item item, int value) {
        return bind(Registries.ITEM.getId(item), value);
    }

    public BinderSetting bind(Identifier id, int value) {
        this.value.put(id, value);
        onChange();
        return this;
    }

    public int get(Item item) {
        return get(Registries.ITEM.getId(item));
    }

    public int get(Identifier id) {
        return value.getOrDefault(id, -2);
    }

    public Set<Map.Entry<Identifier, Integer>> getEntry() {
        return value.entrySet();
    }

    @Override
    public SettingRenderer<?> wrap() {
        return new BinderRenderer(this);
    }

    @Override
    public void save(JsonObject json) {
        JsonObject j = new JsonObject();
        String name = this.name.concat(":binder");
        json.add(name, j);
        for (var entry : value.entrySet()) {
            j.addProperty(entry.getKey().toString(), entry.getValue());
        }
    }

    @Override
    public void load(JsonObject json) {
        String name = this.name.concat(":binder");
        if (json.has(name)) {
            JsonObject j = json.getAsJsonObject(name);
            for (var entry : j.entrySet()) {
                try {
                    value.put(Identifier.tryParse(entry.getKey()), entry.getValue().getAsInt());
                } catch (Exception ignored) {}
            }
        }
    }
}
