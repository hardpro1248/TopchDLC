package gg.topchdlc.vse.shutki.module.modules.impl.misc;

import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.MovementCorrection;
import gg.topchdlc.vse.rotation.all.impl.DefaultRotation;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.EnumChoice;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import gg.topchdlc.vse.utils.math.Priorities;
import gg.topchdlc.vse.utils.math.RotationUtility;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import gg.topchdlc.vse.utils.player.swap.InventoryUtility;

import java.util.ArrayList;
import java.util.List;

/**
 * Create by daun kvass
 */
public class WebTrap extends Module {
    public static final WebTrap INSTANCE = new WebTrap();

    public enum Mode implements EnumChoice {
        SILENT("Silent"),
        PACKET("Packet");

        final String label;
        Mode(String l) { label = l; }
        @Override public String getRenderName() { return label; }
        @Override public boolean isDefaultEnabled() { return true; }
    }

    public enum FillMode implements EnumChoice {
        FEET("Только ноги"),
        FULL("Весь хитбокс");

        final String label;
        FillMode(String l) { label = l; }
        @Override public String getRenderName() { return label; }
        @Override public boolean isDefaultEnabled() { return true; }
    }

    private final Group general = group("Настройки");
    private final EnumSetting<Mode>     mode     = general.enumSetting("Режим", Mode.SILENT);
    private final EnumSetting<FillMode> fillMode = general.enumSetting("Заполнение", FillMode.FEET);
    private final SliderSetting range = general.sliderSetting("Дальность", 4f, 1f, 6f).increment(0.5f);
    private final SliderSetting delay = general.sliderSetting("Задержка (тики)", 2f, 0f, 10f).increment(1f);
    private int blockIndex = 0;
    private List<BlockPos> pendingBlocks = new ArrayList<>();
    private int delayTimer = 0;

    private WebTrap() {
        super("WebTrap", Category.Misc, "Застраивает цель паутиной");
    }

    @Override
    protected void onDisable() {
        TargetsUtility.reset();
        delayTimer = 0;
        blockIndex = 0;
        pendingBlocks.clear();
    }

    EventBus<Event> bus = event -> {
        if (event instanceof EventGameTick) onTick();
    };

    private void onTick() {
        if (nullCheck()) return;

        if (delayTimer > 0) {
            delayTimer--;
            return;
        }

        LivingEntity target = TargetsUtility.find(range.get(), TargetsUtility.Sort.Distance);
        if (target == null) return;

        int webSlot = InventoryUtility.findHotbar(Items.COBWEB);
        if (webSlot == -1) return;

        if (fillMode.get() == FillMode.FEET) {
            placeAtFeet(target, webSlot);
        } else {
            placeFullHitbox(target, webSlot);
        }

        delayTimer = (int) delay.get();
    }

    private void placeAtFeet(LivingEntity target, int webSlot) {
        BlockPos pos = BlockPos.ofFloored(target.getX(), target.getY(), target.getZ());
        tryPlace(pos, webSlot);
    }
    private void placeFullHitbox(LivingEntity target, int webSlot) {
        pendingBlocks.clear();

        double minX = target.getBoundingBox().minX;
        double minY = target.getBoundingBox().minY;
        double maxX = target.getBoundingBox().maxX;
        double maxY = target.getBoundingBox().maxY;
        double minZ = target.getBoundingBox().minZ;
        double maxZ = target.getBoundingBox().maxZ;

        for (int x = (int) Math.floor(minX); x <= (int) Math.floor(maxX - 0.001); x++) {
            for (int y = (int) Math.floor(minY); y <= (int) Math.floor(maxY - 0.001); y++) {
                for (int z = (int) Math.floor(minZ); z <= (int) Math.floor(maxZ - 0.001); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (mc.world.getBlockState(pos).isAir()) {
                        pendingBlocks.add(pos);
                    }
                }
            }
        }
        if (pendingBlocks.isEmpty()) return;
        if (blockIndex >= pendingBlocks.size()) blockIndex = 0;
        BlockPos pos = pendingBlocks.get(blockIndex);
        blockIndex++;

        tryPlace(pos, webSlot);
    }
    private void tryPlace(BlockPos pos, int webSlot) {
        if (mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB) return;
        if (!mc.world.getBlockState(pos).isAir()) return;
        Direction supportDir = null;
        BlockPos supportPos = null;
        for (Direction dir : new Direction[]{Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP}) {
            BlockPos neighbor = pos.offset(dir);
            if (!mc.world.getBlockState(neighbor).isAir()) {
                supportDir = dir.getOpposite();
                supportPos = neighbor;
                break;
            }
        }
        if (supportPos == null) return;
        Vec3d hitVec = Vec3d.ofCenter(supportPos).add(
            Vec3d.of(supportDir.getVector()).multiply(0.5)
        );

        BlockHitResult hitResult = new BlockHitResult(hitVec, supportDir, supportPos, false);

        if (mode.get() == Mode.SILENT) {
            placeSilent(webSlot, hitResult, hitVec);
        } else {
            placePacket(webSlot, hitResult);
        }
    }

    private void placeSilent(int webSlot, BlockHitResult hitResult, Vec3d hitVec) {
        Angle angle = RotationUtility.calculate(hitVec);
        int prevSlot = mc.player.getInventory().getSelectedSlot();

        Client.ROTATION.rotate(
            DefaultRotation.INSTANCE,
            angle,
            1,
            false,
            MovementCorrection.SILENT,
            Priorities.NORMAL
        );

        if (prevSlot != webSlot) NetworkUtility.send(new UpdateSelectedSlotC2SPacket(webSlot));
        NetworkUtility.sendUse(Hand.MAIN_HAND, hitResult);
        if (prevSlot != webSlot) NetworkUtility.send(new UpdateSelectedSlotC2SPacket(prevSlot));
    }

    private void placePacket(int webSlot, BlockHitResult hitResult) {
        int prevSlot = mc.player.getInventory().getSelectedSlot();

        NetworkUtility.send(new UpdateSelectedSlotC2SPacket(webSlot));
        NetworkUtility.sendUse(Hand.MAIN_HAND, hitResult);
        NetworkUtility.send(new UpdateSelectedSlotC2SPacket(prevSlot));
    }
}
