package gg.topchdlc.vse.shutki.other.commands.impl;

import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.Client;
import gg.topchdlc.vse.shutki.other.commands.Command;
import gg.topchdlc.vse.utils.client.text.ChatUtility;

import java.awt.*;
import java.io.File;


/**
 * Create by daun kvass
 */
public class ConfigCommand extends Command {
    public static ConfigCommand INSTANCE = new ConfigCommand();

    public ConfigCommand() {
        super("cfg", "конфиги", "load", "save", "list", "dir", "remove", "clear","reset");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            error("usage .cfg <load | save | list | dir | reset>");
            return;
        }
        String path = args[1];

        switch (path) {
            case "save" -> {

                if (args.length == 3) {
                    Client.CONFIG.save(args[2]);
                    ChatUtility.send("config saved");
                } else {
                    error("usage .cfg save <name>");
                }
            }
            case "load" -> {

                if (args.length == 3) {
                    Client.CONFIG.load(args[2]);
                    ChatUtility.send("config loaded");
                } else {
                    error("usage .cfg load <name>");
                }
            }
            case "list" -> {
                ChatUtility.send("config list:");
                for (String cfg : Client.CONFIG.list()) {
                    ChatUtility.send(cfg);
                }
            }
            case "dir" -> {
                try {
                    File configDir = Client.CLIENT_DIR.resolve("configs").toFile();
                    if (!configDir.exists()) {
                        configDir.mkdirs();
                    }
                    try {
                        Runtime.getRuntime().exec(new String[]{"explorer.exe", configDir.getAbsolutePath()});
                        ChatUtility.send("Открываю папочку с кфг");
                    } catch (Exception e1) {
                        try {
                            Desktop.getDesktop().open(configDir);
                            ChatUtility.send("Открываю папочку с кфг");
                        } catch (Exception e2) {
                            ChatUtility.send("Папка кофнга в : " + configDir.getAbsolutePath());
                        }
                    }
                } catch (Exception e) {
                    error("Не смог " + e.getMessage());
                }
            }
            case "reset" -> {
                for (Module module : Client.MODULES.getModules()) {
                    module.setEnabled(false, false);
                    module.setKey(-1);
                    module.setBindType(Module.BindType.PRESS);
                    module.resetSetting();
                }
                Client.GLASS_GUI.resetGui();
                ChatUtility.send("Ресетнул кфг");
            }
        }
    }
}
