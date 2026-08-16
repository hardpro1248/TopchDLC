package gg.topchdlc.vse.utils.attack;

import gg.topchdlc.MinecraftHolder;
import lombok.experimental.UtilityClass;

import static gg.topchdlc.vse.shutki.module.modules.impl.combat.aura.attack.AttackHandler.ticksOnBlock;

@UtilityClass
public class CalcFallDist implements MinecraftHolder {

    public static double convenientFallOffset() {
        double fallOffset = mc.player.fallDistance;
        if (mc.world != null && !mc.player.isOnGround() && mc.player.getVelocity().y < -.0784000015258789D && mc.world.getBlockState(mc.player.getBlockPos()).getFluidState().isEmpty() && !mc.world.getBlockState(mc.player.getBlockPos().up()).getFluidState().isEmpty()) {
            if (mc.player.fallDistance < -mc.player.getVelocity().y && ticksOnBlock > 6)
                fallOffset = -mc.player.getVelocity().y;
        }
        return fallOffset;
    }

}
