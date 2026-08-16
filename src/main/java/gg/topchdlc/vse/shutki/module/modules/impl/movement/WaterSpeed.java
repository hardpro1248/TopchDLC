package gg.topchdlc.vse.shutki.module.modules.impl.movement;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.keybind.KeybindSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class WaterSpeed extends Module {
    public static final WaterSpeed INSTANCE = new WaterSpeed();

    public enum Mode { FuntimeJump, Legit, MetaHvH }

    public EnumSetting<Mode> mode = enumSetting("Режим", Mode.FuntimeJump);

    public SliderSetting speed = sliderSetting("Скорость", 0.5f, 0.1f, 4.0f).increment(0.01f).visible(() -> mode.is(Mode.MetaHvH));
    public CheckBox useBoost = checkbox("Использовать буст", false).visible(() -> mode.is(Mode.MetaHvH));
    public KeybindSetting boostBind = keybindSetting("Клавиша буста", -1).visible(() -> mode.is(Mode.MetaHvH) && useBoost.get());
    public SliderSetting boostSpeed = sliderSetting("Скорость буста", 1.2f, 0.1f, 5.0f).increment(0.01f).visible(() -> mode.is(Mode.MetaHvH) && useBoost.get());

    private WaterSpeed() {
        super("Water speed", Category.MOVEMENT, "Ускоряет движение в воде");
    }

    EventBus<Event> events = event -> {
        if (event instanceof EventGameTick) {
            if (mc.player == null || mc.world == null || !isEnabled()) return;
            PlayerInput playerInput = mc.player.input.playerInput;
            switch (mode.get()) {

                case FuntimeJump -> {
                    if (mc.player.isTouchingWater()) {
                        Client.ROTATION.rotate(new Angle(mc.player.getYaw(), 32), false);
                        if (mc.player.isOnGround()) {
                            mc.player.jump();
                            Vec3d vec = mc.player.getVelocity();
                            mc.player.setVelocity(vec.x, 0.25, vec.z);
                            NetworkUtility.sendInputPacket(!playerInput.forward(), !playerInput.backward(), playerInput.left(), playerInput.right(), false, true, playerInput.sprint());
                            NetworkUtility.sendInputPacket(playerInput.forward(), playerInput.backward(), playerInput.left(), playerInput.right(), false, false, playerInput.sprint());
                        }
                    }
                }

                case MetaHvH -> {
                    if (mc.player.isSwimming() ) {

                        float forward = (playerInput.forward() ? 1 : 0) - (playerInput.backward() ? 1 : 0);
                        float side = (playerInput.left() ? 1 : 0) - (playerInput.right() ? 1 : 0);

                        if (forward == 0 && side == 0 ) return;

                        boolean isBoostHeld = useBoost.get() && boostBind.getBind() != -1 &&
                                GLFW.glfwGetKey(mc.getWindow().getHandle(), boostBind.getBind()) == GLFW.GLFW_PRESS;

                        double horizontalSpeed = isBoostHeld ? boostSpeed.get() : speed.get();

                        float yaw = mc.player.getYaw();
                        double angle = Math.toRadians(calculateYaw(yaw, forward, side));

                        double motionX = -Math.sin(angle) * horizontalSpeed;
                        double motionZ = Math.cos(angle) * horizontalSpeed;

                        double motionY = mc.player.getVelocity().y;
                        mc.player.setVelocity(motionX, motionY, motionZ);
                    }
                }
            }
        }
    };

    private float calculateYaw(float yaw, float forward, float side) {
        if (forward < 0) yaw += 180;
        float forwardOffset = 90;
        if (forward < 0) forwardOffset = -45;
        else if (forward > 0) forwardOffset = 45;
        if (side > 0) yaw -= forwardOffset;
        else if (side < 0) yaw += forwardOffset;
        return yaw;
    }
}