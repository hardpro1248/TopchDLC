package gg.topchdlc.vse.utils.math;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import gg.topchdlc.vse.rotation.Angle;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Predicate;

import static gg.topchdlc.MinecraftHolder.mc;

public class RayTraceUtility {
    public static BlockHitResult raycast(double range, Angle angle, boolean includeFluids) {
        return raycast(Objects.requireNonNull(mc.player).getCameraPosVec(1.0F), range, angle, includeFluids);
    }

    public static BlockHitResult raycast(Vec3d vec, double range, Angle angle, boolean includeFluids) {
        Entity entity = mc.getCameraEntity();

        if (entity == null) {
            return null;
        }

        Vec3d rotationVec = angle.toVector();
        Vec3d end = vec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range);

        World world = mc.world;
        if (world == null) {
            return null;
        }

        RaycastContext.FluidHandling fluidHandling = includeFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE;
        RaycastContext context = new RaycastContext(vec, end, RaycastContext.ShapeType.OUTLINE, fluidHandling, entity);

        return world.raycast(context);
    }

    public static BlockHitResult raycast(Vec3d start, Vec3d end, RaycastContext.ShapeType shapeType) {
        return raycast(start, end, shapeType, mc.player);
    }

    public static BlockHitResult raycast(Vec3d start, Vec3d end, RaycastContext.ShapeType shapeType, Entity entity) {
        return mc.world.raycast(new RaycastContext(start, end, shapeType, RaycastContext.FluidHandling.NONE, entity));
    }

    public static EntityHitResult raytraceEntity(double range, Angle angle, Predicate<Entity> filter) {
        Entity entity = mc.getCameraEntity();
        if (entity == null) return null;

        Vec3d cameraVec = entity.getCameraPosVec(1.0F);
        Vec3d rotationVec = angle.toVector();

        Vec3d vec3d3 = cameraVec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range);
        Box box = entity.getBoundingBox().stretch(rotationVec.multiply(range)).expand(1.0, 1.0, 1.0);

        return ProjectileUtil.raycast(entity, cameraVec, vec3d3, box, (e) -> !e.isSpectator() && filter.test(e), range * range);
    }

    public static boolean checkRtx(float yaw, float pitch, float distance, float wallDistance, Entity entity) {
        HitResult result = rayTrace(distance, yaw, pitch);
        Vec3d startPoint = mc.player.getEyePos();
        double distancePow2 = Math.pow(distance, 2);

        if (result != null)
            distancePow2 = startPoint.squaredDistanceTo(result.getPos());

        Vec3d rotationVector = getRotationVector(pitch, yaw).multiply(distance);
        Vec3d endPoint = startPoint.add(rotationVector);

        Box entityArea = mc.player.getBoundingBox().stretch(rotationVector).expand(1.0, 1.0, 1.0);

        EntityHitResult ehr;

        double maxDistance = Math.max(distancePow2, Math.pow(wallDistance, 2));

        ehr = ProjectileUtil.raycast(mc.player, startPoint, endPoint, entityArea, e -> !e.isSpectator() && e.canHit() && e == entity, maxDistance);

        if (ehr != null) {
            boolean allowedWallDistance = startPoint.squaredDistanceTo(ehr.getPos()) <= Math.pow(wallDistance, 2);
            boolean wallMissing = result == null;
            boolean wallBehindEntity = startPoint.squaredDistanceTo(ehr.getPos()) < distancePow2;
            boolean allowWallHit = wallMissing || allowedWallDistance || wallBehindEntity;

            if (allowWallHit && startPoint.squaredDistanceTo(ehr.getPos()) <= Math.pow(distance, 2))
                return ehr.getEntity() == entity;
        }

        return false;
    }

    public static @NotNull Vec3d getRotationVector(float yaw, float pitch) {
        return new Vec3d(MathHelper.sin(-pitch * 0.017453292F) * MathHelper.cos(yaw * 0.017453292F), -MathHelper.sin(yaw * 0.017453292F), MathHelper.cos(-pitch * 0.017453292F) * MathHelper.cos(yaw * 0.017453292F));
    }


    public static HitResult rayTrace(double dst, float yaw, float pitch) {
        Vec3d vec3d = mc.player.getCameraPosVec(1f);
        Vec3d vec3d2 = getRotationVector(pitch, yaw);
        Vec3d vec3d3 = vec3d.add(vec3d2.x * dst, vec3d2.y * dst, vec3d2.z * dst);
        return mc.world.raycast(new RaycastContext(vec3d, vec3d3, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
    }

    public static boolean rayTrace(Vec3d clientVec, double range, Box box) {
        Vec3d cameraVec = Objects.requireNonNull(mc.player).getEyePos();
        return box.contains(cameraVec) || box.raycast(cameraVec, cameraVec.add(clientVec.multiply(range))).isPresent();
    }
}
