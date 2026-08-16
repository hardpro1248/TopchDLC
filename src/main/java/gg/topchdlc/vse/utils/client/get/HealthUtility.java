package gg.topchdlc.vse.utils.client.get;

import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.client.client.TargetSettings;
import net.minecraft.entity.LivingEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;

/**
 * Create by daun kvass
 */
public class HealthUtility implements MinecraftHolder {

    public static float get(LivingEntity e) {
        TargetSettings.healthmode mode = TargetSettings.INSTANCE.HealthMode.get();

        if (mode == TargetSettings.healthmode.Scoreboard) {
            Float sbHealth = getHealthFromScoreboard(e);
            if (sbHealth != null) return sbHealth;
        }

        return (float) (Math.floor(e.getHealth() * 10.0f) / 10.0f);
    }

    private static Float getHealthFromScoreboard(LivingEntity entity) {
        try {
            if (mc.world == null) return null;
            Scoreboard scoreboard = mc.world.getScoreboard();
            if (scoreboard != null) {
                ScoreboardObjective healthObjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
                if (healthObjective != null) {
                    ReadableScoreboardScore score = scoreboard.getScore(entity, healthObjective);
                    if (score != null) {
                        return (float) score.getScore();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}