package gg.topchdlc.vse.shutki.module.modules.impl.combat.elytraaura.defensive;

import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.ElytraAura;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@UtilityClass
public class ElytraAuraDef implements MinecraftHolder {
    ElytraAura elytraAura = ElytraAura.INSTANCE;

    public float lastAntiAimYaw = 0;
    public float lastAntiAimPitch = 0;

    public Vec3d updateAntiAimRotation(LivingEntity target, Vec2f[] customYawOffsets, Vec2f[] customPitchOffsets) {
        Random random = new Random();

        Vec2f[] yawOffsets = customYawOffsets;
        Vec2f[] pitchOffsets = customPitchOffsets;

        List<Vec2f> availableYawOffsets = new ArrayList<>();
        Vec3d resultVector = Vec3d.ZERO;

        float targetYaw = (float) Math.toDegrees(Math.atan2(
                (target.getEyePos().z - mc.player.getEyePos().z),
                (target.getEyePos().x - mc.player.getEyePos().x)
        )) - 90;

        for (Vec2f offset : yawOffsets) {
            float potentialYaw = MathHelper.wrapDegrees(targetYaw + offset.x);
            if (Math.abs(MathHelper.wrapDegrees(potentialYaw - lastAntiAimYaw)) > 45f) {
                availableYawOffsets.add(offset);
            }
        }

        if (availableYawOffsets.isEmpty()) {
            availableYawOffsets.addAll(Arrays.asList(yawOffsets));
        }

        Vec2f selectedYawOffset = availableYawOffsets.get(random.nextInt(availableYawOffsets.size()));
        resultVector = new Vec3d(MathHelper.wrapDegrees(targetYaw + selectedYawOffset.x), 0, 0);

        Vec2f selectedPitchOffset = pitchOffsets[random.nextInt(pitchOffsets.length)];
        resultVector = new Vec3d(resultVector.x, selectedPitchOffset.y, 0);

        Vec3d lookVec = getLookVector((float) resultVector.x, (float) resultVector.y);
        BlockHitResult collisionResult = mc.world.raycast(
                new RaycastContext(
                        mc.player.getEyePos(),
                        mc.player.getEyePos().add(lookVec.multiply(20)),
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE,
                        mc.player
                )
        );

        if (collisionResult.getType() == BlockHitResult.Type.BLOCK) {
            for (Vec2f offset : availableYawOffsets) {
                if (!offset.equals(selectedYawOffset)) {
                    float alternativeYaw = MathHelper.wrapDegrees(targetYaw + offset.x);
                    Vec3d altLookVec = getLookVector(alternativeYaw, (float) resultVector.y);

                    BlockHitResult altResult = mc.world.raycast(
                            new RaycastContext(
                                    mc.player.getEyePos(),
                                    mc.player.getEyePos().add(altLookVec.multiply(20)),
                                    RaycastContext.ShapeType.COLLIDER,
                                    RaycastContext.FluidHandling.NONE,
                                    mc.player
                            )
                    );

                    if (altResult.getType() != BlockHitResult.Type.BLOCK) {
                        resultVector = new Vec3d(alternativeYaw, resultVector.y, 0);
                        break;
                    }
                }
            }
        }

        lastAntiAimYaw = (float) resultVector.x;
        lastAntiAimPitch = (float) resultVector.y;

        return resultVector;
    }

    private Vec3d getLookVector(float yaw, float pitch) {
        float radYaw = (float) Math.toRadians(yaw);
        float radPitch = (float) Math.toRadians(pitch);
        float cosPitch = MathHelper.cos(-radPitch);
        return new Vec3d(
                -MathHelper.sin(radYaw) * cosPitch,
                -MathHelper.sin(-radPitch),
                MathHelper.cos(radYaw) * cosPitch
        );
    }
}
