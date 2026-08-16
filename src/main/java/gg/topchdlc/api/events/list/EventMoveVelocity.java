package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;
import lombok.AllArgsConstructor;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

@AllArgsConstructor
public class EventMoveVelocity extends Event {
    public final MovementType type;
    public Vec3d movement;
}
