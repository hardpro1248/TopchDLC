package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;
import lombok.AllArgsConstructor;
import net.minecraft.util.math.Vec3d;

@AllArgsConstructor
public class EventVelocity extends Event {
    public final Vec3d movementInput;
    public final float speed;
    public final float yaw;
    public Vec3d velocity;
}
