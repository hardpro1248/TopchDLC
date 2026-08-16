package gg.topchdlc.vse.utils.attack;

import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.mixin.accessor.ILivingEntity;

public class CalcCooldownStrength implements MinecraftHolder {

    public static boolean getAttackCooldownStrength(MODE mode, boolean old) {
        float base = old ? 0.5f : 1.5f, tick = old ? 1 : 9;
        if (mode.equals(MODE.ELYTRA))
            return getAttackCooldownStrength(MODE.ELYTRA, base, old ? 0.3f : 0.99F, (int) tick);
        else if (mode.equals(MODE.DEFAULT))
            return getAttackCooldownStrength(MODE.DEFAULT, base, old ? 0.3f : 0.94F, (int) tick);
        else
            return true;
    }

    public static boolean getAttackCooldownStrength(MODE mode, float baseTick, float minCD, int ticks) {
        if (mode.equals(MODE.ELYTRA) || mode.equals(MODE.DEFAULT))
            return getAttackCooldownStrength(baseTick, minCD, ticks);
        else
            return true;
    }

    public static boolean getAttackCooldownStrength(float baseTick, float minCD, int ticks) {
        return mc.player.getAttackCooldownProgress(baseTick) >= minCD && ((ILivingEntity)mc.player).client$lastAttackedTicks() > ticks;
    }

    public enum MODE {
        ELYTRA,
        DEFAULT
    }
}
