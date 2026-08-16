package gg.topchdlc.vse.shutki.module.modules.impl.combat.aura.elytra;

import gg.topchdlc.api.render.system.ClientPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.RotationSettings;
import gg.topchdlc.vse.rotation.all.ISnapRotation;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import static gg.topchdlc.MinecraftHolder.mc;
/*
/* create by egoistik da da pizdabol -_-
 */

public class ElytraTarget {

    // ssss
    private final Group elytraSettings;
    public final CheckBox elytraMode;
    public final SliderSetting targetRange;
    public final CheckBox predictOnElytra;
    public final SliderSetting predictTicks;
    public final CheckBox showPredictBox;
    public final EnumSetting<RotationMode> rotationMode;
    private boolean raycastCheck = true;
    private Vec3d lastPredictedPos = null;
    private boolean isSnapping = false;
    private int snapTicks = 0;
    private final RotationSettings rotations;
    private Angle preSnapAngle = null;
    public ElytraTarget(Group parentGroup, RotationSettings rotations) {
        this.rotations = rotations;
        this.elytraSettings = parentGroup.group("Elytra Settings");
        this.elytraMode = elytraSettings.checkbox("Elytra Mode", true).desc("Use different ranges while flying");
        this.targetRange = elytraSettings.sliderSetting("Target Range", 15f, 0f, 50f).increment(0.1f).desc("Target detection range").visible(() -> elytraMode.get());
        this.predictOnElytra = elytraSettings.checkbox("Predict Target", true).desc("Predict target position on elytra").visible(() -> elytraMode.get());
        this.predictTicks = elytraSettings.sliderSetting("Predict Amount", 2.0f, 0.5f, 10.0f).increment(0.1f).desc("How many ticks to predict").visible(() -> elytraMode.get() && predictOnElytra.get());
        this.showPredictBox = elytraSettings.checkbox("Show Predict Box", false).desc("Render predicted position").visible(() -> elytraMode.get() && predictOnElytra.get());
        this.rotationMode = elytraSettings.enumSetting("Rotation Mode", RotationMode.Default).desc("Packet - instant snap, SnapRotation - smooth snap like in aura").visible(() -> elytraMode.get());
    }
    public boolean isElytraFlying() {
        return mc.player != null && mc.player.isGliding();
    }

    public float getTargetRange(float defaultRange) {
        if (elytraMode.get() && isElytraFlying()) {
            return targetRange.get();
        }
        return defaultRange;
    }
    public Vec3d getTargetPoint(LivingEntity target, Vec3d defaultPoint) {
        if (target instanceof PlayerEntity &&
                elytraMode.get() &&
                predictOnElytra.get() &&
                mc.player.isGliding() &&
                target.isGliding()) {
            double distanceToTarget = mc.player.getEyePos().distanceTo(target.getBoundingBox().getCenter());
            float predictAmount = distanceToTarget > 8.0D ? 8.0F : predictTicks.get();
            Vec3d predictedPos = PredictUtils.predict(target, target.getEntityPos(), predictAmount);
            isSnapping = true;
            snapTicks++;
            lastPredictedPos = predictedPos;
            return predictedPos;
        }
        isSnapping = false;
        snapTicks = 0;
        lastPredictedPos = null;
        return defaultPoint;
    }
    public Vec3d getSnapPoint(LivingEntity target, Vec3d defaultPoint) {
        if (target instanceof PlayerEntity &&
                elytraMode.get() &&
                mc.player.isGliding() &&
                target.isGliding() &&
                rotationMode.is(RotationMode.Default)) {
            return defaultPoint;
        }
        return defaultPoint;
    }
    public boolean applyRotation(Angle targetAngle, ISnapRotation snapRotation) {
        if (!isSnapping || !elytraMode.get() || !mc.player.isGliding()) {
            return false;
        }
        if (rotationMode.is(RotationMode.Default) && snapRotation != null) {
            if (preSnapAngle == null) {
                preSnapAngle = Angle.fromPlayer();
                snapRotation.prepareSnap(preSnapAngle);
            }
            rotations.rotate(targetAngle);
            return true;
        }

        return false;
    }

    public void tick(ISnapRotation snapRotation) {
        if (snapRotation != null && isSnapping) {
            snapRotation.tick();
            if (snapRotation.shouldReturn() && preSnapAngle != null) {
                Angle returnAngle = snapRotation.getReturnAngle();
                if (returnAngle != null) {
                    rotations.rotate(returnAngle);
                }
                preSnapAngle = null;
            }
        }
        if (!mc.player.isGliding()) {
            isSnapping = false;
            snapTicks = 0;
            preSnapAngle = null;
        }
    }

    public boolean shouldCheckRaycast() {
        return raycastCheck && elytraMode.get() && mc.player.isGliding();
    }
    public boolean checkAttackDistance(LivingEntity target, float attackRange) {
        if (target instanceof PlayerEntity &&
                elytraMode.get() &&
                predictOnElytra.get() &&
                mc.player.isGliding() &&
                target.isGliding()) {
            Vec3d predictedPos = PredictUtils.predict(target, target.getEntityPos(), predictTicks.get());
            double distanceToTarget = mc.player.getEyePos().distanceTo(predictedPos);
            return distanceToTarget <= attackRange;
        }
        return true;
    }
    public void renderPredictBox(Event3D e, LivingEntity target) {
        if (!showPredictBox.get() || target == null || lastPredictedPos == null) {
            return;
        }
        if (!(target instanceof PlayerEntity) ||
                !elytraMode.get() ||
                !predictOnElytra.get() ||
                !mc.player.isGliding() ||
                !target.isGliding()) {
            return;
        }
        Vec3d predictedPos = lastPredictedPos;
        double boxSize = 0.5;
        Box box = new Box(
            predictedPos.x - boxSize, predictedPos.y - boxSize, predictedPos.z - boxSize,
            predictedPos.x + boxSize, predictedPos.y + boxSize, predictedPos.z + boxSize
        ).expand(0.001D);

        float r = isSnapping ? 1.0f : 0.0f;
        float g = isSnapping ? 0.0f : 1.0f;
        float b = 0.0f;
        float a = 1.0f;
        e.stack.push();
        Client.RENDERER.toCamera(e.stack);
        VertexRendering.drawOutline(
            e.stack,
            e.buffer.getBuffer(ClientPipelines.OUTLINE),
                VoxelShapes.cuboid(box),
                0,0,0,0xFFFFFFFF,1
        );

        e.stack.pop();
    }
    public void reset() {
        isSnapping = false;
        snapTicks = 0;
        lastPredictedPos = null;
        preSnapAngle = null;
    }

    public boolean isSnapping() {
        return isSnapping;
    }

    public int getSnapTicks() {
        return snapTicks;
    }

    public enum RotationMode {
        Default
    }
}
