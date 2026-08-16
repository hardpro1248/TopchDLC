package gg.topchdlc.vse.shutki.module.settings.impl.itemlist;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import gg.topchdlc.vse.shutki.module.settings.Setting;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

/**
 * Create by daun kvass
 */
public class ItemListSetting extends Setting.Basic<Set<Identifier>, ItemListSetting> {

    public ItemListSetting(String name) {
        super(name, new HashSet<>());
        this.value = new HashSet<>();
    }

    public ItemListSetting(String name, Item... defaults) {
        super(name, new HashSet<>());
        this.value = new HashSet<>();
        for (Item item : defaults) {
            this.value.add(Registries.ITEM.getId(item));
        }
    }


    public boolean contains(Item item) {
        return value.contains(Registries.ITEM.getId(item));
    }

    public boolean contains(Identifier id) {
        return value.contains(id);
    }

    public void add(Item item) {
        value.add(Registries.ITEM.getId(item));
        onChange();
    }

    public void remove(Item item) {
        value.remove(Registries.ITEM.getId(item));
        onChange();
    }

    public Set<Identifier> getItems() {
        return value;
    }


    @Override
    public SettingRenderer<?> wrap() {
        return new ItemListRenderer(this);
    }

    @Override
    public void save(JsonObject json) {
        JsonArray arr = new JsonArray();
        for (Identifier id : value) {
            arr.add(id.toString());
        }
        json.add(name + ":itemlist", arr);
    }

    @Override
    public void load(JsonObject json) {
        String key = name + ":itemlist";
        if (json.has(key) && json.get(key).isJsonArray()) {
            value.clear();
            for (var el : json.getAsJsonArray(key)) {
                try {
                    Identifier id = Identifier.tryParse(el.getAsString());
                    if (id != null) value.add(id);
                } catch (Exception ignored) {}
            }
        }
    }
}
