package gg.topchdlc.vse.shutki.module.modules.impl.movement;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventInput;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.MovementCorrection;
import gg.topchdlc.vse.rotation.all.impl.DefaultRotation;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import gg.topchdlc.vse.utils.player.MoveUtility;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import org.nd4j.linalg.api.ops.impl.transforms.same.Negative;

/**
 * Create by daun kvass
 */
public class ElytraBhop extends Module {
    public static final ElytraBhop INSTANCE = new ElytraBhop();
    private final CheckBox ray = checkbox("Ray", false);

    private ElytraBhop() {
        super("ElytraBhop", Category.MOVEMENT, "67");

    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {
        if (mc.player != null) {
        MoveUtility.stopGliding();
        }
    }

    EventBus<Event> events = event -> {
        if (mc.player == null || mc.world == null) return;
        if (event instanceof EventInput e) {
            if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
                e.setJump(true);
                e.setJump(mc.player.age % 2 == 0);
            }
        }

        if (event instanceof EventGameTick) {
            boolean hasElytra = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;

            if (ray.get() && hasElytra) {

                Angle downAngle = new Angle(mc.player.getYaw(), 90);
                mc.options.sprintKey.setPressed(true);
                Client.ROTATION.rotate(
                        DefaultRotation.INSTANCE,
                        downAngle,
                        2,
                        false,
                        MovementCorrection.SILENT,
                        100
                );
            }
        }
    };
}