package gg.topchdlc.vse.utils.player;

import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.MovementCorrection;
import gg.topchdlc.vse.rotation.all.Rotation;
import gg.topchdlc.vse.utils.math.Priorities;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class PathExecutor implements MinecraftHolder {
    private List<BlockPos> path;
    private int pathIndex = 0;
    private BlockPos targetEntityPos;

    private Vec3d lastPos = Vec3d.ZERO;
    private int stuckTicks = 0;
    private int ticksAtWaypoint = 0;
    private int collisionTicks = 0;
    private final int maxStuckTicks = 25;
    private boolean stuck = false;
    private float moveYaw = 0f;
    private boolean active = false;

    private int backoffTicks = 0;
    private int localStuckRetries = 0;

    private static final Rotation LEGIT_ROTATOR = (cur, tgt) -> {
        float step = 18f;
        float dy = MathHelper.clamp(MathHelper.wrapDegrees(tgt.getYaw() - cur.getYaw()), -step, step);
        float dp = MathHelper.clamp(tgt.getPitch() - cur.getPitch(), -step, step);
        return new Angle(cur.getYaw() + dy, MathHelper.clamp(cur.getPitch() + dp, -90f, 90f));
    };

    public List<BlockPos> getPath() {
        return path;
    }

    public int getPathIndex() {
        return pathIndex;
    }

    public void start(List<BlockPos> path, BlockPos targetEntityPos) {
        this.path = path;
        this.targetEntityPos = targetEntityPos;
        this.pathIndex = 0;
        this.stuckTicks = 0;
        this.ticksAtWaypoint = 0;
        this.collisionTicks = 0;
        this.backoffTicks = 0;
        this.localStuckRetries = 0;
        this.stuck = false;
        this.active = true;
        this.lastPos = mc.player.getEntityPos();
    }

    public void stop() {
        this.active = false;
        this.path = null;
        if (mc.options != null) {
            mc.options.forwardKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
            mc.options.backKey.setPressed(false);
        }
    }

    public void tick() {
        if (!active || path == null || pathIndex >= path.size()) {
            stop();
            return;
        }

        if (backoffTicks > 0) {
            backoffTicks--;
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(true);

            if (backoffTicks % 3 == 0) {
                mc.options.jumpKey.setPressed(true);
            } else {
                mc.options.jumpKey.setPressed(false);
            }
            lastPos = mc.player.getEntityPos();
            return;
        }

        double pmX = mc.player.getX() - mc.player.lastX;
        double pmZ = mc.player.getZ() - mc.player.lastZ;
        double speedXZ = Math.sqrt(pmX * pmX + pmZ * pmZ);

        double distMoved = mc.player.getEntityPos().squaredDistanceTo(lastPos);
        if (distMoved < 0.0004) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
        }
        lastPos = mc.player.getEntityPos();

        if (stuckTicks > maxStuckTicks) {
            stuckTicks = 0;
            localStuckRetries++;

            if (localStuckRetries <= 2) {
                backoffTicks = 12;
                return;
            } else {
                stuck = true;
                localStuckRetries = 0;
                return;
            }
        }

        BlockPos waypoint = path.get(pathIndex);
        BlockPos playerPosInt = mc.player.getBlockPos();

        double dx = (waypoint.getX() + 0.5) - mc.player.getX();
        double dz = (waypoint.getZ() + 0.5) - mc.player.getZ();
        double dist2D = Math.hypot(dx, dz);

        double verticalDiff = mc.player.getY() - waypoint.getY();
        boolean verticallyClose = (verticalDiff >= -0.5 && verticalDiff <= 1.25) ||
                (mc.player.getVelocity().y < -0.08 && mc.player.getY() > waypoint.getY());

        double targetSwitchDistance = 0.25;
        if (Math.abs(waypoint.getY() - mc.player.getY()) > 0.1) {
            targetSwitchDistance = 0.15;
        }

        boolean skipWaypoint = false;

        if (verticallyClose) {
            if (dist2D < targetSwitchDistance) {
                skipWaypoint = true;
            } else if (dist2D < 0.60 && ticksAtWaypoint > 10) {
                skipWaypoint = true;
            } else if (speedXZ > 0.10 && dist2D < 0.80) {
                double toWPX = (waypoint.getX() + 0.5) - mc.player.getX();
                double toWPZ = (waypoint.getZ() + 0.5) - mc.player.getZ();
                double dotProduct = pmX * toWPX + pmZ * toWPZ;
                if (dotProduct < 0) {
                    skipWaypoint = true;
                }
            }
        }

        if (skipWaypoint) {
            pathIndex++;
            ticksAtWaypoint = 0;
            if (pathIndex >= path.size()) {
                stop();
                return;
            }
            waypoint = path.get(pathIndex);
            dx = (waypoint.getX() + 0.5) - mc.player.getX();
            dz = (waypoint.getZ() + 0.5) - mc.player.getZ();
        } else {
            ticksAtWaypoint++;
        }

        boolean onClimbable = Pathfinder.isClimbable(playerPosInt) || Pathfinder.isClimbable(playerPosInt.up());
        boolean inWater = mc.player.isTouchingWater();

        BlockState waypointGroundState = mc.world.getBlockState(waypoint.down());
        boolean isStepUp = waypointGroundState.getBlock() instanceof StairsBlock ||
                waypointGroundState.getBlock() instanceof SlabBlock;

        if (mc.player.horizontalCollision && speedXZ < 0.08f) {
            collisionTicks++;
        } else {
            collisionTicks = 0;
        }

        boolean isColliding = mc.player.horizontalCollision || collisionTicks > 0;
        double yDiff = waypoint.getY() - mc.player.getY();

        boolean shouldJump = ((yDiff > 0.5 && (!isStepUp || isColliding))
                || isColliding
                || stuckTicks > 5)
                && mc.player.isOnGround() && !onClimbable && !inWater;

        if (shouldJump) {
            mc.options.jumpKey.setPressed(true);
            collisionTicks = 0;
        } else {
            mc.options.jumpKey.setPressed(false);
        }

        mc.options.backKey.setPressed(false);

        moveYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) -90f;

        float targetPitch = (onClimbable && waypoint.getY() > playerPosInt.getY()) ? -15f : -15f;
        Angle lookAngle = new Angle(moveYaw, targetPitch);

        Client.ROTATION.rotate(LEGIT_ROTATOR, lookAngle, 5, true, MovementCorrection.STRICT, Priorities.NORMAL);

        mc.options.forwardKey.setPressed(true);
    }

    public boolean isCompleted() {
        return active && path != null && pathIndex >= path.size();
    }

    public boolean isStuck() {
        return stuck;
    }

    public void clearStuck() {
        this.stuck = false;
        this.stuckTicks = 0;
        this.ticksAtWaypoint = 0;
        this.collisionTicks = 0;
        this.backoffTicks = 0;
        this.localStuckRetries = 0;
    }

    public boolean isActive() {
        return active;
    }

    public BlockPos getTargetEntityPos() {
        return targetEntityPos;
    }
}