package gg.topchdlc.vse.shutki.module.modules.impl.movement.speed;

import gg.topchdlc.api.events.list.EventInput;
import gg.topchdlc.api.events.list.EventMoveVelocity;
import gg.topchdlc.api.events.list.EventPostMotion;
import gg.topchdlc.api.events.list.EventReceivePacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.ChoiceSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.math.TimeUtility;
import gg.topchdlc.vse.utils.network.NetworkUtility;

/**
 * Create by daun kvass
 */
public class SpeedLonyGrief extends Choice {
    int tick, ground;
     boolean flag, jump;
   public static final SpeedLonyGrief INSTANCE = new SpeedLonyGrief();
    private final float speedlg = 1f;
    private final boolean yMotionlg = true;
    private final float motionY1lg = -0.017f;
    public SpeedLonyGrief() {
        super("SpeedLonyGrief");
    }

    private final Choice minijump = new Choice("minijump") {
        @Override public void onEvent(Event event) {}
    };

    private final CustomChoice custom = new CustomChoice();
    final ChoiceSetting<Choice> preset = choiceSetting("Preset", 0, minijump, custom);
    private static class CustomChoice extends Choice {
        final SliderSetting speed = sliderSetting("Speed", 1, 0, 1).increment(0.001f);
        final CheckBox yMotion = checkbox("Change Y motion", false);
        final SliderSetting motionY = sliderSetting("Motion Y", 0, -1, 1).increment(0.001f);
        CustomChoice() { super("Custom"); }
        @Override public void onEvent(Event event) {}
    }
    public void onEvent(Event event) {
        if (event instanceof EventMoveVelocity e) {
            boolean isLongrif = preset.get() == minijump;
            double factor = 0.03;
            if (tick % 2 == 0) {
                factor = mc.player.isGliding() ? 0.085 : 0.03;
            }
            factor *= isLongrif? speedlg : custom.speed.get() ;
            float yaw = (Client.ROTATION.getRotate().getYaw() + 90f) * 0.017453292f;
            float sin = MathHelper.sin(yaw);
            float cos = MathHelper.cos(yaw);
            e.movement.x += factor * cos;
            e.movement.z += factor * sin;
            if (isLongrif?yMotionlg : custom.yMotion.get() && !mc.player.isGliding()) {
                e.movement.y += isLongrif? motionY1lg : custom.motionY.get();
            }
               jump = false;
        }
        if (event instanceof EventReceivePacket e) {
            if (e.packet instanceof PlayerPositionLookS2CPacket) {
                if (tick % 2 == 1) {
                    tick++;
                }
            }
            if (e.packet instanceof EntityVelocityUpdateS2CPacket p) {
                     flag = false;
                if (p.getEntityId() == mc.player.getId()) {
                            jump = true;
                }
            }
        }
        if (event instanceof EventPostMotion) {
            NetworkUtility.send(new PlayerMoveC2SPacket.OnGroundOnly(true, false));
            if (tick % 2 == 0) {
                NetworkUtility.send(new PlayerMoveC2SPacket.OnGroundOnly(false, false));
                     flag = true;
            }
        }
        if (event instanceof EventInput e) {
               if (jump) e.setJump(true);
        }
    }

    public void onEnabled() {
        tick = 0;
        ground = 0;
        NetworkUtility.send(new PlayerMoveC2SPacket.OnGroundOnly(true, false));
        NetworkUtility.send(new PlayerMoveC2SPacket.OnGroundOnly(false, false));
    }
}