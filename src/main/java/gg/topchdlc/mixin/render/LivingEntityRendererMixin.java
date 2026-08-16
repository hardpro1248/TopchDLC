package gg.topchdlc.mixin.render;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.shutki.module.modules.impl.render.HitAnimation;
import gg.topchdlc.vse.shutki.module.modules.impl.render.PopEffect;
import gg.topchdlc.vse.shutki.module.modules.impl.render.Removals;
import gg.topchdlc.vse.shutki.module.modules.impl.render.SeeInvisible;
import gg.topchdlc.vse.utils.math.ColorUtility;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.Color;
import java.util.Map;
import java.util.WeakHashMap;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
    @Shadow protected M model;
    @Shadow protected abstract void setupTransforms(S state, MatrixStack matrices, float bodyYaw, float baseHeight);
    @Shadow protected abstract void scale(S state, MatrixStack matrices);
    @Shadow public abstract Identifier getTexture(S state);

    @Unique
    private static final Map<LivingEntityRenderState, Integer> STATE_TO_ENTITY_ID = new WeakHashMap<>();

    @Inject(method = "updateRenderState", at = @At("HEAD"))
    private void captureEntity(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        STATE_TO_ENTITY_ID.put(state, entity.getId());
    }

    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void patchVisualRotations(T entity, S state, float tickDelta, CallbackInfo ci) {
        if (entity instanceof ClientPlayerEntity && !Client.IS_PANIC && Client.ROTATION.isRotating()) {
            float visualYaw = Client.ROTATION.getVisual().getYaw();
            float visualPitch = Client.ROTATION.getVisual().getPitch();

            state.bodyYaw = visualYaw;
            state.relativeHeadYaw = 0.0F;
            state.pitch = visualPitch;
        }
    }

    @Inject(method = "scale", at = @At("TAIL"))
    private void applyHitAnimationScale(S state, MatrixStack matrices, CallbackInfo ci) {
        HitAnimation ha = HitAnimation.INSTANCE;
        if (!ha.isEnabled()) return;

        if (state.hurt) {
            Integer entityId = STATE_TO_ENTITY_ID.get(state);
            if (entityId != null) {
                float[] squish = ha.getSquish(entityId);
                if (squish != null) {
                    matrices.scale(squish[0], squish[1], squish[0]);
                }
            }
        }
    }

    @Inject(method = "getRenderLayer", at = @At("HEAD"), cancellable = true)
    private void handleGetRenderLayer(S state, boolean showBody, boolean translucent, boolean showOutline, CallbackInfoReturnable<RenderLayer> cir) {
        boolean seeInvisActive = SeeInvisible.INSTANCE.isEnabled();
        boolean removalInvisActive = Removals.INSTANCE.isEnabled() && Removals.INSTANCE.removals.get(Removals.Removal.Invisibility);

        if ((seeInvisActive || removalInvisActive) && state.invisible) {
            cir.setReturnValue(RenderLayers.itemEntityTranslucentCull(this.getTexture(state)));
        }
    }

    @Inject(method = "getMixColor", at = @At("HEAD"), cancellable = true)
    private void handleInvisibilityMixColor(S state, CallbackInfoReturnable<Integer> cir) {
        boolean seeInvisActive = SeeInvisible.INSTANCE.isEnabled();
        boolean removalInvisActive = Removals.INSTANCE.isEnabled() && Removals.INSTANCE.removals.get(Removals.Removal.Invisibility);

        if ((seeInvisActive || removalInvisActive) && state.invisible) {
            int alpha = seeInvisActive ? SeeInvisible.INSTANCE.alpha.getInt() : 100;
            cir.setReturnValue(ColorUtility.injectAlpha(Color.WHITE, alpha).getRGB());
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderGhostChams(S state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci) {
        PopEffect pop = PopEffect.INSTANCE;
        if (!pop.isEnabled() || !pop.chamsEnabled.get()) return;

        Integer currentEntityId = STATE_TO_ENTITY_ID.get(state);
        if (currentEntityId == null) return;

        for (PopEffect.ChamGhost ghost : PopEffect.CHAMS_GHOSTS) {
            if (ghost.entityId != currentEntityId) continue;
            if (ghost.isFinished()) continue;

            float alpha = ghost.alpha();
            if (alpha <= 0.001f) continue;

            float offset = ghost.riseY();

            Color c = pop.chamsUseCustomColor.get() ? pop.chamsColor.get() : ClientSettings.INSTANCE.getColor(0);
            int fillA = (int)(45 * alpha);
            int fillArgb = (fillA << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();

            renderGhostModel(state, matrices, queue, fillArgb, offset);
            renderGhostModel(state, matrices, queue, fillArgb, -offset);
        }
    }

    @Unique
    private void renderGhostModel(S state, MatrixStack matrices, OrderedRenderCommandQueue queue, int argb, float yOffset) {
        matrices.push();

        float g = state.baseScale;
        matrices.scale(g, g, g);
        this.setupTransforms(state, matrices, state.bodyYaw, g);
        matrices.scale(-1.0F, -1.0F, 1.0F);
        this.scale(state, matrices);

        matrices.translate(0.0F, -1.501F + yOffset, 0.0F);

        RenderLayer layer = ClientPipelines.QUAD;
        queue.submitModel(this.model, state, matrices, layer, state.light, OverlayTexture.DEFAULT_UV, argb, null, state.outlineColor, null);

        matrices.pop();
    }

}