package gg.topchdlc.vse.utils.attack;

import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.AuraModule;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.aura.attack.AttackHandler;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import lombok.experimental.UtilityClass;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.MathHelper;

@UtilityClass
public class ShieldBreakerUtility implements MinecraftHolder {

    public boolean shieldBreaker(int packets) {
        if (TargetsUtility.getTarget() == null ) return false;
        if (TargetsUtility.getTarget().getOffHandStack().getItem().equals(Items.SHIELD) ) {
            if (Math.abs(MathHelper.wrapDegrees(mc.player.getYaw() - TargetsUtility.getTarget().getYaw() - 180)) > 90)
                return false;
            for (int i = 0; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.getItem() instanceof AxeItem) {
                    int currSlot = mc.player.getInventory().getSelectedSlot();
                    if (currSlot != i) {
                        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i));
                    }
                    AttackHandler.attackEntity(TargetsUtility.getTarget());
                    if (currSlot != i) {
                        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(currSlot));
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
