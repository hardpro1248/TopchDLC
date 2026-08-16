package gg.topchdlc.vse.shutki.module.modules.impl.combat.elytraaura.attack;




import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.ElytraAura;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.elytraaura.math.ElytraAuraResolve;
import gg.topchdlc.mixin.accessor.ILivingEntity;
import gg.topchdlc.vse.utils.attack.CalcCooldownStrength;
import gg.topchdlc.vse.utils.math.RayTraceUtility;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;

@UtilityClass
public class ElytraAuraAttackHandler implements MinecraftHolder {
    ElytraAura elytraAura = ElytraAura.INSTANCE;

    public void updateAttack(LivingEntity entity) {
        if (mc.player == null || mc.world == null || entity == null) return;
        if ((mc.player.getEntityPos().distanceTo(ElytraAuraResolve.getPointOnTarget(entity)) <= attackDistance(entity)) && ((ILivingEntity)mc.player).client$lastAttackedTicks() > 5 && CalcCooldownStrength.getAttackCooldownStrength(CalcCooldownStrength.MODE.ELYTRA, false)) {
            mc.interactionManager.attackEntity(mc.player, entity);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    public float attackDistance(LivingEntity entity) {
        return entity.isGliding() ? Math.max(elytraAura.isLeave(entity) ? 4.5f : 3.2f, elytraAura.attackRangeSetting.get()) : 2.8f;
    }

    public boolean raycast(LivingEntity entity) {
        return RayTraceUtility.rayTrace(Client.ROTATION.getRotate().toVector(), attackDistance(entity), entity.getBoundingBox());
    }
}
