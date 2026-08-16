package gg.topchdlc.vse.rotation.all.impl;

import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.all.RotationChoice;
import net.minecraft.util.math.MathHelper;

/**
 * Create by daun kvass
 */
public class NRotation extends RotationChoice {
    public NRotation() {
        super("NRotation");
    }

    @Override
    public Angle calculate(Angle current, Angle target) {
        return new Angle(mc.player.getYaw()+1,mc.player.getPitch() );
    }
}
