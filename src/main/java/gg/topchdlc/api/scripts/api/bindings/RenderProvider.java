package gg.topchdlc.api.scripts.api.bindings;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.TextureUse;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.graalvm.polyglot.HostAccess;
import org.joml.Vector4f;

import java.awt.*;

public class RenderProvider {
    @HostAccess.Export
    public void rect(double x, double y, double width, double height, double round, Color color) {
        Client.RENDERER.rect((float) x, (float) y, (float) width, (float) height, new Vector4f((float) round), 1, color, color, color, color);
    }
    @HostAccess.Export
    public void string(String text, String id, double size, double x, double y, Color color) {
        FontProvider provider = FontProvider.valueOf(id);
        if (TextureUse.values().length < provider.ordinal()) return;
        TextureUse using = TextureUse.values()[provider.ordinal()];
        Client.RENDERER.text(text, (float) x, (float) y, using, (float) size, color);
    }
    @HostAccess.Export
    public float width(String text, String id, double size) {
        FontProvider provider = FontProvider.valueOf(id);
        if (TextureUse.values().length < provider.ordinal()) return -1;
        TextureUse using = TextureUse.values()[provider.ordinal()];
        return Client.RENDERER.textWidth(text, using, (float) size);
    }


    VertexConsumer last = null;
    @HostAccess.Export
    public VertexConsumer lineBuffer(VertexConsumerProvider provider) { return last = provider.getBuffer(RenderLayers.lines()); }
    @HostAccess.Export
    public VertexConsumer quadBuffer(VertexConsumerProvider provider) { return last = provider.getBuffer(RenderLayers.debugQuads()); }
    @HostAccess.Export
    public MatrixStack.Entry entry(MatrixStack in) { return in.peek(); }
    @HostAccess.Export
    public RenderProvider vertex(MatrixStack.Entry stack, double x, double y, double z, Color color, double... normal) {
        if (last == null) return this;
        last.vertex(stack, (float) x, (float) y, (float) z).color(color.getRGB()).normal((float) normal[0], (float) normal[1], (float) normal[2]);
        return this;
    }

    @HostAccess.Export
    public Color color(int r, int g, int b, int a) {
        return new Color(r, g, b, a);
    }
}
