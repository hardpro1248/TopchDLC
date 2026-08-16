package gg.topchdlc.vse.shutki.module.modules.impl.combat;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.render.system.ClientPipelines;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.RotationSettings;
import gg.topchdlc.vse.rotation.all.ISnapRotation;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.aura.elytra.PredictUtils;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;

public class ElytraTarget extends Module {
    public static final ElytraTarget INSTANCE = new ElytraTarget();

    public final SliderSetting targetRange = sliderSetting("Target Range", 15f, 0f, 50f).increment(0.1f).desc("Target detection range on elytra");
    public final CheckBox predictOnElytra = checkbox("Predict Target", true).desc("Predict target position on elytra");
    public final SliderSetting predictTicks = sliderSetting("Predict Amount", 2.0f, 0.5f, 10.0f).increment(0.1f).desc("How many ticks to predict").visible(() -> predictOnElytra.get());
    public final CheckBox showPredictBox = checkbox("Show Predict Box", false).desc("Render predicted position").visible(() -> predictOnElytra.get());
    public final EnumSetting<RotationMode> rotationMode = enumSetting("Rotation Mode", RotationMode.Default).desc("Packet - instant snap, SnapRotation - smooth snap like in aura");

    private boolean raycastCheck = true;
    private Vec3d lastPredictedPos = null;
    private boolean isSnapping = false;
    private int snapTicks = 0;
    private Angle preSnapAngle = null;

    private ElytraTarget() {
        super("ElytraTarget", Category.COMBAT, "Улучшенное наведение и предикт на элитрах");
    }

    public boolean isElytraFlying() {
        return isEnabled() && mc.player != null && mc.player.isGliding();
    }

    public float getTargetRange(float defaultRange) {
        if (isElytraFlying()) {
            return targetRange.get();
        }
        return defaultRange;
    }

    public Vec3d getTargetPoint(LivingEntity target, Vec3d defaultPoint) {
        if (isEnabled() && target instanceof PlayerEntity &&
                predictOnElytra.get() &&
                mc.player != null && mc.player.isGliding() &&
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

    public boolean applyRotation(Angle targetAngle, ISnapRotation snapRotation, RotationSettings rotations) {
        if (!isElytraFlying() || !isSnapping) {
            return false;
        }
        if (rotationMode.is(RotationMode.Default) && snapRotation != null) {
            if (preSnapAngle == null) {
                preSnapAngle = Angle.fromPlayer();
                snapRotation.prepareSnap(preSnapAngle);
            }
            if (rotations != null) {
                rotations.rotate(targetAngle);
            }
            return true;
        }
        return false;
    }

    public void tick(ISnapRotation snapRotation, RotationSettings rotations) {
        if (!isEnabled()) {
            reset();
            return;
        }
        if (snapRotation != null && isSnapping) {
            snapRotation.tick();
            if (snapRotation.shouldReturn() && preSnapAngle != null) {
                Angle returnAngle = snapRotation.getReturnAngle();
                if (returnAngle != null && rotations != null) {
                    rotations.rotate(returnAngle);
                }
                preSnapAngle = null;
            }
        }
        if (mc.player != null && !mc.player.isGliding()) {
            isSnapping = false;
            snapTicks = 0;
            preSnapAngle = null;
        }
    }

    public boolean shouldCheckRaycast() {
        return isEnabled() && raycastCheck && mc.player != null && mc.player.isGliding();
    }

    public void renderPredictBox(Event3D e, LivingEntity target) {
        if (!isEnabled() || !showPredictBox.get() || target == null || lastPredictedPos == null || mc.player == null) {
            return;
        }
        if (!(target instanceof PlayerEntity) ||
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

        e.stack.push();
        Client.RENDERER.toCamera(e.stack);
        VertexRendering.drawOutline(
                e.stack,
                e.buffer.getBuffer(ClientPipelines.OUTLINE),
                VoxelShapes.cuboid(box),
                0, 0, 0, 0xFFFFFFFF, 1
        );
        e.stack.pop();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        reset();
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