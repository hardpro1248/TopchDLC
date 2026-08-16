package gg.topchdlc.vse.utils.math;

import gg.topchdlc.Client;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.aura.utility.CalcRotUtility;
import gg.topchdlc.vse.rotation.point.UBoxPoints;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import gg.topchdlc.vse.rotation.Angle;
import org.joml.Vector4f;

import static gg.topchdlc.MinecraftHolder.mc;
import static java.lang.Math.toDegrees;
import static net.minecraft.util.math.MathHelper.clamp;
import static net.minecraft.util.math.MathHelper.wrapDegrees;

@UtilityClass
public class RotationUtility {

    public Vec3d getClosestVec(Vec3d vec, Box AABB) {
        return new Vec3d(
                clamp(vec.getX(), AABB.minX, AABB.maxX),
                clamp(vec.getY(), AABB.minY, AABB.maxY),
                clamp(vec.getZ(), AABB.minZ, AABB.maxZ)
        );
    }

    public Vec3d getClosestVec(Vec3d vec, Entity entity) {
        return getClosestVec(vec, entity.getBoundingBox());
    }

    public Vec3d getClosestVec(Entity entity) {
        Vec3d eyePosVec = mc.player.getEyePos();
        return getClosestVec(eyePosVec, entity).subtract(eyePosVec);
    }

    public double getStrictDistance(LivingEntity entity) {
        return getClosestVec(entity).length();
    }

    public static Angle calculate(Vec3d vec, LivingEntity entity) {
        final Vec3d diff = vec.subtract(entity.getEyePos());
        final double distance = Math.hypot(diff.x, diff.z);
        float yaw = (float) (MathHelper.atan2(diff.z, diff.x) * MathUtility.TO_DEGREES) - 90.0F;
        final float pitch = (float) (-(MathHelper.atan2(diff.y, distance) * MathUtility.TO_DEGREES));
        return new Angle(yaw, pitch);
    }

    public static Angle calculate(Vec3d vec) {
        final Vec3d diff = vec.subtract(mc.player.getEyePos());
        final double distance = Math.hypot(diff.x, diff.z);
        float yaw = (float) (MathHelper.atan2(diff.z, diff.x) * MathUtility.TO_DEGREES) - 90.0F;
        final float pitch = (float) (-(MathHelper.atan2(diff.y, distance) * MathUtility.TO_DEGREES));
        return new Angle(yaw, pitch);
    }

    public static Angle calcRotateOnElytra(Vec3d vec, boolean eyepos) {
        final Vec3d diff = eyepos ? vec.subtract(mc.player.getEyePos()) : vec;
        float shortestYawPath = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F - Client.ROTATION.getRotate().getYaw() + 540.0F - 180.0F);
        float yaw = Client.ROTATION.getRotate().getYaw() + shortestYawPath;
        float pitch = MathHelper.clamp((float) (-Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.z, diff.x)))), -90, 90);
        yaw = MathHelper.wrapDegrees(yaw);
        return new Angle(yaw, pitch);
    }

    public static Angle calcRotate(Vec3d vec, boolean eyepos) {
        final Vec3d diff = eyepos ? vec.subtract(mc.player.getEyePos()) : vec;
        float shortestYawPath = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F - Client.ROTATION.getRotate().getYaw() + 540.0F - 180.0F);
        float yaw = Client.ROTATION.getRotate().getYaw() + shortestYawPath;
        float pitch = MathHelper.clamp((float) (-Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.z, diff.x)))), -90, 90);
        return new Angle(MathHelper.wrapDegrees(yaw), pitch);
    }

    public Vector4f calculateRotationFromCamera(LivingEntity target) {
        Vec3d vec = UBoxPoints.getBestVector3dOnEntityBox(target.getBoundingBox()).subtract(mc.player.getEyePos());

        float rawYaw = (float) wrapDegrees(toDegrees(Math.atan2(vec.z, vec.x)) - 90F);
        float rawPitch = (float) (-toDegrees(Math.atan2(vec.y, Math.sqrt(Math.pow(vec.x, 2) + Math.pow(vec.z, 2)))));
        float yawDelta = wrapDegrees(rawYaw - Client.ROTATION.getRotate().getYaw());
        float pitchDelta = rawPitch - Client.ROTATION.getRotate().getPitch();

        return new Vector4f(rawYaw, rawPitch, yawDelta, pitchDelta);
    }

    public double calculateFOVFromCamera(LivingEntity target) {
        Vector4f rotation = calculateRotationFromCamera(target);
        float yawDelta = rotation.z;
        float pitchDelta = rotation.w;

        return Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
    }

    public double squaredBoxDistance(Box from, Vec3d to) {
        Vec3d pos = getClosestVec(to, from);
        return pos.squaredDistanceTo(to);
    }

    public double squaredBoxDistance(Entity from, Vec3d to) {
        return squaredBoxDistance(from.getBoundingBox(), to);
    }

    public double squaredBoxDistance(Entity from, Entity to) {
        return squaredBoxDistance(from, to.getEyePos());
    }

    public double squaredBoxDistance(Entity from, Entity to, Vec3d offs) {
        return squaredBoxDistance(from.getBoundingBox().offset(offs.subtract(from.getEntityPos())), to.getEyePos());
    }
}
