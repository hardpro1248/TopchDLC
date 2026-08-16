package gg.topchdlc.vse.utils.attack;

import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.AuraModule;
import gg.topchdlc.mixin.accessor.ILocalPlayer;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.math.TimeUtility;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import org.joml.Vector3f;

@UtilityClass
public class AttackHandle implements MinecraftHolder {
    public int countHit;
    public int randomNumber1 = 2;
    public int randomNumber2 = 2;
    public int ticksOnBlock;
    TimeUtility time = new TimeUtility();
    AuraModule aura = AuraModule.INSTANCE;

    public void attackEntity(LivingEntity target, int packets) {
        boolean keepSprint = aura.auraSetting.get(AuraModule.Feature.KeepSprint);
        for (int packet = 0; packet < packets; packet++) {
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
            mc.player.swingHand(Hand.MAIN_HAND);
            if (keepSprint && !((ILocalPlayer) mc.player).serverSprintState()) {
                if (!mc.player.isOnGround() && CalcFallDist.convenientFallOffset() > 0f) {
                    mc.world.playSound(null, mc.player.getX(), mc.player.getY(), mc.player.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, mc.player.getSoundCategory(), 1f, 1f);
                    mc.player.addCritParticles(target);
                }
            } else {
                mc.player.attack(target);
            }
            mc.player.resetTicksSinceLastAttack();
            time.reset();
            countHit++;
        }
        randomNumber1 = MathUtility.random(2, 8);
        randomNumber2 = MathUtility.random(2, 8);
    }

    public boolean canAttackTick(int ticks) {
        float f2 = mc.player.getAttackCooldownProgress(0.5F + ticks);

        if (mc.player.hasVehicle() && f2 > 0.92) return true;
        boolean flag1 = f2 > (mc.player.getMainHandStack().isEmpty() ? 0.99f : 0.94F) && getFallDistance(1 + ticks) > 0F && !mc.player.isOnGround() && !mc.player.isClimbing() && !mc.player.isTouchingWater();

        if ((mc.player.isTouchingWater() || mc.player.isClimbing()) || (aura.smartCrits.get() && mc.player.isOnGround())) {
            return f2 > (mc.player.getMainHandStack().isEmpty() ? 0.99f : 0.94F);
        }
        return flag1 && mc.player.getLerpedPos(1 + ticks).multiply(1, 0, 1).distanceTo(TargetsUtility.getTarget().getLerpedPos(1 + ticks).multiply(1, 0, 1)) <= aura.attackRangeSetting.get();
    }

    public float getFallDistance(int nextTicks) {
        Vector3f deltaMove = new Vector3f(0, (float) mc.player.getVelocity().y, 0);

        if (deltaMove.y == 0 || mc.player.isOnGround()) return 0;

        float fallDistance = 0;

        double d0 = 0.08D;
        boolean flag = deltaMove.y <= 0.0D;

        if (flag && mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            d0 = 0.01D;
        }

        for (int i = 0; i < nextTicks + 1; i++) {
            double d2 = deltaMove.y;
            d2 -= d0;

            deltaMove.y = (float) (d2 * 0.98F);

            if (deltaMove.y > 0) {
                fallDistance = 0;
            } else {
                fallDistance -= deltaMove.y;
            }
        }

        return fallDistance;
    }

}
