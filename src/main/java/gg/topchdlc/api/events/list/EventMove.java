package gg.topchdlc.api.events.list;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.Vec3d;
import gg.topchdlc.api.events.Event;

@Getter
public class EventMove extends Event {
    private static final EventMove instance = new EventMove();

    public double x, y, z;
    public float yaw, pitch;
    public boolean ground, horizontalCollision;

    @Setter
    public Vec3d motion;

    public Vec3d getPos() {
        return new Vec3d(x, y, z);
    }

    public static EventMove build(double x, double y, double z, float yaw, float pitch, Vec3d motion, boolean ground, boolean horizontalCollision) {
        instance.x = x;
        instance.y = y;
        instance.z = z;
        instance.yaw = yaw;
        instance.pitch = pitch;
        instance.motion = motion;
        instance.ground = ground;
        instance.horizontalCollision = horizontalCollision;
        instance.reset();
        return instance;
    }
}
