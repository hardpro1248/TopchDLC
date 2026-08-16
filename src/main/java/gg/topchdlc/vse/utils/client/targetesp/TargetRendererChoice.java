package gg.topchdlc.vse.utils.client.targetesp;

import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;

public abstract class TargetRendererChoice extends Choice {
    protected TargetRendererChoice(String name) {
        super(name);
    }

    public boolean isWorld() { return true; }

    public abstract void render(MatrixStack stack, VertexConsumerProvider consumers, LivingEntity target, float animation);
}
