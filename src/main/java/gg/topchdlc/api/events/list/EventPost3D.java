package gg.topchdlc.api.events.list;

import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.events.Event;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Create by daun kvass
 */
public class EventPost3D extends Event implements MinecraftHolder {

    private static final EventPost3D instance = new EventPost3D();

    public MatrixStack stack;
    public VertexConsumerProvider buffer;

    public static EventPost3D build(MatrixStack stack, VertexConsumerProvider.Immediate immediate) {
        instance.stack = stack;
        instance.buffer = immediate;
        return instance;
    }
}
