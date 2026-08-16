package gg.topchdlc.api.events.list;

import lombok.Generated;
import net.minecraft.client.util.math.MatrixStack;

public final class EventRender3D {
    private final MatrixStack matrix;
    private final float partialTicks;

    @Generated
    public MatrixStack getMatrix() {
        return this.matrix;
    }

    @Generated
    public float getPartialTicks() {
        return this.partialTicks;
    }

    @Generated
    public EventRender3D(MatrixStack matrix, float partialTicks) {
        this.matrix = matrix;
        this.partialTicks = partialTicks;
    }
}