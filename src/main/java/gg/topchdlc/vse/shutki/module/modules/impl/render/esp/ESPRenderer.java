package gg.topchdlc.vse.shutki.module.modules.impl.render.esp;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;

import java.awt.*;

public abstract class ESPRenderer {
    @Setter @Getter
    int index = 0;
    public float offsetX = 0, offsetY = 0;

    @Setter @Getter
    Align align = Align.TOP;

    public ESPRenderer(Align align) {
        this.align = align;
    }

    public abstract Color render(float minX, float minY, float maxX, float maxY, float width, Entity entity);

    public enum Align {
        TOP, LEFT, RIGHT, BOTTOM
    }
}
