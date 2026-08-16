package gg.topchdlc.api.drags;

import net.minecraft.client.MinecraftClient;
import gg.topchdlc.vse.utils.math.MathUtility;

import java.util.ArrayList;

public class Drags {
    final ArrayList<Drag> drags = new ArrayList<>();

    public void addDrag(Drag drag) {
        drags.add(drag);
    }
    public Drag findDrag(String name) {
        return drags.stream().filter(drag -> drag.name.equals(name)).findFirst().orElse(null);
    }

    public void click(double mouseX, double mouseY, int button) {
        for (Drag drag: drags) {
            if (drag.canDrag.get() && MathUtility.mouseIn(drag.x, drag.y, drag.width, drag.height, mouseX, mouseY)) {

                drag.dX = (float) (mouseX - drag.x);
                drag.dY = (float) (mouseY - drag.y);

                drag.dragging = true;
            }
        }
    }
    public void release(double mouseX, double mouseY) {
        for (Drag drag: drags) {
            drag.dragging = false;
        }
    }
    public void update(double mouseX, double mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return;
        
        float screenWidth = mc.getWindow().getScaledWidth();
        float screenHeight = mc.getWindow().getScaledHeight();
        
        for (Drag drag: drags) {
            if (drag.dragging) {
                drag.x = (float) (mouseX - drag.dX);
                drag.y = (float) (mouseY - drag.dY);

                drag.x = Math.max(0, Math.min(drag.x, screenWidth - drag.width));
                drag.y = Math.max(0, Math.min(drag.y, screenHeight - drag.height));
            }
        }
    }
}
