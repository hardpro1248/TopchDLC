package gg.topchdlc.vse.shutki.other.commands.impl;

import gg.topchdlc.vse.shutki.other.commands.Command;
import gg.topchdlc.vse.utils.player.VClipUtility;

/**
 * Create by daun kvass
 */
public class VClipCommand extends Command {
    public static VClipCommand INSTANCE = new VClipCommand();

    public VClipCommand() {
        super("vclip", "тепаю по вертикали", "up", "down", "<value>");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            error("Usage: .vclip <up | down | value>");
            return;
        }

        String action = args[1];

        if (action.equalsIgnoreCase("up")) {
            VClipUtility.clipUp();
        } else if (action.equalsIgnoreCase("down")) {
            VClipUtility.clipDown();
        } else {
            try {
                double offset = Double.parseDouble(action);
                VClipUtility.clip(offset);
            } catch (NumberFormatException e) {
                error("нето значение");
            }
        }
    }
}