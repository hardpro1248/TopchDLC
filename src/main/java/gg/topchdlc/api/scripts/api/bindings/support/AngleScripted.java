package gg.topchdlc.api.scripts.api.bindings.support;

import gg.topchdlc.vse.rotation.Angle;
import org.joml.Vector3f;

public class AngleScripted {
    private Angle origin;
    public AngleScripted(float yaw, float pitch) {
        this.origin = new Angle(yaw, pitch);
    }
    public Vector3f toVector() {
        return origin.toVector().toVector3f();
    }
    public float yaw() {
        return origin.getYaw();
    }
    public float pitch() {
        return origin.getPitch();
    }
    public void yaw(float v) {
        origin.setYaw(v);
    }
    public void pitch(float v) {
        origin.setPitch(v);
    }
}
