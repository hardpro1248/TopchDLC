package gg.topchdlc.vse.shutki.module.modules.impl.combat.resolver;

import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import net.minecraft.util.math.Vec3d;

public abstract class ResolverMode extends Choice {
    protected ResolverMode(String name) {
        super(name);
    }

    public Vec3d resolveAura(Vec3d origin, float cooldown) { return origin; }
    public Vec3d resolveElytra(Vec3d origin, float cooldown) { return origin; }
    public boolean isFakeLagging() { return false; }
}
