package gg.topchdlc.vse.shutki.module.modules.impl.player;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventInput;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.MovementCorrection;
import gg.topchdlc.vse.rotation.all.Rotation;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.blocklist.BlockListSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.math.Priorities;
import gg.topchdlc.vse.utils.math.RotationUtility;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import gg.topchdlc.vse.utils.player.MoveUtility;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Nuker extends Module {
    public static final Nuker INSTANCE = new Nuker();
    private final Group main = group("Main");
    private final SliderSetting rangeXZ = main.sliderSetting("Range XZ", 4.5f, 1f, 6f);

    private final SliderSetting rangeUp = main.sliderSetting("Range Up", 1.5f, 0f, 4f);
    private final SliderSetting rangeDown = main.sliderSetting("Range Down", 1.5f, 0f, 4f);

    private final Group rotate = group("Rotate");
    private final EnumSetting<RotateMode> rotateMode = rotate
            .enumSetting("Rotate Mode", RotateMode.PACKET);

    private final SliderSetting rotationSpeed = rotate
            .sliderSetting("Rotation Speed", 10, 1, 100)
            .visible(() -> rotateMode.is(RotateMode.SILENT));

    private final Group options = group("Options");
    private final CheckBox autoTool = options.checkbox("Auto Tool", true);

    private final CheckBox useWhitelist = options.checkbox("Use Whitelist", false);
    private final BlockListSetting whitelist = options.blockListSetting("Whitelist Blocks").visible(useWhitelist::get);

    private final CheckBox useBlacklist = options.checkbox("Use Blacklist", false);
    private final BlockListSetting blacklist = options.blockListSetting("Blacklist Blocks").visible(useBlacklist::get);

    public enum RotateMode { NONE, PACKET, SILENT }

    private Float packetRotateYaw = null;
    private BlockPos breakTarget = null;
    private BlockPos lastAttackTarget = null;

    private Nuker() {
        super("Nuker", Category.PLAYER, "Автоматически ломает блоки вокруг");
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        packetRotateYaw = null;
        breakTarget = null;
        lastAttackTarget = null;
        if (mc.options != null) mc.options.attackKey.setPressed(false);
    }

    EventBus<Event> events = e -> {
        if (e instanceof EventGameTick) onTick();
        if (e instanceof EventInput input && rotateMode.is(RotateMode.PACKET) && packetRotateYaw != null) {
            MoveUtility.silentCorrection(input, packetRotateYaw);
        }
    };

    private final Rotation SMOOTH = (cur, tgt) -> {
        float step = rotationSpeed.get();
        float dy = MathHelper.clamp(MathHelper.wrapDegrees(tgt.getYaw() - cur.getYaw()), -step, step);
        float dp = MathHelper.clamp(tgt.getPitch() - cur.getPitch(), -step, step);
        return new Angle(cur.getYaw() + dy, MathHelper.clamp(cur.getPitch() + dp, -90f, 90f));
    };

    private void onTick() {
        if (mc.player == null || mc.world == null) return;

        packetRotateYaw = null;

        if (breakTarget != null) {
            if (!isValid(breakTarget)) {
                breakTarget = null;
            }
        }

        if (breakTarget == null) {
            breakTarget = findTargetBlock();
        }

        if (breakTarget == null) {
            mc.options.attackKey.setPressed(false);
            lastAttackTarget = null;
            return;
        }

        Vec3d targetVec = Vec3d.ofCenter(breakTarget);
        Angle angle = RotationUtility.calculate(targetVec);

        if (rotateMode.is(RotateMode.SILENT)) {
            Client.ROTATION.rotate(SMOOTH, angle, 2, true, MovementCorrection.SILENT, Priorities.NORMAL);

            Angle current = Client.ROTATION.getRotate();
            float yawDiff = Math.abs(MathHelper.wrapDegrees(angle.getYaw() - current.getYaw()));
            float pitchDiff = Math.abs(angle.getPitch() - current.getPitch());

            if (yawDiff < 10f && pitchDiff < 10f) {
                handleBreaking(breakTarget);
            } else {
                mc.options.attackKey.setPressed(false);
            }
        } else if (rotateMode.is(RotateMode.PACKET)) {
            packetRotateYaw = angle.getYaw();
            NetworkUtility.sendAngle(angle);
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                    angle.getYaw(),
                    angle.getPitch(),
                    mc.player.isOnGround(),
                    mc.player.horizontalCollision
            ));
            handleBreaking(breakTarget);
        } else {
            handleBreaking(breakTarget);
        }
    }

    private void handleBreaking(BlockPos pos) {
        int bestSlot = autoTool.get() ? findBestTool(mc.world.getBlockState(pos)) : mc.player.getInventory().getSelectedSlot();
        int oldSlot = mc.player.getInventory().getSelectedSlot();

        if (bestSlot != -1 && bestSlot != oldSlot) {
            mc.player.getInventory().setSelectedSlot(bestSlot);
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(bestSlot));
        }

        Direction side = getBreakFace(pos);

        if (!pos.equals(lastAttackTarget)) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, side));
            mc.player.swingHand(Hand.MAIN_HAND);
            lastAttackTarget = pos;
        }

        mc.options.attackKey.setPressed(true);
    }

    private boolean isValid(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir() || state.getHardness(mc.world, pos) < 0) return false;

        double maxVerticalRange = Math.max(rangeUp.get(), rangeDown.get());
        double maxAllowedDistance = Math.sqrt(Math.pow(rangeXZ.get(), 2) + Math.pow(maxVerticalRange, 2)) + 1.0;

        if (mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos)) > maxAllowedDistance) return false;

        if (useWhitelist.get() && !whitelist.contains(state.getBlock())) return false;
        if (useBlacklist.get() && blacklist.contains(state.getBlock())) return false;

        return true;
    }

    private Direction getBreakFace(BlockPos pos) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d center = Vec3d.ofCenter(pos);
        double dx = eye.x - center.x;
        double dy = eye.y - center.y;
        double dz = eye.z - center.z;
        double ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
        if (ax > ay && ax > az) return dx > 0 ? Direction.EAST : Direction.WEST;
        if (ay > ax && ay > az) return dy > 0 ? Direction.UP : Direction.DOWN;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private BlockPos findTargetBlock() {
        List<BlockPos> blocks = new ArrayList<>();
        int xzR = (int) Math.ceil(rangeXZ.get());

        int upR = (int) Math.ceil(rangeUp.get());
        int downR = (int) Math.ceil(rangeDown.get());

        BlockPos playerPos = mc.player.getBlockPos();

        for (int x = -xzR; x <= xzR; x++) {
            for (int y = -downR; y <= upR; y++) {
                for (int z = -xzR; z <= xzR; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (isValid(pos)) {
                        double distXZ = Math.sqrt(Math.pow(mc.player.getX() - (pos.getX() + 0.5), 2) + Math.pow(mc.player.getZ() - (pos.getZ() + 0.5), 2));
                        if (distXZ <= rangeXZ.get()) {
                            blocks.add(pos);
                        }
                    }
                }
            }
        }

        return blocks.stream()
                .min(Comparator.comparingDouble(p -> mc.player.getEyePos().distanceTo(Vec3d.ofCenter(p))))
                .orElse(null);
    }

    private int findBestTool(BlockState state) {
        float bestSpeed = 1f;
        int bestSlot = mc.player.getInventory().getSelectedSlot();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        return bestSlot;
    }
}