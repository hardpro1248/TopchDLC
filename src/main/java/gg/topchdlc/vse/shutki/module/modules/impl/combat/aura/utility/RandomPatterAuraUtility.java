package gg.topchdlc.vse.shutki.module.modules.impl.combat.aura.utility;


import gg.topchdlc.vse.rotation.Angle;
import lombok.experimental.UtilityClass;
import net.minecraft.util.math.MathHelper;

import java.security.SecureRandom;

@UtilityClass
public class RandomPatterAuraUtility {

    public float random(Angle target, float min, float max) {
        SecureRandom random = new SecureRandom();
        float pat = randomPattern(target) == 0 ? 0.01f : randomPattern(target);
        random.setSeed((long) (System.currentTimeMillis() * random.nextGaussian() * pat));
        return (float) (min + (random.nextGaussian() * (max - min)));
    }

    public float randomPattern(Angle targetAngle) {
        float[] deltaS = CalcRotUtility.getDeltas(targetAngle);

        float normalizedYaw = MathHelper.clamp(Math.abs(deltaS[0]) / 180f, 0f, 1f);
        float normalizedPitch = MathHelper.clamp(Math.abs(deltaS[1]) / 90f, 0f, 1f);
        float combinedDeviation = (normalizedYaw * 0.5f + normalizedPitch * 0.5f);

        float legitimacyFactor = 1f - combinedDeviation;
        return MathHelper.clamp(legitimacyFactor, 0.1f, 1f);
    }
}
