package gg.topchdlc.vse.shutki.other.commands.impl;

import gg.topchdlc.Client;
import gg.topchdlc.vse.shutki.other.commands.Command;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import net.minecraft.text.Text;

import java.awt.*;

public class FriendCommand extends Command {
    public static FriendCommand INSTANCE = new FriendCommand();

    public FriendCommand() {
        super("friend", "ну друзья ", "add", "remove", "list");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            error("usage: .friend <add | remove | list> [name]");
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "add" -> {
                if (args.length < 3) {
                    error("usage: .friend add <name>");
                    return;
                }
                String name = args[2];
                if (Client.FRIENDS.isFriend(name)) {
                    ChatUtility.send(Text.literal(name + " уже в друзьях").withColor(new Color(255, 200, 100).getRGB()));
                } else {
                    Client.FRIENDS.addFriend(name);
                    ChatUtility.send(Text.literal("Добавлен в друзья: " + name).withColor(new Color(100, 255, 100).getRGB()));
                }
            }
            case "remove", "del" -> {
                if (args.length < 3) {
                    error("usage: .friend remove <name>");
                    return;
                }
                String name = args[2];
                if (!Client.FRIENDS.isFriend(name)) {
                    ChatUtility.send(Text.literal(name + " не в друзьях").withColor(new Color(255, 200, 100).getRGB()));
                } else {
                    Client.FRIENDS.removeFriend(name);
                    ChatUtility.send(Text.literal("Удален из друзей: " + name).withColor(new Color(255, 100, 100).getRGB()));
                }
            }
            case "list" -> {
                if (Client.FRIENDS.getFriendList().isEmpty()) {
                    ChatUtility.send(Text.literal("Список друзей пуст").withColor(new Color(200, 200, 200).getRGB()));
                } else {
                    ChatUtility.send(Text.literal("Список друзей:").withColor(new Color(100, 200, 255).getRGB()));
                    for (String friend : Client.FRIENDS.getFriendList()) {
                        ChatUtility.send(Text.literal("  - " + friend).withColor(new Color(200, 200, 200).getRGB()));
                    }
                }
            }
            case "clear" -> {
                if (Client.FRIENDS.getFriendList().isEmpty()) {
                    ChatUtility.send(Text.literal("У тебя нету друзей и так уже").withColor(new Color(200, 200, 200).getRGB()));
                } else {
                    int count = Client.FRIENDS.getFriendList().size();
                    Client.FRIENDS.getFriendList().clear();
                    Client.FRIENDS.save();
                    ChatUtility.send(Text.literal("Ты бросил вот стоко друзей: " + count).withColor(new Color(255, 100, 100).getRGB()));
                }
            }
            default -> error("usage: .friend <add | remove | list | clear> [name]");
        }
    }
}
