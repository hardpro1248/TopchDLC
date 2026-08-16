package gg.topchdlc.vse.shutki.module.modules.impl.combat.elytraaura.math;


import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.scripts.api.integrate.ScriptHook;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.ElytraAura;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.Resolver;
import gg.topchdlc.vse.shutki.module.modules.impl.movement.SuperFirework;
import gg.topchdlc.vse.utils.client.mixin.IShooterEntity;
import gg.topchdlc.vse.utils.client.mixin.ResolvedPositionEntity;
import gg.topchdlc.vse.utils.math.RotationUtility;
import gg.topchdlc.vse.utils.player.MoveUtility;
import lombok.experimental.UtilityClass;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

@UtilityClass
public class ElytraAuraResolve implements MinecraftHolder {
    ElytraAura elytraAura = ElytraAura.INSTANCE;

    public Vec3d getFinalTargetVector(LivingEntity entity, boolean fromYou) {
        Object o = Client.SCRIPTS.getScriptHook().solute(ScriptHook.HookValue.ELYTRA_RESOLVER, entity);
        if (o != null) {
            return new Vec3d((Vector3f) o);
        }

        Vec3d point = getPointOnTarget(entity);
        if (!entity.isGliding() || entity.isOnGround() || isStoyak(entity))
            return entity.getEntityPos().add(0, entity.getHeight(), 0);

        Vec3d getServerPos = ((ResolvedPositionEntity)entity).hachclientport$getResolvedPos();

        point = Resolver.INSTANCE.resolveElytra(point, mc.player.getAttackCooldownProgress(1.5f) - 0.01f);
        point = point.add(getServerPos.subtract(getUnLerpedPos(entity)).multiply(shouldMovePredict(entity) ?
                entity instanceof AbstractClientPlayerEntity pl ? mc.getNetworkHandler().getPlayerListEntry(pl.getUuid()).getLatency() / 50F : 1.5F
                        : 0.4F));

        if (fromYou)
            point = point.subtract(mc.player.getEyePos());

        return point;
    }

    public Vec3d getPointOnTarget(LivingEntity entity) {
        return getPoint(entity, switch (elytraAura.typeAim.get()) {
            case Resolve, Middle -> GETPOINT.MIDDLE;
            case FireWork -> GETPOINT.FIREWORK;
            case Default -> GETPOINT.DEFAULT;
        });
    }

    public Vec3d getPoint(LivingEntity entity, GETPOINT getpoint) {
        return getPoint(mc.player.getEyePos(), entity, getpoint);
    }

    public Vec3d getPoint(Vec3d pos, LivingEntity entity, GETPOINT getpoint) {
        if (pos == null) return Vec3d.ZERO;
        Vec3d vec3d = null;
        Vec3d resolve = ((ResolvedPositionEntity) entity).hachclientport$getResolvedPos() != null ? ((ResolvedPositionEntity) entity).hachclientport$getResolvedPos() : getPoint(entity, GETPOINT.DEFAULT);
        float tick = mc.getRenderTickCounter().getTickProgress(true);
        switch (getpoint) {
            case DEFAULT ->
                    vec3d = new Vec3d(
                            MathHelper.clamp(pos.x, entity.getBoundingBox().minX + 0.05f, entity.getBoundingBox().maxX - 0.05f),
                            MathHelper.clamp(pos.y, entity.getBoundingBox().minY + 0.05f, entity.getBoundingBox().maxY - 0.05f),
                            MathHelper.clamp(pos.z, entity.getBoundingBox().minZ + 0.05f, entity.getBoundingBox().maxZ - 0.05f)
                    );
            case FIREWORK -> {
                Vec3d interpolatedPos = getPoint(entity, GETPOINT.MIDDLE);

                for (Entity e : mc.world.getEntities()) {
                    float maxDistance = 0.1f;
                    float md = maxDistance * maxDistance;
                    if (e instanceof FireworkRocketEntity firework && ((IShooterEntity) firework).client$getShooter() == entity && !ElytraAura.INSTANCE.isLeave(entity)) {
                        var fireworkResolved = MoveUtility.getResolvedPos(firework);
                        if (RotationUtility.squaredBoxDistance(mc.player, fireworkResolved) < md) {
                           interpolatedPos = fireworkResolved;
                        }
                    }
                }

                double minX = interpolatedPos.x - entity.getWidth() / 2.0;
                double maxX = interpolatedPos.x + entity.getWidth() / 2.0;
                double minY = interpolatedPos.y;
                double maxY = interpolatedPos.y + entity.getHeight();
                double minZ = interpolatedPos.z - entity.getWidth() / 2.0;
                double maxZ = interpolatedPos.z + entity.getWidth() / 2.0;

                vec3d = new Vec3d(
                        MathHelper.clamp(pos.x, minX + 0.05f, maxX - 0.05f),
                        MathHelper.clamp(pos.y, minY + 0.05f, maxY - 0.05f),
                        MathHelper.clamp(pos.z, minZ + 0.05f, maxZ - 0.05f)
                );
            }

            case MIDDLE -> {
                Vec3d interpolatedPos = getPoint(entity, GETPOINT.DEFAULT).add(resolve.subtract(getPoint(entity, GETPOINT.DEFAULT)));
                if (elytraAura.typeAim.is(ElytraAura.TypeAim.Resolve))
                    interpolatedPos = resolve;

                double minX = interpolatedPos.x - entity.getWidth() / 2.0;
                double maxX = interpolatedPos.x + entity.getWidth() / 2.0;
                double minY = interpolatedPos.y;
                double maxY = interpolatedPos.y + entity.getHeight();
                double minZ = interpolatedPos.z - entity.getWidth() / 2.0;
                double maxZ = interpolatedPos.z + entity.getWidth() / 2.0;

                vec3d = new Vec3d(
                        MathHelper.clamp(pos.x, minX + 0.05f, maxX - 0.05f),
                        MathHelper.clamp(pos.y, minY + 0.05f, maxY - 0.05f),
                        MathHelper.clamp(pos.z, minZ + 0.05f, maxZ - 0.05f)
                );
            }
        }

        return vec3d;
    }

    public Vec3d getUnLerpedPos(LivingEntity entity) {
        return new Vec3d(entity.lastX, entity.lastY, entity.lastZ);
    }

    public boolean shouldMovePredict(LivingEntity entity) {
        return SuperFirework.bpstarget(entity) >= 25 && !isStoyak(entity) && ElytraAura.INSTANCE.isLeave(entity) && entity.isGliding();
    }

    public boolean isStoyak(LivingEntity entity) {
        if (entity == null) return false;
        double d = entity.lastRenderX;
        double e = entity.lastRenderY;
        double f = entity.lastRenderZ;
        float offset = (float) entity.getLerpedPos(mc.getRenderTickCounter().getTickProgress(true)).subtract(new Vec3d(d, e, f)).length();

        return offset == 0 ;
    }

    public enum GETPOINT {
        DEFAULT,
        MIDDLE,
        FIREWORK
    }
}
