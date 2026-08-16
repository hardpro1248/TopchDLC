package gg.topchdlc.api.macros.constructor.impl;

import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.macros.constructor.MacroBlock;

public class JumpBlock implements MacroBlock, MinecraftHolder {
    @Override
    public void execute() {
        if (mc.player.isOnGround() && !mc.options.jumpKey.isPressed())
            mc.player.jump();
    }

    @Override
    public String renderName() {
        return "Jump from ground";
    }
}
