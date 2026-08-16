package gg.topchdlc.vse.rotation;

import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.utils.math.RotationUtility;
import gg.topchdlc.vse.utils.math.SensUtility;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import gg.topchdlc.vse.utils.math.MathUtility;

import static net.minecraft.util.math.MathHelper.wrapDegrees;

@Getter
@Setter
@Data
public class Angle implements MinecraftHolder {
    public float yaw, pitch;

    public Angle(float yaw, float pitch) {
        this.yaw = Float.isNaN(yaw) ? 0 : yaw;
        this.pitch = Float.isNaN(pitch) ? 0 : pitch;
    }

    public Angle(float[] angles) {
        this(angles[0], angles[1]);
    }

    public Angle(Vec2f vec2f) {
        this.yaw = vec2f.x;
        this.pitch = vec2f.y;
    }

    public Angle(float yaw, float pitch, boolean forPlayer) {
        if (forPlayer) {
            this.yaw = Client.ROTATION.getRotate().getYaw() + (Float.isNaN(yaw) ? 0 : yaw);
            this.pitch = MathHelper.clamp(Client.ROTATION.getRotate().getPitch() + (Float.isNaN(pitch) ? 0 : pitch), -90, 90);
        } else {
            this.yaw = Float.isNaN(yaw) ? 0 : yaw;
            this.pitch = Float.isNaN(pitch) ? 0 : pitch;
        }
    }

    public Angle() {
        yaw = 0;
        pitch = 0;
    }

    public final Vec3d toVector() {
        return Vec3d.fromPolar(pitch, yaw);
    }

    public void set(Angle other) {
        yaw = other.yaw;
        pitch = other.pitch;
    }

    public Angle add(Angle other, boolean applyGcd) {
        float yaw = (float) (this.yaw + (applyGcd ? SensUtility.getSensitivity(other.yaw) : other.yaw));
        float pitch = (float) (this.pitch + (applyGcd ? SensUtility.getSensitivity(other.pitch) : other.pitch));
        return new Angle(yaw, pitch);
    }

    public Angle copy() {
        return new Angle(yaw, pitch);
    }

    public Angle lerpTowards(Angle other, float yawFactor, float pitchFactor) {
        return new Angle(MathUtility.linear(yaw, other.yaw, yawFactor), MathUtility.linear(pitch, other.pitch, pitchFactor));
    }

    public Angle clamp(float yawSpeed, float pitchSpeed) {
        return new Angle(MathHelper.clamp(yaw, -yawSpeed, yawSpeed), MathHelper.clamp(pitch, -pitchSpeed, pitchSpeed));
    }

    public Angle towards(Angle other, float yawFactor, float pitchFactor) {
        Angle delta = delta(other);
        float length = delta.length();
        float slYaw = Math.abs(delta.yaw / length) * yawFactor;
        float slPitch = Math.abs(delta.pitch / length) * pitchFactor;

        return new Angle(yaw + MathHelper.clamp(delta.yaw, -slYaw, slYaw), pitch + MathHelper.clamp(delta.pitch, -slPitch, slPitch));
    }

    public Angle delta(Angle other) {
        float yaw = MathHelper.wrapDegrees(other.getYaw() - this.yaw);
        float pitch = other.getPitch() - this.pitch;
        return new Angle(yaw, MathHelper.clamp(pitch, -90, 90));
    }

    public float length() {
        return MathHelper.sqrt(yaw * yaw + pitch * pitch);
    }

    public Angle fix() {
        Angle current = Client.ROTATION.getRotate();
        float getYaw = current.getYaw();
        float getPitch = current.getPitch();
        float yawDelta = wrapDegrees(yaw - getYaw);
        float pitchDelta = wrapDegrees(pitch - getPitch);

        yawDelta = (float) SensUtility.getSensitivity(yawDelta);
        pitchDelta = (float) SensUtility.getSensitivity(pitchDelta);
        return new Angle(getYaw + yawDelta, MathHelper.clamp(getPitch + pitchDelta, -90, 90));
    }

    public float angleTo(Angle other) {
        return delta(other).length();
    }

    public Angle towardsLinear(Angle target, float yawSpeed, float pitchSpeed) {
        Angle diff = delta(target);
        float angle = diff.length();
        float slYaw = Math.abs(diff.yaw / angle) * yawSpeed;
        float slPitch = Math.abs(diff.pitch / angle) * pitchSpeed;

        return new Angle(
                yaw + MathHelper.clamp(diff.yaw, -slYaw, slYaw),
                pitch + MathHelper.clamp(diff.pitch, -slPitch, slPitch)
        );
    }

    public static Angle fromPlayer() {
        return fromEntity(mc.player);
    }

    public static Angle fromEntity(Entity entity) {
        return new Angle(entity.getYaw(), entity.getPitch());
    }

    public static Angle deltaFrom(Angle current, Vec3d vector) {
        Angle target = RotationUtility.calculate(vector);
        float deltaYaw = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
        float deltaPitch = target.getPitch() - current.getPitch();
        return new Angle(deltaYaw, deltaPitch);
    }

    public static Angle fromRelative(Vec3d point, Vec3d from) {
        return fromDiffs(point.subtract(from));
    }

    public static Angle fromDiffs(Vec3d diffs) {
        return new Angle(
                MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(diffs.z, diffs.x)) - 90f),
                MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffs.y, Math.hypot(diffs.x, diffs.z))))
        );
    }
}
