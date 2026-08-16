package gg.topchdlc.vse.utils.animations.impl;


import gg.topchdlc.vse.utils.animations.OldAnimation;
import gg.topchdlc.vse.utils.animations.Direction;

public class DecelerateAnimation extends OldAnimation {

    public DecelerateAnimation(int ms, double endPoint) {
        super(ms, endPoint);
    }

    public DecelerateAnimation(int ms, double endPoint, Direction direction) {
        super(ms, endPoint, direction);
    }

    protected double getEquation(double x) {
        double x1 = x / duration;
        return 1 - ((x1 - 1) * (x1 - 1));
    }
}
