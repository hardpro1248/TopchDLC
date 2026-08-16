package gg.topchdlc.mixin.render;

import gg.topchdlc.vse.shutki.module.modules.impl.render.HandShaderModule;
import gg.topchdlc.vse.shutki.module.modules.impl.render.SwingAnimations;
import gg.topchdlc.vse.shutki.module.modules.impl.render.ViewModel;
import gg.topchdlc.vse.utils.render.HandShaderRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private EntityRenderManager entityRenderDispatcher;
    @Shadow private ItemStack offHand;

    @Shadow public abstract void renderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext renderMode, MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue, int light);
    @Shadow protected abstract void swingArm(float swingProgress, MatrixStack matrixStack, int i, Arm arm);
    @Shadow protected abstract void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress);
    @Shadow protected abstract void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress);
    @Shadow protected abstract void applyBrushTransformation(MatrixStack matrices, float tickProgress, Arm arm, PlayerEntity playerEntity);
    @Shadow protected abstract void applyEatOrDrinkTransformation(MatrixStack matrices, float tickProgress, Arm arm, ItemStack stack, PlayerEntity player);
    @Shadow protected abstract void renderMapInBothHands(MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue, int light, float pitch, float equipProgress, float swingProgress);
    @Shadow protected abstract void renderMapInOneHand(MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue, int light, float equipProgress, Arm arm, float swingProgress, ItemStack stack);
    @Shadow protected abstract void renderArmHoldingItem(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, float equipProgress, float swingProgress, Arm arm);

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("HEAD"))
    private void soul$onRenderItemPre(float tickProgress, MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue, ClientPlayerEntity player, int light, CallbackInfo ci) {
        HandShaderRenderer.getInstance().captureSceneBeforeHands();
    }

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("TAIL"))
    private void soul$onRenderItemPost(float tickProgress, MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue, ClientPlayerEntity player, int light, CallbackInfo ci) {
        HandShaderRenderer renderer = HandShaderRenderer.getInstance();
        renderer.captureSceneAfterHands();
        renderer.renderFireEffect();
    }

    @Unique private static java.lang.reflect.Method applySwingTransformsMethod;
    @Unique private static java.lang.reflect.Method applyUseTransformsMethod;

    static {
        try {
            Class<?> lancingClass = Class.forName("net.minecraft.client.render.entity.state.Lancing");
            for (java.lang.reflect.Method m : lancingClass.getDeclaredMethods()) {
                if ((m.getName().equals("method_75391") || m.getName().equals("applySwingTransforms") || m.getName().equals("applySwingOffset"))
                        && m.getParameterCount() == 4) {
                    m.setAccessible(true);
                    applySwingTransformsMethod = m;
                }
                if ((m.getName().equals("method_75396") || m.getName().equals("applyUseTransforms") || m.getName().equals("applyUseOffset"))
                        && m.getParameterCount() == 5) {
                    m.setAccessible(true);
                    applyUseTransformsMethod = m;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Unique
    private static void invokeLancingSwing(float swingProgress, MatrixStack matrices, int side, Arm arm) {
        if (applySwingTransformsMethod != null) {
            try {
                applySwingTransformsMethod.invoke(null, swingProgress, matrices, side, arm);
            } catch (Exception ignored) {}
        }
    }

    @Unique
    private static void invokeLancingUse(float time, MatrixStack matrices, float f, Arm arm, ItemStack item) {
        if (applyUseTransformsMethod != null) {
            try {
                applyUseTransformsMethod.invoke(null, time, matrices, f, arm, item);
            } catch (Exception ignored) {}
        }
    }

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void client$renderFirstPersonItem(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue, int light, CallbackInfo ci) {
        if (player.isUsingSpyglass()) return;

        ci.cancel();

        HandShaderModule hsm = HandShaderModule.INSTANCE;
        if (hsm.isEnabled() && hsm.fillItem.get()) {
            HandShaderModule.RenderMode renderMode = hsm.fillRenderMode.get();

            if (renderMode == HandShaderModule.RenderMode.REPLACE) {
                HandShaderModule.INSTANCE.updateUniforms();
                HandShaderModule.isRenderingShader = true;
                renderFirstPersonItemInternal(player, tickProgress, pitch, hand, swingProgress, item, equipProgress, matrices, orderedRenderCommandQueue, light);
                HandShaderModule.isRenderingShader = false;
            } else {
                HandShaderModule.isRenderingShader = false;
                renderFirstPersonItemInternal(player, tickProgress, pitch, hand, swingProgress, item, equipProgress, matrices, orderedRenderCommandQueue, light);
                HandShaderModule.INSTANCE.updateUniforms();
                HandShaderModule.isRenderingShader = true;
                renderFirstPersonItemInternal(player, tickProgress, pitch, hand, swingProgress, item, equipProgress, matrices, orderedRenderCommandQueue, light);
                HandShaderModule.isRenderingShader = false;
            }
        } else {
            HandShaderModule.isRenderingShader = false;
            renderFirstPersonItemInternal(player, tickProgress, pitch, hand, swingProgress, item, equipProgress, matrices, orderedRenderCommandQueue, light);
        }
    }

    @Unique
    private void renderFirstPersonItemInternal(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue, int light) {
        boolean isMainHand = hand == Hand.MAIN_HAND;
        Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
        matrices.push();

        ViewModel.INSTANCE.apply(hand, player.getMainArm(), matrices);

        if (item.isEmpty()) {
            if (isMainHand && !player.isInvisible()) {
                this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm);
            }
        } else if (item.contains(DataComponentTypes.MAP_ID)) {
            if (isMainHand && this.offHand.isEmpty()) {
                this.renderMapInBothHands(matrices, orderedRenderCommandQueue, light, pitch, equipProgress, swingProgress);
            } else {
                this.renderMapInOneHand(matrices, orderedRenderCommandQueue, light, equipProgress, arm, swingProgress, item);
            }
        } else if (item.isOf(Items.CROSSBOW)) {
            this.applyEquipOffset(matrices, arm, equipProgress);
            boolean isCharged = CrossbowItem.isCharged(item);
            boolean isRightArm = arm == Arm.RIGHT;
            int i = isRightArm ? 1 : -1;

            if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand && !isCharged) {
                matrices.translate(i * -0.4785682F, -0.094387F, 0.05731531F);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-11.935F));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 65.3F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * -9.785F));
                float f = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickProgress + 1.0F);
                float g = f / (float)CrossbowItem.getPullTime(item, player);
                g = MathHelper.clamp(g, 0.0F, 1.0F);

                if (g > 0.1F) {
                    float h = MathHelper.sin((f - 0.1F) * 1.3F);
                    float k = h * (g - 0.1F);
                    matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                }

                matrices.translate(g * 0.0F, g * 0.0F, g * 0.04F);
                matrices.scale(1.0F, 1.0F, 1.0F + g * 0.2F);
                matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(i * 45.0F));
            } else {
                this.swingArm(swingProgress, matrices, i, arm);
                if (isCharged && swingProgress < 0.001F && isMainHand) {
                    matrices.translate(i * -0.641864F, 0.0F, 0.0F);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 10.0F));
                }
            }
            this.renderItem(player, item, isRightArm ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, matrices, orderedRenderCommandQueue, light);

        } else {
            boolean isRightArm = arm == Arm.RIGHT;
            int side = isRightArm ? 1 : -1;

            if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                UseAction useAction = item.getUseAction();
                if (!useAction.hasNoOffset()) {
                    this.applyEquipOffset(matrices, arm, equipProgress);
                }

                switch (useAction) {
                    case NONE -> {}
                    case EAT, DRINK -> {
                        if (!(ViewModel.INSTANCE.isEnabled() && ViewModel.INSTANCE.removeEatAnimation.get())) {
                            this.applyEatOrDrinkTransformation(matrices, tickProgress, arm, item, player);
                        }
                        this.applyEquipOffset(matrices, arm, equipProgress);
                    }
                    case BLOCK -> {
                        if (!(item.getItem() instanceof ShieldItem)) {
                            matrices.translate(side * -0.14142136F, 0.08F, 0.14142136F);
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-102.25F));
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 13.365F));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 78.05F));
                        }
                    }
                    case BOW -> {
                        matrices.translate(side * -0.2785682F, 0.18344387F, 0.15731531F);
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-13.935F));
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 35.3F));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * -9.785F));
                        float f = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickProgress + 1.0F);
                        float g = f / 20.0F;
                        g = (g * g + g * 2.0F) / 3.0F;
                        if (g > 1.0F) g = 1.0F;
                        if (g > 0.1F) {
                            float h = MathHelper.sin((f - 0.1F) * 1.3F);
                            float k = h * (g - 0.1F);
                            matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                        }
                        matrices.translate(g * 0.0F, g * 0.0F, g * 0.04F);
                        matrices.scale(1.0F, 1.0F, 1.0F + g * 0.2F);
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(side * 45.0F));
                    }
                    case TRIDENT -> {
                        matrices.translate(side * -0.5F, 0.7F, 0.1F);
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-55.0F));
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 35.3F));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * -9.785F));
                        float f = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickProgress + 1.0F);
                        float g = f / 10.0F;
                        if (g > 1.0F) g = 1.0F;
                        if (g > 0.1F) {
                            float h = MathHelper.sin((f - 0.1F) * 1.3F);
                            float k = h * (g - 0.1F);
                            matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                        }
                        matrices.translate(0.0F, 0.0F, g * 0.2F);
                        matrices.scale(1.0F, 1.0F, 1.0F + g * 0.2F);
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(side * 45.0F));
                    }
                    case BRUSH -> this.applyBrushTransformation(matrices, tickProgress, arm, player);
                    case BUNDLE -> this.swingArm(swingProgress, matrices, side, arm);
                    case SPEAR -> {
                        matrices.translate(side * 0.56F, -0.52F, -0.72F);
                        float f = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickProgress + 1.0F);
                        invokeLancingUse(player.getTimeSinceLastKineticAttack(tickProgress), matrices, f, arm, item);
                    }
                }
            } else if (player.isUsingRiptide()) {
                this.applyEquipOffset(matrices, arm, equipProgress);
                matrices.translate(side * -0.4F, 0.8F, 0.3F);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * 65.0F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * -85.0F));
            } else {
                boolean handled = false;
                if (SwingAnimations.INSTANCE.shouldApply()) {
                    handled = SwingAnimations.INSTANCE.handleMatrix(swingProgress, equipProgress, matrices, side, arm);
                }

                if (!handled) {
                    this.applyEquipOffset(matrices, arm, equipProgress);

                    switch (item.getSwingAnimation().type()) {
                        case NONE -> {}
                        case WHACK -> this.swingArm(swingProgress, matrices, side, arm);
                        case STAB -> invokeLancingSwing(swingProgress, matrices, side, arm);
                    }
                }
            }

            this.renderItem(player, item, isRightArm ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, matrices, orderedRenderCommandQueue, light);
        }

        matrices.pop();
    }
}