package gg.topchdlc.api.macros.constructor.impl;

import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.macros.constructor.MacroBlock;
import net.minecraft.util.Hand;

public class SwingBlock implements MacroBlock, MinecraftHolder {
    @Override
    public void execute() {
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    @Override
    public String renderName() {
        return "Swing hand";
    }
}
