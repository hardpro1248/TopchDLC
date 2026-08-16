package gg.topchdlc.vse.shutki.other.commands.impl;

import gg.topchdlc.Client;
import gg.topchdlc.vse.shutki.other.commands.Command;
import gg.topchdlc.vse.utils.client.text.ChatUtility;

public class HotswapCommand extends Command {
    public static HotswapCommand INSTANCE = new HotswapCommand();

    public HotswapCommand() {
        super("", "reloading scripts");
    }

    @Override
    public void execute(String[] args) {
        Client.SCRIPTS.RELOAD();
        ChatUtility.send("Scripts reloaded");
    }
}
