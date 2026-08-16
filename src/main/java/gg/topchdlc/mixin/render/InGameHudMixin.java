package gg.topchdlc.mixin.render;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.vse.shutki.module.modules.impl.render.BetterMinecraft;
import gg.topchdlc.vse.shutki.screen.hud.ui.impl.main.EffectsElement;
import gg.topchdlc.vse.shutki.screen.hud.ui.impl.main.HotbarElement;
import gg.topchdlc.vse.shutki.module.modules.impl.render.Interface;
import gg.topchdlc.vse.shutki.module.modules.impl.render.Removals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.SpectatorHud;
import net.minecraft.client.gui.hud.SubtitlesHud;
import net.minecraft.client.gui.hud.bar.Bar;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.list.Event2D;
import gg.topchdlc.api.render.system.sys2d.ClientLayer;

import java.util.Map;
import java.util.function.Supplier;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin implements MinecraftHolder {
    @Shadow
    private int overlayRemaining;
    @Shadow
    @Nullable
    private Text overlayMessage;
    @Shadow
    private int heldItemTooltipFade;
    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    public abstract void render(DrawContext context, RenderTickCounter tickCounter);
    @Shadow
    @Final
    private SpectatorHud spectatorHud;
    @Shadow
    private Pair<InGameHud.BarType, Bar> currentBar;
    @Shadow
    @Final
    private Map<InGameHud.BarType, Supplier<Bar>> bars;

    @Shadow
    @Final
    private SubtitlesHud subtitlesHud;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void client$render(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (Client.IS_PANIC) return;
        ci.cancel();
        if (this.client.currentScreen == null || !(this.client.currentScreen instanceof LevelLoadingScreen)) {
            Client.RENDERER.setDrawContext(context);
            Client.RENDERER.getCrenderSystem().layer(CRenderSystem.RenderLayer.HUD);
            context.state.addSimpleElement(new ClientLayer());
            Client.EVENTS.post(Event2D.build());

            if (!this.client.options.hudHidden) {
                this.renderMiscOverlays(context, tickCounter);
                this.renderCrosshair(context, tickCounter);
                context.createNewRootLayer();
                if (this.client.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR) {
                    this.spectatorHud.renderSpectatorMenu(context);
                } else {
                    BetterMinecraft bmc = BetterMinecraft.INSTANCE;
                    boolean applyHotbarAnim = bmc.isEnabled() && bmc.hotbarAnim.get();
                    float hotbarT = bmc.getHotbarAnim();
                    if (applyHotbarAnim && hotbarT > 0.001f) {
                        int sh = this.client.getWindow().getScaledHeight();
                        float offsetY = 30f * (1f - hotbarT);
                        context.getMatrices().pushMatrix();
                        context.getMatrices().translate(0, offsetY);
                    }
                    if (Interface.INSTANCE.isEnabled() && HotbarElement.INSTANCE.isEnabled()) {
                        HotbarElement.renderHotbar(context, tickCounter);
                    } else {
                        renderHotbar(context, tickCounter);
                    }
                    if (applyHotbarAnim && hotbarT > 0.001f) {
                        context.getMatrices().popMatrix();
                    }
                }

                if (this.client.interactionManager.hasStatusBars()) {
                    this.renderStatusBars(context);
                }

                this.renderMountHealth(context);
                InGameHud.BarType barType = this.getCurrentBarType();
                if (barType != this.currentBar.getKey()) {
                    this.currentBar = Pair.of(barType, this.bars.get(barType).get());
                }

                this.currentBar.getValue().renderBar(context, tickCounter);
                if (this.client.interactionManager.hasExperienceBar() && this.client.player.experienceLevel > 0) {
                    Bar.drawExperienceLevel(context, this.client.textRenderer, this.client.player.experienceLevel);
                }

                this.currentBar.getValue().renderAddons(context, tickCounter);
                if (this.client.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
                    this.renderHeldItemTooltip(context);
                } else if (this.client.player.isSpectator()) {
                    this.spectatorHud.render(context);
                }
                if (!Interface.INSTANCE.isEnabled() || !EffectsElement.INSTANCE.isEnabled()) {
                    this.renderStatusEffectOverlay(context, tickCounter);
                }
                this.renderBossBarHud(context, tickCounter);
            }
            this.renderSleepOverlay(context, tickCounter);
            if (!this.client.options.hudHidden) {
                this.renderDemoTimer(context, tickCounter);

                this.client.getDebugHud().render(context);

                if (!Removals.INSTANCE.isEnabled() || !Removals.INSTANCE.removals.get(Removals.Removal.Scoreboard)) this.renderScoreboardSidebar(context, tickCounter);
                this.renderOverlayMessage(context, tickCounter);
                this.renderTitleAndSubtitle(context, tickCounter);
                this.renderChat(context, tickCounter);
                this.renderPlayerList(context, tickCounter);

                this.subtitlesHud.render(context);
            }
        }
        Client.RENDERER.runTasks();
    }
    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderVignette(DrawContext context, Entity entity, CallbackInfo ci) {
        if (Removals.INSTANCE.isEnabled() && Removals.INSTANCE.removals.get(Removals.Removal.Vignette)) {
            ci.cancel();
        }
    }

    @Shadow
    protected abstract void renderMiscOverlays(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderCrosshair(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderHotbar(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderStatusBars(DrawContext context);

    @Shadow
    protected abstract void renderMountHealth(DrawContext context);

    @Shadow
    protected abstract InGameHud.BarType getCurrentBarType();

    @Shadow
    protected abstract void renderHeldItemTooltip(DrawContext context);

    @Shadow
    protected abstract void renderStatusEffectOverlay(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderBossBarHud(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderSleepOverlay(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderDemoTimer(DrawContext context, RenderTickCounter tickCounter);


    @Shadow
    protected abstract void renderScoreboardSidebar(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderOverlayMessage(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderTitleAndSubtitle(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderChat(DrawContext context, RenderTickCounter tickCounter);

    @Shadow
    protected abstract void renderPlayerList(DrawContext context, RenderTickCounter tickCounter);
    @Redirect(
            method = "renderPlayerList",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"),
            require = 0
    )
    private boolean topchdlc$forceTabDuringAnim(KeyBinding keyBinding) {
        BetterMinecraft mod = BetterMinecraft.INSTANCE;
        if (mod.isEnabled() && mod.tabAnim.get() && mod.getTabAnim() > 0.001f) {
            return true;
        }
        return keyBinding.isPressed();
    }

    @Inject(method = "renderPlayerList", at = @At("HEAD"), cancellable = true)
    private void topchdlc$tabHead(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        BetterMinecraft mod = BetterMinecraft.INSTANCE;
        if (!mod.isEnabled() || !mod.tabAnim.get()) return;

        float t = mod.getTabAnim();
        if (t <= 0.001f) {
            ci.cancel();
            return;
        }
        float offsetY = -60f * (1f - t);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0, offsetY);
    }

    @Inject(method = "renderPlayerList", at = @At("TAIL"))
    private void topchdlc$tabTail(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        BetterMinecraft mod = BetterMinecraft.INSTANCE;
        if (!mod.isEnabled() || !mod.tabAnim.get()) return;
        if (mod.getTabAnim() <= 0.001f) return;

        context.getMatrices().popMatrix();
    }

    @Inject(method = "renderPortalOverlay",at = @At("HEAD"),cancellable = true)
    private void hookportal(DrawContext context, float nauseaStrength,CallbackInfo ci){
        if (Removals.INSTANCE.isEnabled() && Removals.INSTANCE.removals.get(Removals.Removal.Poral))
            ci.cancel();
    }
    @Inject(method = "renderNauseaOverlay", at = @At("HEAD"), cancellable = true)
    private void hookNausea(DrawContext context, float nauseaStrength, CallbackInfo ci) {
        if (Removals.INSTANCE.isEnabled() && Removals.INSTANCE.removals.get(Removals.Removal.Nausea))
            ci.cancel();
    }
}