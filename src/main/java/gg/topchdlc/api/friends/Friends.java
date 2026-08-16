package gg.topchdlc.api.friends;

import gg.topchdlc.Client;
import gg.topchdlc.vse.utils.other.LogUtility;
import gg.topchdlc.vse.utils.secure.CryptUtility;
import lombok.Getter;
import net.minecraft.entity.player.PlayerEntity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class Friends {
    @Getter
    private ArrayList<String> friendList = new ArrayList<>();

    private final File file = Client.CLIENT_DIR.resolve("friends.topchdlc").toFile();

    public Friends() {
        if (!file.exists()) {
            try {
                new FileOutputStream(file).write("".getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void addFriend(PlayerEntity player) {
        if (!friendList.contains(player.getGameProfile().name()))
            friendList.add(player.getGameProfile().name());
    }
    public void addFriend(String playerName) {
        if (!friendList.contains(playerName))
            friendList.add(playerName);
    }

    public void removeFriend(PlayerEntity player) {
        friendList.remove(player.getGameProfile().name());
    }
    public void removeFriend(String playerName) {
        friendList.remove(playerName);
    }

    public boolean isFriend(PlayerEntity player) {
        return friendList.contains(player.getGameProfile().name());
    }
    public boolean isFriend(String playerName) {
        return friendList.contains(playerName);
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
            for (String friend : friendList) {
                builder.append(friend).append("\n");
            }
            out.write(CryptUtility.proccessXOR(builder.toString().getBytes(StandardCharsets.UTF_8)));

            LogUtility.debug(String.format("saved friends"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void load() {
        try {

            FileInputStream in = new FileInputStream(file);
            String s = new String(CryptUtility.proccessXOR(in.readAllBytes()), StandardCharsets.UTF_8);
            String[] friends = s.split("\n");

            friendList.clear();
            if (!s.isBlank())
                friendList.addAll(Arrays.asList(friends));

            LogUtility.debug(String.format("loaded friends: %s", Arrays.toString(friends)));
        } catch (Exception e) {
            LogUtility.error(e, "loading friends");
        }
    }

}
