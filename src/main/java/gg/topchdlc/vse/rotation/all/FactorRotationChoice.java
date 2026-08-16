package gg.topchdlc.vse.rotation.all;

import gg.topchdlc.vse.rotation.Angle;

public abstract class FactorRotationChoice extends RotationChoice {
    protected FactorRotationChoice(String name) {
        super(name);
    }

    public abstract Angle getFactors(Angle current, Angle target);

    @Override
    public Angle calculate(Angle current, Angle target) {
        Angle factors = getFactors(current, target);
        return current.towardsLinear(target, factors.getYaw(), factors.getPitch());
    }
}
