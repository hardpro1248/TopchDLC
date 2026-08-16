package gg.topchdlc.vse.shutki.module.modules.impl.render;

import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.utils.client.targetesp.TargetRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

public class TargetESP extends Module {
    public static final TargetESP INSTANCE = new TargetESP();

    public final TargetRenderer renderer = add(new TargetRenderer());

    private TargetESP() {
        super("TargetESP", Category.RENDER, "Эффект на текущей цели");
    }

    public void render(MatrixStack matrices, VertexConsumerProvider provider) {
        if (isEnabled()) {
            renderer.render(matrices, provider);
        }
    }
}
