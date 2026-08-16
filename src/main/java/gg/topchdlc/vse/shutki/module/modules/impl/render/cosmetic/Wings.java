package gg.topchdlc.vse.shutki.module.modules.impl.render.cosmetic;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.colorsetting.ColorSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.math.ColorUtility;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.*;

import static gg.topchdlc.MinecraftHolder.mc;

/**
 * Created by daun kvass
 */
public class Wings {
    public static final Wings INSTANCE = new Wings();
    private boolean enabled = false;
    public final CheckBox showOnSelf = new CheckBox("Self", true);
    public final CheckBox showOnFriends = new CheckBox("Friends", true);
    public final CheckBox showOnOthers = new CheckBox("Others", false);
    public final CheckBox useCustomColor = new CheckBox("Custom Color", false);
    public final ColorSetting color = new ColorSetting("Color", new Color(0, 180, 255, 255));
    public final SliderSetting size = new SliderSetting("Size", 1.0f, 0.5f, 2.0f).increment(0.05f);
    public final SliderSetting fillAlpha = new SliderSetting("Fill Alpha", 100f, 5f, 255f).increment(1f);

    static class WingAnimationState {
        float smoothYaw, forwardAnim, flapAnim, waterAnim;
        boolean yawInitialized;
    }
    private final Map<UUID, WingAnimationState> animationStates = new HashMap<>();
    private static final WingPoint[] WING_SHAPE = {
            new WingPoint(0.08f, 0.10f, 0.88f), new WingPoint(0.28f, 0.34f, 0.78f),
            new WingPoint(0.56f, 0.82f, 0.62f), new WingPoint(0.86f, 0.30f, 0.52f),
            new WingPoint(1.14f, 0.46f, 0.40f), new WingPoint(1.24f, 0.04f, 0.30f),
            new WingPoint(1.02f, -0.18f, 0.28f), new WingPoint(1.18f, -0.64f, 0.22f),
            new WingPoint(0.86f, -0.46f, 0.20f), new WingPoint(0.80f, -0.98f, 0.14f),
            new WingPoint(0.54f, -0.74f, 0.16f), new WingPoint(0.30f, -1.16f, 0.12f),
            new WingPoint(0.10f, -0.54f, 0.18f)
    };

    public final EventBus<Event> bus = event -> {
        if (!enabled) return;
        if (event instanceof Event3D e) {

            if (mc.world == null || mc.player == null) return;
            MatrixStack stack = e.stack;
            float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
            Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();

            for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
                if (!shouldRenderFor(player)) continue;
                if (player == mc.player && mc.options.getPerspective().isFirstPerson()) continue;

                renderWings(stack, e, player, tickDelta, cam);
            }
        }
    };

    private boolean shouldRenderFor(AbstractClientPlayerEntity player) {
        if (player == mc.player) return showOnSelf.get();
        if (Client.FRIENDS.isFriend(player.getName().getString())) return showOnFriends.get();
        return showOnOthers.get();
    }

    private void renderWings(MatrixStack stack, Event3D e, AbstractClientPlayerEntity player, float tickDelta, Vec3d cam) {
        WingAnimationState state = animationStates.computeIfAbsent(player.getUuid(), uuid -> new WingAnimationState());

        double ix = MathHelper.lerp(tickDelta, player.lastX, player.getX());
        double iy = MathHelper.lerp(tickDelta, player.lastY, player.getY());
        double iz = MathHelper.lerp(tickDelta, player.lastZ, player.getZ());

        float smoothYaw = yaw(player, tickDelta, state);
        float move = MathHelper.clamp(player.limbAnimator.getSpeed(), 0f, 1f);

        float targetWater = player.isTouchingWater() ? 1f : 0f;
        state.waterAnim += (targetWater - state.waterAnim) * 0.08f;

        float bodyYawRad = (float) Math.toRadians(-smoothYaw);
        float motionX = (float) (player.getX() - player.lastX);
        float motionZ = (float) (player.getZ() - player.lastZ);
        float targetForward = (float) ((motionX * Math.sin(bodyYawRad)) + (motionZ * Math.cos(bodyYawRad)));
        state.forwardAnim += (MathHelper.clamp(targetForward * 22f, -1f, 1f) - state.forwardAnim) * 0.08f;

        WingPose pose = pose(player, tickDelta);
        pose.pitchRotation += (25f - pose.pitchRotation) * state.waterAnim;
        pose.scaleFactor += (0.9f - pose.scaleFactor) * state.waterAnim;
        pose.opennessMultiplier += (0.85f - pose.opennessMultiplier) * state.waterAnim;

        state.flapAnim += (pose.flapStrength + (move * 6f) - state.flapAnim) * 0.08f;
        float flapAngle = (float) Math.sin((player.age + tickDelta) * pose.flapFrequency) * state.flapAnim;
        float spread = (8 + flapAngle + move * pose.motionSpreadBonus * 1.8f) * pose.opennessMultiplier;
        float dynamicSpread = spread + state.forwardAnim * 16f;

        Color c = useCustomColor.get() ? color.get() : ClientSettings.INSTANCE.getColor(0);
        int fillArgb = ColorUtility.injectAlpha(c, (int) fillAlpha.get()).getRGB();
        int lineArgb = ColorUtility.injectAlpha(c, 255).getRGB();

        stack.push();

        stack.translate(ix - cam.x, iy - cam.y + player.getHeight() * 0.65f, iz - cam.z);

        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-smoothYaw));

        boolean isGliding = player.isGliding() || player.isGliding();
        float leaning = player.getLeaningPitch(tickDelta);
        if (isGliding) {
            leaning = 1.0f;
        }

        if (leaning > 0.01f) {
            float pitch = player.getPitch(tickDelta);
            float dynamicPitch = leaning * (pitch - 90f);
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(dynamicPitch));

            stack.translate(0f, 0.18f * leaning, -0.08f * leaning);
        } else if (player.isSneaking()) {
            stack.translate(0f, -0.15f, 0.08f);
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(28f));
        }

        stack.translate(0f, 0f, -0.15f);
        float fScale = size.get() * pose.scaleFactor;
        stack.scale(fScale, fScale, fScale);

        if (pose.pitchRotation != 0f) stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pose.pitchRotation));
        if (pose.rollRotation != 0f) stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(pose.rollRotation));

        drawWingSide(stack, e, -1f, dynamicSpread, fillArgb, lineArgb, pose, state);
        drawWingSide(stack, e, 1f, dynamicSpread, fillArgb, lineArgb, pose, state);

        stack.pop();
    }

    private void drawWingSide(MatrixStack stack, Event3D e, float dir, float spread, int fillCol, int lineCol, WingPose pose, WingAnimationState state) {
        stack.push();
        stack.translate(dir * pose.sideOffset, 0, pose.sideDepthOffset);
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(dir * spread));
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(dir * pose.sideRollAngle));
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pose.sidePitchAngle + (state.forwardAnim * 10f)));

        Matrix4f m = stack.peek().getPositionMatrix();

        VertexConsumer vcFill = e.buffer.getBuffer(ClientPipelines.FILL);
        float centerX = 0, centerY = 0;
        for (WingPoint p : WING_SHAPE) { centerX += p.x; centerY += p.y; }
        centerX /= WING_SHAPE.length; centerY /= WING_SHAPE.length;

        for (int i = 0; i < WING_SHAPE.length; i++) {
            WingPoint p1 = WING_SHAPE[i];
            WingPoint p2 = WING_SHAPE[(i + 1) % WING_SHAPE.length];
            vcFill.vertex(m, dir * centerX, centerY, 0).color(fillCol);
            vcFill.vertex(m, dir * p1.x, p1.y, 0).color(fillCol);
            vcFill.vertex(m, dir * p2.x, p2.y, 0).color(fillCol);
            vcFill.vertex(m, dir * p2.x, p2.y, 0).color(fillCol);
        }

        VertexConsumer vcOutline = e.buffer.getBuffer(ClientPipelines.OUTLINE);
        for (int i = 0; i <= WING_SHAPE.length; i++) {
            WingPoint p = WING_SHAPE[i % WING_SHAPE.length];
            vcOutline.vertex(m, dir * p.x, p.y, 0).color(lineCol);
        }

        stack.pop();
    }

    private float yaw(PlayerEntity player, float delta, WingAnimationState state) {
        float target = MathHelper.lerpAngleDegrees(delta, player.lastBodyYaw, player.bodyYaw);
        if (!state.yawInitialized) { state.smoothYaw = target; state.yawInitialized = true; }
        float diff = MathHelper.wrapDegrees(target - state.smoothYaw);
        state.smoothYaw += MathHelper.clamp(diff, -14f, 14f);
        return state.smoothYaw;
    }

    private WingPose pose(PlayerEntity p, float delta) {
        if (p.isGliding() || p.isGliding()) {
            return new WingPose(
                    0f,
                    0f,
                    0.75f,
                    0.85f,
                    0.1f,
                    1.5f,
                    0.05f,
                    0.06f,
                    -45f,
                    0f,
                    0.06f
            );
        }

        return new WingPose(0, 0, 1f, 1f, 0.18f, 4.5f, 0.06f, 0.02f, -11f, -4f, 0.12f);
    }

    public void setEnabled(boolean v) {
        enabled = v;
        if (!v) animationStates.clear();
    }

    protected void onDisable() {
        animationStates.clear();
    }

    public boolean isEnabled() { return enabled; }

    record WingPoint(float x, float y, float alphaMul) {}

    static class WingPose {
        float pitchRotation, rollRotation, opennessMultiplier, scaleFactor, motionSpreadBonus, flapStrength, sideOffset, sideDepthOffset, sideRollAngle, sidePitchAngle, flapFrequency;
        WingPose(float pitch, float roll, float open, float scale, float mSpread, float flapS, float sOff, float sDOff, float sRoll, float sPitch, float flapFreq) {
            this.pitchRotation = pitch; this.rollRotation = roll; this.opennessMultiplier = open; this.scaleFactor = scale;
            this.motionSpreadBonus = mSpread; this.flapStrength = flapS; this.sideOffset = sOff; this.sideDepthOffset = sDOff;
            this.sideRollAngle = sRoll; this.sidePitchAngle = sPitch; this.flapFrequency = flapFreq;
        }
    }
}