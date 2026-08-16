package gg.topchdlc.vse.shutki.module.modules.impl.combat;

import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.Event3D;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.MovementCorrection;
import gg.topchdlc.vse.rotation.all.impl.SnapRotation;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.math.RotationUtility;

import gg.topchdlc.api.render.system.ClientPipelines;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CrystalAura extends Module {
    public static final CrystalAura INSTANCE = new CrystalAura();

    public enum SwapMode {
        Packet("Packet"),
        Client("Client");

        private final String name;
        SwapMode(String name) { this.name = name; }
        @Override public String toString() { return name; }
    }

    private final SliderSetting range = sliderSetting("Range", 4.5f, 1f, 6f).increment(0.1f);
    private final SliderSetting maxSelf = sliderSetting("Max Self", 5f, 0f, 20f).increment(0.1f);
    private final SliderSetting minTargetDamage = sliderSetting("Min Target Damage", 4.0f, 0f, 20f).increment(0.1f);
    private final EnumSetting<SwapMode> swapMode = enumSetting("Swap Mode", SwapMode.Client);
    private final CheckBox autoObsidian = checkbox("Auto Obsidian", true);
    private final CheckBox breakBlocks = checkbox("Break Any Blocks", true);
    private final CheckBox renderActive = checkbox("Render Target Blocks", true);
    private final CheckBox renderFill = checkbox("Fill Blocks", true);
    private final CheckBox renderOutline = checkbox("Outline Blocks", true);

    private final SnapRotation snap = SnapRotation.INSTANCE;

    private Action currentAction;
    private BlockPos miningPos;
    private int miningToolSlot = -1;
    private long miningStartedAt;
    private boolean miningStarted;

    private int obsidianCooldown = 0;
    private BlockPos lastPlacedObsidianPos = null;

    private CrystalAura() {
        super("CrystalAura", Category.COMBAT, "Smart fast crystal aura");
    }

    EventBus<Event> events = e -> {
        if (e instanceof EventGameTick) {
            tick();
        }
        if (e instanceof Event3D event3D) {
            render(event3D);
        }
    };

    @Override
    protected void onEnable() {
        super.onEnable();
        resetState();
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        resetState();
    }

    private void tick() {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) {
            resetState();
            return;
        }

        if (obsidianCooldown > 0) {
            obsidianCooldown--;
        }

        PlayerEntity target = findTarget();
        if (target == null) {
            resetState();
            return;
        }

        Action action = selectBestAction(target);
        if (action == null) {
            resetState();
            return;
        }

        if (isActionChanged(currentAction, action)) {
            if (currentAction != null && currentAction.type() == ActionType.BREAK_BLOCK) {
                abortMining();
            }
            currentAction = action;
            snap.reset();
            snap.prepareSnap(Client.ROTATION.getRotate());
        }

        Angle targetAngle = RotationUtility.calcRotate(action.lookVec(), true);
        Client.ROTATION.rotate(
                snap,
                targetAngle,
                1,
                false,
                MovementCorrection.SILENT,
                200
        );

        if (snap.isAimedAt(Client.ROTATION.getRotate(), targetAngle, getAimThreshold(action.type()))) {
            performAction(action);
        }
    }

    private Action selectBestAction(PlayerEntity target) {
        Action breakAction = findBestBreakAction(target);
        Action placeAction = findBestPlaceAction(target);
        Action obsidianAction = autoObsidian.get() ? findBestObsidianAction(target) : null;
        Action blockBreakAction = breakBlocks.get() ? findBestBlockBreakAction(target) : null;

        Action best = null;
        if (isBetterAction(breakAction, best)) best = breakAction;
        if (isBetterAction(placeAction, best)) best = placeAction;
        if (isBetterAction(obsidianAction, best)) best = obsidianAction;
        if (isBetterAction(blockBreakAction, best)) best = blockBreakAction;

        return best;
    }

    private Action findBestBreakAction(PlayerEntity target) {
        Action best = null;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity crystal) || !crystal.isAlive() || crystal.isRemoved()) {
                continue;
            }

            Vec3d crystalPos = crystal.getEntityPos();
            if (mc.player.getEyePos().distanceTo(crystalPos) > range.get()) {
                continue;
            }

            float targetDamage = CrystalAuto.INSTANCE.estimateDamage(crystalPos, target);
            float selfDamage = CrystalAuto.INSTANCE.estimateDamage(crystalPos, mc.player);

            if (!isGoodDamage(targetDamage, selfDamage, crystalPos, target)) {
                continue;
            }

            Action action = new Action(
                    ActionType.BREAK,
                    null,
                    crystal,
                    null,
                    crystalPos,
                    targetDamage,
                    selfDamage
            );

            if (isBetterAction(action, best)) best = action;
        }

        return best;
    }

    private Action findBestPlaceAction(PlayerEntity target) {
        Action best = null;

        for (BlockPos pos : iterateTargetFeetPositions(target)) {
            if (!canCrystal(pos)) continue;

            Vec3d crystalPos = getCrystalVec(pos);
            if (mc.player.getEyePos().distanceTo(crystalPos) > range.get()) continue;

            float targetDamage = CrystalAuto.INSTANCE.estimateDamage(crystalPos, target);
            float selfDamage = CrystalAuto.INSTANCE.estimateDamage(crystalPos, mc.player);

            if (!isGoodDamage(targetDamage, selfDamage, crystalPos, target)) {
                continue;
            }

            Action action = new Action(
                    ActionType.PLACE_CRYSTAL,
                    pos.toImmutable(),
                    null,
                    null,
                    crystalPos,
                    targetDamage,
                    selfDamage
            );

            if (isBetterAction(action, best)) best = action;
        }

        return best;
    }

    private Action findBestObsidianAction(PlayerEntity target) {
        if (findObsidianSlot() == -1) return null;
        if (obsidianCooldown > 0) return null;

        Action best = null;
        for (BlockPos pos : iterateTargetFeetPositions(target)) {
            if (lastPlacedObsidianPos != null && pos.equals(lastPlacedObsidianPos)) continue;

            Direction face = findPlaceFace(pos);
            if (!canPlaceObsidian(pos, target, face)) continue;

            Vec3d crystalPos = getCrystalVec(pos);
            float targetDamage = CrystalAuto.INSTANCE.estimateDamage(crystalPos, target);
            float selfDamage = CrystalAuto.INSTANCE.estimateDamage(crystalPos, mc.player);

            if (!isGoodDamage(targetDamage, selfDamage, crystalPos, target)) {
                continue;
            }

            Vec3d hitVec = getPlaceHitVec(pos, face);
            Action action = new Action(
                    ActionType.PLACE_OBSIDIAN,
                    pos.toImmutable(),
                    null,
                    face,
                    hitVec,
                    targetDamage,
                    selfDamage
            );

            if (isBetterAction(action, best)) best = action;
        }

        return best;
    }

    private Action findBestBlockBreakAction(PlayerEntity target) {
        Action best = null;

        for (BlockPos base : iterateTargetFeetPositions(target)) {
            if (mc.player.getEyePos().distanceTo(getCrystalVec(base)) > range.get()) continue;

            BlockPos breakPos = findBlockToBreakForSpot(base);
            if (breakPos == null || mc.player.getEyePos().distanceTo(Vec3d.ofCenter(breakPos)) > range.get()) continue;

            Vec3d crystalPos = getCrystalVec(breakPos);

            BlockState originalState = mc.world.getBlockState(breakPos);
            mc.world.setBlockState(breakPos, Blocks.OBSIDIAN.getDefaultState(), 19);

            float targetDamage = 0;
            float selfDamage = 0;
            try {
                targetDamage = CrystalAuto.INSTANCE.estimateDamage(crystalPos, target);
                selfDamage = CrystalAuto.INSTANCE.estimateDamage(crystalPos, mc.player);
            } finally {
                mc.world.setBlockState(breakPos, originalState, 19);
            }

            if (!isGoodDamage(targetDamage, selfDamage, crystalPos, target)) {
                continue;
            }

            Action action = new Action(
                    ActionType.BREAK_BLOCK,
                    breakPos.toImmutable(),
                    null,
                    getBreakFace(breakPos),
                    Vec3d.ofCenter(breakPos),
                    targetDamage,
                    selfDamage
            );

            if (isBetterAction(action, best)) best = action;
        }

        return best;
    }

    private boolean isGoodDamage(float targetDamage, float selfDamage, Vec3d crystalPos, PlayerEntity target) {
        if (targetDamage < minTargetDamage.get()) return false;
        if (selfDamage > maxSelf.get()) return false;

        if (selfDamage >= targetDamage && selfDamage > 1.0f) {
            return false;
        }

        if (crystalPos.y > target.getY() + 1.25) {
            return false;
        }

        return true;
    }

    private boolean isBetterAction(Action candidate, Action currentBest) {
        if (candidate == null) return false;
        if (currentBest == null) return true;

        return scoreAction(candidate) > scoreAction(currentBest);
    }


    private double scoreAction(Action action) {
        Vec3d crystalVec = getActionCrystalVec(action);
        PlayerEntity target = findTarget();
        if (target == null) return 0;

        double score = (action.targetDamage() * 250.0) - (action.selfDamage() * 450.0);

        double yDiff = Math.abs(crystalVec.y - (target.getY() + 1.0));
        score -= yDiff * 150.0;

        if (action.type() == ActionType.BREAK_BLOCK) {
            score -= 200.0;
        }

        return score;
    }

    private Vec3d getActionCrystalVec(Action action) {
        if (action.type() == ActionType.BREAK && action.crystal() != null) {
            return action.crystal().getEntityPos();
        }
        if (action.pos() != null) {
            return getCrystalVec(action.pos());
        }
        return action.lookVec();
    }

    private float getAimThreshold(ActionType type) {
        return switch (type) {
            case BREAK -> 10.0f;
            case PLACE_CRYSTAL, PLACE_OBSIDIAN, BREAK_BLOCK -> 7.0f;
        };
    }

    private void performAction(Action action) {
        switch (action.type()) {
            case BREAK -> attack(action.crystal());
            case PLACE_CRYSTAL -> placeCrystal(action.pos());
            case PLACE_OBSIDIAN -> placeObsidian(action.pos(), action.face());
            case BREAK_BLOCK -> breakBlock(action.pos());
        }
    }

    private void placeCrystal(BlockPos pos) {
        if (pos == null || !canCrystal(pos)) return;

        int slot = findCrystalSlot();
        if (slot == -1) return;

        Hand hand = (slot == 40) ? Hand.OFF_HAND : Hand.MAIN_HAND;

        if (hand == Hand.MAIN_HAND) {
            if (!prepareItemInHand(slot, Items.END_CRYSTAL)) {
                return;
            }
        }

        mc.getNetworkHandler().sendPacket(
                new PlayerInteractBlockC2SPacket(
                        hand,
                        new BlockHitResult(getCrystalVec(pos), Direction.UP, pos, false),
                        0
                )
        );
    }

    private void placeObsidian(BlockPos pos, Direction face) {
        if (pos == null || face == null || !canPlaceObsidian(pos, null, face)) return;

        int slot = findObsidianSlot();
        if (slot == -1) return;

        if (!prepareItemInHand(slot, Items.OBSIDIAN)) {
            return;
        }

        BlockPos neighbor = pos.offset(face);
        Direction clickFace = face.getOpposite();

        mc.getNetworkHandler().sendPacket(
                new PlayerInteractBlockC2SPacket(
                        Hand.MAIN_HAND,
                        new BlockHitResult(getPlaceHitVec(pos, face), clickFace, neighbor, false),
                        0
                )
        );

        obsidianCooldown = 8;
        lastPlacedObsidianPos = pos.toImmutable();
    }

    private boolean prepareItemInHand(int slot, Item expectedItem) {
        if (slot < 0 || slot > 8) return false;

        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));

        if (swapMode.get() == SwapMode.Client) {
            mc.player.getInventory().setSelectedSlot(slot);
            ItemStack held = mc.player.getMainHandStack();
            return !held.isEmpty() && held.getItem() == expectedItem;
        }

        return true;
    }

    private void attack(EndCrystalEntity crystal) {
        if (crystal == null || !crystal.isAlive() || crystal.isRemoved()) return;

        mc.getNetworkHandler().sendPacket(
                PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking())
        );
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private PlayerEntity findTarget() {
        return mc.world.getPlayers().stream()
                .filter(player -> player != mc.player && player.isAlive() && !player.isRemoved())
                .filter(player -> !Client.FRIENDS.isFriend(player.getName().getString()))
                .filter(player -> mc.player.distanceTo(player) <= range.get() + 2.5f)
                .min(Comparator.comparingDouble(player -> mc.player.distanceTo(player)))
                .orElse(null);
    }

    private boolean canCrystal(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (!(state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK))) return false;

        BlockPos upPos = pos.up();
        if (!mc.world.getBlockState(upPos).isAir()) {
            return false;
        }

        return !hasBlockingEntityForCrystal(pos);
    }

    private boolean canPlaceObsidian(BlockPos pos, PlayerEntity target, Direction face) {
        if (pos == null || face == null) return false;
        if (!mc.world.getBlockState(pos).isAir()) return false;
        if (mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos)) > range.get()) return false;
        if (!mc.world.getBlockState(pos.up()).isAir()) return false;

        BlockPos neighbor = pos.offset(face);
        BlockState neighborState = mc.world.getBlockState(neighbor);
        if (neighborState.isAir() || !neighborState.isSolidBlock(mc.world, neighbor)) return false;

        Box placeBox = new Box(pos);
        if (placeBox.intersects(mc.player.getBoundingBox())) return false;
        if (target != null && placeBox.intersects(target.getBoundingBox())) return false;

        return !hasBlockingEntityForCrystal(pos);
    }

    private BlockPos findBlockToBreakForSpot(BlockPos base) {
        if (base == null) return null;

        BlockState baseState = mc.world.getBlockState(base);

        if (!baseState.isAir() && isBreakable(base)) {
            return base;
        }

        BlockPos first = base.up();
        if (!mc.world.getBlockState(first).isAir() && isBreakable(first)) {
            return first;
        }

        return null;
    }

    private boolean isBreakable(BlockPos pos) {
        if (pos == null || mc.world == null) return false;

        BlockState state = mc.world.getBlockState(pos);
        return !state.isAir()
                && !state.isOf(Blocks.BEDROCK)
                && !state.isOf(Blocks.OBSIDIAN)
                && state.getHardness(mc.world, pos) >= 0.0f
                && findBestToolSlot(state) != -1;
    }

    private void breakBlock(BlockPos pos) {
        if (pos == null || !isBreakable(pos)) {
            clearMiningState();
            return;
        }

        int bestTool = findBestToolSlot(mc.world.getBlockState(pos));
        if (bestTool == -1) {
            clearMiningState();
            return;
        }

        if (miningPos == null || !miningPos.equals(pos) || miningToolSlot != bestTool) {
            abortMining();
            miningPos = pos.toImmutable();
            miningToolSlot = bestTool;
            miningStarted = false;
            miningStartedAt = 0L;
        }

        Direction face = getBreakFace(pos);
        if (!miningStarted) {
            prepareItemInHand(miningToolSlot, mc.player.getInventory().getStack(miningToolSlot).getItem());
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, face));
            mc.interactionManager.updateBlockBreakingProgress(pos, face);
            mc.player.swingHand(Hand.MAIN_HAND);
            miningStarted = true;
            miningStartedAt = System.currentTimeMillis();
        } else {
            prepareItemInHand(miningToolSlot, mc.player.getInventory().getStack(miningToolSlot).getItem());
            mc.interactionManager.updateBlockBreakingProgress(pos, face);
        }

        if (System.currentTimeMillis() - miningStartedAt >= getBreakTimeMs(pos, miningToolSlot)) {
            prepareItemInHand(miningToolSlot, mc.player.getInventory().getStack(miningToolSlot).getItem());
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, face));
            mc.player.swingHand(Hand.MAIN_HAND);
            clearMiningState();
        }
    }

    private int findBestToolSlot(BlockState state) {
        int bestSlot = mc.player.getInventory().getSelectedSlot();
        float bestSpeed = state == null ? 1.0f : mc.player.getInventory().getStack(bestSlot).getMiningSpeedMultiplier(state);
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private Direction getBreakFace(BlockPos pos) {
        Vec3d diff = mc.player.getEyePos().subtract(Vec3d.ofCenter(pos));
        return Direction.getFacing(diff.x, diff.y, diff.z);
    }

    private long getBreakTimeMs(BlockPos pos, int toolSlot) {
        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir()) return 0L;

        int savedSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(toolSlot);
        float delta = state.calcBlockBreakingDelta(mc.player, mc.world, pos);
        mc.player.getInventory().setSelectedSlot(savedSlot);

        if (delta >= 1.0f) return 0L;
        if (delta <= 0.0f) return 10000L;

        int ticks = (int) Math.ceil(1.0f / delta);
        return Math.max(50L, ticks * 50L);
    }

    private void abortMining() {
        if (mc.player != null && mc.getNetworkHandler() != null && miningPos != null && miningStarted) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
                    miningPos,
                    Direction.UP
            ));
        }
        clearMiningState();
    }

    private void clearMiningState() {
        miningPos = null;
        miningToolSlot = -1;
        miningStartedAt = 0L;
        miningStarted = false;
    }


    private List<BlockPos> iterateTargetFeetPositions(PlayerEntity target) {
        List<BlockPos> positions = new ArrayList<>();
        BlockPos currentBase = target.getBlockPos();

        Vec3d vel = target.getVelocity();
        Vec3d predictedVec = target.getEntityPos().add(vel.x * 2.0, vel.y * 1.0, vel.z * 2.0);
        BlockPos predictedBase = BlockPos.ofFloored(predictedVec);

        for (BlockPos b : BlockPos.iterate(currentBase.add(-2, -1, -2), currentBase.add(2, 0, 2))) {
            positions.add(b.toImmutable());
        }
        for (BlockPos b : BlockPos.iterate(predictedBase.add(-2, -1, -2), predictedBase.add(2, 0, 2))) {
            BlockPos immutable = b.toImmutable();
            if (!positions.contains(immutable)) {
                positions.add(immutable);
            }
        }

        return positions;
    }

    private boolean hasBlockingEntityForCrystal(BlockPos pos) {
        Box crystalBox = new Box(
                pos.getX() + 0.001,
                pos.getY() + 1.0,
                pos.getZ() + 0.001,
                pos.getX() + 0.999,
                pos.getY() + 3.0,
                pos.getZ() + 0.999
        );

        return !mc.world.getOtherEntities(
                null,
                crystalBox,
                entity -> entity.isAlive() && !entity.isRemoved() && !(entity instanceof EndCrystalEntity)
        ).isEmpty();
    }

    private Direction findPlaceFace(BlockPos pos) {
        Direction[] priority = {
                Direction.DOWN,
                Direction.NORTH,
                Direction.SOUTH,
                Direction.EAST,
                Direction.WEST,
                Direction.UP
        };

        for (Direction direction : priority) {
            BlockPos neighbor = pos.offset(direction);
            BlockState state = mc.world.getBlockState(neighbor);
            if (!state.isAir() && state.isSolidBlock(mc.world, neighbor)) {
                return direction;
            }
        }

        return null;
    }

    private int findCrystalSlot() {
        if (mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL) {
            return 40;
        }

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof EndCrystalItem) {
                return i;
            }
        }
        return -1;
    }

    private int findObsidianSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.OBSIDIAN) {
                return i;
            }
        }
        return -1;
    }

    private Vec3d getCrystalVec(BlockPos pos) {
        return new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
    }

    private Vec3d getPlaceHitVec(BlockPos pos, Direction face) {
        BlockPos neighbor = pos.offset(face);
        Direction clickFace = face.getOpposite();
        return new Vec3d(
                neighbor.getX() + 0.5 + clickFace.getOffsetX() * 0.5,
                neighbor.getY() + 0.5 + clickFace.getOffsetY() * 0.5,
                neighbor.getZ() + 0.5 + clickFace.getOffsetZ() * 0.5
        );
    }

    private boolean isActionChanged(Action previous, Action next) {
        if (previous == null || next == null) return previous != next;
        if (previous.type() != next.type()) return true;
        if (previous.pos() != null ? !previous.pos().equals(next.pos()) : next.pos() != null) return true;
        if (previous.face() != next.face()) return true;
        if (previous.crystal() == null || next.crystal() == null) return previous.crystal() != next.crystal();
        return previous.crystal().getId() != next.crystal().getId();
    }

    private void resetState() {
        currentAction = null;
        abortMining();
        obsidianCooldown = 0;
        lastPlacedObsidianPos = null;
        snap.reset();
        Client.ROTATION.reset();
    }

    private void render(Event3D event) {
        if (!renderActive.get() || currentAction == null || mc.player == null) return;

        MatrixStack matrices = event.stack;
        if (matrices == null) return;

        BlockPos targetPos = currentAction.pos();
        if (targetPos == null && currentAction.crystal() != null) {
            targetPos = currentAction.crystal().getBlockPos();
        }

        if (targetPos != null) {
            Color fillCol;
            Color outCol;

            switch (currentAction.type()) {
                case PLACE_OBSIDIAN -> {
                    fillCol = new Color(130, 0, 255, 45);
                    outCol = new Color(130, 0, 255, 230);
                }
                case BREAK_BLOCK -> {
                    fillCol = new Color(255, 50, 0, 45);
                    outCol = new Color(255, 50, 0, 230);
                }
                case PLACE_CRYSTAL -> {
                    fillCol = new Color(0, 255, 220, 45);
                    outCol = new Color(0, 255, 220, 230);
                }
                default -> {
                    fillCol = new Color(0, 200, 255, 30);
                    outCol = new Color(0, 200, 255, 200);
                }
            }

            drawBox(matrices, targetPos, fillCol, outCol, renderFill.get(), renderOutline.get());
        }
    }

    private void drawBox(MatrixStack matrices, BlockPos pos, Color fillCol, Color outCol, boolean fill, boolean outline) {
        Vec3d camera = mc.getEntityRenderDispatcher().camera.getCameraPos();
        double x = pos.getX() - camera.x;
        double y = pos.getY() - camera.y;
        double z = pos.getZ() - camera.z;

        VertexConsumerProvider.Immediate provider = mc.getBufferBuilders().getEntityVertexConsumers();

        matrices.push();
        matrices.translate(x, y, z);

        if (fill) {
            VertexConsumer consumer = provider.getBuffer(ClientPipelines.FILL);
            drawSolidBox(matrices, consumer, 0, 0, 0, 1, 1, 1, fillCol);
        }
        if (outline) {
            VertexConsumer consumer = provider.getBuffer(ClientPipelines.OUTLINE);
            drawOutlineBox(matrices, consumer, 0, 0, 0, 1, 1, 1, outCol);
        }

        provider.draw();
        matrices.pop();
    }

    private void drawSolidBox(MatrixStack matrices, VertexConsumer consumer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Color color) {
        MatrixStack.Entry entry = matrices.peek();
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();

        vertex(entry, consumer, minX, minY, minZ, r, g, b, a);
        vertex(entry, consumer, maxX, minY, minZ, r, g, b, a);
        vertex(entry, consumer, maxX, minY, maxZ, r, g, b, a);
        vertex(entry, consumer, minX, minY, maxZ, r, g, b, a);

        vertex(entry, consumer, minX, maxY, minZ, r, g, b, a);
        vertex(entry, consumer, minX, maxY, maxZ, r, g, b, a);
        vertex(entry, consumer, maxX, maxY, maxZ, r, g, b, a);
        vertex(entry, consumer, maxX, maxY, minZ, r, g, b, a);

        vertex(entry, consumer, minX, minY, minZ, r, g, b, a);
        vertex(entry, consumer, minX, maxY, minZ, r, g, b, a);
        vertex(entry, consumer, maxX, maxY, minZ, r, g, b, a);
        vertex(entry, consumer, maxX, minY, minZ, r, g, b, a);

        vertex(entry, consumer, minX, minY, maxZ, r, g, b, a);
        vertex(entry, consumer, maxX, minY, maxZ, r, g, b, a);
        vertex(entry, consumer, maxX, maxY, maxZ, r, g, b, a);
        vertex(entry, consumer, minX, maxY, maxZ, r, g, b, a);

        vertex(entry, consumer, minX, minY, minZ, r, g, b, a);
        vertex(entry, consumer, minX, minY, maxZ, r, g, b, a);
        vertex(entry, consumer, minX, maxY, maxZ, r, g, b, a);
        vertex(entry, consumer, minX, maxY, minZ, r, g, b, a);

        vertex(entry, consumer, maxX, minY, minZ, r, g, b, a);
        vertex(entry, consumer, maxX, maxY, minZ, r, g, b, a);
        vertex(entry, consumer, maxX, maxY, maxZ, r, g, b, a);
        vertex(entry, consumer, maxX, minY, maxZ, r, g, b, a);
    }

    private void drawOutlineBox(MatrixStack matrices, VertexConsumer consumer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Color color) {
        MatrixStack.Entry entry = matrices.peek();
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();

        line(entry, consumer, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        line(entry, consumer, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        line(entry, consumer, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        line(entry, consumer, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

        line(entry, consumer, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(entry, consumer, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        line(entry, consumer, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        line(entry, consumer, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

        line(entry, consumer, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        line(entry, consumer, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(entry, consumer, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        line(entry, consumer, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    private void vertex(MatrixStack.Entry entry, VertexConsumer consumer, double x, double y, double z, int r, int g, int b, int a) {
        consumer.vertex(entry, (float) x, (float) y, (float) z)
                .color(r, g, b, a)
                .normal(entry, 0f, 1f, 0f);
    }

    private void line(MatrixStack.Entry entry, VertexConsumer consumer, double x1, double y1, double z1, double x2, double y2, double z2, int r, int g, int b, int a) {
        vertex(entry, consumer, x1, y1, z1, r, g, b, a);
        vertex(entry, consumer, x2, y2, z2, r, g, b, a);
    }

    private enum ActionType {
        PLACE_CRYSTAL,
        BREAK,
        PLACE_OBSIDIAN,
        BREAK_BLOCK
    }

    private record Action(
            ActionType type,
            BlockPos pos,
            EndCrystalEntity crystal,
            Direction face,
            Vec3d lookVec,
            float targetDamage,
            float selfDamage
    ) {}
}