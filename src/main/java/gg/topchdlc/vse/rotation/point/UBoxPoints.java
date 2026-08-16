package gg.topchdlc.vse.rotation.point;

import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.utils.math.MathUtility;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Create by daun kvass
 */
public class UBoxPoints implements MinecraftHolder {


    public static Vec3d getBestVector3dOnEntityBox(Box box, Vec3d eyes) {
        if (box == null) return mc.player != null ? mc.player.getEyePos() : Vec3d.ZERO;
        if (eyes == null) eyes = mc.player.getEyePos();

        List<Vec3d> candidatePoints = generateSmartPoints(box);

        Vec3d bestVisiblePoint = null;
        double bestVisibleDistance = Double.MAX_VALUE;

        Vec3d bestAnyPoint = null;
        double bestAnyDistance = Double.MAX_VALUE;

        for (Vec3d point : candidatePoints) {
            double distSq = eyes.squaredDistanceTo(point);

            if (distSq < bestAnyDistance) {
                bestAnyDistance = distSq;
                bestAnyPoint = point;
            }

            if (isVisible(eyes, point)) {
                if (distSq < bestVisibleDistance) {
                    bestVisibleDistance = distSq;
                    bestVisiblePoint = point;
                }
            }
        }

        return bestVisiblePoint != null ? bestVisiblePoint : (bestAnyPoint != null ? bestAnyPoint : box.getCenter());
    }

    public static Vec3d getBestVector3dOnEntityBox(Box box) {
        return getBestVector3dOnEntityBox(box, mc.player != null ? mc.player.getEyePos() : Vec3d.ZERO);
    }

    private static List<Vec3d> generateSmartPoints(Box box) {
        List<Vec3d> points = new ArrayList<>(15);
        double minX = box.minX;
        double maxX = box.maxX;
        double minY = box.minY;
        double maxY = box.maxY;
        double minZ = box.minZ;
        double maxZ = box.maxZ;
        double centerX = minX + (maxX - minX) * 0.5;
        double centerZ = minZ + (maxZ - minZ) * 0.5;
        double innerX = (maxX - minX) * 0.25;
        double innerZ = (maxZ - minZ) * 0.25;
        double[] heights = new double[] {
                minY + (maxY - minY) * 0.85D,
                minY + (maxY - minY) * 0.50D,
                minY + (maxY - minY) * 0.15D
        };

        for (double y : heights) {
            points.add(new Vec3d(centerX, y, centerZ));
            points.add(new Vec3d(centerX + innerX, y, centerZ));
            points.add(new Vec3d(centerX - innerX, y, centerZ));
            points.add(new Vec3d(centerX, y, centerZ + innerZ));
            points.add(new Vec3d(centerX, y, centerZ - innerZ));
        }

        return points;
    }

    public static boolean isVisible(Vec3d start, Vec3d end) {
        if (mc.world == null || mc.player == null) return false;

        HitResult result = mc.world.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        return result.getType() == HitResult.Type.MISS;
    }
}