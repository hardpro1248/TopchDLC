package gg.topchdlc.api.friends;

import gg.topchdlc.Client;
import gg.topchdlc.vse.utils.other.LogUtility;
import lombok.Getter;
import net.minecraft.entity.player.PlayerEntity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Список игроков, которые играют с этим клиентом (topchdlc).
 * Используется для отображения логотипа "Т" возле ника.
 */
public class ClientUsers {
    @Getter
    private ArrayList<String> userList = new ArrayList<>();

    private final File file = Client.CLIENT_DIR.resolve("clientusers.topchdlc").toFile();

    public ClientUsers() {
        if (!file.exists()) {
            try {
                new FileOutputStream(file).write("".getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void addUser(PlayerEntity player) {
        if (!userList.contains(player.getGameProfile().name()))
            userList.add(player.getGameProfile().name());
    }
    public void addUser(String playerName) {
        if (!userList.contains(playerName))
            userList.add(playerName);
    }

    public void removeUser(PlayerEntity player) {
        userList.remove(player.getGameProfile().name());
    }
    public void removeUser(String playerName) {
        userList.remove(playerName);
    }

    public boolean isUser(PlayerEntity player) {
        return userList.contains(player.getGameProfile().name());
    }
    public boolean isUser(String playerName) {
        return userList.contains(playerName);
    }

    public void save() {
        try {
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            FileOutputStream out = new FileOutputStream(file);
            StringBuilder builder = new StringBuilder();
            for (String user : userList) {
                builder.append(user).append("\n");
            }
            out.write(builder.toString().getBytes(StandardCharsets.UTF_8));

            LogUtility.debug(String.format("saved client users"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void load() {
        try {
            FileInputStream in = new FileInputStream(file);
            String s = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String[] users = s.split("\n");

            userList.clear();
            if (!s.isBlank())
                userList.addAll(Arrays.asList(users));

            LogUtility.debug(String.format("loaded client users: %s", Arrays.toString(users)));
        } catch (Exception e) {
            LogUtility.error(e, "loading client users");
        }
    }
}
