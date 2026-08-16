package gg.topchdlc.vse.shutki.module.modules.impl.movement;

import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.list.EventInput;
import gg.topchdlc.api.events.list.EventMoveVelocity;
import gg.topchdlc.api.events.list.EventPostMotion;
import gg.topchdlc.api.events.list.EventReceivePacket;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.utils.network.NetworkUtility;

/**
 * Create by daun kvass
 */
public class LevitationControl extends Module {
    public static final LevitationControl INSTANCE = new LevitationControl();

    private float motionY = -0.05f;
    private float motionYVER = 0.03f;


    private LevitationControl() {
        super("LevitationControl", Category.MOVEMENT, "Позволят почти фулл контролирвовать левитацию");
    }

    int tick, ground;

    public void onEvent(Event event) {
        if (mc.player == null || !mc.player.hasStatusEffect(StatusEffects.LEVITATION)) {
            return;
        }

        if (event instanceof EventMoveVelocity e) {
            if (mc.options.jumpKey.isPressed()){
                e.movement.y += motionYVER;
            }else

            if (!mc.player.isOnGround() || mc.options.sneakKey.isPressed()) {
                e.movement.y += motionY;
            }
        }

        if (event instanceof EventReceivePacket e) {
            if (e.packet instanceof PlayerPositionLookS2CPacket) {
                if (tick % 2 == 1) {
                    tick++;
                }
            }
            if (e.packet instanceof EntityVelocityUpdateS2CPacket p) {
                if (p.getEntityId() == mc.player.getId()) {

                }
            }
        }

        if (event instanceof EventPostMotion) {
            NetworkUtility.send(new PlayerMoveC2SPacket.OnGroundOnly(true, false));
            if (tick % 2 == 0) {
                NetworkUtility.send(new PlayerMoveC2SPacket.OnGroundOnly(false, false));
            }
        }

        if (event instanceof EventInput e) {
        }
    }

    public void onEnabled() {
        tick = 0;
        ground = 0;
        if (mc.player != null && mc.player.hasStatusEffect(StatusEffects.LEVITATION)) {
            NetworkUtility.send(new PlayerMoveC2SPacket.OnGroundOnly(true, false));
            NetworkUtility.send(new PlayerMoveC2SPacket.OnGroundOnly(false, false));
        }
    }
}