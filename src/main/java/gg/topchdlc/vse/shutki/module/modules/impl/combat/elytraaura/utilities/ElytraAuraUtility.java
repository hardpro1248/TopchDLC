package gg.topchdlc.vse.shutki.module.modules.impl.combat.elytraaura.utilities;



import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.ElytraAura;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.elytraaura.math.ElytraAuraResolve;
import gg.topchdlc.mixin.accessor.ILivingEntity;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import gg.topchdlc.vse.utils.math.TimeUtility;
import gg.topchdlc.vse.utils.player.swap.InventoryUtility;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Vec2f;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ElytraAuraUtility implements MinecraftHolder {
    ElytraAura elytraAura = ElytraAura.INSTANCE;
    TimeUtility fireworkTime = new TimeUtility();

    public boolean hut = false;

    public Vec2f[] parsePitchOffsets(String pitchOffsetsString) {
        if (pitchOffsetsString == null || pitchOffsetsString.trim().isEmpty()) {
            return new Vec2f[]{new Vec2f(0, 0)};
        }

        String[] parts = pitchOffsetsString.split(",");
        List<Vec2f> offsets = new ArrayList<>();

        for (String part : parts) {
            try {
                float pitchValue = Float.parseFloat(part.trim());
                offsets.add(new Vec2f(0, pitchValue));
            } catch (NumberFormatException e) {
            }
        }

        if (offsets.isEmpty()) {
            return new Vec2f[]{new Vec2f(0, 0)};
        }

        return offsets.toArray(new Vec2f[0]);
    }

    public boolean useFireWork(LivingEntity entity) {
        if (elytraAura.autoFireWorkSetting.get()) {

            if (elytraAura.autoAirStackSetting.get() && elytraAura.ifworkAitStack.get(ElytraAura.Feature.IF_TARGET_ON_STOYAK)) {
                boolean iso = mc.player.getEntityPos().distanceTo(TargetsUtility.getTarget().getEntityPos()) > 5.4 && ElytraAuraResolve.isStoyak(TargetsUtility.getTarget()) && !TargetsUtility.getTarget().isOnGround() && !elytraAura.isLeave(TargetsUtility.getTarget());
                if (iso)
                    return false;
            }

            if (((ILivingEntity)mc.player).client$lastAttackedTicks() == 2) {
                if (fireworkTime.reached(200, true) && mc.player.isGliding()) {
                    useItemOnHotbar(Items.FIREWORK_ROCKET);
                    return true;
                }
            }
            if (elytraAura.defensive.get()) {
                if (elytraAura.lastAntiaim && !elytraAura.antiAimIsActive) {
                    elytraAura.lastAntiaim = false;
                    useItemOnHotbar(Items.FIREWORK_ROCKET);
                    return true;
                }
            }
            if (elytraAura.smartUseFireWorkSetting.get()) {
                int delay = 400;
                if (mc.player.getEntityPos().distanceTo(entity.getEntityPos()) < 4) delay = 270;
                if (elytraAura.targetIsLeave(entity)) delay = 400;
                if (entity.isOnGround() || !entity.isGliding() || ElytraAuraResolve.isStoyak(entity)) delay = 450;
                if (!elytraAura.antiAimIsActive && hut) {
                    if (fireworkTime.reached(10, true)) {
                        useItemOnHotbar(Items.FIREWORK_ROCKET);
                        hut = false;
                        return true;
                    }
                }
                if (fireworkTime.reached(delay, true) && mc.player.isGliding()) {
                    useItemOnHotbar(Items.FIREWORK_ROCKET);
                    return true;
                }
            } else {
                if (fireworkTime.reached((long) elytraAura.delayUseFireWorkSetting.get(), true) && mc.player.isGliding()) {
                    useItemOnHotbar(Items.FIREWORK_ROCKET);
                    return true;
                }
            }
        }
        return false;
    }

    public void useItemOnHotbar(Item item) {
        int slot = getItemOnHotbar(item);

        if (slot == -1) {
            int fireworkSlot = InventoryUtility.find(Items.FIREWORK_ROCKET);
            if (fireworkSlot != -1) {
                int freeHotbarSlot = findFreeHotbarSlot();
                if (freeHotbarSlot != -1) {
                    swapSlots(fireworkSlot, freeHotbarSlot);
                    slot = freeHotbarSlot;
                }
            }
        }

        if (slot != -1 && !mc.player.getItemCooldownManager().isCoolingDown(item.getDefaultStack())) {
            InventoryUtility.useItem(slot, false, true, false);
        }
    }

    private int findFreeHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private void swapSlots(int fromSlot, int toSlot) {
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                fromSlot,
                toSlot,
                SlotActionType.SWAP,
                mc.player
        );
    }

    public int getItemOnHotbar(Item items) {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == items) {
                slot = i;
                break;
            }
        }
        return slot;
    }
}
