package gg.topchdlc.vse.shutki.module.modules.impl.combat;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
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
import gg.topchdlc.api.events.list.EventInput;
import gg.topchdlc.api.events.list.EventKey;
import gg.topchdlc.vse.utils.player.MoveUtility;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.keybind.KeybindSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.math.RotationUtility;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.MovementCorrection;
import gg.topchdlc.vse.rotation.all.impl.DefaultRotation;
import gg.topchdlc.vse.utils.network.NetworkUtility;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
/**
 * Create by daun kvass
 */
public class CrystalAuto extends Module {
    public static final CrystalAuto INSTANCE = new CrystalAuto();

    private CrystalAuto() {
        super("CrystalAuto", Category.COMBAT, "Кристалл хелпер g по бинду x");
    }
    private final SliderSetting targetRange = sliderSetting("Range ", 10f, 3f, 15f).increment(0.5f);
    private final CheckBox ignoreFriends = checkbox("Ignore friend", true);
    private final CheckBox rotate = checkbox("Rotate", true);
    private final EnumSetting<RotateMode> rotateMode = enumSetting("Mode Rotate", RotateMode.SILENT).visible(rotate::get);
    private final EnumSetting<SwapMode> swapMode = enumSetting("test (please use packet)", SwapMode.PACKET);
    private final CheckBox autoPlaceCrystal = checkbox("AutoExplosiv default", false);
    public enum SwapMode { PACKET, SILENT }
    private final CheckBox autoBreakEnabled = checkbox("Enable AutoBreak", true);
    private final SliderSetting breakRange = sliderSetting("Range Break", 4.5f, 1f, 6f).increment(0.1f).visible(autoBreakEnabled::get);
    private final SliderSetting breakDelay = sliderSetting("Break time", 0, 0, 500).visible(autoBreakEnabled::get);
    private final CheckBox breakOnlyOwn = checkbox("Only own", false).visible(autoBreakEnabled::get);
    private final CheckBox breakSwing = checkbox("Swing", true).visible(autoBreakEnabled::get);
    private final Group placeGroup = group("Place (Bind)");
    private final CheckBox placeEnabled = placeGroup.checkbox("On", false);
    private final KeybindSetting placeBind = placeGroup.keybindSetting("Bind", -1);
    private final EnumSetting<PlaceBindMode> placeBindMode = placeGroup.enumSetting("Bind use?", PlaceBindMode.TOGGLE);
    public enum PlaceBindMode { TOGGLE, HOLD }
    private boolean holdActive = false;
    private final CheckBox placeObsidian = placeGroup.checkbox("Place obsidian?", true);
    private final CheckBox placeCrystal = placeGroup.checkbox("Place crystal?", true);
    private final CheckBox placeAutoBreak = placeGroup.checkbox("Auto explosiv after", true);
    private final SliderSetting placeRange = placeGroup.sliderSetting("Place Range", 4.5f, 1f, 6f).increment(0.1f);
    private final CheckBox placeToTarget = placeGroup.checkbox("[TEST] To target", false);
    private final SliderSetting minTargetDmg = placeGroup.sliderSetting("Min.damage target", 4f, 0f, 20f).increment(0.5f).visible(placeToTarget::get);
    private final CheckBox obsidianCheckCrystal = placeGroup.checkbox("Crystal space check", true).visible(placeObsidian::get);
    private final CheckBox obsidianOnlyGround = placeGroup.checkbox("Obsidian to the grass", true).visible(placeObsidian::get);
    public enum RotateMode { SILENT, PACKET }
    private Float packetRotateYaw = null;
    private final List<PlacedCrystal> trackedCrystals = new ArrayList<>();
    private long lastBreakTime = 0;
    private EndCrystalEntity pendingBreakCrystal = null;
    private boolean waitingBreakRotation = false;
    private int savedSlot = -1;
    private int swappedToSlot = -1;
    private boolean isSwapped = false;
    private final List<PendingObsidian> pendingObsidians = new ArrayList<>();

    @Override
    protected void onEnable() {
        super.onEnable();
        reset();
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        restoreSlot();
        reset();
        holdActive = false;
    }

    private void reset() {
        trackedCrystals.clear();
        pendingBreakCrystal = null;
        waitingBreakRotation = false;
        savedSlot = -1;
        swappedToSlot = -1;
        isSwapped = false;
        pendingObsidians.clear();
        packetRotateYaw = null;
    }
    public void onObsidianPlaced(BlockPos pos) {
        if (!isEnabled() || !autoPlaceCrystal.get()) return;
        if (findCrystalSlot() == -1) return;
        pendingObsidians.clear();
        pendingObsidians.add(new PendingObsidian(pos.toImmutable(), System.currentTimeMillis(), false));
    }

    public void onCrystalPlace(BlockPos pos) {
        trackedCrystals.add(new PlacedCrystal(pos.toImmutable(), System.currentTimeMillis()));
    }

    public List<PlacedCrystal> getTrackedCrystals() {
        return trackedCrystals;
    }

    EventBus<Event> events = event -> {
        if (event instanceof EventKey e) onKey(e);
        if (event instanceof EventGameTick) onTick();
        if (event instanceof EventInput e && rotateMode.is(RotateMode.PACKET) && packetRotateYaw != null) {
            MoveUtility.silentCorrection(e, packetRotateYaw);
        }
    };
    public boolean isSafeToExplode(Vec3d crystalPos) {


        float selfDmg = estimateDamage(crystalPos, mc.player);
        if (selfDmg <= 0) return true;

        PlayerEntity target = findTarget();
        if (target == null) {
            return selfDmg <= 0.5f;
        }

        float targetDmg = estimateDamage(crystalPos, target);
        return selfDmg < targetDmg;
    }


    private void onKey(EventKey e) {
        if (!placeEnabled.get()) return;
        if (placeBind.getBind() == -1) return;
        if (e.key != placeBind.getBind()) return;
        if (mc.player == null || mc.world == null) return;

        if (placeBindMode.is(PlaceBindMode.HOLD)) {
            if (e.action == 1) holdActive = true;
            else if (e.action == 0) holdActive = false;
        } else {
            if (e.action != 1) return;
            executeInstant();
        }
    }

    public void swapTo(int slot) {
        if (slot < 0 || slot > 8) return;
        if (!isSwapped) {
            savedSlot = mc.player.getInventory().getSelectedSlot();
        }
        switch (swapMode.get()) {
            case PACKET -> mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            case SILENT -> {
                mc.player.getInventory().setSelectedSlot(slot);
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            }
        }
        swappedToSlot = slot;
        isSwapped = true;
    }

    public void restoreSlot() {
        if (!isSwapped || savedSlot == -1) return;
        switch (swapMode.get()) {
            case PACKET -> mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(savedSlot));
            case SILENT -> {
                mc.player.getInventory().setSelectedSlot(savedSlot);
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(savedSlot));
            }
        }
        isSwapped = false;
        savedSlot = -1;
        swappedToSlot = -1;
    }

    public int getActiveServerSlot() {
        if (isSwapped && swappedToSlot != -1) return swappedToSlot;
        return mc.player.getInventory().getSelectedSlot();
    }

    private void executeInstant() {
        BlockPos crystalPos = null;
        if (placeCrystal.get()) {
            crystalPos = findBestCrystalPos();
        }

        if (crystalPos == null && placeObsidian.get() && findObsidianSlot() != -1) {
            ObsidianPlacement obsPl = findBestObsidianPos();
            if (obsPl != null) {
                if (rotateMode.is(RotateMode.PACKET)) {
                    Vec3d hitVec = getPlaceHitVec(obsPl.pos, obsPl.face);
                    packetRotateYaw = RotationUtility.calculate(hitVec).getYaw();
                    doPlaceObsidianRaw(obsPl.pos, obsPl.face);
                } else {
                    if (rotate.get()) {
                        rotateTo(getPlaceHitVec(obsPl.pos, obsPl.face));
                    }
                    doPlaceObsidianPacket(obsPl.pos, obsPl.face);
                }
                if (autoPlaceCrystal.get() && placeCrystal.get()) {
                    pendingObsidians.clear();
                    pendingObsidians.add(new PendingObsidian(obsPl.pos.toImmutable(), System.currentTimeMillis(), false));
                }
            }
            return;
        }

        if (crystalPos == null) return;

        if (placeCrystal.get() && findCrystalSlot() != -1) {
            if (rotateMode.is(RotateMode.PACKET)) {
                Vec3d placeVec = new Vec3d(crystalPos.getX() + 0.5, crystalPos.getY() + 1.0, crystalPos.getZ() + 0.5);
                packetRotateYaw = RotationUtility.calculate(placeVec).getYaw();
                doPlaceCrystalRaw(crystalPos);
            } else {
                if (rotate.get()) {
                    Vec3d placeVec = new Vec3d(crystalPos.getX() + 0.5, crystalPos.getY() + 1.0, crystalPos.getZ() + 0.5);
                    rotateTo(placeVec);
                }
                doPlaceCrystalPacket(crystalPos);
            }
            if (placeAutoBreak.get()) {
                EndCrystalEntity crystal = findCrystalNear(crystalPos, 3.0);
                if (crystal != null) {
                    if (!isSafeToExplode(crystal.getEntityPos())) return;
                    doBreakCrystal(crystal);
                }
            }
        }
    }

    private void onTick() {
        if (mc.player == null || mc.world == null) return;
        long now = System.currentTimeMillis();

        if (rotateMode.is(RotateMode.PACKET)) packetRotateYaw = null;

        trackedCrystals.removeIf(pc -> now - pc.time > 5000);
        if (!pendingObsidians.isEmpty()) {
            PendingObsidian pending = pendingObsidians.get(pendingObsidians.size() - 1);
            pendingObsidians.clear();

            boolean tooOld = now - pending.time > 2000;
            double dist = mc.player.getBlockPos().getSquaredDistance(pending.pos);
            boolean tooFar = dist > (placeRange.get() + 2) * (placeRange.get() + 2);

            if (!tooOld && !tooFar) {
                if (pending.waitingBreak) {
                    EndCrystalEntity crystal = findCrystalNear(pending.pos, 3.0, true);
                    if (crystal != null && isSafeToExplode(crystal.getEntityPos())) {
                        doBreakCrystal(crystal);
                    } else {
                        pendingObsidians.add(new PendingObsidian(pending.pos, pending.time, true));
                    }
                } else if (isObsidianOrBedrock(pending.pos) && canPlaceCrystalIfObsidian(pending.pos) && findCrystalSlot() != -1) {
                    if (rotateMode.is(RotateMode.PACKET)) {
                        Vec3d placeVec = new Vec3d(pending.pos.getX() + 0.5, pending.pos.getY() + 1.0, pending.pos.getZ() + 0.5);
                        packetRotateYaw = RotationUtility.calculate(placeVec).getYaw();
                        doPlaceCrystalRaw(pending.pos);
                    } else {
                        doPlaceCrystalPacket(pending.pos);
                    }
                    pendingObsidians.add(new PendingObsidian(pending.pos, now, true));
                } else if (!isObsidianOrBedrock(pending.pos)) {
                    pendingObsidians.add(pending);
                }
            }
        }

        if (placeEnabled.get() && placeBindMode.is(PlaceBindMode.HOLD) && holdActive) {
            executeInstant();
        }

        if (autoBreakEnabled.get()) {
            tickAutoBreak(now);
        }
    }
    private boolean tickAutoBreak(long now) {
        if (waitingBreakRotation && pendingBreakCrystal != null && !rotateMode.is(RotateMode.PACKET)) {
            if (!pendingBreakCrystal.isAlive()) {
                waitingBreakRotation = false;
                pendingBreakCrystal = null;
                return false;
            }
            if (!isSafeToExplode(pendingBreakCrystal.getEntityPos())) {
                waitingBreakRotation = false;
                pendingBreakCrystal = null;
                return false;
            }
            Vec3d center = getCrystalCenter(pendingBreakCrystal);
            if (isRotatedTo(center, 15f)) {
                doBreakCrystal(pendingBreakCrystal);
                waitingBreakRotation = false;
                pendingBreakCrystal = null;
                lastBreakTime = now;
                return true;
            }
            return true;
        }
        if (now - lastBreakTime < breakDelay.getInt()) return false;

        EndCrystalEntity best = findBestCrystal(breakRange.get(), breakOnlyOwn.get());
        if (best == null) return false;
        if (!isSafeToExplode(best.getEntityPos())) return false;

        if (rotate.get() && !rotateMode.is(RotateMode.PACKET)) {
            Vec3d center = getCrystalCenter(best);
            rotateTo(center);
            pendingBreakCrystal = best;
            waitingBreakRotation = true;
            return true;
        } else {
            doBreakCrystal(best);
            lastBreakTime = now;
            return true;
        }
    }

    public void doBreakCrystal(EndCrystalEntity crystal) {
        if (crystal == null || !crystal.isAlive()) return;
        Angle angle = RotationUtility.calculate(getCrystalCenter(crystal));
        mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround(
                angle.getYaw(), angle.getPitch(), mc.player.isOnGround(), mc.player.horizontalCollision));
        mc.getNetworkHandler().sendPacket(
                PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking())
        );
        if (breakSwing.get()) {
            mc.player.swingHand(Hand.MAIN_HAND);
        } else {
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
        PlacedCrystal matching = findMatchingPlaced(crystal);
        if (matching != null) {
            trackedCrystals.remove(matching);
        }
    }
    public void doPlaceCrystalRaw(BlockPos pos) {
        int crystalSlot = findCrystalSlot();
        if (crystalSlot == -1) return;

        boolean needSwap = false;
        Hand useHand;

        if (crystalSlot == 40) {
            useHand = Hand.OFF_HAND;
        } else if (crystalSlot == getActiveServerSlot()) {
            useHand = Hand.MAIN_HAND;
        } else {
            swapTo(crystalSlot);
            useHand = Hand.MAIN_HAND;
            needSwap = true;
        }

        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        NetworkUtility.sendAngle(RotationUtility.calculate(hitVec));
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, false);
        NetworkUtility.sendUse(useHand, hitResult);
        mc.player.swingHand(useHand);
        onCrystalPlace(pos);

        if (needSwap) restoreSlot();
    }

    public void doPlaceObsidianRaw(BlockPos pos, Direction face) {
        int obsSlot = findObsidianSlot();
        if (obsSlot == -1) return;
        boolean needSwap = obsSlot != getActiveServerSlot();
        if (needSwap) swapTo(obsSlot);

        BlockPos neighborPos = pos.offset(face);
        Direction clickFace = face.getOpposite();
        Vec3d hitVec = new Vec3d(
                neighborPos.getX() + 0.5 + clickFace.getOffsetX() * 0.5,
                neighborPos.getY() + 0.5 + clickFace.getOffsetY() * 0.5,
                neighborPos.getZ() + 0.5 + clickFace.getOffsetZ() * 0.5
        );
        NetworkUtility.sendAngle(RotationUtility.calculate(hitVec));
        BlockHitResult hitResult = new BlockHitResult(hitVec, clickFace, neighborPos, false);
        NetworkUtility.sendUse(Hand.MAIN_HAND, hitResult);
        mc.player.swingHand(Hand.MAIN_HAND);

        if (needSwap) restoreSlot();
    }

  public void doPlaceObsidianPacket(BlockPos pos, Direction face) {
        int obsSlot = findObsidianSlot();
        if (obsSlot == -1) return;
        boolean needSwap = obsSlot != getActiveServerSlot();
        if (needSwap) swapTo(obsSlot);

        BlockPos neighborPos = pos.offset(face);
        Direction clickFace = face.getOpposite();
        Vec3d hitVec = new Vec3d(
                neighborPos.getX() + 0.5 + clickFace.getOffsetX() * 0.5,
                neighborPos.getY() + 0.5 + clickFace.getOffsetY() * 0.5,
                neighborPos.getZ() + 0.5 + clickFace.getOffsetZ() * 0.5
        );
        BlockHitResult hitResult = new BlockHitResult(hitVec, clickFace, neighborPos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        mc.player.swingHand(Hand.MAIN_HAND);

        if (needSwap) restoreSlot();
    }

    public void doPlaceCrystalPacket(BlockPos pos) {
        int crystalSlot = findCrystalSlot();
        if (crystalSlot == -1) return;

        boolean needSwap = false;
        Hand useHand;

        if (crystalSlot == 40) {
            useHand = Hand.OFF_HAND;
        } else if (crystalSlot == getActiveServerSlot()) {
            useHand = Hand.MAIN_HAND;
        } else {
            swapTo(crystalSlot);
            useHand = Hand.MAIN_HAND;
            needSwap = true;
        }

        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, false);
        mc.interactionManager.interactBlock(mc.player, useHand, hitResult);
        mc.player.swingHand(useHand);
        onCrystalPlace(pos);

        if (needSwap) restoreSlot();
    }

    private BlockPos findBestCrystalPos() {
        if (placeToTarget.get()) {
            PlayerEntity target = findTarget();
            if (target != null) {
                BlockPos pos = findBestCrystalPosByDamage(target, placeRange.get(), minTargetDmg.get());
                if (pos != null) return pos;
            }
        }
        return findBestCrystalPosLook();
    }

    private ObsidianPlacement findBestObsidianPos() {
        if (placeToTarget.get()) {
            PlayerEntity target = findTarget();
            if (target != null) {
                ObsidianPlacement pl = findBestObsidianPosByDamage(target, placeRange.get(), minTargetDmg.get());
                if (pl != null) return pl;
            }
        }
        return findBestObsidianPosLook();
    }

    private BlockPos findBestCrystalPosLook() {
        Vec3d eyes = mc.player.getEyePos();
        Vec3d lookDir = mc.player.getRotationVecClient();
        List<BlockPos> candidates = new ArrayList<>();
        int r = (int) Math.ceil(placeRange.get());
        for (int x = -r; x <= r; x++)
            for (int y = -3; y <= 3; y++)
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = mc.player.getBlockPos().add(x, y, z);
                    if (!canPlaceCrystal(pos)) continue;
                    Vec3d posCenter = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                    if (eyes.distanceTo(posCenter) > placeRange.get()) continue;
                    if (!isSafeToExplode(posCenter)) continue;
                    candidates.add(pos);
                }
        if (candidates.isEmpty()) return null;
        candidates.sort(Comparator.comparingDouble(pos -> {
            Vec3d toBlock = new Vec3d(pos.getX() + 0.5 - eyes.x, pos.getY() + 1.0 - eyes.y, pos.getZ() + 0.5 - eyes.z).normalize();
            return -lookDir.dotProduct(toBlock);
        }));
        return candidates.get(0);
    }

    private ObsidianPlacement findBestObsidianPosLook() {
        Vec3d eyes = mc.player.getEyePos();
        Vec3d lookDir = mc.player.getRotationVecClient();
        List<ObsidianPlacement> candidates = new ArrayList<>();
        int r = (int) Math.ceil(placeRange.get());
        for (int x = -r; x <= r; x++)
            for (int y = -3; y <= 3; y++)
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = mc.player.getBlockPos().add(x, y, z);
                    ObsidianPlacement pl = checkObsidianCandidate(pos, null, placeRange.get());
                    if (pl != null) candidates.add(pl);
                }
        if (candidates.isEmpty()) return null;
        candidates.sort(Comparator.comparingDouble(p -> {
            Vec3d toBlock = Vec3d.ofCenter(p.pos).subtract(eyes).normalize();
            return -lookDir.dotProduct(toBlock);
        }));
        return candidates.get(0);
    }


    public BlockPos findBestCrystalPosByDamage(PlayerEntity target, float range, float minDmg) {
        Vec3d eyes = mc.player.getEyePos();
        List<ScoredPos> scored = new ArrayList<>();
        int r = (int) Math.ceil(range);
        for (int x = -r; x <= r; x++)
            for (int y = -3; y <= 3; y++)
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = mc.player.getBlockPos().add(x, y, z);
                    if (!canPlaceCrystal(pos)) continue;
                    Vec3d crystalPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                    if (eyes.distanceTo(crystalPos) > range) continue;
                    float targetDmg = estimateDamage(crystalPos, target);
                    float selfDmg = estimateDamage(crystalPos, mc.player);
                    if (targetDmg < minDmg) continue;
                  //  if (antiSuicide.get() && selfDmg >= targetDmg) continue;
                    scored.add(new ScoredPos(pos, targetDmg, selfDmg));
                }
        if (scored.isEmpty()) return null;
        scored.sort((a, b) -> {
            int cmp = Float.compare(b.targetDamage, a.targetDamage);
            if (cmp != 0) return cmp;
            return Float.compare(a.selfDamage, b.selfDamage);
        });
        return scored.get(0).pos;
    }

    public ObsidianPlacement findBestObsidianPosByDamage(PlayerEntity target, float range, float minDmg) {
        List<ScoredObsidian> scored = new ArrayList<>();
        int r = (int) Math.ceil(range);
        for (int x = -r; x <= r; x++)
            for (int y = -3; y <= 3; y++)
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = mc.player.getBlockPos().add(x, y, z);
                    ObsidianPlacement pl = checkObsidianCandidate(pos, target, range);
                    if (pl == null) continue;
                    Vec3d crystalPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                    float targetDmg = estimateDamage(crystalPos, target);
                    float selfDmg = estimateDamage(crystalPos, mc.player);
                    if (targetDmg < minDmg) continue;
                   // if ( selfDmg >= targetDmg) continue;
                    scored.add(new ScoredObsidian(pos, pl.face, targetDmg, selfDmg));
                }
        if (scored.isEmpty()) return null;
        scored.sort((a, b) -> {
            int cmp = Float.compare(b.targetDamage, a.targetDamage);
            if (cmp != 0) return cmp;
            return Float.compare(a.selfDamage, b.selfDamage);
        });
        ScoredObsidian best = scored.get(0);
        return new ObsidianPlacement(best.pos, best.face);
    }

    public ObsidianPlacement checkObsidianCandidate(BlockPos pos, PlayerEntity target, float range) {
        if (!mc.world.getBlockState(pos).isAir()) return null;
        Vec3d posCenter = Vec3d.ofCenter(pos);
        if (mc.player.getEyePos().distanceTo(posCenter) > range) return null;
        if (obsidianOnlyGround.get()) {
            BlockState below = mc.world.getBlockState(pos.down());
            if (!below.isSolidBlock(mc.world, pos.down())) return null;
        }
        Direction placeFace = findPlaceFace(pos);
        if (placeFace == null) return null;
        if (obsidianCheckCrystal.get()) {
            if (!mc.world.getBlockState(pos.up()).isAir()) return null;
            if (!mc.world.getBlockState(pos.up(2)).isAir()) return null;
        }
        Box placeBox = new Box(pos);
        if (placeBox.intersects(mc.player.getBoundingBox())) return null;
        if (target != null && placeBox.intersects(target.getBoundingBox())) return null;
        Vec3d crystalPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        if (!isSafeToExplode(crystalPos)) return null;
        return new ObsidianPlacement(pos, placeFace);
    }

    public boolean canPlaceCrystal(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK)) return false;
        if (!mc.world.getBlockState(pos.up()).isAir()) return false;
        if (!mc.world.getBlockState(pos.up(2)).isAir()) return false;
        Box crystalBox = new Box(pos.up());
        if (!mc.world.getOtherEntities(null, crystalBox, e -> e instanceof EndCrystalEntity).isEmpty()) return false;
        Box spawnBox = new Box(pos.up()).expand(0.0, 1.0, 0.0).union(new Box(pos.up(2)));
        return mc.world.getOtherEntities(null, spawnBox, e -> e instanceof PlayerEntity).isEmpty();
    }

    public boolean canPlaceCrystalIfObsidian(BlockPos pos) {
        if (!mc.world.getBlockState(pos.up()).isAir()) return false;
        if (!mc.world.getBlockState(pos.up(2)).isAir()) return false;
        Box crystalBox = new Box(pos.up());
        return mc.world.getOtherEntities(null, crystalBox, e -> e instanceof EndCrystalEntity).isEmpty();
    }

    public Direction findPlaceFace(BlockPos targetEntityPos) {
        Direction[] priority = { Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP };
        for (Direction dir : priority) {
            BlockPos neighbor = targetEntityPos.offset(dir);
            BlockState state = mc.world.getBlockState(neighbor);
            if (!state.isAir() && state.isSolidBlock(mc.world, neighbor)) return dir;
        }
        return null;
    }


    public EndCrystalEntity findBestCrystal(float range, boolean onlyOwn) {
        EndCrystalEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity crystal)) continue;
            if (!crystal.isAlive()) continue;
            double dist = mc.player.distanceTo(crystal);
            if (dist > range) continue;
            if (onlyOwn && !isOurCrystal(crystal)) continue;
            if (dist < bestDist) { bestDist = dist; best = crystal; }
        }
        return best;
    }

    public EndCrystalEntity findCrystalNear(BlockPos pos, double radius, boolean onlyOwn) {
        Vec3d center = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        Box box = new Box(center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        for (Entity entity : mc.world.getOtherEntities(null, box)) {
            if (entity instanceof EndCrystalEntity crystal && crystal.isAlive()) {
                if (crystal.getEntityPos().distanceTo(center) < radius) {
                    if (onlyOwn && !isOurCrystal(crystal)) continue;
                    return crystal;
                }
            }
        }
        return null;
    }

    public EndCrystalEntity findCrystalNear(BlockPos pos, double radius) {
        return findCrystalNear(pos, radius, false);
    }



    public PlayerEntity findTarget() {
        PlayerEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (!player.isAlive()) continue;
            if (player.isCreative() || player.isSpectator()) continue;
            if (ignoreFriends.get() && Client.FRIENDS.isFriend(player)) continue;
            double dist = mc.player.distanceTo(player);
            if (dist > targetRange.get()) continue;
            if (dist < bestDist) { bestDist = dist; best = player; }
        }
        return best;
    }


    public void rotateTo(Vec3d pos) {
        if (rotateMode.is(RotateMode.PACKET)) {
            packetRotateYaw = RotationUtility.calculate(pos).getYaw();
            NetworkUtility.sendAngle(RotationUtility.calculate(pos));
        } else {
            Angle targetAngle = RotationUtility.calculate(pos);
            Client.ROTATION.rotate(DefaultRotation.INSTANCE, targetAngle, 1, false, MovementCorrection.SILENT, 100);
        }
    }

    public boolean isRotatedTo(Vec3d pos, float maxAngle) {
        Angle currentRot = Client.ROTATION.getRotate();
        Angle targetAngle = RotationUtility.calculate(pos);
        return currentRot.angleTo(targetAngle) < maxAngle;
    }



    public float estimateDamage(Vec3d crystalPos, Entity entity) {
        double dist = entity.getEntityPos().distanceTo(crystalPos);
        if (dist > 12) return 0;
        double exposure = (1.0 - dist / 12.0);
        float damage = (float) (exposure * exposure * 42.0);
        if (entity instanceof PlayerEntity player) {
            int armorValue = player.getArmor();
            damage *= (1.0f - Math.min(armorValue * 0.04f, 0.8f));
            if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
                int resistLevel = player.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1;
                damage *= (1.0f - resistLevel * 0.2f);
            }
        }
        return Math.max(0, damage);
    }


    public int findCrystalSlot() {
        if (mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL) return 40;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.END_CRYSTAL) return i;
        }
        return -1;
    }

    public int findObsidianSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.OBSIDIAN) return i;
        }
        return -1;
    }


    public boolean isOurCrystal(EndCrystalEntity crystal) {
        return findMatchingPlaced(crystal) != null;
    }

    private PlacedCrystal findMatchingPlaced(EndCrystalEntity crystal) {
        Vec3d crystalPos = crystal.getEntityPos();
        for (PlacedCrystal pc : trackedCrystals) {
            Vec3d expected = new Vec3d(pc.pos.getX() + 0.5, pc.pos.getY() + 1.0, pc.pos.getZ() + 0.5);
            if (crystalPos.distanceTo(expected) < 1.5) return pc;
        }
        return null;
    }
    public Vec3d getCrystalCenter(EndCrystalEntity crystal) {
        return crystal.getEntityPos().add(0, crystal.getHeight() / 2.0, 0);
    }

    public Vec3d getPlaceHitVec(BlockPos pos, Direction face) {
        BlockPos neighbor = pos.offset(face);
        Direction clickFace = face.getOpposite();
        return new Vec3d(
                neighbor.getX() + 0.5 + clickFace.getOffsetX() * 0.5,
                neighbor.getY() + 0.5 + clickFace.getOffsetY() * 0.5,
                neighbor.getZ() + 0.5 + clickFace.getOffsetZ() * 0.5
        );
    }

    public boolean isObsidianOrBedrock(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK);
    }

    public record PlacedCrystal(BlockPos pos, long time) {}
    public record ObsidianPlacement(BlockPos pos, Direction face) {}
    public record ScoredPos(BlockPos pos, float targetDamage, float selfDamage) {}
    public record ScoredObsidian(BlockPos pos, Direction face, float targetDamage, float selfDamage) {}

    public static class PendingObsidian {
        public final BlockPos pos;
        public final long time;
        public final boolean waitingBreak;
        public PendingObsidian(BlockPos pos, long time, boolean waitingBreak) {
            this.pos = pos;
            this.time = time;
            this.waitingBreak = waitingBreak;
        }
    }
}