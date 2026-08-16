package gg.topchdlc.vse.shutki.module.modules.impl.movement.speed;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.aura.elytra.PredictUtils;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class SpeedTargetSpeed extends Choice {

    private final SliderSetting targetSpeedValue = sliderSetting("Speed", 8.0f, 1.0f, 20.0f).increment(0.1f);
    private final SliderSetting targetSpeedRange = sliderSetting("Range", 3.0f, 0.5f, 10.0f).increment(0.1f);
    private final CheckBox targetSpeedRequireMoving = checkbox("Require Moving", true);

    public final CheckBox overtake = checkbox("Overtake", false);
    public final SliderSetting overtakeTicks = sliderSetting("Overtake Ticks", 10.0f, 1.0f, 50.0f).increment(1.0f).visible(overtake::get);

    public SpeedTargetSpeed() {
        super("Target Speed");
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventGameTick) {
            handleTargetSpeed();
        }
    }

    private void handleTargetSpeed() {
        if (mc.player == null || mc.world == null) return;

        if (targetSpeedRequireMoving.get()) {
            double vx = mc.player.getVelocity().x;
            double vz = mc.player.getVelocity().z;
            if (Math.abs(vx) < 0.01 && Math.abs(vz) < 0.01) {
                return;
            }
        }

        Entity targetEntity = null;

        LivingEntity auraTarget = TargetsUtility.getTarget();
        if (auraTarget != null && auraTarget.isAlive()) {
            boolean isFriend = (auraTarget instanceof PlayerEntity player) && Client.FRIENDS.isFriend(player);
            if (!isFriend) {
                double dx = auraTarget.getX() - mc.player.getX();
                double dz = auraTarget.getZ() - mc.player.getZ();
                double sq = dx * dx + dz * dz;
                double maxRangeSq = targetSpeedRange.get() * targetSpeedRange.get();
                if (sq <= maxRangeSq) {
                    targetEntity = auraTarget;
                }
            }
        }

        if (targetEntity == null) {
            double bestSq = Double.MAX_VALUE;
            double maxRangeSq = targetSpeedRange.get() * targetSpeedRange.get();

            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player) continue;
                if (Client.FRIENDS.isFriend(player)) continue;

                double dx = player.getX() - mc.player.getX();
                double dz = player.getZ() - mc.player.getZ();
                double sq = dx * dx + dz * dz;

                if (sq <= maxRangeSq && sq < bestSq) {
                    bestSq = sq;
                    targetEntity = player;
                }
            }
        }

        if (targetEntity != null) {
            double finalSpeed = targetSpeedValue.get() * 0.01;

            Vec3d targetPos = targetEntity.getEntityPos();

            if (overtake.get() && targetEntity instanceof LivingEntity livingTarget) {
                targetPos = PredictUtils.predictGround(livingTarget, targetPos, overtakeTicks.get());
            }

            Vec3d from = mc.player.getEntityPos();
            double dx = targetPos.x - from.x;
            double dz = targetPos.z - from.z;
            double len = Math.sqrt(dx * dx + dz * dz);

            if (len > 0.0f) {
                mc.player.addVelocity(dx / len * finalSpeed, 0.0, dz / len * finalSpeed);
            }
        }
    }
}