package gg.topchdlc.vse.shutki.module.modules.impl.combat.aura.utility;



import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.AuraModule;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.utils.attack.AttackHandle;
import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.math.RayTraceUtility;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;

import static gg.topchdlc.vse.utils.math.MathUtility.random;
import static java.lang.Math.toDegrees;
import static net.minecraft.util.math.MathHelper.*;

@UtilityClass
public class CalcRotUtility implements MinecraftHolder {
    AuraModule aura = AuraModule.INSTANCE;

    public float[] getDeltas(Angle angle) {
        float deltaX = MathHelper.wrapDegrees(angle.getYaw() - Client.ROTATION.getRotate().getYaw());
        float deltaY = angle.getPitch() - Client.ROTATION.getRotate().getPitch();
        return new float[]{deltaX, MathHelper.clamp(deltaY, -90, 90)};
    }

    public float[] getRandomJitter(Angle angle, float amplitude, float delimiter) {
        int hit = AttackHandle.countHit;
        int patternHit = hit % 8;
        int devisor = (int) MathUtility.logicalRandom(1, 1.5f, 450, 0.5f);
        double time = System.currentTimeMillis() * (amplitude / delimiter);
        float sin = (float) (Math.sin(time * ((double) AttackHandle.randomNumber1 / 10)) * amplitude * 0.9);
        float cos = (float) (Math.cos(time * ((double) AttackHandle.randomNumber2 / 10 * 0.5f)) * amplitude * 0.3);

        float[] jitterValue = new float[]{0, 0};

        switch (patternHit) {
            case 0 -> {
                jitterValue[0] = sin;
                jitterValue[1] = cos / devisor;
            }
            case 1 -> {
                jitterValue[0] = cos;
                jitterValue[1] = sin / devisor;
            }
            case 2 -> {
                jitterValue[0] = -cos;
                jitterValue[1] = sin / devisor;
            }
            case 3 -> {
                jitterValue[0] = cos;
                jitterValue[1] = -sin / devisor;
            }
            case 4 -> {
                jitterValue[0] = sin;
                jitterValue[1] = -cos / devisor;
            }
            case 5 -> {
                jitterValue[0] = -sin;
                jitterValue[1] = cos / devisor;
            }
            case 6 -> {
                jitterValue[0] = -sin;
                jitterValue[1] = -cos / devisor;
            }
            case 7 -> {
                jitterValue[0] = -cos;
                jitterValue[1] = -sin / devisor;
            }
        }

        jitterValue[0] += RandomPatterAuraUtility.random(angle, 0.95f, 1.05f);
        jitterValue[1] += RandomPatterAuraUtility.random(angle, 0.95f, 1.05f);

        return jitterValue;
    }

    public static Vec3d getPoint(Entity target) {
        if (target == null) return Vec3d.ZERO;
        return getBestPoint(mc.player.getEyePos(), target);
    }

    public Vec3d getBestPoint(Vec3d pos, Entity entity) {
        if (entity == null) return Vec3d.ZERO;
        return new Vec3d(
                clamp(pos.x,entity.getBoundingBox().minX ,entity.getBoundingBox().maxX ),
                clamp(pos.y,entity.getBoundingBox().minY ,entity.getBoundingBox().maxY ),
                clamp(pos.z,entity.getBoundingBox().minZ ,entity.getBoundingBox().maxZ )
        );
    }

    private Vec3d rotationPoint = Vec3d.ZERO;
    private Vec3d rotationMotion = Vec3d.ZERO;
    private SecureRandom random = new SecureRandom();


    public Vec3d getLegitLook(Entity target) {
        float minMotionXZ = 0.003f;
        float maxMotionXZ = 0.03f;
        float minMotionY = 0.001f;
        float maxMotionY = 0.03f;

        double lengthX = target.getBoundingBox().getLengthX();
        double lengthY = target.getBoundingBox().getLengthY();
        double lengthZ = target.getBoundingBox().getLengthZ();

        if (rotationMotion.equals(Vec3d.ZERO)) {
            rotationMotion = new Vec3d(
                    random.nextBoolean() ? random(minMotionXZ, maxMotionXZ) : -random(minMotionXZ, maxMotionXZ),
                    random.nextBoolean() ? random(minMotionY, maxMotionY) : -random(minMotionY, maxMotionY),
                    random.nextBoolean() ? random(minMotionXZ, maxMotionXZ) : -random(minMotionXZ, maxMotionXZ)
            );
        }

        float attackDist = aura.attackRange() * aura.attackRange();

        rotationPoint = rotationPoint.add(rotationMotion);
        boolean collision = false;

        if (rotationPoint.x >= (lengthX - 0.05) / 2f) {
            rotationMotion = new Vec3d(-random(minMotionXZ, maxMotionXZ),
                    random.nextBoolean() ? random(minMotionY, maxMotionY) : -random(minMotionY, maxMotionY),
                    random.nextBoolean() ? random(minMotionXZ, maxMotionXZ) : -random(minMotionXZ, maxMotionXZ));
            collision = true;
        } else if (rotationPoint.x <= -(lengthX - 0.05) / 2f) {
            rotationMotion = new Vec3d(random(minMotionXZ, maxMotionXZ),
                    random.nextBoolean() ? random(minMotionY, maxMotionY) : -random(minMotionY, maxMotionY),
                    random.nextBoolean() ? random(minMotionXZ, maxMotionXZ) : -random(minMotionXZ, maxMotionXZ));
            collision = true;
        }

        if (rotationPoint.y >= lengthY) {
            rotationMotion = new Vec3d(random.nextBoolean() ? random(minMotionXZ, maxMotionXZ) : -random(minMotionXZ, maxMotionXZ),
                    -random(minMotionY, maxMotionY),
                    random.nextBoolean() ? random(minMotionXZ, maxMotionXZ) : -random(minMotionXZ, maxMotionXZ));
            collision = true;
        } else if (rotationPoint.y <= 0.05) {
            rotationMotion = new Vec3d(random.nextBoolean() ? random(minMotionXZ, maxMotionXZ) : -random(minMotionXZ, maxMotionXZ),
                    random(minMotionY, maxMotionY),
                    random.nextBoolean() ? random(minMotionXZ, maxMotionXZ) : -random(minMotionXZ, maxMotionXZ));
            collision = true;
        }

        if (rotationPoint.z >= (lengthZ - 0.05) / 2f) {
            rotationMotion = new Vec3d(random.nextBoolean() ? random(minMotionXZ, maxMotionXZ) : -random(minMotionXZ, maxMotionXZ),
                    random.nextBoolean() ? random(minMotionY, maxMotionY) : -random(minMotionY, maxMotionY),
                    -random(minMotionXZ, maxMotionXZ));
            collision = true;
        }
        else if (rotationPoint.z <= -(lengthZ - 0.05) / 2f) {
            rotationMotion = new Vec3d(random.nextBoolean() ? random(minMotionXZ, maxMotionXZ) : -random(minMotionXZ, maxMotionXZ),
                    random.nextBoolean() ? random(minMotionY, maxMotionY) : -random(minMotionY, maxMotionY),
                    random(minMotionXZ, maxMotionXZ));
            collision = true;
        }

        if (collision) {
            rotationPoint = rotationPoint.add(
                    random(-0.05f, 0.05f),
                    random(-0.02f, 0.02f),
                    random(-0.05f, 0.05f)
            );
        } else {
            rotationPoint = rotationPoint.add(
                    random(-0.01f, 0.01f),
                    0f,
                    random(-0.01f, 0.01f)
            );
        }

        if (random.nextFloat() < 0.005f) {
            rotationMotion = new Vec3d(
                    random.nextBoolean() ? random(minMotionXZ, maxMotionXZ) : -random(minMotionXZ, maxMotionXZ),
                    random.nextBoolean() ? random(minMotionY, maxMotionY) : -random(minMotionY, maxMotionY),
                    random.nextBoolean() ? random(minMotionXZ, maxMotionXZ) : -random(minMotionXZ, maxMotionXZ)
            );
        }

        float[] rotation;

        if (!RayTraceUtility.checkRtx(Client.ROTATION.getRotate().getYaw(), Client.ROTATION.getRotate().getPitch(), aura.attackRange(), aura.attackRange(), target)) {
            float[] rotation1 = calcAngle(target.getEntityPos().add(0, target.getEyeHeight(target.getPose()) / 2f, 0));

            if (squaredDistanceFromEyes(target.getEntityPos().add(0, target.getEyeHeight(target.getPose()) / 2f, 0)) <= attackDist
                    && RayTraceUtility.checkRtx(rotation1[0], rotation1[1], aura.attackRange(), 0, target)) {
                rotationPoint = new Vec3d(random(-0.1f, 0.1f), target.getEyeHeight(target.getPose()) / (random(1.8f, 2.5f)), random(-0.1f, 0.1f));
            } else {
                float halfBox = (float) (lengthX / 2f);

                for (float x1 = -halfBox; x1 <= halfBox; x1 += 0.05f) {
                    for (float z1 = -halfBox; z1 <= halfBox; z1 += 0.05f) {
                        for (float y1 = 0.05f; y1 <= target.getBoundingBox().getLengthY(); y1 += 0.15f) {

                            Vec3d v1 = new Vec3d(target.getX() + x1, target.getY() + y1, target.getZ() + z1);

                            if (squaredDistanceFromEyes(v1) > attackDist) continue;

                            rotation = calcAngle(v1);
                            if (RayTraceUtility.checkRtx(rotation[0], rotation[1], aura.attackRange(), 0, target)) {
                                rotationPoint = new Vec3d(x1, y1, z1);
                                break;
                            }
                        }
                    }
                }
            }
        }

        return target.getEntityPos().add(rotationPoint);
    }

    public static float[] calcAngle(Vec3d to) {
        if (to == null) return null;
        double difX = to.x - mc.player.getEyePos().x;
        double difY = (to.y - mc.player.getEyePos().y) * -1.0;
        double difZ = to.z - mc.player.getEyePos().z;
        double dist = sqrt((float) (difX * difX + difZ * difZ));
        return new float[]{(float) wrapDegrees(toDegrees(Math.atan2(difZ, difX)) - 90.0), (float) wrapDegrees(toDegrees(Math.atan2(difY, dist)))};
    }

    public static float squaredDistanceFromEyes(Vec3d targetPos) {
        if (mc.player == null) return 0.0f;

        double dx = targetPos.x - mc.player.getX();
        double dy = targetPos.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double dz = targetPos.z - mc.player.getZ();

        return (float) (dx * dx + dy * dy + dz * dz);
    }

    public float[] calculateSmart(Vec3d vec) {
        final Vec3d diff = vec;
        final double distance = Math.hypot(diff.x, diff.z);
        float shortestYawPath = (float) ((Math.atan2(diff.z, diff.x) * MathUtility.TO_DEGREES) - 90.0F - Client.ROTATION.getRotate().getYaw() + 540.0F - 180.0F);
        float yaw = Client.ROTATION.getRotate().getYaw() + shortestYawPath;
        float pitch = MathHelper.clamp((float) (-(Math.atan2(diff.y, distance) * MathUtility.TO_DEGREES)), -90, 90);
        return new float[]{yaw, pitch};
    }

    public enum VEC_TYPE {
        FLOATING_POINT,
        MULTIPOINT,
        DEFAULT_POINT
    }
}
