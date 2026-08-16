package gg.topchdlc.vse.rotation.all;

import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.rotation.Angle;

public interface Rotation extends MinecraftHolder {
    Angle calculate(Angle current, Angle target);
}
