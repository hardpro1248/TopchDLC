package gg.topchdlc.api.scripts.api.bindings.support;

import gg.topchdlc.api.drags.Drag;

public class DragScripted {
    private Drag origin;
    public DragScripted(Drag origin) {
        this.origin = origin;
    }
    public float getX() { return origin.x; }
    public float getY() { return origin.y; }
    public float getWidth() { return origin.width; }
    public float getHeight() { return origin.height; }

    public void setX(float x) { origin.x = x; }
    public void setY(float y) { origin.y = y; }
    public void setWidth(float width) { origin.width = width; }
    public void setHeight(float height) { origin.height = height; }
}
