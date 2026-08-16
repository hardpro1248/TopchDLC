package gg.topchdlc.vse.shutki.other.commands.impl;

import gg.topchdlc.Client;
import gg.topchdlc.vse.shutki.other.commands.Command;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import net.minecraft.text.Text;

import java.awt.*;

/**
 * Команда для управления списком игроков, которые играют с клиентом.
 * Возле их ника отображается логотип "Т".
 */
public class ClientUserCommand extends Command {
    public static ClientUserCommand INSTANCE = new ClientUserCommand();

    public ClientUserCommand() {
        super("clientuser", "игроки с клиентом", "add", "remove", "list");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            error("usage: .clientuser <add | remove | list> [name]");
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "add" -> {
                if (args.length < 3) {
                    error("usage: .clientuser add <name>");
                    return;
                }
                String name = args[2];
                if (Client.CLIENT_USERS.isUser(name)) {
                    ChatUtility.send(Text.literal(name + " уже в списке клиента").withColor(new Color(255, 200, 100).getRGB()));
                } else {
                    Client.CLIENT_USERS.addUser(name);
                    Client.CLIENT_USERS.save();
                    ChatUtility.send(Text.literal("Добавлен в список клиента: " + name).withColor(new Color(100, 255, 100).getRGB()));
                }
            }
            case "remove", "del" -> {
                if (args.length < 3) {
                    error("usage: .clientuser remove <name>");
                    return;
                }
                String name = args[2];
                if (!Client.CLIENT_USERS.isUser(name)) {
                    ChatUtility.send(Text.literal(name + " не в списке клиента").withColor(new Color(255, 200, 100).getRGB()));
                } else {
                    Client.CLIENT_USERS.removeUser(name);
                    Client.CLIENT_USERS.save();
                    ChatUtility.send(Text.literal("Удален из списка клиента: " + name).withColor(new Color(255, 100, 100).getRGB()));
                }
            }
            case "list" -> {
                if (Client.CLIENT_USERS.getUserList().isEmpty()) {
                    ChatUtility.send(Text.literal("Список игроков с клиентом пуст").withColor(new Color(200, 200, 200).getRGB()));
                } else {
                    ChatUtility.send(Text.literal("Список игроков с клиентом:").withColor(new Color(100, 200, 255).getRGB()));
                    for (String user : Client.CLIENT_USERS.getUserList()) {
                        ChatUtility.send(Text.literal("  - " + user).withColor(new Color(200, 200, 200).getRGB()));
                    }
                }
            }
            default -> error("usage: .clientuser <add | remove | list> [name]");
        }
    }
}
