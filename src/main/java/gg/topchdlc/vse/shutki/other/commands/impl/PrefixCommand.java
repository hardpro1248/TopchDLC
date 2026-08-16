package gg.topchdlc.vse.shutki.other.commands.impl;

import gg.topchdlc.vse.shutki.other.commands.Command;
import gg.topchdlc.vse.shutki.other.commands.Commands;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import net.minecraft.text.Text;
import java.awt.*;

/**
 * Create by daun kvass
 */
public class PrefixCommand extends Command {
    public static PrefixCommand INSTANCE = new PrefixCommand();

    public PrefixCommand() {
        super("prefix", "изменить префикс команд для . тип", "set");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 3 || !args[1].equalsIgnoreCase("set")) {
            error("Использование: " + Commands.PREFIX + "prefix set <символ(ы)>");
            return;
        }

        String newPrefix = args[2];



        Commands.PREFIX = newPrefix;

        ChatUtility.send(Text.literal("Префикс теперь не . а " + newPrefix + " тоесть все ты обошел эту ебнутую проверку поняли да")
                .withColor(new Color(49, 161, 49).getRGB()));
    }
}