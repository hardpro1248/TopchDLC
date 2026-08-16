package gg.topchdlc.vse.shutki.other.macros;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventKey;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MacroManager implements MinecraftHolder {
    private final List<Macro> macros = new ArrayList<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File macrosFile;

    public MacroManager() {
        macrosFile = Client.CLIENT_DIR.resolve("macros.json").toFile();
        load();
        Client.EVENTS.register(this);
    }

    EventBus<Event> eventBus = event -> {
        if (event instanceof EventKey eventKey) {
            if (eventKey.action == 1) {
                for (Macro macro : macros) {
                    if (macro.getKey() == eventKey.getKey()) {
                        if (mc.player != null && mc.currentScreen == null) {
                            mc.player.networkHandler.sendChatMessage(macro.getMessage());
                        }
                        break;
                    }
                }
            }
        }
    };

    public void addMacro(int key, String message) {
        macros.removeIf(m -> m.getKey() == key);
        macros.add(new Macro(key, message));
        save();
    }

    public boolean removeMacro(int key) {
        boolean removed = macros.removeIf(m -> m.getKey() == key);
        if (removed) {
            save();
        }
        return removed;
    }

    public void clearMacros() {
        macros.clear();
        save();
    }

    public List<Macro> getMacros() {
        return new ArrayList<>(macros);
    }

    public Macro getMacro(int key) {
        return macros.stream()
                .filter(m -> m.getKey() == key)
                .findFirst()
                .orElse(null);
    }

    private void save() {
        try (FileWriter writer = new FileWriter(macrosFile)) {
            gson.toJson(macros, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void load() {
        if (!macrosFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(macrosFile)) {
            Type listType = new TypeToken<ArrayList<Macro>>(){}.getType();
            List<Macro> loaded = gson.fromJson(reader, listType);
            if (loaded != null) {
                macros.addAll(loaded);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
