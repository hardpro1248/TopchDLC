package gg.topchdlc.api.scripts.api.bindings;

import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.scripts.api.bindings.support.DragScripted;

public class DragProvider {
    public DragScripted create(String name, float baseX, float baseY, float baseWidth, float baseHeight) {
        Drag drag = new Drag(name, () -> true).bound(baseX, baseY, baseWidth, baseHeight);
        if (Client.DRAGS.findDrag(name) == null) {
            Client.DRAGS.addDrag(drag);
        } else {
            drag = Client.DRAGS.findDrag(name);
        }
        return new DragScripted(drag);
    }
}
