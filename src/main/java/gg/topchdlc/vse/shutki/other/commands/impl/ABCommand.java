package gg.topchdlc.vse.shutki.other.commands.impl;

import net.minecraft.text.Text;
import gg.topchdlc.vse.shutki.module.modules.impl.farm.AutoBuy;
import gg.topchdlc.vse.shutki.other.commands.Command;
import gg.topchdlc.vse.utils.client.text.ChatUtility;

import java.awt.Color;
import java.util.List;

/**
 * Create by daun kvass
 */
public class ABCommand extends Command {
    public static final ABCommand INSTANCE = new ABCommand();
    
    private ABCommand() {
        super("ab", "кидаете типо в бан у кого не будете покупать", "nobuy", "allow", "list");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            sendHelp();
            return;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "nobuy":
                if (args.length < 3) {
                    ChatUtility.send(Text.literal("Используй - .ab nobuy <ник>").withColor(new Color(255, 100, 100).getRGB()));
                    return;
                }
                String playerToAdd = args[2];
                if (AutoBuy.INSTANCE.isPlayerIgnored(playerToAdd)) {
                    ChatUtility.send(Text.literal("Игрок " + playerToAdd + " уже в черном списке").withColor(new Color(255, 200, 100).getRGB()));
                } else {
                    AutoBuy.INSTANCE.addIgnoredPlayer(playerToAdd);
                    ChatUtility.send(Text.literal("Игрок " + playerToAdd + " добавлен в черный список").withColor(new Color(100, 255, 100).getRGB()));
                }
                break;

            case "allow":
                if (args.length < 3) {
                    ChatUtility.send(Text.literal("Использую вот атк .ab allow <ник>").withColor(new Color(255, 100, 100).getRGB()));
                    return;
                }
                String playerToRemove = args[2];
                if (!AutoBuy.INSTANCE.isPlayerIgnored(playerToRemove)) {
                    ChatUtility.send(Text.literal("Игрок " + playerToRemove + " не в черном списке").withColor(new Color(255, 200, 100).getRGB()));
                } else {
                    AutoBuy.INSTANCE.removeIgnoredPlayer(playerToRemove);
                    ChatUtility.send(Text.literal("Игрок " + playerToRemove + " удален из черного списка").withColor(new Color(100, 255, 100).getRGB()));
                }
                break;

            case "list":
                List<String> ignoredPlayers = AutoBuy.INSTANCE.getIgnoredPlayers();
                if (ignoredPlayers.isEmpty()) {
                    ChatUtility.send(Text.literal("Черный список пуст").withColor(new Color(200, 200, 200).getRGB()));
                } else {
                    ChatUtility.send(Text.literal("Черный список (" + ignoredPlayers.size() + "):").withColor(new Color(100, 200, 255).getRGB()));
                    for (String player : ignoredPlayers) {
                        ChatUtility.send(Text.literal("  - " + player).withColor(new Color(200, 200, 200).getRGB()));
                    }
                }
                break;

            case "clear":
                int count = AutoBuy.INSTANCE.getIgnoredPlayers().size();
                AutoBuy.INSTANCE.getIgnoredPlayers().clear();
                ChatUtility.send(Text.literal("Черный список очищен (" + count + " игроков)").withColor(new Color(100, 255, 100).getRGB()));
                break;

            default:
                sendHelp();
                break;
        }
    }

    private void sendHelp() {
        ChatUtility.send(Text.literal("chleeeen").withColor(new Color(100, 200, 255).getRGB()));
        ChatUtility.send(Text.literal(".ab nobuy <ник> - Добнуть в чс").withColor(new Color(200, 200, 200).getRGB()));
        ChatUtility.send(Text.literal(".ab allow <ник> - Удались из чс").withColor(new Color(200, 200, 200).getRGB()));
        ChatUtility.send(Text.literal(".ab list - Чекнуть чс").withColor(new Color(200, 200, 200).getRGB()));
        ChatUtility.send(Text.literal(".ab clear - Очистить чс").withColor(new Color(200, 200, 200).getRGB()));
    }
}
