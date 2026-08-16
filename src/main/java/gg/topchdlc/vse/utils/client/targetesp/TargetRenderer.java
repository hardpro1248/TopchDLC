package gg.topchdlc.vse.utils.client.targetesp;

import gg.topchdlc.vse.shutki.module.settings.impl.choice.ChoiceSetting;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.animations.impl.SmoothStepAnimation;
import gg.topchdlc.vse.utils.client.targetesp.impl.*;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public class TargetRenderer extends ChoiceSetting<TargetRendererChoice> {
    public SmoothStepAnimation ANIMATION = new SmoothStepAnimation(300, 1);

    public TargetRenderer() {
        super("Target Renderer", 0, new ImageTargetRenderer(),
                new DNAHelixTargetRenderer(),
                new Crystal2TargetRenderer(),
                new PrizrakTargetRenderer(), new DonutTargetRenderer());
    }

    public void render(MatrixStack stack, VertexConsumerProvider provider) {
        if (TargetsUtility.getTarget() != null) {
            TargetsUtility.setLastTarget(TargetsUtility.getTarget());
        }
        if (TargetsUtility.getLastTarget() == null) {
            return;
        }
        ANIMATION.setDirection(TargetsUtility.getTarget() == null ? Direction.BACKWARDS : Direction.FORWARDS);

        stack.push();
        LivingEntity target = TargetsUtility.getLastTarget();

        Vec3d pos = mc.gameRenderer.getCamera().getCameraPos().subtract(target.getLerpedPos(mc.getRenderTickCounter().getTickProgress(true)));
        stack.translate(-pos.x, -pos.y, -pos.z);

        get().render(stack, provider, target, ANIMATION.getOutput());

        stack.pop();
    }
}
