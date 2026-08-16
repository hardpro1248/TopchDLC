package gg.topchdlc.vse.shutki.module.modules.impl.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventInput;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import gg.topchdlc.vse.utils.player.MoveUtility;

import java.util.Random;

/**
 * Create by daun kvass
 */
public class AimAssist extends Module {
    public static final AimAssist INSTANCE = new AimAssist();

    private final SliderSetting range    = sliderSetting("Дальность", 5f, 1f, 20f).increment(0.5f);
    private final SliderSetting fovLimit = sliderSetting("FOV лимит", 90f, 5f, 180f).increment(1f);
    private final SliderSetting rangeElytra = sliderSetting("dop dist", 20f, 1f, 80f).increment(1f);

    private final SliderSetting yawSpeed   = sliderSetting("Скорость Yaw", 2f, 0.01f, 40f).increment(0.01f);
    private final SliderSetting pitchSpeed = sliderSetting("Скорость Pitch", 1f, 0.01f, 10f).increment(0.01f);
    private final SliderSetting smoothing  = sliderSetting("Плавность", 0.1f, 0.01f, 0.3f).increment(0.01f);

    private final CheckBox noiseEnabled = checkbox("Включить шум", true);
    private final SliderSetting noiseYaw   = sliderSetting("Шум Yaw", 0.2f, 0f, 2f).increment(0.05f)
            .visible(noiseEnabled::get);
    private final SliderSetting noisePitch = sliderSetting("Шум Pitch", 0.1f, 0f, 2f).increment(0.05f)
            .visible(noiseEnabled::get);

    private float targetYaw   = 0f;
    private float targetPitch = 0f;
    private boolean hasTarget = false;
    private LivingEntity currentTarget = null;

    private final Random random = new Random();

    private AimAssist() {
        super("AimAssist", Category.COMBAT, "Помогает в наводке на цель");
    }

    EventBus<Event> bus = event -> {
        if (event instanceof EventGameTick) onTick();
        if (event instanceof Event3D)       onFrame();
        if (event instanceof EventInput e && hasTarget) MoveUtility.silentCorrection(e, mc.player.getYaw());
    };

    private void onTick() {
        if (nullCheck()) {
            resetTarget();
            return;
        }

        float maxRange = mc.player.isGliding() ? rangeElytra.get() : range.get();

        if (currentTarget != null && (!currentTarget.isAlive() || mc.player.distanceTo(currentTarget) > maxRange)) {
            currentTarget = null;
        }

        if (currentTarget == null) {
            currentTarget = TargetsUtility.find(maxRange, TargetsUtility.Sort.Angle);
        }

        if (currentTarget == null) {
            hasTarget = false;
            return;
        }

        float currentYaw   = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        Vec3d eye = mc.player.getEyePos();
        Vec3d center = currentTarget.getEyePos();

        Angle toCenter = Angle.fromRelative(center, eye);
        float deltaYaw = MathHelper.wrapDegrees(toCenter.getYaw() - currentYaw);

        if (Math.abs(deltaYaw) > fovLimit.get()) {
            currentTarget = null;
            hasTarget = false;
            return;
        }
        Box box = currentTarget.getBoundingBox();
        double dx = center.x - eye.x;
        double dz = center.z - eye.z;
        double distXZ = Math.sqrt(dx * dx + dz * dz);

        double lookY = eye.y - Math.tan(Math.toRadians(currentPitch)) * distXZ;
        double clampedY = MathHelper.clamp(lookY, box.minY, box.maxY);

        Vec3d nearestPoint = new Vec3d(center.x, clampedY, center.z);
        Angle toNearest = Angle.fromRelative(nearestPoint, eye);

        float deltaPitch = toNearest.getPitch() - currentPitch;

        float stepYaw   = MathHelper.clamp(deltaYaw,   -yawSpeed.get(),   yawSpeed.get());
        float stepPitch = MathHelper.clamp(deltaPitch, -pitchSpeed.get(), pitchSpeed.get());

        if (noiseEnabled.get()) {
            stepYaw   += (random.nextFloat() * 2f - 1f) * noiseYaw.get();
            stepPitch += (random.nextFloat() * 2f - 1f) * noisePitch.get();
        }

        targetYaw   = currentYaw   + stepYaw;
        targetPitch = MathHelper.clamp(currentPitch + stepPitch, -90f, 90f);
        hasTarget   = true;
    }

    private void onFrame() {
        if (nullCheck() || !hasTarget) return;

        float s = smoothing.get();
        float currentYaw   = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float newYaw   = currentYaw   + MathHelper.wrapDegrees(targetYaw   - currentYaw)   * s;
        float newPitch = currentPitch + (targetPitch - currentPitch) * s;

        mc.player.setYaw(newYaw);
        mc.player.setPitch(MathHelper.clamp(newPitch, -90f, 90f));
    }

    private void resetTarget() {
        hasTarget = false;
        currentTarget = null;
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        resetTarget();
        TargetsUtility.reset();
    }
}