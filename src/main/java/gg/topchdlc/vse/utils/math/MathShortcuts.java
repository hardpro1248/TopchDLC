package gg.topchdlc.vse.utils.math;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MathShortcuts {
    public final double PI = Math.PI;
    public final float FPI = (float)PI;
    public final double DtoRadians = MathUtility.TO_RADIANS;
    public final float FtoRadians = (float)DtoRadians;

    public double dcos(double value) {
        return Math.cos(value);
    }

    public float cos(float value) {
        return (float)dcos(value);
    }

    public double dsin(double value) {
        return Math.sin(value);
    }

    public float sin(float value) {
        return (float)dsin(value);
    }

    public double datan2(double y, double x) {
        return Math.atan2(y, x);
    }

    public float atan2(float y, float x) {
        return (float)datan2(y, x);
    }
}
