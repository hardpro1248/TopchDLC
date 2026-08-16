package gg.topchdlc.vse.shutki.module.modules.impl.combat;

import gg.topchdlc.api.events.list.EventAttack;
import gg.topchdlc.api.events.list.EventDirection;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventPostTick;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import javax.security.auth.callback.CallbackHandler;

/**
 * Create by daun kvass
 */
public class BowSpam extends Module {
    public static final BowSpam INSTANCE = new BowSpam();
    private BowSpam(){super("BowSpam", Category.COMBAT,"x");}
    private final SliderSetting ticks = sliderSetting("delay",3,0,20).increment(1f);
    @EventHandler
    public void onSync(EventGameTick event) {
        if ((mc.player.getOffHandStack().getItem() == Items.BOW || mc.player.getMainHandStack().getItem() == Items.BOW) && mc.player.isUsingItem()) {
            if (mc.player.getItemUseTime() >= this.ticks.get()) {
                sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, mc.player.getHorizontalFacing()));
                sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(mc.player.getOffHandStack().getItem() == Items.BOW ? Hand.OFF_HAND : Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
                mc.player.stopUsingItem();
            }
        }
    }
}
