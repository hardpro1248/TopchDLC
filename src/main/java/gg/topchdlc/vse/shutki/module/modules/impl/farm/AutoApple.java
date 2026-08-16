package gg.topchdlc.vse.shutki.module.modules.impl.farm;

import gg.topchdlc.vse.shutki.module.modules.impl.player.AutoInvis;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.player.swap.InventoryUtility;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.MovementCorrection;
import gg.topchdlc.vse.rotation.all.Rotation;
import gg.topchdlc.vse.utils.math.Priorities;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import gg.topchdlc.vse.utils.math.RotationUtility;
import gg.topchdlc.vse.utils.math.TimeUtility;
import gg.topchdlc.vse.utils.network.NetworkUtility;

import java.util.ArrayList;
import java.util.List;

/**
 * Create by daun kvass
 */
public class AutoApple extends Module {
    public static final AutoApple INSTANCE = new AutoApple();
    public enum GrowMode {
        BONE_MEAL("Костная мука"),
        HOE_RMB("Мотыга ПКМ");
        private final String display;
        GrowMode(String d) { this.display = d; }
        @Override public String toString() { return display; }
    }
    private enum State {
        CHECK_ITEMS, PLACE_SAPLING, GROW, BREAK,
        REFILL_BONE_MEAL, REPAIR_HOE
    }
    private final EnumSetting<GrowMode> growMode = enumSetting("Режим роста", GrowMode.BONE_MEAL);
    private  final SliderSetting stripss = sliderSetting("SPeed",8,1,20).increment(1);


    private static final int BREAK_H = 6;
    private static final int BREAK_V = 6;
    private static final int PLACE_RANGE = 6;
    private final CheckBox repairHoe =
            checkbox("Тест", false)
                    .visible(() -> growMode.is(GrowMode.HOE_RMB));
    private State state = State.CHECK_ITEMS;
    private final TimeUtility timer = new TimeUtility();
    private BlockPos saplingPos = null;
    private final List<BlockPos> treeBlocks = new ArrayList<>();
    private BlockPos breakTarget = null;
    private BlockPos lastAttackTarget = null;
    private BlockPos chestPos = null;
    private Angle currentAimTarget = null;
    private boolean repairing = false;
    private int repairXpSlot = -1;
    private int repairHoeSlot = -1;
    private final TimeUtility repairTimer = new TimeUtility();

    private AutoApple() {
        super("AutoApple", Category.PLAYER, "само вырашивает деревья и вскапывает их ради яблок");
    }

    @Override
    protected void onEnable() {
        state = State.CHECK_ITEMS;
        saplingPos = null; treeBlocks.clear(); breakTarget = null; lastAttackTarget = null; chestPos = null;
        repairing = false; currentAimTarget = null;
        if (mc.player != null) {
            List<String> missing = getMissingItems();
            if (!missing.isEmpty())
                ChatUtility.send("Не хватает: §f" + String.join("§c, §f", missing));
        }
    }

    @Override
    protected void onDisable() {
        saplingPos = null; treeBlocks.clear(); breakTarget = null; lastAttackTarget = null; chestPos = null;
        repairing = false; currentAimTarget = null;
        if (mc.options != null) mc.options.attackKey.setPressed(false);
    }

    EventBus<Event> bus = event -> { if (event instanceof EventGameTick) onTick(); };
    private  final Rotation SMOOTH = (cur, tgt) -> {
        float step = stripss.get();
        float dy = MathHelper.clamp(MathHelper.wrapDegrees(tgt.getYaw() - cur.getYaw()), -step, step);
        float dp = MathHelper.clamp(tgt.getPitch() - cur.getPitch(), -step, step);
        return new Angle(cur.getYaw() + dy, MathHelper.clamp(cur.getPitch() + dp, -90f, 90f));
    };

    private void smoothLook(Vec3d worldPos) {
        Angle target = RotationUtility.calculate(worldPos);
        currentAimTarget = target;
        Client.ROTATION.rotate(SMOOTH, target, 2, true, MovementCorrection.SILENT, Priorities.NORMAL);
    }

    private void clearAim() { currentAimTarget = null; }

    private void onTick() {
        if (nullCheck()) return;
        if (AutoInvis.INSTANCE.isDrinking) return;
        if (growMode.is(GrowMode.HOE_RMB) && repairHoe.get() && state != State.REPAIR_HOE) {
            if (shouldRepairHoe()) {
                state = State.REPAIR_HOE;
                repairing = true;
                repairTimer.reset();
                return;
            }
        }

        if (state == State.REPAIR_HOE) {
            if (chestPos != null && repairStep == RepairStep.OPEN_CHEST
                    && !(mc.currentScreen instanceof GenericContainerScreen)) {
                smoothLook(Vec3d.ofCenter(chestPos));
                Angle t = RotationUtility.calculate(Vec3d.ofCenter(chestPos));
                Angle c = Client.ROTATION.getRotate();
                if (Math.abs(MathHelper.wrapDegrees(t.getYaw() - c.getYaw())) < 8f
                        && Math.abs(t.getPitch() - c.getPitch()) < 8f) {
                    Vec3d hv = Vec3d.ofCenter(chestPos);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                            new BlockHitResult(hv, Direction.UP, chestPos, false));
                }
            }
            tickRepairHoe();
            return;
        }
        if (state == State.BREAK) { breakTree(); return; }
        if (state == State.GROW && saplingPos != null) {
            smoothLook(Vec3d.ofCenter(saplingPos).add(0, 0.5, 0));
        }
        if (state == State.REFILL_BONE_MEAL && chestPos != null && !(mc.currentScreen instanceof GenericContainerScreen)) {
            smoothLook(Vec3d.ofCenter(chestPos));
        }

        if (!timer.reached(200, true)) return;
        switch (state) {
            case CHECK_ITEMS      -> checkItems();
            case PLACE_SAPLING    -> placeSapling();
            case GROW             -> growSapling();
            case REFILL_BONE_MEAL -> refillBoneMeal();
        }
    }
    private void breakTree() {
        BlockPos playerPos = mc.player.getBlockPos();
        Vec3d playerVec = mc.player.getEntityPos();

        treeBlocks.clear();
        for (int dx = -BREAK_H; dx <= BREAK_H; dx++)
            for (int dy = -BREAK_V; dy <= BREAK_V; dy++)
                for (int dz = -BREAK_H; dz <= BREAK_H; dz++) {
                    BlockPos pos = playerPos.add(dx, dy, dz);
                    Block b = mc.world.getBlockState(pos).getBlock();
                    if (isOakLog(b) || b instanceof LeavesBlock) treeBlocks.add(pos);
                }

        if (treeBlocks.isEmpty()) {
            mc.options.attackKey.setPressed(false);
            clearAim();
            state = State.CHECK_ITEMS; saplingPos = null; breakTarget = null;
            return;
        }
        treeBlocks.sort((a, b) -> Double.compare(
            Vec3d.ofCenter(a).squaredDistanceTo(playerVec),
            Vec3d.ofCenter(b).squaredDistanceTo(playerVec)
        ));

        if (breakTarget != null) {
            Block cur = mc.world.getBlockState(breakTarget).getBlock();
            if (!isOakLog(cur) && !(cur instanceof LeavesBlock)) {
                breakTarget = null;
                lastAttackTarget = null;
            }
        }
        if (breakTarget == null) breakTarget = treeBlocks.get(0);

        Block targetBlock = mc.world.getBlockState(breakTarget).getBlock();
        if (!isOakLog(targetBlock) && !(targetBlock instanceof LeavesBlock)) {
            breakTarget = null; return;
        }

        boolean isLog = isOakLog(targetBlock);
        int toolSlot = isLog ? findAxeSlot() : findHoeSlot();
        if (toolSlot == -1) { state = State.CHECK_ITEMS; return; }

        int currentSlot = mc.player.getInventory().getSelectedSlot();
        Item held = mc.player.getInventory().getStack(currentSlot).getItem();
        boolean wrongTool = isLog ? !(held instanceof AxeItem) : !(held instanceof HoeItem);
        if (wrongTool || currentSlot != toolSlot) {
            mc.options.attackKey.setPressed(false);
            mc.player.getInventory().setSelectedSlot(toolSlot);
            NetworkUtility.send(new UpdateSelectedSlotC2SPacket(toolSlot));
            return;
        }

        smoothLook(Vec3d.ofCenter(breakTarget));
        if (!breakTarget.equals(lastAttackTarget)) {
            Direction face = getBreakFace(breakTarget);
            NetworkUtility.send(new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(
                    net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                    breakTarget, face));
            if (mc.player.getEyePos().distanceTo(Vec3d.ofCenter(breakTarget)) <= 6.0) {
                mc.interactionManager.attackBlock(breakTarget, face);
            }
            lastAttackTarget = breakTarget;
        }
        mc.options.attackKey.setPressed(true);
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

    private boolean shouldRepairHoe() {
        int slot = findHoeSlotAnywhere();
        if (slot == -1) return false;
        ItemStack hoe = mc.player.getInventory().getStack(slot);
        if (hoe.isEmpty() || !hoe.isDamageable()) return false;
        int maxDmg = hoe.getMaxDamage();
        if (maxDmg == 0) return false;
        int durability = maxDmg - hoe.getDamage();
        return durability <= (int)(maxDmg * 0.08f);
    }

    private enum RepairStep {
        FIND_XP,
        OPEN_CHEST,
        TAKE_XP_CHEST,
        CLOSE_CHEST,
        PITCH_DOWN,
        THROW_XP,
        DONE
    }
    private RepairStep repairStep = RepairStep.FIND_XP;

    private void tickRepairHoe() {
        if (!repairTimer.reached(200, true)) return;
        switch (repairStep) {

            case FIND_XP -> {
                if (InventoryUtility.find(Items.EXPERIENCE_BOTTLE) != -1) {
                    repairStep = RepairStep.PITCH_DOWN;
                    return;
                }
                BlockPos chest = findNearbyChest();
                if (chest == null) {
                    ChatUtility.send("§cНет опыта и сундука для починки мотыги!");
                    finishRepair();
                    return;
                }
                chestPos = chest;
                repairStep = RepairStep.OPEN_CHEST;
            }

            case OPEN_CHEST -> {
                if (mc.currentScreen instanceof GenericContainerScreen) {
                    repairStep = RepairStep.TAKE_XP_CHEST;
                }
            }

            case TAKE_XP_CHEST -> {
                if (!(mc.currentScreen instanceof GenericContainerScreen)) {
                    repairStep = RepairStep.OPEN_CHEST;
                    return;
                }
                ScreenHandler handler = mc.player.currentScreenHandler;
                int containerSlots = handler.slots.size() - 36;
                for (int i = 0; i < containerSlots; i++) {
                    ItemStack s = handler.slots.get(i).getStack();
                    if (!s.isEmpty() && s.getItem() == Items.EXPERIENCE_BOTTLE) {
                        mc.interactionManager.clickSlot(handler.syncId, i, 40, SlotActionType.SWAP, mc.player);
                        repairStep = RepairStep.CLOSE_CHEST;
                        return;
                    }
                }
                mc.player.closeHandledScreen();
                ChatUtility.send("§cВ сундуке нет опыта!");
                finishRepair();
            }

            case CLOSE_CHEST -> {
                mc.player.closeHandledScreen();
                repairStep = RepairStep.PITCH_DOWN;
            }

            case PITCH_DOWN -> {
                smoothLook(mc.player.getEntityPos().add(0, -10, 0));
                repairStep = RepairStep.THROW_XP;
            }

            case THROW_XP -> {
                int xpSlot = InventoryUtility.find(Items.EXPERIENCE_BOTTLE);
                if (xpSlot == -1) {
                    ItemStack offhand = mc.player.getOffHandStack();
                    if (!offhand.isEmpty() && offhand.getItem() == Items.EXPERIENCE_BOTTLE) {
                        smoothLook(mc.player.getEntityPos().add(0, -10, 0));
                        mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
                        if (!shouldRepairHoe()) repairStep = RepairStep.DONE;
                        return;
                    }
                    repairStep = RepairStep.FIND_XP;
                    return;
                }
                if (xpSlot >= 9) {
                    int free = findFreeHotbarSlot();
                    if (free == -1) { finishRepair(); return; }
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId,
                            xpSlot, free, SlotActionType.SWAP, mc.player);
                    return;
                }
                smoothLook(mc.player.getEntityPos().add(0, -10, 0));
                mc.player.getInventory().setSelectedSlot(xpSlot);
                NetworkUtility.send(new UpdateSelectedSlotC2SPacket(xpSlot));
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                if (!shouldRepairHoe()) repairStep = RepairStep.DONE;
            }

            case DONE -> finishRepair();
        }
    }

    private void finishRepair() {
        repairing = false;
        repairStep = RepairStep.FIND_XP;
        state = State.CHECK_ITEMS;
    }
    private List<String> getMissingItems() {
        List<String> missing = new ArrayList<>();
        if (InventoryUtility.findHotbar(Items.OAK_SAPLING) == -1) missing.add("Саженец дуба");
        if (growMode.is(GrowMode.BONE_MEAL)) {
            if (InventoryUtility.findHotbar(Items.BONE_MEAL) == -1 && InventoryUtility.find(Items.BONE_MEAL) == -1)
                missing.add("Костная мука");
        }
        if (findAxeSlot() == -1) missing.add("Топор");
        if (findHoeSlot() == -1) missing.add("Мотыга");
        return missing;
    }

    private void checkItems() {
        if (growMode.is(GrowMode.BONE_MEAL)) {
            if (InventoryUtility.findHotbar(Items.BONE_MEAL) == -1 && InventoryUtility.find(Items.BONE_MEAL) == -1) {
                BlockPos chest = findNearbyChest();
                if (chest != null) { chestPos = chest; state = State.REFILL_BONE_MEAL; return; }
            }
        }
        List<String> missing = getMissingItems();
        if (!missing.isEmpty()) { ChatUtility.send("Не хватает: §f" + String.join("§c, §f", missing)); return; }
        state = State.PLACE_SAPLING;
    }

    private void placeSapling() {
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos foundSapling = null, foundLog = null;
        double bestSapDist = Double.MAX_VALUE, bestLogDist = Double.MAX_VALUE;

        for (int dx = -PLACE_RANGE; dx <= PLACE_RANGE; dx++) {
            for (int dy = -2; dy <= 18; dy++) {
                for (int dz = -PLACE_RANGE; dz <= PLACE_RANGE; dz++) {
                    BlockPos check = playerPos.add(dx, dy, dz);
                    Block b = mc.world.getBlockState(check).getBlock();
                    double dist = check.getSquaredDistance(playerPos);
                    if (b == Blocks.OAK_SAPLING && dist < bestSapDist) { bestSapDist = dist; foundSapling = check; }
                    if (isOakLog(b) && dist < bestLogDist) { bestLogDist = dist; foundLog = check; }
                }
            }
        }

        if (foundLog != null) { saplingPos = foundLog; state = State.BREAK; return; }
        if (foundSapling != null) { saplingPos = foundSapling; state = State.GROW; return; }
        BlockPos bestPos = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -PLACE_RANGE; dx <= PLACE_RANGE; dx++) {
            for (int dz = -PLACE_RANGE; dz <= PLACE_RANGE; dz++) {
                for (int dy = -3; dy <= 3; dy++) {
                    BlockPos candidate = playerPos.add(dx, dy, dz);
                    if (!mc.world.getBlockState(candidate).isAir()) continue;
                    if (!isSoilBlock(mc.world.getBlockState(candidate.down()).getBlock())) continue;
                    double dist = candidate.getSquaredDistance(playerPos);
                    if (dist < bestDist) { bestDist = dist; bestPos = candidate; }
                }
            }
        }

        if (bestPos == null) { ChatUtility.sendDebug("Нет земли, жду..."); return; }
        int saplingSlot = InventoryUtility.findHotbar(Items.OAK_SAPLING);
        if (saplingSlot == -1) { state = State.CHECK_ITEMS; return; }

        Vec3d hitVec = Vec3d.ofCenter(bestPos.down()).add(0, 0.5, 0);
        smoothLook(hitVec);
        mc.player.getInventory().setSelectedSlot(saplingSlot);
        NetworkUtility.send(new UpdateSelectedSlotC2SPacket(saplingSlot));
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                new BlockHitResult(hitVec, Direction.UP, bestPos.down(), false));
        saplingPos = bestPos;
        state = State.GROW;
    }

    private void growSapling() {
        if (saplingPos == null || mc.world.getBlockState(saplingPos).getBlock() != Blocks.OAK_SAPLING) {
            if (saplingPos != null && isOakLog(mc.world.getBlockState(saplingPos).getBlock())) {
                clearAim(); state = State.BREAK; return;
            }
            BlockPos found = null;
            double bestDist = Double.MAX_VALUE;
            BlockPos playerPos = mc.player.getBlockPos();
            for (int dx = -PLACE_RANGE; dx <= PLACE_RANGE; dx++)
                for (int dy = -2; dy <= 18; dy++)
                    for (int dz = -PLACE_RANGE; dz <= PLACE_RANGE; dz++) {
                        BlockPos check = playerPos.add(dx, dy, dz);
                        if (mc.world.getBlockState(check).getBlock() == Blocks.OAK_SAPLING) {
                            double dist = check.getSquaredDistance(playerPos);
                            if (dist < bestDist) { bestDist = dist; found = check; }
                        }
                        if (isOakLog(mc.world.getBlockState(check).getBlock())) {
                            saplingPos = check; clearAim(); state = State.BREAK; return;
                        }
                    }
            if (found != null) saplingPos = found;
            else { state = State.PLACE_SAPLING; return; }
        }

        Vec3d hitVec = Vec3d.ofCenter(saplingPos).add(0, 0.5, 0);

        if (growMode.is(GrowMode.BONE_MEAL)) {
            int boneMealSlot = InventoryUtility.findHotbar(Items.BONE_MEAL);
            if (boneMealSlot == -1) {
                int invSlot = InventoryUtility.find(Items.BONE_MEAL);
                if (invSlot == -1) { state = State.CHECK_ITEMS; return; }
                int freeHotbar = findFreeHotbarSlot();
                if (freeHotbar == -1) { state = State.CHECK_ITEMS; return; }
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId,
                        invSlot, freeHotbar, SlotActionType.SWAP, mc.player);
                boneMealSlot = freeHotbar;
            }
            mc.player.getInventory().setSelectedSlot(boneMealSlot);
            NetworkUtility.send(new UpdateSelectedSlotC2SPacket(boneMealSlot));
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                    new BlockHitResult(hitVec, Direction.UP, saplingPos, false));
        } else {
            int hoeSlot = findHoeSlot();
            if (hoeSlot == -1) { state = State.CHECK_ITEMS; return; }
            mc.player.getInventory().setSelectedSlot(hoeSlot);
            NetworkUtility.send(new UpdateSelectedSlotC2SPacket(hoeSlot));
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                    new BlockHitResult(hitVec, Direction.UP, saplingPos, false));
        }
    }
    private void refillBoneMeal() {
        if (chestPos == null) { state = State.CHECK_ITEMS; return; }
        if (mc.currentScreen instanceof GenericContainerScreen) {
            ScreenHandler handler = mc.player.currentScreenHandler;
            int containerSlots = handler.slots.size() - 36;
            for (int i = 0; i < containerSlots; i++) {
                ItemStack stack = handler.slots.get(i).getStack();
                if (!stack.isEmpty() && stack.getItem() == Items.BONE_MEAL) {
                    mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                    return;
                }
            }
            mc.player.closeHandledScreen();
            chestPos = null;
            state = State.CHECK_ITEMS;
            return;
        }
        openChest(chestPos);
    }

    private void openChest(BlockPos pos) {
        Vec3d hitVec = Vec3d.ofCenter(pos);
        Angle target = RotationUtility.calculate(hitVec);
        Angle cur = Client.ROTATION.getRotate();
        float dyaw = Math.abs(MathHelper.wrapDegrees(target.getYaw() - cur.getYaw()));
        float dpitch = Math.abs(target.getPitch() - cur.getPitch());
        if (dyaw < 15f && dpitch < 15f) {
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                    new BlockHitResult(hitVec, Direction.UP, pos, false));
        }
    }

    private BlockPos findNearbyChest() {
        BlockPos playerPos = mc.player.getBlockPos();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos pos = new BlockPos(playerPos.getX() + dx, playerPos.getY(), playerPos.getZ() + dz);
                Block b = mc.world.getBlockState(pos).getBlock();
                if (b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST || b == Blocks.BARREL) return pos;
            }
        }
        return null;
    }
    private boolean isOakLog(Block block) {
        return block == Blocks.OAK_LOG || block == Blocks.STRIPPED_OAK_LOG;
    }

    private boolean isSoilBlock(Block block) {
        return block == Blocks.GRASS_BLOCK || block == Blocks.DIRT
                || block == Blocks.COARSE_DIRT || block == Blocks.ROOTED_DIRT
                || block == Blocks.PODZOL      || block == Blocks.MYCELIUM;
    }
    private int findAxeSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (!s.isEmpty() && s.getItem() instanceof AxeItem) return i;
        }
        return -1;
    }

    private int findHoeSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (!s.isEmpty() && s.getItem() instanceof HoeItem) return i;
        }
        return -1;
    }

    private int findHoeSlotAnywhere() {
        for (int i = 0; i < 36; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (!s.isEmpty() && s.getItem() instanceof HoeItem) return i;
        }
        return -1;
    }

    private int findFreeHotbarSlot() {
        int axe = findAxeSlot(), hoe = findHoeSlot(), sap = InventoryUtility.findHotbar(Items.OAK_SAPLING);
        for (int i = 0; i < 9; i++) {
            if (i == axe || i == hoe || i == sap) continue;
            if (mc.player.getInventory().getStack(i).isEmpty()) return i;
        }
        for (int i = 0; i < 9; i++) {
            if (i == axe || i == hoe || i == sap) continue;
            return i;
        }
        return -1;
    }
}
