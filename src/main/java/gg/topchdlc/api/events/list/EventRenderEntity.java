package gg.topchdlc.api.events.list;

import gg.topchdlc.api.events.Event;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

public class EventRenderEntity extends Event {
    private static EventRenderEntity instance = new EventRenderEntity();

    public VertexConsumerProvider.Immediate vertexConsumers;
    public MatrixStack matrixStack;
    public static EventRenderEntity build(VertexConsumerProvider.Immediate vertexConsumers, MatrixStack matrixStack) {
        instance.vertexConsumers = vertexConsumers;
        instance.matrixStack = matrixStack;
        instance.reset();
        return instance;
    }
}
