package gg.topchdlc.api.events.list;

import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.events.Event;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

public class Event3D extends Event implements MinecraftHolder {

    private static final Event3D instance = new Event3D();

    public MatrixStack stack;
    public VertexConsumerProvider buffer;

    public static Event3D build(MatrixStack stack, VertexConsumerProvider.Immediate immediate) {
        instance.stack = stack;
        instance.buffer = immediate;
        return instance;
    }
}
