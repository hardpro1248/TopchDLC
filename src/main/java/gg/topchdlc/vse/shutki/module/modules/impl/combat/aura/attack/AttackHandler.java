package gg.topchdlc.vse.shutki.module.modules.impl.combat.aura.attack;



import gg.topchdlc.vse.shutki.module.modules.impl.combat.Criticals;
import net.minecraft.block.Blocks;
import gg.topchdlc.Client;
import gg.topchdlc.api.scripts.api.integrate.ScriptHook;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.AuraModule;
import gg.topchdlc.vse.utils.client.mixin.IEntity;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.math.RotationUtility;
import gg.topchdlc.vse.utils.math.TimeUtility;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import org.joml.Vector3f;

import static gg.topchdlc.MinecraftHolder.mc;

@UtilityClass
public class AttackHandler {
    public static int countHit;
    public static long lastAttackTime = 0;
    public static int randomNumber1 = 2;
    public static int randomNumber2 = 2;
    public static int ticksOnBlock;
    static TimeUtility time = new TimeUtility();
    static AuraModule aura = AuraModule.INSTANCE;

    public void attackEntity(LivingEntity target) {
        if (Criticals.INSTANCE.isEnabled() && Criticals.INSTANCE.mode.get() == Criticals.Mode.MiniJump) {
            Criticals.INSTANCE.doCrit();
        }

        lastAttackTime = System.currentTimeMillis();

        boolean bypass = aura.antiCheat.is(AuraModule.AntiCheat.FunTime) || aura.antiCheat.is(AuraModule.AntiCheat.SpookyTime);

        if (bypass) {
            boolean wasSprinting = mc.player.isSprinting();
            mc.player.attack(target);
            if (wasSprinting && !mc.player.isSprinting()) {
                mc.player.setSprinting(true);
            }
        } else if (!aura.auraSetting.get(AuraModule.Feature.KeepSprint)) {
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
        } else {
            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));

            if (!mc.player.isOnGround() && convenientFallOffset() > 0.0F) {
                mc.world.playSound(null, mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, mc.player.getSoundCategory(), 1.0f, 1.0f);
                mc.player.addCritParticles(target);
            }

            mc.player.swingHand(Hand.MAIN_HAND);
        }

        mc.player.resetTicksSinceLastAttack();

        time.reset();
        countHit++;
        randomNumber1 = MathUtility.random(2, 8);
        randomNumber2 = MathUtility.random(2, 8);
    }

    public static double convenientFallOffset() {
        double fallOffset = mc.player.fallDistance;
        if (mc.world != null && !mc.player.isOnGround() && mc.player.getVelocity().y < -.0784000015258789D && mc.world.getBlockState(mc.player.getBlockPos()).getFluidState().isEmpty() && !mc.world.getBlockState(mc.player.getBlockPos().up()).getFluidState().isEmpty()) {
            if (mc.player.fallDistance < -mc.player.getVelocity().y && ticksOnBlock > 6)
                fallOffset = -mc.player.getVelocity().y;
        }
        return fallOffset;
    }

    public boolean shouldResetSprinting() {
        return !mc.player.isOnGround() && TargetsUtility.getTarget() != null && (canAttackTick(1) || shouldAttack());
    }

    public boolean canAttackTick(int ticks) {
        if (TargetsUtility.getTarget() == null) return false;

        float f2 = mc.player.getAttackCooldownProgress(0.5F + ticks);

        if (mc.player.hasVehicle() && f2 > 0.92) return true;

        boolean isBypassingFallRequirement = mc.player.isTouchingWater()
                || mc.player.isClimbing()
                || ((IEntity) mc.player).client$inWeb();

        if (isBypassingFallRequirement || (aura.smartCrits.get() && mc.player.isOnGround())) {
            return f2 > (mc.player.getMainHandStack().isEmpty() ? 0.99f : 0.94F);
        }

        boolean flag1 = f2 > (mc.player.getMainHandStack().isEmpty() ? 0.99f : 0.94F)
                && getFallDistance(1 + ticks) > 0F
                && !mc.player.isOnGround()
                && !mc.player.isClimbing()
                && !mc.player.isTouchingWater();

        boolean result = flag1 && mc.player.getLerpedPos(1 + ticks).multiply(1, 0, 1)
                .distanceTo(TargetsUtility.getTarget().getLerpedPos(1 + ticks).multiply(1, 0, 1)) <= aura.attackRangeSetting.get();

        if (ticks > 0) result |= canAttackTick(ticks - 1);
        return result;
    }

    public boolean shouldAttack() {
        java.lang.Object o = Client.SCRIPTS.getScriptHook().solute(ScriptHook.HookValue.AURA_ATTACK, TargetsUtility.getTarget());
        if (o != null) {
            return (boolean) o;
        }

    if ((AuraModule.INSTANCE.resetSprint.is(AuraModule.ResetSprint.Legit) && mc.player.isSprinting() && !mc.player.isTouchingWater()))
        return false;
        if (RotationUtility.getStrictDistance(TargetsUtility.getTarget()) > aura.attackRange())
            return false;

        boolean inWeb = ((IEntity) mc.player).client$inWeb();
        boolean climbing = mc.player.isClimbing();
        if (inWeb || climbing || mc.player.isInLava() || mc.player.isTouchingWater() || mc.player.isSubmergedInWater() || mc.player.getAbilities().flying) {
            return true;
        }

        if (!aura.checkCrit.get() || aura.smartCrits.get()) {
            return true;
        }

        boolean isMiniJump = Criticals.INSTANCE.isEnabled() && Criticals.INSTANCE.mode.get() == Criticals.Mode.MiniJump;
        if (isMiniJump) {
            if (mc.player.isOnGround()) {
                Criticals.INSTANCE.doCrit();
                return false;
            }
            boolean isFalling = mc.player.getVelocity().y < 0 || mc.player.fallDistance > 0;
            return isFalling;
        }

        if (mc.player.isOnGround()) {
            return mc.player.getAttackCooldownProgress(0.5f) >= 1.0f;
        }

        boolean isFalling = mc.player.getVelocity().y < -0.2f || mc.player.fallDistance > 0.15f;
        return isFalling && mc.player.getAttackCooldownProgress(0.5f) >= 1.0f;
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
