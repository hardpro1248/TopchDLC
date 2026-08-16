package gg.topchdlc.vse.shutki.module.modules.impl.player;

import com.google.common.eventbus.Subscribe;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventAttack;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.sound.SoundEvents;

public class Sprint extends Module {
    public static final Sprint INSTANCE = new Sprint();
    private enum mode { Legit, HVH }

    public EnumSetting<mode> Mode = enumSetting("Modes", mode.Legit);
    public CheckBox ignoreHunger = checkbox("Ignore hunger", false);

    private Sprint() {
        super("Sprint", Category.PLAYER, "Автоматически бежит");
        setEnabled(true, false);
    }
    OtherClientPlayerEntity fakePlayers;

    @Subscribe
    public void onUpdate(EventGameTick event) {
        if (mc.player == null) return;

        if (Mode.is(mode.Legit)) {
            mc.options.sprintKey.setPressed(true);

            boolean hasEnoughFood = mc.player.getHungerManager().getFoodLevel() > 6 || mc.player.getAbilities().flying;

            boolean legitCondition =
                    ((!mc.player.isTouchingWater() || mc.player.isSubmergedInWater()) &&
                            !mc.player.isGliding() && (ignoreHunger.get() || hasEnoughFood) &&
                            !mc.player.isUsingItem() && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS) &&
                            mc.player.input.hasForwardMovement());

            mc.player.setSprinting(legitCondition);
        } else {
            mc.options.sprintKey.setPressed(true);
        }
    }

    @Override
    protected void onDisable() {
        if (mc.player == null) return;
        super.onDisable();
        mc.player.setSprinting(false);
        NetworkUtility.send(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
    }
}