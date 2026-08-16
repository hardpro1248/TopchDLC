package gg.topchdlc.vse.utils.player;

import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.mixin.accessor.ILivingEntity;

public class HitUtility implements MinecraftHolder {
    public static boolean isPerfect() {
        return mc.player.getAttackCooldownProgress(1.5f) >= (mc.player.isOnGround() || mc.player.fallDistance == 0 ? 0.99f : 0.9f)
                && ((ILivingEntity)mc.player).client$lastAttackedTicks() > 9;
    }
}
