package gg.topchdlc.api.scripts;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.scripts.api.integrate.ScriptHook;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Scripts {
    private final File dir = Client.CLIENT_DIR.resolve("scripts").toFile();

    @Getter
    private final ArrayList<Script> scripts = new ArrayList<>();

    @Getter
    private final ScriptHook scriptHook = new ScriptHook();

    public Scripts() {
        dir.mkdirs();

        Client.EVENTS.register(this);
        this.updateFolder();
    }

    public void updateFolder() {
        scripts.clear();
        if (dir.exists() && dir.isDirectory() && dir.listFiles() != null) {
            for (File f : dir.listFiles()) {
                if (f.getName().endsWith(".js")) {
                    scripts.add(new Script(f.getName(), f));
                }
            }
        }
    }

    public String[] list() {
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();

            List<String> fileNames = new ArrayList<>();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".js")) {
                        String fileName = file.getName().replace(".js", "");
                        fileNames.add(fileName);
                    }
                }
            }

            return fileNames.toArray(new String[0]);
        }
        return null;
    }

    public void RELOAD() {
        for (Script script: Client.SCRIPTS.getScripts()) {
            script.init();
        }
    }

    public void shutdown() {
        for (Script script : scripts) {
            script.close();
        }
    }

    EventBus<Event> events = event -> {
        for (Script script : scripts) {
            if (script.getEventProvider() != null && script.getScriptedModule().isEnabled()) {
                MinecraftClient.getInstance().execute(() -> {
                    try {
                        if (event instanceof Event3D e) {
                            e.stack.push();
                            Client.RENDERER.toCamera(e.stack);
                            script.getEventProvider().call(event);
                            e.stack.pop();
                        } else {
                            script.getEventProvider().call(event);
                        }
                    } catch (Exception e) {
                        ChatUtility.send(String.format("Script error %s: %s", script.getName(), e.getMessage()));
                    }
                });
            }
        }
    };

}
