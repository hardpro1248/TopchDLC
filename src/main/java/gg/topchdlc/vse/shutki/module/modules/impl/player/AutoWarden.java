package gg.topchdlc.vse.shutki.module.modules.impl.player;

import gg.topchdlc.Client;
import gg.topchdlc.mixin.accessor.IBossBarHud;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event2D;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventReceivePacket;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.MovementCorrection;
import gg.topchdlc.vse.rotation.all.Rotation;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.text.TextSetting;
import gg.topchdlc.vse.utils.block.BlockLocationTracker;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import gg.topchdlc.vse.utils.math.Priorities;
import gg.topchdlc.vse.utils.math.RotationUtility;
import gg.topchdlc.vse.utils.math.TimeUtility;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import gg.topchdlc.vse.utils.player.BaritoneBridge;
import gg.topchdlc.vse.utils.player.PathExecutor;
import gg.topchdlc.vse.utils.player.Pathfinder;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.HangingSignBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.TrappedChestBlock;
import net.minecraft.block.WallHangingSignBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

import java.awt.Color;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoWarden extends Module {

    public static final AutoWarden INSTANCE = new AutoWarden();

    private final Group main = group("Варден");

    public final TextSetting wardenHome = main.text("Варден хом", "стежа");
    private enum WarehouseFindMode { SIGN, CONTAINER_NAME }

    public final TextSetting clanBase = main.text("Клан хом", "склад");
    public final TextSetting anarchyNumber = main.text("Номер анархии", "103");
    public final EnumSetting<WarehouseFindMode> warehouseFindMode = main.enumSetting("Поиск склада", WarehouseFindMode.SIGN);
    public final TextSetting depositSign = main.text("Текст таблички склада", "warden").visible(() -> warehouseFindMode.get() == WarehouseFindMode.SIGN);
    public final TextSetting depositContainerName = main.text("Имя сундука склада", "warden").visible(() -> warehouseFindMode.get() == WarehouseFindMode.CONTAINER_NAME);
    public final CheckBox autoLoot = main.checkbox("Авто лут", true);
    public final CheckBox autoDeposit = main.checkbox("Авто депозит", true);
    public final SliderSetting openRetryDelay = main.sliderSetting("Задержка открытия", 150, 50, 1500).increment(10);
    public final SliderSetting rejoinLead = main.sliderSetting("Заходить за (мс)", 3000, 0, 15000).increment(100);
    public final SliderSetting hubWaitLead = main.sliderSetting("В хаб если таймер до (сек)", 180, 30, 600).increment(5);
    public final SliderSetting combatCooldown = main.sliderSetting("КД после боя (сек)", 30, 10, 60).increment(1);
    public final SliderSetting lootTransferDelay = main.sliderSetting("Задержка лута (мс)", 90, 20, 500).increment(10);
    public final SliderSetting postCommandDelay = main.sliderSetting("Пауза после команд (мс)", 1600, 250, 5000).increment(50);
    public final SliderSetting postTeleportDelay = main.sliderSetting("Пауза после тп (мс)", 2200, 500, 8000).increment(100);
    public final CheckBox stopOnDisconnect = main.checkbox("Остановить при дисконнекте", true);
    public final CheckBox showRejoinTimerHud = main.checkbox("Показывать таймер возврата", true);

    private enum State {
        IDLE, GO_WARDEN, AT_WARDEN, WAIT_HUB, GO_ANARCHY,
        BACK_TO_CHEST, LOOT_CHEST, WAIT_COOLDOWN,
        GO_WAREHOUSE, AT_WAREHOUSE, BACK_HOME
    }

    private State currentState = State.IDLE;
    private int stateTicks = 0;
    private int warmupTicks = 0;

    private final PathExecutor pathExecutor = new PathExecutor();
    private final TimeUtility openTimer = new TimeUtility();
    private final TimeUtility deathTimer = new TimeUtility();
    private final TimeUtility hubTimer = new TimeUtility();
    private final TimeUtility combatTimer = new TimeUtility();
    private final TimeUtility stateTimer = new TimeUtility();

    private boolean justDied = false;
    private BlockPos lastMineTarget = null;
    private BlockPos lastChestPos = null;
    private int lastTimerSeconds = -1;
    private int interactStableTicks = 0;
    private BlockPos lockedChestPos = null;
    private BlockPos recentSkippedChestPos = null;
    private long recentSkippedChestUntil = 0L;
    private BlockPos lockedWarehouseChestPos = null;
    private final TimeUtility depositTimer = new TimeUtility();
    private int failedDepositAttempts = 0;
    private boolean lootedCurrentChest = false;
    private long lootedAt = 0L;
    private long targetChestOpenAt = -1L;
    private long lastHubReturnAttemptAt = 0L;
    private long commandCooldownUntil = 0L;
    private long teleportCooldownUntil = 0L;
    private boolean disconnectHandled = false;
    private long containerOpenedAt = 0L;

    private BlockPos randomWalkTarget = null;
    private final TimeUtility randomWalkTimer = new TimeUtility();

    private final ChestTracker chestTracker = new ChestTracker();
    private final SignTracker signTracker = new SignTracker();

    private final Set<BlockPos> processedChests = new HashSet<>();
    private int failedOpenAttempts = 0;
    private BlockPos failedOpenChest = null;

    private static final BlockPos[] PREDEFINED_WARDEN_CHESTS = {
            new BlockPos(-2052, -55, -1961),
            new BlockPos(-2045, -49, -1961),
            new BlockPos(-2006, -54, -1954),
            new BlockPos(-2025, -55, -1952),
            new BlockPos(-2001, -55, -2005),
            new BlockPos(-2033, -55, -2009),
            new BlockPos(-2036, -55, -2015),
            new BlockPos(-2033, -55, -1994),
            new BlockPos(-2036, -55, -1988),
            new BlockPos(-1987, -55, -2007),
            new BlockPos(-2005, -54, -2027),
            new BlockPos(-2011, -48, -2042),
            new BlockPos(-2005, -55, -2056),
            new BlockPos(-1977, -55, -2056),
            new BlockPos(-1944, -55, -2045),
            new BlockPos(-1960, -55, -2030),
            new BlockPos(-1938, -48, -2034),
            new BlockPos(-1965, -55, -2008),
            new BlockPos(-1955, -55, -1995),
            new BlockPos(-1952, -48, -1982),
            new BlockPos(-1961, -48, -1974),
            new BlockPos(-1951, -49, -1948),
            new BlockPos(-1936, -55, -1948),
            new BlockPos(-1972, -54, -1944),
            new BlockPos(-1970, -55, -1959),
            new BlockPos(-1989, -55, -1982),
            new BlockPos(-2008, -55, -1963),
            new BlockPos(-1979, -56, -1979),
            new BlockPos(-2045, -55, -2045),
            new BlockPos(-2052, -49, -2042),
            new BlockPos(-2053, -55, -2042)
    };

    private final Random random = new Random();
    private long inventoryCooldownUntil = 0L;
    private boolean movementBlockedDueToInventory = false;
    private boolean isClosingInventory = false;
    private long closeInventoryScheduledAt = 0L;

    private int movementStartTicks = 0;
    private boolean sprintBlocked = false;
    private int randomStrafeTimer = 0;
    private int strafeDirection = 0;

    private BlockPos lastPosition = null;
    private int stuckTicks = 0;
    private long cooldownStartTime = 0L;

    private int moveTicksCounter = 0;
    private int pauseTicks = 0;

    private float currentYaw = 0f;
    private float currentPitch = 0f;

    private long getJitteredDelay(long baseDelay, float variance) {
        if (baseDelay <= 0) return 0;
        long delta = (long) (baseDelay * variance * (random.nextDouble() * 2 - 1));
        return Math.max(10, baseDelay + delta);
    }

    private void resetCycleState() {
        inventoryCooldownUntil = 0L;
        movementBlockedDueToInventory = false;
        isClosingInventory = false;
        closeInventoryScheduledAt = 0L;
        lastMineTarget = null;
        lastChestPos = null;
        lastTimerSeconds = -1;
        interactStableTicks = 0;
        lockedChestPos = null;
        recentSkippedChestPos = null;
        recentSkippedChestUntil = 0L;
        lockedWarehouseChestPos = null;
        failedDepositAttempts = 0;
        lootedCurrentChest = false;
        lootedAt = 0L;
        targetChestOpenAt = -1L;
        lastHubReturnAttemptAt = 0L;
        containerOpenedAt = 0L;
        depositTimer.reset();
        failedOpenAttempts = 0;
        failedOpenChest = null;
        warmupTicks = 0;
        randomWalkTarget = null;
        randomWalkTimer.reset();
        lastPosition = null;
        stuckTicks = 0;
        cooldownStartTime = 0L;
        movementStartTicks = 0;
        sprintBlocked = false;
        randomStrafeTimer = 0;
        strafeDirection = 0;
        moveTicksCounter = 0;
        pauseTicks = 0;
        currentYaw = 0f;
        currentPitch = 0f;
    }

    private boolean isMovementBlockedByInventory() {
        if (inventoryCooldownUntil > System.currentTimeMillis()) return true;
        if (isClosingInventory) {
            if (System.currentTimeMillis() - closeInventoryScheduledAt < getJitteredDelay(150, 0.3f)) return true;
            else isClosingInventory = false;
        }
        return false;
    }

    private void blockMovementForInventory(long durationMs) {
        long actualDuration = getJitteredDelay(durationMs, 0.2f);
        inventoryCooldownUntil = System.currentTimeMillis() + actualDuration;
        movementBlockedDueToInventory = true;
    }

    // ==================== ЗАКРЫТИЕ ИНВЕНТАРЯ (без WAITING_FOR_CLOSE) ====================
    private void closeScreen() {
        if (mc.currentScreen instanceof HandledScreen<?> handled) {
            NetworkUtility.send(new CloseHandledScreenC2SPacket(handled.getScreenHandler().syncId));
            blockMovementForInventory(100 + random.nextInt(80)); // 100-180 мс
        }
        mc.setScreen(null);
    }

    // ==================== ДВИЖЕНИЕ С МИКРО-ПАУЗАМИ ====================
    private void applyMovementPattern(boolean forward, boolean sprint, boolean jump, float strafe) {
        if (mc.options == null) return;
        randomStrafeTimer++;
        if (randomStrafeTimer > 50 + random.nextInt(31)) {
            randomStrafeTimer = 0;
            strafeDirection = random.nextBoolean() ? 1 : -1;
            if (random.nextInt(3) == 0) strafeDirection = 0;
        }
        float finalStrafe = strafe + strafeDirection * 0.3f;
        finalStrafe = MathHelper.clamp(finalStrafe, -1f, 1f);

        if (moveTicksCounter++ % (5 + random.nextInt(6)) == 0) {
            pauseTicks = 1 + random.nextInt(3);
        }
        if (pauseTicks > 0) {
            pauseTicks--;
            releaseKeys();
            return;
        }

        mc.options.forwardKey.setPressed(forward);
        mc.options.backKey.setPressed(!forward && random.nextInt(100) < 3);
        if (finalStrafe > 0.1f) {
            mc.options.rightKey.setPressed(true);
            mc.options.leftKey.setPressed(false);
        } else if (finalStrafe < -0.1f) {
            mc.options.leftKey.setPressed(true);
            mc.options.rightKey.setPressed(false);
        } else {
            mc.options.rightKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
        }
        if (sprint && !sprintBlocked) mc.options.sprintKey.setPressed(true);
        else mc.options.sprintKey.setPressed(false);
        mc.options.jumpKey.setPressed(jump);
    }

    private void releaseKeys() {
        if (mc.options == null) return;
        BaritoneBridge.cancel();
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
        mc.options.attackKey.setPressed(false);
        pauseTicks = 0;
    }

    // ==================== ПРОВЕРКА ПРИЦЕЛА ====================
    private boolean isLookingAtBlock(BlockPos pos) {
        if (mc.player == null || mc.world == null) return false;
        Vec3d eyes = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVector();
        double maxDist = 4.5;
        Vec3d end = eyes.add(lookVec.multiply(maxDist));
        BlockHitResult hit = mc.world.raycast(new RaycastContext(eyes, end,
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
        return hit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK && hit.getBlockPos().equals(pos);
    }

    // ==================== ОСТАЛЬНЫЕ ВСПОМОГАТЕЛЬНЫЕ ====================
    private Vec3d adjustDirectionForBlindness(Vec3d direction) {
        if (mc.player == null) return direction;
        boolean hasBlindness = mc.player.hasStatusEffect(StatusEffects.BLINDNESS) || mc.player.hasStatusEffect(StatusEffects.DARKNESS);
        if (!hasBlindness) return direction;
        double angleDeviation = (random.nextDouble() - 0.5) * 0.2;
        double cos = Math.cos(angleDeviation);
        double sin = Math.sin(angleDeviation);
        double x = direction.x * cos - direction.z * sin;
        double z = direction.x * sin + direction.z * cos;
        return new Vec3d(x, direction.y, z).normalize();
    }

    private boolean isBlockedWithJumpCheck(Vec3d direction, boolean[] shouldJump) {
        if (mc.player == null || mc.world == null) {
            if (shouldJump != null) shouldJump[0] = false;
            return false;
        }
        Vec3d start = mc.player.getEyePos();
        Vec3d end = start.add(direction.multiply(2.0));
        BlockHitResult result = mc.world.raycast(new RaycastContext(start, end,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        if (result.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            double dist = start.distanceTo(result.getPos());
            if (dist < 1.5) {
                Vec3d feetStart = mc.player.getEntityPos();
                Vec3d feetEnd = feetStart.add(direction.multiply(1.2));
                BlockHitResult feetResult = mc.world.raycast(new RaycastContext(feetStart, feetEnd,
                        RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
                if (feetResult.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
                    if (shouldJump != null) shouldJump[0] = true;
                }
                return true;
            }
        }
        if (shouldJump != null) shouldJump[0] = false;
        return false;
    }

    private Vec3d avoidObstaclesAndBlindness(Vec3d desiredDirection) {
        if (mc.player == null) return desiredDirection;
        if (desiredDirection.lengthSquared() < 0.001) return desiredDirection;
        Vec3d adjusted = adjustDirectionForBlindness(desiredDirection);
        boolean[] jump = new boolean[]{false};
        if (isBlockedWithJumpCheck(adjusted, jump)) {
            Vec3d right = new Vec3d(-adjusted.z, 0, adjusted.x).normalize();
            Vec3d left = right.multiply(-1);
            boolean rightFree = !isBlockedWithJumpCheck(right, null);
            boolean leftFree = !isBlockedWithJumpCheck(left, null);
            if (rightFree && leftFree) {
                adjusted = adjusted.add(right.multiply(random.nextBoolean() ? 0.5 : -0.5)).normalize();
            } else if (rightFree) {
                adjusted = adjusted.add(right.multiply(0.5)).normalize();
            } else if (leftFree) {
                adjusted = adjusted.add(left.multiply(0.5)).normalize();
            } else {
                if (jump[0]) adjusted = adjusted.multiply(0.8);
                else adjusted = adjusted.multiply(-0.3);
            }
        }
        return adjusted;
    }

    private BlockPos getRandomWardenPos() {
        if (mc.player == null) return null;
        int minX = -2070, maxX = -1936;
        int minZ = -2063, maxZ = -1936;
        int minY = -58, maxY = -40;
        for (int attempt = 0; attempt < 20; attempt++) {
            int x = minX + random.nextInt(maxX - minX + 1);
            int z = minZ + random.nextInt(maxZ - minZ + 1);
            int y = minY + random.nextInt(maxY - minY + 1);
            BlockPos pos = new BlockPos(x, y, z);
            if (isInWardenArea(pos) && mc.world.getBlockState(pos).isAir()) return pos;
        }
        return null;
    }

    // ==================== ДВИЖЕНИЕ ====================
    private void walkTo(BlockPos target) {
        if (isMovementBlockedByInventory()) { releaseKeys(); return; }
        if (mc.player == null || mc.world == null || target == null) return;
        BlockPos currentPos = mc.player.getBlockPos();
        if (lastPosition != null && lastPosition.equals(currentPos)) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            lastPosition = currentPos;
        }
        if (stuckTicks > 40) {
            pathExecutor.stop();
            stuckTicks = 0;
            mc.options.jumpKey.setPressed(true);
            try { Thread.sleep(100 + random.nextInt(200)); } catch (InterruptedException ignored) {}
            mc.options.jumpKey.setPressed(false);
            ChatUtility.send(Text.literal("§7[AutoWarden] Застрял, прыгаю."));
        }
        if (BaritoneBridge.isAvailable()) {
            walkWithBaritone(target);
            return;
        }
        if (pathExecutor.isStuck()) pathExecutor.clearStuck();
        if (!pathExecutor.isActive() || pathExecutor.isCompleted()) {
            List<BlockPos> path = Pathfinder.findPath(mc.player.getBlockPos(), target);
            if (path != null && !path.isEmpty()) {
                pathExecutor.start(path, target);
                return;
            } else {
                fallbackMove(target);
                return;
            }
        }
        pathExecutor.tick();
    }

    private void fallbackMove(BlockPos target) {
        if (isMovementBlockedByInventory()) { releaseKeys(); return; }
        if (mc.player == null || mc.world == null) return;
        Vec3d center = Vec3d.ofCenter(target);
        Vec3d direction = new Vec3d(center.x - mc.player.getX(), 0, center.z - mc.player.getZ()).normalize();
        boolean blocked = isBlockedWithJumpCheck(direction, null);
        float strafe = 0f;
        if (blocked) {
            Vec3d right = new Vec3d(-direction.z, 0, direction.x).normalize();
            Vec3d left = right.multiply(-1);
            boolean rightFree = !isBlockedWithJumpCheck(right, null);
            boolean leftFree = !isBlockedWithJumpCheck(left, null);
            if (rightFree) { direction = direction.add(right.multiply(0.5)).normalize(); strafe = 0.5f; }
            else if (leftFree) { direction = direction.add(left.multiply(0.5)).normalize(); strafe = -0.5f; }
            else { direction = direction.multiply(-0.3); strafe = 0f; }
        }
        Angle targetAngle = RotationUtility.calculate(center);
        Client.ROTATION.rotate(SMOOTH_ROTATION, targetAngle, 2, true, MovementCorrection.STRICT, Priorities.NORMAL);
        boolean forward = direction.lengthSquared() > 0.01;
        boolean jump = blocked && random.nextInt(100) < 40;
        if (jump) {
            BlockPos headPos = mc.player.getBlockPos().up(2);
            if (!mc.world.getBlockState(headPos).isAir()) jump = false;
        }
        boolean sprint = movementStartTicks > 4 && !sprintBlocked;
        applyMovementPattern(forward, sprint, jump, strafe);
        if (forward) movementStartTicks++;
    }

    private void walkWithBaritone(BlockPos target) {
        pathExecutor.stop();
        if (mc.player != null && mc.player.getEyePos().distanceTo(Vec3d.ofCenter(target)) > 3.0 && !BaritoneBridge.hasGoal(target)) {
            BaritoneBridge.goTo(target, 2);
        }
    }

    private void walkToInteract(BlockPos target) {
        if (target == null) return;
        walkTo(target);
    }

    // ==================== ПЛАВНЫЙ ПОВОРОТ ====================
    private final Rotation SMOOTH_ROTATION = (cur, tgt) -> {
        float maxStep = 18f + (float)(Math.random() * 7f);
        float dy = MathHelper.wrapDegrees(tgt.getYaw() - cur.getYaw());
        float dp = MathHelper.clamp(tgt.getPitch() - cur.getPitch(), -maxStep, maxStep);
        if (Math.abs(dy) > maxStep) dy = Math.signum(dy) * maxStep;
        dy += (float)(Math.random() - 0.5) * 0.8f;
        dp += (float)(Math.random() - 0.5) * 0.8f;
        return new Angle(cur.getYaw() + dy, MathHelper.clamp(cur.getPitch() + dp, -90f, 90f));
    };

    private void lookAtBlockSmooth(BlockPos pos) {
        if (mc.player == null) return;
        Vec3d point = getInteractPoint(pos, getInteractFace(pos)).add(0.0, 0.15, 0.0);
        Angle target = RotationUtility.calculate(point);
        Client.ROTATION.rotate(SMOOTH_ROTATION, target, 2, true, MovementCorrection.STRICT, Priorities.NORMAL);
    }

    // ==================== ИНВЕНТАРЬ ====================
    private void scheduleCloseInventory() {
        if (isClosingInventory) return;
        isClosingInventory = true;
        closeInventoryScheduledAt = System.currentTimeMillis();
    }

    private boolean storageHasItems() {
        if (!(mc.currentScreen instanceof HandledScreen<?> screen)) return false;
        ScreenHandler h = screen.getScreenHandler();
        int storage = getContainerSlotCount(h);
        for (int i = 0; i < storage; i++) {
            if (h.getSlot(i).hasStack()) return true;
        }
        return false;
    }

    private boolean playerHasItems() {
        if (!(mc.currentScreen instanceof HandledScreen<?> screen)) return false;
        ScreenHandler h = screen.getScreenHandler();
        for (int i = h.slots.size() - 36; i < h.slots.size(); i++) {
            if (h.getSlot(i).hasStack()) return true;
        }
        return false;
    }

    // ==================== ПРЯМОЙ ЛУТ (без очереди) ====================
    private void transferFromStorage() {
        if (!(mc.currentScreen instanceof HandledScreen<?> screen)) return;
        ScreenHandler h = screen.getScreenHandler();
        int storage = getContainerSlotCount(h);
        boolean moved = false;
        // Собираем слоты в случайном порядке
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < storage; i++) slots.add(i);
        Collections.shuffle(slots, random);
        for (int slot : slots) {
            if (h.getSlot(slot).hasStack()) {
                mc.interactionManager.clickSlot(h.syncId, h.getSlot(slot).id, 0, SlotActionType.QUICK_MOVE, mc.player);
                moved = true;
                // Задержка между кликами 30-70 мс
                try { Thread.sleep(30 + random.nextInt(41)); } catch (InterruptedException ignored) {}
                blockMovementForInventory(30);
            }
        }
        if (moved) {
            openTimer.reset();
            blockMovementForInventory(80);
        }
    }

    private boolean transferToStorage() {
        if (!(mc.currentScreen instanceof HandledScreen<?> screen)) return false;
        ScreenHandler h = screen.getScreenHandler();
        int playerStart = Math.max(0, h.slots.size() - 36);
        List<Integer> slots = new ArrayList<>();
        for (int i = playerStart; i < h.slots.size(); i++) slots.add(i);
        Collections.shuffle(slots, random);
        for (int slot : slots) {
            if (h.getSlot(slot).hasStack()) {
                mc.interactionManager.clickSlot(h.syncId, h.getSlot(slot).id, 0, SlotActionType.QUICK_MOVE, mc.player);
                try { Thread.sleep(30 + random.nextInt(41)); } catch (InterruptedException ignored) {}
                blockMovementForInventory(30);
                return true;
            }
        }
        return false;
    }

    private int getContainerSlotCount(ScreenHandler h) {
        if (h == null) return 0;
        int total = h.slots.size();
        if (total <= 0) return 0;
        if (total <= 36) return total;
        return Math.max(0, total - 36);
    }

    // ==================== ОСТАЛЬНЫЕ МЕТОДЫ (без изменений) ====================
    public AutoWarden() {
        super("AutoWarden", Category.PLAYER, "Авто ферма вардена");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        openTimer.reset();
        deathTimer.reset();
        hubTimer.reset();
        combatTimer.reset();
        stateTimer.reset();
        justDied = false;
        currentState = State.IDLE;
        stateTicks = 0;
        warmupTicks = 0;
        processedChests.clear();
        failedOpenAttempts = 0;
        failedOpenChest = null;
        resetCycleState();
        Client.ChunkScanner.subscribe(chestTracker);
        Client.ChunkScanner.subscribe(signTracker);
        if (mc.player != null && mc.world != null) {
            if (isInWardenArea(mc.player.getBlockPos())) {
                currentState = State.AT_WARDEN;
                warmupTicks = 20;
                ChatUtility.send(Text.literal("§a[AutoWarden] Уже на Вардене, начинаю поиск сундука без /home."));
            } else {
                ChatUtility.send(Text.literal("§a[AutoWarden] Включён."));
            }
        }
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        pathExecutor.stop();
        releaseKeys();
        closeScreen();
        Client.ChunkScanner.unsubscribe(chestTracker);
        Client.ChunkScanner.unsubscribe(signTracker);
        chestTracker.clearAllChunks();
        signTracker.clearAllChunks();
        processedChests.clear();
        resetCycleState();
    }

    EventBus<Event> events = e -> {
        if (e instanceof EventReceivePacket ev) {
            if (ev.packet instanceof GameMessageS2CPacket pkt) {
                String msg = Formatting.strip(pkt.content().getString()).toLowerCase();
                if (msg.contains("погибли") || msg.contains("помянем") || msg.contains("вы умерли")) justDied = true;
                if (msg.contains("забанены") || msg.contains("бан")) {
                    ChatUtility.send(Text.literal("§c[AutoWarden] Обнаружен бан! Отключаю модуль."));
                    toggle();
                }
                if (msg.contains("режим боя") && msg.contains("закончен") || msg.contains("combat tag")) {
                    long reactionDelay = 450 + random.nextInt(351);
                    try { Thread.sleep(reactionDelay); } catch (InterruptedException ignored) {}
                }
            }
        } else if (e instanceof EventGameTick) {
            onTick();
        } else if (e instanceof Event2D ev2) {
            render2D(ev2);
        }
    };

    private void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (warmupTicks > 0) { warmupTicks--; releaseKeys(); return; }
        if (isMovementBlockedByInventory()) { releaseKeys(); }
        if (stopOnDisconnect.get() && mc.currentScreen instanceof DisconnectedScreen) {
            if (!disconnectHandled) {
                disconnectHandled = true;
                ChatUtility.send(Text.literal("§c[AutoWarden] Обнаружен экран дисконнекта. Мод остановлен для безопасности."));
                toggle();
            }
            return;
        } else {
            disconnectHandled = false;
        }
        if (justDied || mc.player.getHealth() <= 0) { handleDeath(); return; }
        if (lastMineTarget != null && mc.world.getBlockState(lastMineTarget).isAir()) lastMineTarget = null;

        switch (currentState) {
            case IDLE:
                if (++stateTicks < 20) return;
                stateTicks = 0;
                if (isInWardenArea(mc.player.getBlockPos())) {
                    currentState = State.AT_WARDEN;
                    warmupTicks = 10;
                    interactStableTicks = 0;
                    return;
                }
                startWarden();
                break;
            case GO_WARDEN: goWardenTick(); break;
            case AT_WARDEN: atWardenTick(); break;
            case WAIT_HUB: waitHubTick(); break;
            case GO_ANARCHY: goAnarchyTick(); break;
            case BACK_TO_CHEST: backToChestTick(); break;
            case LOOT_CHEST: lootChestTick(); break;
            case WAIT_COOLDOWN: waitCooldownTick(); break;
            case GO_WAREHOUSE: goWarehouseTick(); break;
            case AT_WAREHOUSE: atWarehouseTick(); break;
            case BACK_HOME: backHomeTick(); break;
        }
        handleScheduledClose();
    }

    private void handleScheduledClose() {
        if (isClosingInventory) {
            long delay = getJitteredDelay(100, 0.45f);
            if (System.currentTimeMillis() - closeInventoryScheduledAt >= delay) {
                closeScreen();
                isClosingInventory = false;
                blockMovementForInventory(100);
            }
        }
    }

    private void handleDeath() {
        pathExecutor.stop();
        releaseKeys();
        closeScreen();
        if (mc.player == null || mc.player.getHealth() <= 0 || mc.currentScreen != null) { deathTimer.reset(); return; }
        if (deathTimer.reached((long) rejoinLead.get(), false)) {
            justDied = false;
            currentState = State.IDLE;
            stateTicks = 0;
            resetCycleState();
            ChatUtility.send(Text.literal("§a[AutoWarden] Возродились, продолжаем ферму."));
        }
    }

    private void startWarden() {
        pathExecutor.stop();
        releaseKeys();
        closeScreen();
        resetCycleState();
        currentState = State.GO_WARDEN;
        stateTicks = 0;
        stateTimer.reset();
    }

    private void goWardenTick() {
        if (isMovementBlockedByInventory()) return;
        pathExecutor.stop();
        releaseKeys();
        if (mc.player == null) return;
        if (isInWardenArea(mc.player.getBlockPos())) {
            if (!teleportDelayPassed()) return;
            currentState = State.AT_WARDEN;
            warmupTicks = 10;
            stateTicks = 0;
            interactStableTicks = 0;
            return;
        }
        if (stateTicks == 0) {
            if (!commandDelayPassed()) return;
            sendServerCommand("home " + wardenHome.getText());
            ChatUtility.send(Text.literal("§a[AutoWarden] /home " + wardenHome.getText()));
        }
        stateTicks++;
        if (isInWardenArea(mc.player.getBlockPos())) {
            currentState = State.AT_WARDEN;
            warmupTicks = 10;
            stateTicks = 0;
            interactStableTicks = 0;
        } else if (stateTicks > 600) {
            stateTicks = 0;
        }
    }

    private void atWardenTick() {
        if (isMovementBlockedByInventory()) return;
        if (mc.player == null || mc.world == null) return;
        if (lockedChestPos == null || !isContainer(lockedChestPos) || processedChests.contains(lockedChestPos)) {
            lockedChestPos = selectBestWardenChest();
            if (lockedChestPos != null && !lockedChestPos.equals(failedOpenChest)) {
                failedOpenAttempts = 0;
                failedOpenChest = null;
            }
        }
        if (lockedChestPos == null) {
            pathExecutor.stop();
            releaseKeys();
            stateTicks++;
            if (stateTicks > 100) {
                List<BlockPos> allChests = buildChestList(256);
                if (allChests.isEmpty()) {
                    ChatUtility.send(Text.literal("§c[AutoWarden] В зоне Вардена нет сундуков. Отключаем."));
                    toggle();
                    return;
                }
                boolean allProcessed = true;
                for (BlockPos p : allChests) {
                    if (!processedChests.contains(p)) { allProcessed = false; break; }
                }
                if (allProcessed) {
                    ChatUtility.send(Text.literal("§a[AutoWarden] Все сундуки на Вардене залутаны! Отключаем."));
                    toggle();
                    return;
                }
                stateTicks = 0;
            }
            return;
        }
        stateTicks = 0;

        if (inventoryNearlyFull() && lootedCurrentChest) {
            startWarehouse();
            return;
        }
        if (lootedCurrentChest) {
            currentState = State.WAIT_COOLDOWN;
            stateTicks = 0;
            combatTimer.reset();
            cooldownStartTime = System.currentTimeMillis();
            movementStartTicks = 0;
            sprintBlocked = true;
            return;
        }

        BlockPos chest = lockedChestPos;
        lastChestPos = chest;

        if (mc.currentScreen instanceof HandledScreen<?>) {
            pathExecutor.stop();
            releaseKeys();
            if (containerOpenedAt == 0L) containerOpenedAt = System.currentTimeMillis();
            long openAge = System.currentTimeMillis() - containerOpenedAt;
            if (storageHasItems()) {
                lootedCurrentChest = true;
                if (lootedAt == 0L) lootedAt = System.currentTimeMillis();
                targetChestOpenAt = -1L;
                lastTimerSeconds = -1;
                transferFromStorage();
                processedChests.add(chest);
                failedOpenAttempts = 0;
                failedOpenChest = null;
                scheduleCloseInventory();
                currentState = State.WAIT_COOLDOWN;
                combatTimer.reset();
                cooldownStartTime = System.currentTimeMillis();
                movementStartTicks = 0;
                sprintBlocked = true;
                return;
            }
            if (openAge < 400L) return;
            scheduleCloseInventory();
            markChestSkipped(chest, 30000L);
            lockedChestPos = null;
            currentState = State.AT_WARDEN;
            containerOpenedAt = 0L;
            stateTicks = 0;
            stateTimer.reset();
            interactStableTicks = 0;
            return;
        }

        double dist = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(chest));
        if (!stabilizeForContainer(chest, dist, false)) return;

        int timer = secondsRemainingNear(chest);
        if (timer > 0) {
            lastTimerSeconds = timer;
            long hubThresholdSeconds = Math.max(1L, (long) hubWaitLead.get());
            if (timer > hubThresholdSeconds) {
                long skipDuration = (timer - hubThresholdSeconds) * 1000L + 10000L;
                markChestSkipped(chest, skipDuration);
                lockedChestPos = null;
                currentState = State.AT_WARDEN;
                stateTicks = 0;
                interactStableTicks = 0;
                ChatUtility.send(Text.literal("§7[AutoWarden] Таймер " + timer + " сек. > " + hubThresholdSeconds + ", пропускаю сундук."));
                return;
            }
            lockedChestPos = chest;
            if (isInCombat()) { runAroundWarden(); return; }
            if (!commandDelayPassed()) return;
            targetChestOpenAt = System.currentTimeMillis() + timer * 1000L;
            ChatUtility.send(Text.literal("§a[AutoWarden] Сундук откроется через " + timer + " сек. Уходим в хаб."));
            sendServerCommand("hub");
            currentState = State.WAIT_HUB;
            stateTicks = 0;
            hubTimer.reset();
            stateTimer.reset();
            lastHubReturnAttemptAt = 0L;
            return;
        }
        openContainer(chest);
    }

    private void waitHubTick() {
        if (isMovementBlockedByInventory()) return;
        pathExecutor.stop();
        releaseKeys();
        if (targetChestOpenAt <= 0L) {
            if (hubTimer.reached(60000, false)) {
                ChatUtility.send(Text.literal("§c[AutoWarden] Нет targetChestOpenAt, возвращаюсь fallback-ом."));
                goToAnarchy();
            }
            return;
        }
        long now = System.currentTimeMillis();
        long remainingMs = targetChestOpenAt - now;
        long rejoinLeadMs = Math.max(0L, (long) rejoinLead.get());
        stateTicks++;
        boolean shouldReturn = remainingMs <= rejoinLeadMs;
        boolean canRetry = now - lastHubReturnAttemptAt >= 3000L;
        if (shouldReturn && canRetry) {
            lastHubReturnAttemptAt = now;
            ChatUtility.send(Text.literal("§a[AutoWarden] Возвращаюсь с хаба, remainingMs=" + remainingMs));
            goToAnarchy();
        }
    }

    private void goToAnarchy() {
        String anarchy = sanitizeAnarchyNumber();
        if (anarchy.isEmpty()) {
            ChatUtility.send(Text.literal("§c[AutoWarden] Неверно указан номер анархии."));
            toggle();
            return;
        }
        if (!commandDelayPassed()) return;
        ChatUtility.send(Text.literal("§a[AutoWarden] Заходим на анархию " + anarchy + "."));
        sendChatCommandRaw("/an" + anarchy);
        currentState = State.GO_ANARCHY;
        stateTicks = 0;
        stateTimer.reset();
    }

    private void goAnarchyTick() {
        if (isMovementBlockedByInventory()) return;
        pathExecutor.stop();
        releaseKeys();
        if (mc.player == null) return;
        stateTicks++;
        if (isInWardenArea(mc.player.getBlockPos())) {
            currentState = State.BACK_TO_CHEST;
            warmupTicks = 5;
            stateTicks = 0;
            targetChestOpenAt = -1L;
            lastHubReturnAttemptAt = 0L;
            lockedChestPos = null;
        } else if (stateTicks > 100) {
            stateTicks = 0;
            String anarchy = sanitizeAnarchyNumber();
            if (!anarchy.isEmpty()) {
                if (!commandDelayPassed()) return;
                ChatUtility.send(Text.literal("§e[AutoWarden] Повторяю вход на анархию " + anarchy));
                sendChatCommandRaw("/an" + anarchy);
                stateTimer.reset();
            }
        }
    }

    private void backToChestTick() {
        if (isMovementBlockedByInventory()) return;
        if (mc.player == null) return;
        if (lastChestPos == null) {
            currentState = State.AT_WARDEN;
            stateTicks = 0;
            lockedChestPos = null;
            return;
        }
        double dist = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(lastChestPos));
        if (!stabilizeForContainer(lastChestPos, dist, false)) return;
        currentState = State.LOOT_CHEST;
        stateTicks = 0;
    }

    private void lootChestTick() {
        if (isMovementBlockedByInventory()) return;
        if (mc.player == null) return;
        if (lastChestPos == null) {
            currentState = State.AT_WARDEN;
            stateTicks = 0;
            lockedChestPos = null;
            return;
        }
        if (mc.currentScreen instanceof HandledScreen<?>) {
            pathExecutor.stop();
            releaseKeys();
            if (containerOpenedAt == 0L) containerOpenedAt = System.currentTimeMillis();
            if (storageHasItems()) {
                lootedCurrentChest = true;
                if (lootedAt == 0L) lootedAt = System.currentTimeMillis();
                transferFromStorage();
                processedChests.add(lastChestPos);
                failedOpenAttempts = 0;
                failedOpenChest = null;
                scheduleCloseInventory();
                currentState = State.WAIT_COOLDOWN;
                combatTimer.reset();
                cooldownStartTime = System.currentTimeMillis();
                movementStartTicks = 0;
                sprintBlocked = true;
                return;
            }
            if (System.currentTimeMillis() - containerOpenedAt < 250L) return;
            scheduleCloseInventory();
            markChestSkipped(lastChestPos, 30000L);
            currentState = State.AT_WARDEN;
            lockedChestPos = null;
            containerOpenedAt = 0L;
            stateTicks = 0;
            stateTimer.reset();
            interactStableTicks = 0;
            return;
        }
        double dist = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(lastChestPos));
        if (!stabilizeForContainer(lastChestPos, dist, false)) return;
        openContainer(lastChestPos);
    }

    private void waitCooldownTick() {
        if (isMovementBlockedByInventory()) return;
        targetChestOpenAt = -1L;
        lastTimerSeconds = -1;
        long cooldownMs = (long) (combatCooldown.get() * 1000);
        long elapsed = System.currentTimeMillis() - cooldownStartTime;
        long remaining = cooldownMs - elapsed;
        if (remaining < 5000 && remaining > 0) {
            releaseKeys();
        } else if (isInCombat()) {
            runAroundWarden();
        } else {
            if (randomWalkTarget == null || mc.player.getEyePos().distanceTo(Vec3d.ofCenter(randomWalkTarget)) < 1.5 || randomWalkTimer.reached(5000 + random.nextInt(10000), true)) {
                randomWalkTarget = getRandomWardenPos();
                if (randomWalkTarget == null) { releaseKeys(); } else { randomWalkTimer.reset(); }
            }
            if (randomWalkTarget != null && remaining > 5000) {
                walkTo(randomWalkTarget);
                if (movementStartTicks > 4) sprintBlocked = false;
            } else {
                releaseKeys();
            }
        }
        if (elapsed > cooldownMs) {
            long extraDelay = 2000 + random.nextInt(3000);
            if (elapsed > cooldownMs + extraDelay) {
                if (lootedCurrentChest) {
                    startWarehouse();
                } else {
                    currentState = State.AT_WARDEN;
                    stateTicks = 0;
                }
            }
        }
    }

    private void startWarehouse() {
        pathExecutor.stop();
        releaseKeys();
        closeScreen();
        lootedCurrentChest = false;
        lootedAt = 0L;
        randomWalkTarget = null;
        randomWalkTimer.reset();
        currentState = State.GO_WAREHOUSE;
        stateTicks = 0;
        stateTimer.reset();
        lockedWarehouseChestPos = null;
        failedDepositAttempts = 0;
        depositTimer.reset();
        ChatUtility.send(Text.literal("§a[AutoWarden] Идём на склад сдавать ресурсы."));
    }

    private void goWarehouseTick() {
        if (isMovementBlockedByInventory()) return;
        pathExecutor.stop();
        releaseKeys();
        if (mc.currentScreen != null) { closeScreen(); stateTicks = 0; return; }
        if (stateTicks == 0) {
            if (!commandDelayPassed()) return;
            sendServerCommand("clan home " + clanBase.getText());
            ChatUtility.send(Text.literal("§a[AutoWarden] /clan home " + clanBase.getText()));
        }
        stateTicks++;
        if (lockedWarehouseChestPos == null || !isContainer(lockedWarehouseChestPos)) {
            lockedWarehouseChestPos = findDepositChest();
            if (lockedWarehouseChestPos != null) {
                ChatUtility.send(Text.literal("§7[AutoWarden] Найден складской сундук " + formatPos(lockedWarehouseChestPos)));
            }
        }
        if (lockedWarehouseChestPos != null) {
            currentState = State.AT_WAREHOUSE;
            stateTicks = 0;
            interactStableTicks = 0;
        } else {
            if (stateTicks > 600) stateTicks = 0;
        }
    }

    private void atWarehouseTick() {
        if (isMovementBlockedByInventory()) return;
        BlockPos chest = lockedWarehouseChestPos;
        if (chest == null || !isContainer(chest)) {
            lockedWarehouseChestPos = findDepositChest();
            chest = lockedWarehouseChestPos;
        }
        if (chest == null) {
            pathExecutor.stop();
            releaseKeys();
            if (++stateTicks > 400) startWarehouse();
            return;
        }
        stateTicks = 0;
        double dist = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(chest));
        if (!stabilizeForWarehouseContainer(chest, dist)) return;
        if (mc.currentScreen instanceof HandledScreen<?> handled) {
            if (!isExpectedWarehouseContainer(handled)) {
                ChatUtility.send(Text.literal("§c[AutoWarden] Открылся не тот контейнер склада: " + handled.getTitle().getString()));
                closeScreen();
                failedDepositAttempts++;
                if (failedDepositAttempts >= 4) {
                    lockedWarehouseChestPos = null;
                    failedDepositAttempts = 0;
                }
                stateTicks = 0;
                return;
            }
            failedDepositAttempts = 0;
            if (autoDeposit.get() && playerHasItems()) {
                if (depositTimer.reached(120, true)) {
                    boolean moved = transferToStorage();
                    if (moved) {
                        ChatUtility.send(Text.literal("§a[AutoWarden] Складываю ресурсы в складской сундук..."));
                    } else {
                        ChatUtility.send(Text.literal("§7[AutoWarden] QUICK_MOVE не смог переложить вещи, завершаю склад."));
                        closeScreen();
                        currentState = State.BACK_HOME;
                        stateTicks = 0;
                        stateTimer.reset();
                        lockedChestPos = null;
                        lockedWarehouseChestPos = null;
                        resetCycleState();
                    }
                }
                return;
            }
            ChatUtility.send(Text.literal("§a[AutoWarden] Склад пуст/ресурсы сложены, возвращаюсь на Варден."));
            closeScreen();
            currentState = State.BACK_HOME;
            stateTicks = 0;
            stateTimer.reset();
            lockedChestPos = null;
            lockedWarehouseChestPos = null;
            resetCycleState();
            return;
        }
        openWarehouseContainer(chest);
    }

    private void backHomeTick() {
        if (isMovementBlockedByInventory()) return;
        pathExecutor.stop();
        releaseKeys();
        if (stateTicks == 0) {
            if (!commandDelayPassed()) return;
            sendServerCommand("home " + wardenHome.getText());
            ChatUtility.send(Text.literal("§a[AutoWarden] /home " + wardenHome.getText()));
        }
        stateTicks++;
        if (mc.player != null && isInWardenArea(mc.player.getBlockPos())) {
            currentState = State.AT_WARDEN;
            warmupTicks = 10;
            stateTicks = 0;
            resetCycleState();
        } else if (stateTicks > 600) {
            stateTicks = 0;
        }
    }

    private boolean inventoryNearlyFull() {
        if (mc.player == null) return false;
        int used = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (!mc.player.getInventory().getStack(i).isEmpty()) used++;
        }
        return used >= 34;
    }

    private boolean stabilizeForContainer(BlockPos pos, double dist, boolean allowMining) {
        if (isMovementBlockedByInventory()) { interactStableTicks = 0; return false; }
        if (mc.player == null) return false;
        boolean canInteract = canInteractWithContainer(pos);
        if (dist > 3.5) {
            interactStableTicks = 0;
            if (allowMining && dist > 4.0 && mineIfBlocked(pos)) return false;
            walkToInteract(pos);
            return false;
        }
        pathExecutor.stop();
        releaseKeys();
        lookAtBlockSmooth(pos);
        interactStableTicks++;
        return interactStableTicks >= 3;
    }

    private boolean stabilizeForWarehouseContainer(BlockPos pos, double dist) {
        if (isMovementBlockedByInventory()) { interactStableTicks = 0; return false; }
        if (mc.player == null) return false;
        if (dist > 3.5) { interactStableTicks = 0; walkToInteract(pos); return false; }
        pathExecutor.stop();
        releaseKeys();
        lookAtBlockSmooth(pos);
        interactStableTicks++;
        return interactStableTicks >= 3;
    }

    private void openContainer(BlockPos pos) {
        if (isMovementBlockedByInventory()) return;
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (pos == null) return;
        double dist = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos));
        if (dist > 3.8) return;
        if (!canInteractWithContainer(pos) && dist > 3.2) return;
        if (!isLookingAtBlock(pos)) {
            lookAtBlockSmooth(pos);
            try { Thread.sleep(50 + random.nextInt(80)); } catch (InterruptedException ignored) {}
            if (!isLookingAtBlock(pos)) {
                ChatUtility.send(Text.literal("§7[AutoWarden] Не могу навестись на сундук, пропускаю."));
                markChestSkipped(pos, 30000L);
                lockedChestPos = null;
                currentState = State.AT_WARDEN;
                return;
            }
        }
        long delay = getJitteredDelay((long) openRetryDelay.get(), 0.15f);
        if (openTimer.reached(delay, true)) {
            if (pos.equals(failedOpenChest)) {
                failedOpenAttempts++;
            } else {
                failedOpenChest = pos;
                failedOpenAttempts = 1;
            }
            if (failedOpenAttempts >= 8) {
                markChestSkipped(pos, 30000L);
                lockedChestPos = null;
                currentState = State.AT_WARDEN;
                ChatUtility.send(Text.literal("§7[AutoWarden] Пропускаю сундук " + formatPos(pos) + " (слишком много неудачных попыток)."));
                return;
            }
            Direction side = getInteractFace(pos);
            Direction[] sides = { side, side.rotateYClockwise(), side.rotateYCounterclockwise(), side.getOpposite(), Direction.UP, Direction.DOWN };
            boolean opened = false;
            for (Direction d : sides) {
                Vec3d hitVec = getInteractPoint(pos, d);
                BlockHitResult hit = new BlockHitResult(hitVec, d != null ? d : Direction.UP, pos, false);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                mc.player.swingHand(Hand.MAIN_HAND);
                try { Thread.sleep(50 + random.nextInt(80)); } catch (InterruptedException ignored) {}
                if (mc.currentScreen instanceof HandledScreen) { opened = true; break; }
            }
            if (!opened) {
                BlockHitResult centerHit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, centerHit);
                mc.player.swingHand(Hand.MAIN_HAND);
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            }
            blockMovementForInventory(60);
            openTimer.reset();
        }
    }

    private void openWarehouseContainer(BlockPos pos) {
        if (isMovementBlockedByInventory()) return;
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        long delay = getJitteredDelay((long) openRetryDelay.get(), 0.15f);
        if (!openTimer.reached(delay, true)) return;
        double dist = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos));
        if (dist > 3.8) return;
        Direction preferred = getInteractFace(pos);
        Direction[] order = { preferred, preferred.rotateYClockwise(), preferred.rotateYCounterclockwise(), preferred.getOpposite(), Direction.UP };
        for (Direction side : order) {
            Vec3d hitPoint = getWarehouseInteractPoint(pos, side);
            BlockHitResult hit = new BlockHitResult(hitPoint, side, pos, false);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            mc.player.swingHand(Hand.MAIN_HAND);
            break;
        }
        blockMovementForInventory(60);
    }

    private boolean canInteractWithContainer(BlockPos pos) {
        if (pos == null || mc.player == null || mc.world == null) return false;
        Vec3d eye = mc.player.getEyePos();
        Vec3d center = Vec3d.ofCenter(pos);
        if (eye.distanceTo(center) > 4.5) return false;
        Direction side = getInteractFace(pos);
        Vec3d hit = getInteractPoint(pos, side);
        BlockHitResult ray = mc.world.raycast(new RaycastContext(eye, hit,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return ray instanceof BlockHitResult bhr && bhr.getBlockPos().equals(pos);
    }

    private Direction getInteractFace(BlockPos pos) {
        if (mc.player == null) return Direction.UP;
        Vec3d eye = mc.player.getEyePos();
        Vec3d center = Vec3d.ofCenter(pos);
        double dx = eye.x - center.x;
        double dz = eye.z - center.z;
        if (Math.abs(dx) > Math.abs(dz)) return dx > 0 ? Direction.EAST : Direction.WEST;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private Vec3d getInteractPoint(BlockPos pos, Direction side) {
        Vec3d center = Vec3d.ofCenter(pos);
        return switch (side) {
            case EAST -> center.add(0.5, 0.0, 0.0);
            case WEST -> center.add(-0.5, 0.0, 0.0);
            case SOUTH -> center.add(0.0, 0.0, 0.5);
            case NORTH -> center.add(0.0, 0.0, -0.5);
            default -> center;
        };
    }

    private Vec3d getWarehouseInteractPoint(BlockPos pos, Direction side) {
        Vec3d center = Vec3d.ofCenter(pos);
        double inset = 0.38;
        return switch (side) {
            case EAST -> center.add(inset, -0.08, 0.0);
            case WEST -> center.add(-inset, -0.08, 0.0);
            case SOUTH -> center.add(0.0, -0.08, inset);
            case NORTH -> center.add(0.0, -0.08, -inset);
            case UP -> center.add(0.0, 0.2, 0.0);
            default -> center.add(0.0, -0.08, 0.0);
        };
    }

    // ==================== ОСТАЛЬНЫЕ МЕТОДЫ ====================
    private void markChestSkipped(BlockPos chest, long durationMs) {
        recentSkippedChestPos = chest == null ? null : chest.toImmutable();
        recentSkippedChestUntil = System.currentTimeMillis() + durationMs;
    }

    private boolean isRecentlySkippedChest(BlockPos chest) {
        return chest != null && recentSkippedChestPos != null && chest.equals(recentSkippedChestPos) && System.currentTimeMillis() < recentSkippedChestUntil;
    }

    private List<BlockPos> buildChestList(double radius) {
        if (mc.player == null) return new ArrayList<>();
        BlockPos playerPos = mc.player.getBlockPos();
        List<BlockPos> list = new ArrayList<>();
        for (BlockPos pos : PREDEFINED_WARDEN_CHESTS) {
            if (!isInWardenArea(pos)) continue;
            double dx = playerPos.getX() - pos.getX();
            double dz = playerPos.getZ() - pos.getZ();
            if (dx * dx + dz * dz <= radius * radius && !list.contains(pos)) list.add(pos);
        }
        for (BlockPos pos : chestTracker.streamPositions().toList()) {
            if (!isInWardenArea(pos)) continue;
            double dx = playerPos.getX() - pos.getX();
            double dz = playerPos.getZ() - pos.getZ();
            if (dx * dx + dz * dz <= radius * radius && !list.contains(pos)) list.add(pos);
        }
        list.sort(Comparator.comparingDouble(p -> {
            double dx = playerPos.getX() - p.getX();
            double dz = playerPos.getZ() - p.getZ();
            return dx * dx + dz * dz;
        }));
        return list;
    }

    private BlockPos selectBestWardenChest() {
        List<BlockPos> candidates = buildChestList(256);
        if (candidates.isEmpty()) return null;
        if (mc.player == null) return null;
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos bestUntimed = null;
        double bestUntimedScore = Double.MAX_VALUE;
        BlockPos bestTimedReadyForHub = null;
        double bestTimedReadyForHubScore = Double.MAX_VALUE;
        long hubThresholdSeconds = Math.max(1L, (long) hubWaitLead.get());
        for (BlockPos chest : candidates) {
            if (processedChests.contains(chest)) continue;
            if (isRecentlySkippedChest(chest)) continue;
            if (!isContainer(chest)) continue;
            int timer = secondsRemainingNear(chest);
            if (timer > hubThresholdSeconds) continue;
            double dx = playerPos.getX() - chest.getX();
            double dz = playerPos.getZ() - chest.getZ();
            double distSq = dx * dx + dz * dz;
            if (timer <= 0) {
                if (bestUntimed == null || distSq < bestUntimedScore) {
                    bestUntimed = chest;
                    bestUntimedScore = distSq;
                }
            } else {
                double timedScore = timer * 1000.0 + distSq;
                if (bestTimedReadyForHub == null || timedScore < bestTimedReadyForHubScore) {
                    bestTimedReadyForHub = chest;
                    bestTimedReadyForHubScore = timedScore;
                }
            }
        }
        if (bestUntimed != null) return bestUntimed;
        return bestTimedReadyForHub;
    }

    private BlockPos findDepositChest() {
        return switch (warehouseFindMode.get()) {
            case SIGN -> findDepositChestBySign();
            case CONTAINER_NAME -> findDepositChestByContainerName();
        };
    }

    private BlockPos findDepositChestBySign() {
        String expected = normalizeSignText(depositSign.getText());
        if (expected.isEmpty()) return null;
        List<BlockPos> signs = signTracker.streamPositions().toList();
        BlockPos bestChest = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos signPos : signs) {
            String signText = normalizeSignText(signTextAt(signPos));
            if (signText.isEmpty() || !matchesSignText(signText, expected)) continue;
            BlockPos candidate = findContainerAttachedToSign(signPos);
            if (candidate == null) continue;
            double dist = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(candidate));
            if (dist < bestDist) { bestDist = dist; bestChest = candidate; }
        }
        if (bestChest != null) return bestChest;
        if (mc.player == null) return null;
        BlockPos playerPos = mc.player.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(playerPos.add(-48, -6, -48), playerPos.add(48, 6, 48))) {
            BlockState state = mc.world.getBlockState(pos);
            Block block = state.getBlock();
            if (!(block instanceof SignBlock || block instanceof WallSignBlock || block instanceof HangingSignBlock || block instanceof WallHangingSignBlock)) continue;
            String signText = normalizeSignText(signTextAt(pos));
            if (signText.isEmpty() || !matchesSignText(signText, expected)) continue;
            BlockPos candidate = findContainerAttachedToSign(pos);
            if (candidate == null) continue;
            double dist = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(candidate));
            if (dist < bestDist) { bestDist = dist; bestChest = candidate; }
        }
        return bestChest;
    }

    private BlockPos findDepositChestByContainerName() {
        String expected = normalizeSignText(depositContainerName.getText());
        if (expected.isEmpty()) return null;
        if (mc.player == null) return null;
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos bestChest = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.iterate(playerPos.add(-32, -4, -32), playerPos.add(32, 4, 32))) {
            if (!isContainer(pos)) continue;
            double dist = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos));
            if (dist < bestDist) { bestDist = dist; bestChest = pos.toImmutable(); }
        }
        return bestChest;
    }

    private String signTextAt(BlockPos pos) {
        if (mc.world == null) return "";
        if (mc.world.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            StringBuilder sb = new StringBuilder();
            for (Text line : sign.getFrontText().getMessages(false)) sb.append(' ').append(line.getString());
            for (Text line : sign.getBackText().getMessages(false)) sb.append(' ').append(line.getString());
            return Formatting.strip(sb.toString()).trim();
        }
        return "";
    }

    private BlockPos findContainerAttachedToSign(BlockPos signPos) {
        if (mc.world == null) return null;
        BlockState signState = mc.world.getBlockState(signPos);
        Block signBlock = signState.getBlock();
        if (signBlock instanceof WallSignBlock) {
            Direction facing = signState.get(net.minecraft.block.WallSignBlock.FACING);
            BlockPos attached = signPos.offset(facing.getOpposite());
            if (isContainer(attached)) return attached;
            BlockPos front = signPos.offset(facing);
            if (isContainer(front)) return front;
        }
        if (signBlock instanceof WallHangingSignBlock) {
            Direction facing = signState.get(net.minecraft.block.WallHangingSignBlock.FACING);
            BlockPos attached = signPos.offset(facing.getOpposite());
            if (isContainer(attached)) return attached;
            BlockPos front = signPos.offset(facing);
            if (isContainer(front)) return front;
        }
        for (Direction dir : Direction.values()) {
            BlockPos adj = signPos.offset(dir);
            if (isContainer(adj)) return adj;
        }
        for (Direction dir : Direction.Type.HORIZONTAL) {
            for (int step = 2; step <= 3; step++) {
                BlockPos farther = signPos.offset(dir, step);
                if (isContainer(farther)) return farther;
            }
        }
        for (BlockPos pos : BlockPos.iterate(signPos.add(-2, -1, -2), signPos.add(2, 2, 2))) {
            if (isContainer(pos)) return pos.toImmutable();
        }
        return null;
    }

    private boolean isExpectedWarehouseContainer(HandledScreen<?> handled) {
        if (warehouseFindMode.get() == WarehouseFindMode.SIGN) return true;
        String expected = normalizeSignText(depositContainerName.getText());
        if (expected.isEmpty()) return true;
        String title = normalizeSignText(handled.getTitle().getString());
        return matchesSignText(title, expected);
    }

    private boolean matchesSignText(String signText, String expected) {
        if (signText == null || signText.isEmpty() || expected == null || expected.isEmpty()) return false;
        String normalizedSign = normalizeSignText(signText).replaceAll("\\s+", " ").trim();
        String normalizedExpected = normalizeSignText(expected).replaceAll("\\s+", " ").trim();
        if (normalizedSign.contains(normalizedExpected)) return true;
        for (String part : normalizedExpected.split("[,;]+")) {
            String clean = normalizeSignText(part).replaceAll("\\s+", " ").trim();
            if (!clean.isEmpty() && normalizedSign.contains(clean)) return true;
        }
        return false;
    }

    private boolean isContainer(BlockPos pos) {
        if (mc.world == null) return false;
        Block b = mc.world.getBlockState(pos).getBlock();
        return b instanceof ChestBlock || b instanceof TrappedChestBlock || b instanceof BarrelBlock;
    }

    private boolean isInWardenArea(BlockPos pos) {
        int x = Math.abs(pos.getX());
        int z = Math.abs(pos.getZ());
        return x >= 1936 && x <= 2070 && z >= 1936 && z <= 2063 && pos.getY() >= -58 && pos.getY() <= -40;
    }

    private int secondsRemainingNear(BlockPos pos) {
        if (pos == null || mc.world == null) return -1;
        Entity hologram = findChestTimerHologram(pos);
        if (hologram == null) return -1;
        String text = getHologramText(hologram);
        if (text.isEmpty()) return -1;
        int parsed = parseSeconds(text);
        return parsed >= 0 ? parsed : -1;
    }

    private boolean isTimerText(String text) {
        String clean = Formatting.strip(text).toLowerCase().trim();
        if (!clean.matches(".*\\d+.*")) return false;
        return clean.contains(":") || clean.contains("сек") || clean.contains("секунд") || clean.contains("мин") ||
                clean.contains("мину") || clean.matches(".*\\d+\\s*м\\b.*") || clean.matches(".*\\d+\\s*с\\b.*") ||
                clean.contains("sec") || clean.contains("min") || clean.matches(".*\\d+\\s*s\\b.*") ||
                clean.matches(".*\\d+\\s*m\\b.*");
    }

    private int parseSeconds(String text) {
        String clean = Formatting.strip(text).toLowerCase().trim();
        if (clean.isEmpty()) return -1;
        Pattern hms = Pattern.compile("(\\d+):(\\d+):(\\d+)");
        Matcher hmsMatcher = hms.matcher(clean);
        if (hmsMatcher.find()) {
            try { return Integer.parseInt(hmsMatcher.group(1)) * 3600 + Integer.parseInt(hmsMatcher.group(2)) * 60 + Integer.parseInt(hmsMatcher.group(3)); } catch (Exception ignored) {}
        }
        Pattern mmss = Pattern.compile("(\\d+):(\\d{1,2})");
        Matcher mmssMatcher = mmss.matcher(clean);
        if (mmssMatcher.find()) {
            try { return Integer.parseInt(mmssMatcher.group(1)) * 60 + Integer.parseInt(mmssMatcher.group(2)); } catch (Exception ignored) {}
        }
        int total = 0;
        boolean matched = false;
        Matcher hours = Pattern.compile("(\\d+)\\s*(час|часа|часов|hr|h)").matcher(clean);
        while (hours.find()) { total += Integer.parseInt(hours.group(1)) * 3600; matched = true; }
        Matcher minutes = Pattern.compile("(\\d+)\\s*(мин|минута|минуты|минут|м\\b|min|m\\b)").matcher(clean);
        while (minutes.find()) { total += Integer.parseInt(minutes.group(1)) * 60; matched = true; }
        Matcher seconds = Pattern.compile("(\\d+)\\s*(сек|секунда|секунды|секунд|с\\b|s\\b|sec)").matcher(clean);
        while (seconds.find()) { total += Integer.parseInt(seconds.group(1)); matched = true; }
        return matched ? total : -1;
    }

    private Entity findChestTimerHologram(BlockPos chestPos) {
        if (mc.world == null) return null;
        Vec3d chestCenter = Vec3d.ofCenter(chestPos);
        Entity best = null;
        double bestScore = Double.MAX_VALUE;
        for (Entity e : mc.world.getEntities()) {
            String text = getHologramText(e);
            if (text.isEmpty() || !isTimerText(text)) continue;
            Vec3d entityPos = e.getEntityPos();
            double dx = Math.abs(entityPos.x - chestCenter.x);
            double dz = Math.abs(entityPos.z - chestCenter.z);
            double dy = entityPos.y - chestCenter.y;
            if (dx > 2.2 || dz > 2.2) continue;
            if (dy < 0.2 || dy > 5.0) continue;
            double score = dx * dx + dz * dz + Math.abs(dy - 1.8) * 0.35;
            if (score < bestScore) { bestScore = score; best = e; }
        }
        return best;
    }

    private String getHologramText(Entity entity) {
        if (entity == null) return "";
        if (entity.getCustomName() != null) return entity.getCustomName().getString();
        if (entity instanceof DisplayEntity.TextDisplayEntity textDisplay && textDisplay.getText() != null)
            return textDisplay.getText().getString();
        return "";
    }

    private String normalizeSignText(String text) {
        return Formatting.strip(text == null ? "" : text).toLowerCase().trim().replace('ё', 'е');
    }

    private String sanitizeAnarchyNumber() {
        return anarchyNumber.getText() == null ? "" : anarchyNumber.getText().replaceAll("\\D+", "").trim();
    }

    private String formatPos(BlockPos pos) {
        return pos == null ? "null" : "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
    }

    private boolean isInCombat() {
        if (mc.inGameHud == null || mc.inGameHud.getBossBarHud() == null) return false;
        java.util.Map<java.util.UUID, ClientBossBar> bossBars = ((IBossBarHud) mc.inGameHud.getBossBarHud()).client$getBossBars();
        if (bossBars == null || bossBars.isEmpty()) return false;
        for (ClientBossBar bossBar : bossBars.values()) {
            String title = normalizeSignText(bossBar.getName().getString()).replaceAll("\\s+", " ").trim();
            if (title.contains("режим боя") || title.contains("вы в бою") || title.contains("combat tag") ||
                    title.contains("combat logger") || title.matches(".*\\bcombat\\b.*") || title.matches(".*\\bpvp\\b.*") ||
                    title.matches(".*\\bпвп\\b.*")) return true;
        }
        return false;
    }

    private void runAroundWarden() {
        if (mc.player == null) return;
        BlockPos current = mc.player.getBlockPos();
        BlockPos best = null;
        double bestDist = -1.0;
        for (BlockPos pos : PREDEFINED_WARDEN_CHESTS) {
            if (!isInWardenArea(pos)) continue;
            double distSq = current.getSquaredDistance(pos);
            if (distSq > bestDist && distSq <= 96 * 96) { bestDist = distSq; best = pos; }
        }
        if (best != null) walkTo(best);
        else { mc.options.sprintKey.setPressed(true); mc.options.forwardKey.setPressed(true); }
    }

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(java.util.Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    private boolean commandDelayPassed() { return System.currentTimeMillis() >= commandCooldownUntil; }
    private boolean teleportDelayPassed() { return System.currentTimeMillis() >= teleportCooldownUntil; }

    private void sendServerCommand(String command) {
        NetworkUtility.sendCommand(command);
        long now = System.currentTimeMillis();
        long delay = getJitteredDelay((long) postCommandDelay.get(), 0.2f);
        long teleDelay = getJitteredDelay((long) postTeleportDelay.get(), 0.2f);
        commandCooldownUntil = now + delay;
        teleportCooldownUntil = now + Math.max(delay, teleDelay);
    }

    private void sendChatCommandRaw(String message) {
        mc.player.networkHandler.sendChatMessage(message);
        long now = System.currentTimeMillis();
        long delay = getJitteredDelay((long) postCommandDelay.get(), 0.2f);
        long teleDelay = getJitteredDelay((long) postTeleportDelay.get(), 0.2f);
        commandCooldownUntil = now + delay;
        teleportCooldownUntil = now + Math.max(delay, teleDelay);
    }

    private boolean mineIfBlocked(BlockPos target) { return false; }

    private void mineBlock(BlockPos pos) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        int bestSlot = findBestTool(mc.world.getBlockState(pos));
        if (bestSlot != -1 && bestSlot != mc.player.getInventory().getSelectedSlot()) {
            mc.player.getInventory().setSelectedSlot(bestSlot);
            NetworkUtility.send(new UpdateSelectedSlotC2SPacket(bestSlot));
        }
        Direction side = getBreakFace(pos);
        if (!pos.equals(lastMineTarget)) {
            NetworkUtility.send(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, side));
            mc.player.swingHand(Hand.MAIN_HAND);
            lastMineTarget = pos;
        }
        mc.options.attackKey.setPressed(true);
    }

    private int findBestTool(BlockState state) {
        if (mc.player == null) return -1;
        float bestSpeed = 1f;
        int bestSlot = mc.player.getInventory().getSelectedSlot();
        for (int i = 0; i < 9; i++) {
            float speed = mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) { bestSpeed = speed; bestSlot = i; }
        }
        return bestSlot;
    }

    private Direction getBreakFace(BlockPos pos) {
        if (mc.player == null) return Direction.UP;
        Vec3d eye = mc.player.getEyePos();
        Vec3d center = Vec3d.ofCenter(pos);
        double dx = eye.x - center.x, dy = eye.y - center.y, dz = eye.z - center.z;
        double ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
        if (ax > ay && ax > az) return dx > 0 ? Direction.EAST : Direction.WEST;
        if (ay > ax && ay > az) return dy > 0 ? Direction.UP : Direction.DOWN;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private void render2D(Event2D event) {
        if (mc.player == null || mc.world == null) return;
        String status = "AutoWarden | " + currentState.name();
        String rejoinLine = null;
        if (showRejoinTimerHud.get() && currentState == State.WAIT_HUB && targetChestOpenAt > 0L) {
            long remainingMs = Math.max(0L, targetChestOpenAt - System.currentTimeMillis());
            rejoinLine = "Возврат через: " + formatDuration(remainingMs);
        }
        float fontScale = 3.5f;
        float width = Client.RENDERER.textWidth(status, TextureUse.SFMEDIUM, fontScale) + 10;
        if (rejoinLine != null) width = Math.max(width, Client.RENDERER.textWidth(rejoinLine, TextureUse.SFMEDIUM, fontScale) + 10);
        float lineHeight = fontScale + 6;
        float height = rejoinLine != null ? lineHeight * 2 : lineHeight;
        Color bg = new Color(20, 20, 20, 160);
        Client.RENDERER.rect(4, 4, width, height, new Vector4f(2), 1, bg, bg, bg, bg);
        Client.RENDERER.text(Text.literal(status).withColor(0xFFFFFFFF), 7, 7, TextureUse.SFMEDIUM, fontScale);
        if (rejoinLine != null) {
            Client.RENDERER.text(Text.literal(rejoinLine).withColor(0xFFB9FFB9), 7, 7 + lineHeight, TextureUse.SFMEDIUM, fontScale);
        }
    }

    private class ChestTracker extends BlockLocationTracker.BlockPos2State<Boolean> {
        @Override
        public @Nullable Boolean getStateFor(BlockPos pos, BlockState state) {
            if (!isInWardenArea(pos)) return null;
            if (state.isOf(net.minecraft.block.Blocks.CHEST) || state.isOf(net.minecraft.block.Blocks.TRAPPED_CHEST)) return Boolean.TRUE;
            return null;
        }
    }

    private class SignTracker extends BlockLocationTracker.BlockPos2State<Boolean> {
        @Override
        public @Nullable Boolean getStateFor(BlockPos pos, BlockState state) {
            Block b = state.getBlock();
            if (b instanceof SignBlock || b instanceof WallSignBlock || b instanceof HangingSignBlock || b instanceof WallHangingSignBlock) return Boolean.TRUE;
            return null;
        }
    }
}
