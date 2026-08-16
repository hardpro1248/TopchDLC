package gg.topchdlc.api.configs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.impl.farm.AutoBuy;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.other.LogUtility;
import gg.topchdlc.vse.utils.secure.CryptUtility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Configs implements MinecraftHolder {
    private final File dir = Client.CLIENT_DIR.resolve("configs").toFile();
    private final ArrayList<String> configs = new ArrayList<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static boolean IS_LOADING = false;

    public Configs() {
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.updateFolder();
    }

    public void updateFolder() {
        configs.clear();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".gW")) {
                    configs.add(f.getName());
                }
            }
        }
    }

    public void save(String name) {
        JsonObject json = new JsonObject();

        try {
            ClientSettings.INSTANCE.save(json);
            for (Module module : Client.MODULES.getModules()) {
                module.save(json);
            }

            Client.HUD.save(json);
            Client.GLASS_GUI.save(json);

            AutoBuy.INSTANCE.save(json);

            File configFile = new File(dir, name + ".gW");
            String content = gson.toJson(json);

            try (FileOutputStream fos = new FileOutputStream(configFile)) {
                fos.write(CryptUtility.proccessXOR(content.getBytes(StandardCharsets.UTF_8)));
            }

            updateFolder();
            LogUtility.debug("Config saved: " + name);
        } catch (Exception e) {
            LogUtility.error(e, "Error while saving config: " + name);
        }
    }

    public void load(String name) {
        File configFile = new File(dir, name + ".gW");
        if (!configFile.exists()) {
            LogUtility.debug("Config not found: " + name);
            return;
        }

        IS_LOADING = true;
        try (FileInputStream fis = new FileInputStream(configFile)) {
            byte[] bytes = fis.readAllBytes();
            String decrypted = new String(CryptUtility.proccessXOR(bytes), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(decrypted).getAsJsonObject();

            ClientSettings.INSTANCE.load(json);

            for (Module module : Client.MODULES.getModules()) {
                try {
                    module.load(json);
                } catch (Exception e) {
                    LogUtility.error(e, "Error loading module: " + module.getName());
                }
            }

            Client.HUD.load(json);
            Client.GLASS_GUI.load(json);

            AutoBuy.INSTANCE.load(json);

            LogUtility.debug("Config loaded: " + name);
        } catch (Exception e) {
            LogUtility.error(e, "Error while loading config: " + name);
        } finally {
            IS_LOADING = false;
        }
    }

    public void delete(String name) {
        File configFile = new File(dir, name + ".gW");
        if (configFile.exists()) {
            configFile.delete();
            LogUtility.debug("Config deleted: " + name);
        }
        updateFolder();
    }

    public String[] list() {
        updateFolder();
        List<String> names = new ArrayList<>();
        for (String c : configs) {
            names.add(c.replace(".gW", ""));
        }
        return names.toArray(new String[0]);
    }
}