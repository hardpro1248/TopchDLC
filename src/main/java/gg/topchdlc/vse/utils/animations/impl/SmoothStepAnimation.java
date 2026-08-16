package gg.topchdlc.vse.utils.animations.impl;

import gg.topchdlc.vse.utils.animations.OldAnimation;
import gg.topchdlc.vse.utils.animations.Direction;

public class SmoothStepAnimation extends OldAnimation {

    public SmoothStepAnimation(int ms, double endPoint) {
        super(ms, endPoint);
    }

    public SmoothStepAnimation(int ms, double endPoint, Direction direction) {
        super(ms, endPoint, direction);
    }

    protected double getEquation(double x) {
        double x1 = x / (double) duration;
        return -2 * Math.pow(x1, 3) + (3 * Math.pow(x1, 2));
    }

}
