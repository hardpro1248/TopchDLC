package gg.topchdlc.vse.utils.player;

import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import lombok.experimental.UtilityClass;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;

@UtilityClass
public class VClipUtility implements MinecraftHolder {

    public void clip(double yOffset) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (yOffset == 0) {
            ChatUtility.send("не незя 0");
            return;
        }
        int elytraSlot = findElytraSlot();
        boolean hasElytra = elytraSlot != -1 && elytraSlot != -2;
        if (hasElytra) {
            swapElytra(elytraSlot, true);
        }
        teleport(yOffset, hasElytra);
        if (hasElytra) {
            swapElytra(elytraSlot, false);
        }
    }

    public void clipUp() {
        if (mc.player == null || mc.world == null) {
            return;
        }
        float offset = scanVerticalOffset(mc.player.getBlockPos(), true);
        if (offset == 0) {
            fail();
            return;
        }
        clip(offset);
    }

    public void clipDown() {
        if (mc.player == null || mc.world == null) {
            return;
        }
        float offset = scanVerticalOffset(mc.player.getBlockPos(), false);
        if (offset == 0) {
            fail();
            return;
        }
        clip(offset);
    }

    private void teleport(double yOffset, boolean elytra) {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        boolean horizCollision = mc.player.horizontalCollision;

        if (elytra) {
            for (int i = 0; i < 2; i++) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false, horizCollision));
            }
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + yOffset, z, false, horizCollision));
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        } else {
            int packets = Math.max((int) (Math.abs(yOffset) / 1000.0), 3);
            for (int i = 0; i < packets; i++) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(mc.player.isOnGround(), horizCollision));
            }
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + yOffset, z, false, horizCollision));
        }

        mc.player.setPosition(x, y + yOffset, z);
        mc.player.fallDistance = 0;

        String unit = Math.abs(yOffset) > 1 ? "блоков" : "блок";
        ChatUtility.send(String.format("тепаю наа", yOffset, unit));
    }

    private float scanVerticalOffset(BlockPos playerPos, boolean up) {
        int startY = up ? 3 : -1;
        int endY = up ? 320 : -320;
        int step = up ? 1 : -1;

        for (int i = startY; i != endY; i += step) {
            BlockPos targetPos = playerPos.add(0, i, 0);
            if (mc.world.getBlockState(targetPos).isAir()) {
                return i + (up ? 1 : -1);
            }
            if (!up && mc.world.getBlockState(targetPos).isOf(Blocks.BEDROCK)) {
                ChatUtility.send("тут низя под бедрок");
                return 0;
            }
        }
        return 0;
    }

    private void fail() {
        ChatUtility.send("чет не получилось");
    }

    private void swapElytra(int elytraSlot, boolean equip) {
        final int chestSlot = 6;
        if (mc.interactionManager == null) return;

        if (equip) {
            mc.interactionManager.clickSlot(0, elytraSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(0, chestSlot, 0, SlotActionType.PICKUP, mc.player);
        } else {
            mc.interactionManager.clickSlot(0, chestSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(0, elytraSlot, 0, SlotActionType.PICKUP, mc.player);
        }
    }

    private int findElytraSlot() {
        if (mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
            return -2;
        }
        int slot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(Items.ELYTRA)) {
                slot = i;
                break;
            }
        }
        if (slot >= 0 && slot < 9) {
            slot += 36;
        }
        return slot;
    }
}