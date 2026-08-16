package gg.topchdlc.vse.shutki.module.modules.impl.movement;

import com.google.common.eventbus.Subscribe;
import gg.topchdlc.api.events.list.EventMoveVelocity;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.aura.elytra.PredictUtils;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import gg.topchdlc.vse.utils.player.MoveUtility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;

/**
 * Create by daun kvass
 */
public class Strafe extends Module {
    public static final Strafe INSTANCE = new Strafe();

    public final CheckBox followTarget = checkbox("Follow Aura Target", false);
    public final CheckBox overtake = checkbox("Overtake", false).visible(followTarget::get);
    public final SliderSetting overtakeTicks = sliderSetting("Overtake Ticks", 2.0f, 0.1f, 10.0f).increment(0.1f).visible(() -> followTarget.get() && overtake.get());

    public Strafe() {
        super("Strafe", Category.MOVEMENT, "ччч");
    }

    @Subscribe
    public void onMove(EventMoveVelocity event) {
        if (!isEnabled() || mc.player == null || mc.world == null) return;

        if (mc.player.isOnGround() || mc.player.isTouchingWater() || mc.player.isSubmergedInWater() || mc.player.getAbilities().flying) {
            return;
        }

        PlayerInput input = mc.player.input.playerInput;
        float forward = (input.forward() ? 1.0f : 0.0f) - (input.backward() ? 1.0f : 0.0f);
        float strafe = (input.left() ? 1.0f : 0.0f) - (input.right() ? 1.0f : 0.0f);
        float yaw = mc.player.getYaw();

        LivingEntity auraTarget = TargetsUtility.getTarget();
        boolean hasTarget = followTarget.get() && auraTarget != null && auraTarget.isAlive();

        if (forward == 0 && strafe == 0 && !hasTarget) {
            return;
        }

        double currentSpeed = Math.sqrt(event.movement.x * event.movement.x + event.movement.z * event.movement.z);
        double speed = Math.max(currentSpeed, 0.2873);

        if (hasTarget) {
            Vec3d targetPos = auraTarget.getEntityPos();
            if (overtake.get() && MoveUtility.getBPS(auraTarget) > 0f) {
                targetPos = PredictUtils.predictGround(auraTarget, auraTarget.getEntityPos(), overtakeTicks.get());
            }
            double dx = targetPos.x - mc.player.getX();
            double dz = targetPos.z - mc.player.getZ();
            yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;

            forward = 1.0f;
            strafe = 0.0f;
        } else {
            if (forward != 0.0f) {
                if (strafe > 0.0f) {
                    yaw += (float) (forward > 0.0f ? -45 : 45);
                } else if (strafe < 0.0f) {
                    yaw += (float) (forward > 0.0f ? 45 : -45);
                }
                strafe = 0.0f;
                if (forward > 0.0f) {
                    forward = 1.0f;
                } else if (forward < 0.0f) {
                    forward = -1.0f;
                }
            }
        }

        double cos = Math.cos(Math.toRadians(yaw + 90.0f));
        double sin = Math.sin(Math.toRadians(yaw + 90.0f));

        double moveX = (double) forward * speed * cos + (double) strafe * speed * sin;
        double moveZ = (double) forward * speed * sin - (double) strafe * speed * cos;

        event.movement = new Vec3d(moveX, event.movement.y, moveZ);
    }

    @Override
    public void onDisable() {
    }
}