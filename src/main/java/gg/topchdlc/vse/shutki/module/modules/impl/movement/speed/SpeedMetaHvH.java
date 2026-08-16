package gg.topchdlc.vse.shutki.module.modules.impl.movement.speed;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.aura.elytra.PredictUtils;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import gg.topchdlc.vse.utils.player.MoveUtility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;

/**
 * Create by daun kvass
 */
public class SpeedMetaHvH extends Choice {
    public SpeedMetaHvH() {
        super("MetaHvH");
    }

    public SliderSetting speedDefault = sliderSetting("Base Speed", 0.5f, 0.1f, 2.0f).increment(0.01f);
    public SliderSetting speed1 = sliderSetting("Speed I Speed", 0.6f, 0.1f, 2.0f).increment(0.01f);
    public SliderSetting speed2 = sliderSetting("Speed II Speed", 0.7f, 0.1f, 2.0f).increment(0.01f);
    public SliderSetting speed3 = sliderSetting("Speed III+ Speed", 0.8f, 0.1f, 3.0f).increment(0.01f);

    public CheckBox autojump = checkbox("AutoJump", false);
    public CheckBox customjMotion = checkbox("Custom Jump", false).visible(autojump::get);
    public SliderSetting jumpMotion = sliderSetting("JumpMotion", 0.42f, 0, 1).increment(0.01f).visible(() -> customjMotion.get() && autojump.get());

    public CheckBox followTarget = checkbox("Follow Aura Target", false);
    public CheckBox overtake = checkbox("Overtake", false).visible(followTarget::get);
    public SliderSetting overtakeTicks = sliderSetting("Overtake Ticks", 10.0f, 1.0f, 50.0f).increment(1.0f).visible(() -> followTarget.get() && overtake.get());

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventGameTick) {
            if (mc.player == null || mc.world == null) return;

            PlayerInput input = mc.player.input.playerInput;
            float forward = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
            float side = (input.left() ? 1 : 0) - (input.right() ? 1 : 0);

            LivingEntity auraTarget = TargetsUtility.getTarget();

            if (forward == 0 && side == 0 && (auraTarget == null || !followTarget.get())) return;

            if (mc.player.isOnGround() && autojump.get()) {
                mc.player.jump();
                Vec3d vel = mc.player.getVelocity();
                mc.player.setVelocity(vel.x, customjMotion.get() ? jumpMotion.get() : 0.42f, vel.z);
            }

            double currentSpeed = getSpeedByEffect();
            double angle;

            if (followTarget.get() && auraTarget != null && auraTarget.isAlive()) {

                Vec3d targetPos = auraTarget.getEntityPos();

                if (overtake.get()) {
                    targetPos = PredictUtils.predictGround(auraTarget, auraTarget.getEntityPos(), overtakeTicks.get());
                }

                angle = Math.toRadians(getYawToVec(targetPos));
            } else {
                if (forward == 0 && side == 0) return;
                angle = Math.toRadians(calculateYaw(mc.player.getYaw(), forward, side));
            }

            double motionX = -Math.sin(angle) * currentSpeed;
            double motionZ = Math.cos(angle) * currentSpeed;

            mc.player.setVelocity(motionX, mc.player.getVelocity().y, motionZ);
        }
    }

    private float getYawToVec(Vec3d pos) {
        double dx = pos.x - mc.player.getX();
        double dz = pos.z - mc.player.getZ();
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
    }

    private double getSpeedByEffect() {
        StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
        if (speedEffect == null) return speedDefault.get();
        int level = speedEffect.getAmplifier() + 1;
        return switch (level) {
            case 1 -> speed1.get();
            case 2 -> speed2.get();
            default -> speed3.get();
        };
    }

    private float calculateYaw(float yaw, float forward, float side) {
        if (forward < 0) yaw += 180;
        float forwardOffset = 90;
        if (forward < 0) forwardOffset = -45;
        else if (forward > 0) forwardOffset = 45;
        if (side > 0) yaw -= forwardOffset;
        else if (side < 0) yaw += forwardOffset;
        return yaw;
    }
}