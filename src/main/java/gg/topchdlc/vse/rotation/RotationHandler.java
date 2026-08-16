package gg.topchdlc.vse.rotation;

import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventInput;
import gg.topchdlc.api.events.list.EventVelocity;
import gg.topchdlc.vse.rotation.all.ISnapRotation;
import gg.topchdlc.vse.rotation.all.Rotation;
import gg.topchdlc.vse.rotation.all.impl.DefaultRotation;
import gg.topchdlc.vse.utils.math.Priorities;
import gg.topchdlc.vse.utils.player.MoveUtility;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;

import static gg.topchdlc.MinecraftHolder.mc;

@Getter
public class RotationHandler {
    private Angle rotate = null;
    private boolean rotateInitialized = false;
    private Angle prevRotate = null;
    private Angle targetAngle = null;
    private final Angle visual = new Angle();

    public Angle getRotate() {
        if (rotate == null || !rotateInitialized) {
            if (mc.player != null) {
                rotate = Angle.fromPlayer();
                rotateInitialized = true;
            }
            return Angle.fromPlayer();
        }
        return rotate;
    }

    private int timeout = 0;
    private MovementCorrection correction = MovementCorrection.NONE;
    private Rotation executor;
    private boolean shouldLerp = false;
    private int priority = 0;

    private boolean rotating = false;

    public RotationHandler() {
        Client.EVENTS.register(this);
    }
    public void reset() {
        this.rotating = false;
        this.timeout = 0;
        this.priority = Integer.MIN_VALUE;
        this.correction = MovementCorrection.NONE;
        this.executor = null;
        this.targetAngle = null;
        this.prevRotate = null;
        if (mc.player != null) {
            this.rotate = Angle.fromPlayer();
            this.rotateInitialized = true;
            this.visual.setYaw(mc.player.getYaw());
            this.visual.setPitch(mc.player.getPitch());
        }
    }
    @Deprecated
    public void rotate(Angle target, boolean shouldLerp) {
        rotate(DefaultRotation.INSTANCE, target, 5, shouldLerp, MovementCorrection.STRICT, Priorities.NORMAL);
    }


    public void rotate(Rotation executor, Angle targetAngle, int timeout, boolean shouldLerp, MovementCorrection correction, int priority) {
        if (this.priority > priority) return;
        this.executor = executor;
        this.targetAngle = targetAngle;
        this.timeout = timeout;
        this.shouldLerp = shouldLerp;
        this.correction = correction;
        this.rotating = true;
        this.prevRotate = this.rotate.copy();
        this.rotate = executor.calculate(this.rotate, targetAngle).fix();
        this.priority = priority;
    }

    EventBus<Event> ontick = event -> {
        if (event instanceof EventGameTick) {
            if (targetAngle != null && executor != null && timeout > 0) {
                timeout--;

                Angle current = getRotate();
                rotate = executor.calculate(current, targetAngle).fix();
            } else {
                priority = Integer.MIN_VALUE;
                if (rotating) {
                    boolean snapReturning = executor instanceof ISnapRotation
                            && ((ISnapRotation) executor).isReturning();
                    float d = Math.abs(MathHelper.wrapDegrees(mc.player.getYaw() - rotate.getYaw())) + Math.abs(mc.player.getPitch() - rotate.getPitch());
                    if (snapReturning) {
                        rotate = executor.calculate(rotate, Angle.fromPlayer()).fix();
                        if (!((ISnapRotation) executor).isReturning()) {
                            rotating = false;
                        }
                    } else if (shouldLerp) {
                        rotate = executor.calculate(rotate, Angle.fromPlayer()).fix();
                        if (d < 5) {
                            rotating = false;
                        }
                    } else {
                        rotate = Angle.fromPlayer();
                        correction = MovementCorrection.NONE;
                        rotating = false;
                    }
                }
            }
            if (!rotating) {
                rotate = Angle.fromPlayer();
            }
        }

        if (event instanceof Event3D) {
            float tick = mc.getRenderTickCounter().getTickProgress(true);

            Angle rotate = getRotate();
            float targetYaw = rotate.getYaw();
            float targetPitch = rotate.getPitch();

            visual.setYaw(MathHelper.lerp(tick, visual.getYaw(), visual.getYaw() + MathHelper.wrapDegrees(targetYaw - visual.getYaw()) * 0.5f));
            visual.setPitch(MathHelper.lerp(tick, visual.getPitch(), targetPitch));
            boolean isSnapRotation = executor instanceof ISnapRotation;
            if (correction == MovementCorrection.LOOK && rotating && !isSnapRotation) {
                mc.player.setYaw((float) (mc.player.getYaw() + MathHelper.wrapDegrees(visual.getYaw() - mc.player.getYaw()) * 0.5));
                mc.player.setPitch(mc.player.getPitch() + (visual.getPitch() - mc.player.getPitch()) * 0.5f);
            }
        }

        if (event instanceof EventInput e && rotate != null && correction == MovementCorrection.SILENT) {
            MoveUtility.silentCorrection(e, rotate.getYaw());
        }

        if (event instanceof EventInput e && rotate != null && correction == MovementCorrection.TARGET) {
            handleTargetCorrection(e);
        }

        if (event instanceof EventVelocity e && rotate != null && correction != MovementCorrection.NONE) {
            e.velocity = Entity.movementInputToVelocity(e.movementInput, e.speed, rotate.getYaw());
        }
    };

    private void handleTargetCorrection(EventInput e) {
        if (mc.player == null || TargetsUtility.getTarget() == null) return;

        float forward = e.getForward();
        float strafe = e.getStrafe();

        if (forward == 0 && strafe == 0) return;
        if (forward != 1 || strafe != 0) return;

        Vec3d playerPos = mc.player.getEntityPos();
        Vec3d targetPos = TargetsUtility.getTarget().getEntityPos();

        double deltaX = targetPos.x - playerPos.x;
        double deltaZ = targetPos.z - playerPos.z;
        double angleToTarget = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0;
        angleToTarget = MathHelper.wrapDegrees(angleToTarget);

        float yaw = rotate.getYaw();
        float bestForward = 0F;
        float bestStrafe = 0F;
        float minDifference = Float.MAX_VALUE;

        for (float testForward = -1F; testForward <= 1F; testForward += 1F) {
            for (float testStrafe = -1F; testStrafe <= 1F; testStrafe += 1F) {
                if (testForward == 0F && testStrafe == 0F) {
                    continue;
                }

                double moveAngle = getMoveDirection(yaw, testForward, testStrafe);
                moveAngle = Math.toDegrees(moveAngle);
                moveAngle = MathHelper.wrapDegrees(moveAngle);

                double difference = Math.abs(MathHelper.wrapDegrees(angleToTarget - moveAngle));
                difference = Math.min(difference, 360 - difference);

                if (difference < minDifference) {
                    minDifference = (float) difference;
                    bestForward = testForward;
                    bestStrafe = testStrafe;
                }
            }
        }

        e.setForward(bestForward);
        e.setStrafe(bestStrafe);
    }
    public void set(float yaw, float pitch) {
        Angle target = new Angle(yaw, pitch);
        this.rotate(DefaultRotation.INSTANCE, target, 5, false, correction, Priorities.FLY);
    }
    private double getMoveDirection(float yaw, float forward, float strafe) {
        if (forward == 0.0f && strafe == 0.0f) {
            return 0.0;
        }
        double angle = Math.atan2(-strafe, forward);
        angle += Math.toRadians(yaw);
        return angle;
    }
}
