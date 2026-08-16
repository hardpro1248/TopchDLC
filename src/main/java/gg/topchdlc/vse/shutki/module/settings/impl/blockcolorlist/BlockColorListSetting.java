package gg.topchdlc.vse.shutki.module.settings.impl.blockcolorlist;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import gg.topchdlc.vse.shutki.module.settings.Setting;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


public class BlockColorListSetting extends Setting.Basic<Map<Identifier, Color>, BlockColorListSetting> {

    public BlockColorListSetting(String name) {
        super(name, new LinkedHashMap<>());
        this.value = new LinkedHashMap<>();
    }
    public boolean contains(Block block) {
        return value.containsKey(Registries.BLOCK.getId(block));
    }

    public Color getColor(Block block) {
        return value.getOrDefault(Registries.BLOCK.getId(block), new Color(255, 0, 0, 150));
    }

    public void add(Block block, Color color) {
        value.put(Registries.BLOCK.getId(block), color);
        onChange();
    }

    public void setColor(Identifier id, Color color) {
        if (value.containsKey(id)) {
            value.put(id, color);
            onChange();
        }
    }

    public void remove(Identifier id) {
        value.remove(id);
        onChange();
    }

    public Set<Map.Entry<Identifier, Color>> entries() {
        return value.entrySet();
    }

    public Map<Identifier, Color> getBlocks() {
        return value;
    }
    public static Item getBlockItem(Identifier blockId) {
        Block block = Registries.BLOCK.get(blockId);
        Item item = block.asItem();
        return (item instanceof BlockItem) ? item : null;
    }


    @Override
    public SettingRenderer<?> wrap() {
        return new BlockColorListRenderer(this);
    }

    @Override
    public void save(JsonObject json) {
        JsonArray arr = new JsonArray();
        for (var entry : value.entrySet()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", entry.getKey().toString());
            obj.addProperty("color", entry.getValue().getRGB());
            arr.add(obj);
        }
        json.add(name + ":blockcolorlist", arr);
    }

    @Override
    public void load(JsonObject json) {
        String key = name + ":blockcolorlist";
        if (json.has(key) && json.get(key).isJsonArray()) {
            value.clear();
            for (var el : json.getAsJsonArray(key)) {
                try {
                    JsonObject obj = el.getAsJsonObject();
                    Identifier id = Identifier.tryParse(obj.get("id").getAsString());
                    Color color = new Color(obj.get("color").getAsInt(), true);
                    if (id != null) value.put(id, color);
                } catch (Exception ignored) {}
            }
        }
    }
}
