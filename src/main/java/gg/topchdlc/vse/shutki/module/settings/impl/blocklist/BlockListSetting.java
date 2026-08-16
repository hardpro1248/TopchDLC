package gg.topchdlc.vse.shutki.module.settings.impl.blocklist;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import gg.topchdlc.vse.shutki.module.settings.Setting;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

public class BlockListSetting extends Setting.Basic<Set<Identifier>, BlockListSetting> {

    public BlockListSetting(String name) {
        super(name, new HashSet<>());
        this.value = new HashSet<>();
    }

    public BlockListSetting(String name, Block... defaults) {
        super(name, new HashSet<>());
        this.value = new HashSet<>();
        for (Block block : defaults) {
            this.value.add(Registries.BLOCK.getId(block));
        }
    }

    public boolean contains(Block block) {
        return value.contains(Registries.BLOCK.getId(block));
    }

    public boolean contains(Identifier id) {
        return value.contains(id);
    }

    public void add(Block block) {
        value.add(Registries.BLOCK.getId(block));
        onChange();
    }

    public void remove(Block block) {
        value.remove(Registries.BLOCK.getId(block));
        onChange();
    }

    public Set<Identifier> getBlocks() {
        return value;
    }

    @Override
    public SettingRenderer<?> wrap() {
        return new BlockListRenderer(this);
    }

    @Override
    public void save(JsonObject json) {
        JsonArray arr = new JsonArray();
        for (Identifier id : value) {
            arr.add(id.toString());
        }
        json.add(name + ":blocklist", arr);
    }

    @Override
    public void load(JsonObject json) {
        String key = name + ":blocklist";
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