package gg.topchdlc.vse.shutki.other.commands.impl;

import gg.topchdlc.Client;
import gg.topchdlc.vse.rotation.all.impl.Builder;
import gg.topchdlc.vse.rotation.builder.BuilderProfile;
import gg.topchdlc.vse.shutki.other.commands.Command;
import gg.topchdlc.vse.utils.client.text.ChatUtility;

import java.awt.*;
import java.io.File;

/**
 * Create by daun kvass
 */
public class BuilderCommand extends Command {
    public static final BuilderCommand INSTANCE = new BuilderCommand();

    public BuilderCommand() {
        super("builder", "67", "load", "train", "list", "info","dir");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            error("Use: .builder load <name> | .builder train | .builder list | .builder info");
            return;
        }

        String action = args[1].toLowerCase();

        if (action.equals("train")) {
            mc.execute(() -> Client.BUILDER_TRAINING.setOpened(true));
        } else if (action.equals("load") && args.length >= 3) {
            String profileName = args[2];
            BuilderProfile loaded = BuilderProfile.load(profileName);
            Builder.activeProfile = loaded;
            ChatUtility.send("§aBuilder Loaded profile: §e" + loaded.name);
        } else if (action.equals("info")) {
            BuilderProfile p = Builder.activeProfile;
            ChatUtility.send("§aBuilder Active Profile Info " + p.name );
            ChatUtility.send(" Avg  §e" + String.format("%.2f", p.avgSpeed) + " §7Max: " + String.format("%.1f", p.maxSpeed) );
            ChatUtility.send(" Yaw/Pitch Ratio §e" + String.format("%.2f", p.yawPitchRatio));
            ChatUtility.send(" Arc Curvature: §e" + String.format("%.2f", p.arcCurvature));
            ChatUtility.send(" Learned Anchors: §e" + (p.relativePointX != null ? p.relativePointX.length : 0) + " clusters");
            ChatUtility.send(" Prediction Weight: §e" + String.format("%.2f", p.predictionWeight));
        } else if (action.equals("list")) {
            File dir = Client.CLIENT_DIR.resolve("builder_profiles").toFile();
            if (!dir.exists() || dir.listFiles() == null) {
                ChatUtility.send("§Builder No saved profiles found.");
                return;
            }

            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (files == null || files.length == 0) {
                ChatUtility.send("§cBuilder No saved profiles found.");
                return;
            }

            ChatUtility.send("§aBuilder Saved Profiles (" + files.length + "):");
            for (File file : files) {
                String pName = file.getName().replace(".json", "");
                boolean isActive = pName.equalsIgnoreCase(Builder.activeProfile.name);
                ChatUtility.send(" §7- §e" + pName + (isActive ? " §aActive" : ""));
            }
        } else if(action.equals("dir")){
            try {
                File configDir = Client.CLIENT_DIR.resolve("builder_profiles").toFile();
                if (!configDir.exists()) {
                    configDir.mkdirs();
                }
                try {
                    Runtime.getRuntime().exec(new String[]{"explorer.exe", configDir.getAbsolutePath()});
                    ChatUtility.send("Открываю попочку с профилями билдера");
                } catch (Exception e1) {
                    try {
                        Desktop.getDesktop().open(configDir);
                        ChatUtility.send("Открываю папочку с профилями");
                    } catch (Exception e2) {
                        ChatUtility.send("Папка кофнга в : " + configDir.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                error("Не смог " + e.getMessage());
            }

        }
        else {
            error("Use: .builder load <name> | .builder train | .builder list | .builder info | .builder dir");
        }
    }
}