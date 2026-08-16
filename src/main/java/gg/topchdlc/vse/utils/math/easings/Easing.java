package gg.topchdlc.vse.utils.math.easings;

@FunctionalInterface
public interface Easing {
    double ease(double value);
    default float easef(float value) {
        return (float)ease(value);
    }
}