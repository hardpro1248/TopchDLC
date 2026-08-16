package gg.topchdlc.vse.shutki.other.commands.impl;

import gg.topchdlc.Client;
import gg.topchdlc.vse.shutki.other.commands.Command;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import net.minecraft.text.Text;

import java.awt.*;


public class HelpCommand extends Command {
    public static HelpCommand INSTANCE = new HelpCommand();

    public HelpCommand() {
        super("help", "shows this list");
    }

    @Override
    public void execute(String[] args) {
        for (Command command : Client.COMMANDS.getCommands()) {
            ChatUtility.send(String.format("%s - %s",command.getName(), command.getDescription()));
        }
    }
}
