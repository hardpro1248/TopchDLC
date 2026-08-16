package gg.topchdlc.vse.shutki.module.modules.impl.player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.MovementCorrection;
import gg.topchdlc.vse.rotation.all.Rotation;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import gg.topchdlc.vse.utils.math.Priorities;
import gg.topchdlc.vse.utils.math.RotationUtility;
import gg.topchdlc.vse.utils.network.NetworkUtility;
import gg.topchdlc.vse.utils.player.PathExecutor;
import gg.topchdlc.vse.utils.player.Pathfinder;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AutoMine extends Module {
    public static final AutoMine INSTANCE = new AutoMine();

    private final EnumSetting<VersionMode> versionMode = enumSetting("Anarchy Version", VersionMode.BOTH);

    public enum VersionMode {
        V1_16_5,
        V1_21,
        BOTH
    }

    private static final int MIN_X = -84, MAX_X = -66;
    private static final int MIN_Z = -3, MAX_Z = 14;
    private static final int INNER_MIN_X = -82, INNER_MAX_X = -68;
    private static final int INNER_MIN_Z = -1, INNER_MAX_Z = 12;
    private static final int TARGET_Y = 73;
    private static final int MIN_Y = 73, MAX_Y = 73;
    private static final BlockPos WARP_SPAWN = new BlockPos(-58, 82, -2);
    private static final BlockPos MINE_ENTRY = new BlockPos(-71, 82, 2);

    private enum State {
        IDLE,
        WAITING_ANARCHY,
        TELEPORTING_WARP,
        WALKING_TO_MINE,
        DIGGING_DOWN,
        CLEARING_LAYER
    }

    private State currentState = State.IDLE;
    private final PathExecutor pathExecutor = new PathExecutor();

    private BlockPos breakTarget = null;
    private BlockPos lastAttackTarget = null;

    private int currentAnarchy = -1;
    private volatile int pendingAnarchy = -1;
    private long lastSwitchTimestamp = 0;
    private int stateTicks = 0;

    private Thread tgThread;

    private static final Rotation SMOOTH_ROTATION = (cur, tgt) -> {
        float step = 12f;
        float dy = MathHelper.clamp(MathHelper.wrapDegrees(tgt.getYaw() - cur.getYaw()), -step, step);
        float dp = MathHelper.clamp(tgt.getPitch() - cur.getPitch(), -step, step);
        return new Angle(cur.getYaw() + dy, MathHelper.clamp(cur.getPitch() + dp, -90f, 90f));
    };

    public AutoMine() {
        super("AutoMine", Category.PLAYER, "x");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        if (mc.player == null || mc.world == null) return;

        stateTicks = 0;
        breakTarget = null;
        pendingAnarchy = -1;
        lastSwitchTimestamp = 0;

        checkPositionAndStart();
        startTgListener();
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        pathExecutor.stop();
        currentState = State.IDLE;
        if (tgThread != null && tgThread.isAlive()) {
            tgThread.interrupt();
        }
        stopMovementKeys();
        if (mc.options != null) {
            mc.options.attackKey.setPressed(false);
        }
    }

    EventBus<Event> events = e -> {
        if (e instanceof EventGameTick) onTick();
    };

    private void checkPositionAndStart() {
        if (mc.player == null) return;

        BlockPos playerPos = mc.player.getBlockPos();

        if (isInZone(playerPos)) {
            if (playerPos.getY() <= TARGET_Y) {
                ChatUtility.send(Text.literal("§a[AutoMine] Уже на высоте Y=73! Начинаем расчистку слоя."));
                currentState = State.CLEARING_LAYER;
            } else {
                ChatUtility.send(Text.literal("§a[AutoMine] Внутри углов шахты! Копаем под себя до Y=73."));
                pathExecutor.stop();
                stopMovementKeys();
                currentState = State.DIGGING_DOWN;
            }
        } else {
            executeWarpMine();
        }
    }

    private void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (pendingAnarchy != -1) {
            int target = pendingAnarchy;
            pendingAnarchy = -1;

            long elapsed = System.currentTimeMillis() - lastSwitchTimestamp;
            if (target != currentAnarchy && target >= 100 && elapsed >= 15000) {
                currentAnarchy = target;
                lastSwitchTimestamp = System.currentTimeMillis();

                pathExecutor.stop();
                stopMovementKeys();

                sendAnarchyJoinCommand(target);
                return;
            }
        }
        switch (currentState) {
            case WAITING_ANARCHY -> {
                pathExecutor.stop();
                stopMovementKeys();
                stateTicks++;
                if (stateTicks >= 60 && mc.currentScreen == null) {
                    ChatUtility.send(Text.literal("§a[AutoMine] Мир прогружен. Телепортируемся на /warp mine"));
                    executeWarpMine();
                }
            }

            case TELEPORTING_WARP -> {
                pathExecutor.stop();
                stopMovementKeys();
                stateTicks++;
                BlockPos pPos = mc.player.getBlockPos();
                if (isInZone(pPos)) {
                    stateTicks = 0;
                    checkPositionAndStart();
                    return;
                }
                if (pPos.isWithinDistance(WARP_SPAWN, 4.0)) {
                    stateTicks = 0;
                    ChatUtility.send(Text.literal("§a[AutoMine] Успешно на варпе (-58). Бежим внутрь шахты..."));
                    startPathToZone();
                    return;
                }
                if (stateTicks >= 120) {
                    stateTicks = 0;
                    ChatUtility.send(Text.literal("§c[AutoMine] Повторный запрос /warp mine..."));
                    executeWarpMine();
                }
            }

            case WALKING_TO_MINE -> {
                BlockPos playerPos = mc.player.getBlockPos();
                if (isInsideInnerZone(playerPos) || (isInZone(playerPos) && playerPos.getY() <= TARGET_Y)) {
                    pathExecutor.stop();
                    stopMovementKeys();
                    if (playerPos.getY() <= TARGET_Y) {
                        ChatUtility.send(Text.literal("§a[AutoMine] Спустились на Y=73! Очищаем слой."));
                        currentState = State.CLEARING_LAYER;
                    } else {
                        ChatUtility.send(Text.literal("§a[AutoMine] Вступили в шахту (Y=" + playerPos.getY() + ")! Копаем под себя до 73."));
                        currentState = State.DIGGING_DOWN;
                    }
                    return;
                }

                if (pathExecutor.isStuck()) {
                    pathExecutor.clearStuck();
                    startPathToZone();
                    return;
                }

                if (!pathExecutor.isActive() || pathExecutor.isCompleted()) {
                    startPathToZone();
                } else {
                    pathExecutor.tick();
                }
            }

            case DIGGING_DOWN -> {
                stopMovementKeys();
                if (mc.player.getBlockY() <= TARGET_Y) {
                    ChatUtility.send(Text.literal("§a[AutoMine] Достигли высоты Y=73! Начинаем выкапывать слой 73."));
                    currentState = State.CLEARING_LAYER;
                    mc.options.attackKey.setPressed(false);
                    return;
                }

                BlockPos underPos = mc.player.getBlockPos().down();
                BlockState underState = mc.world.getBlockState(underPos);

                if (!underState.isAir() && underState.getHardness(mc.world, underPos) >= 0) {
                    Vec3d targetVec = Vec3d.ofCenter(underPos);
                    Angle angle = RotationUtility.calculate(targetVec);
                    Client.ROTATION.rotate(SMOOTH_ROTATION, angle, 2, true, MovementCorrection.SILENT, Priorities.FLY);
                    Angle currentAngle = Client.ROTATION.getRotate();
                    float pitchDiff = Math.abs(angle.getPitch() - currentAngle.getPitch());
                    if (pitchDiff < 10f || currentAngle.getPitch() > 75f) {
                        mineBlock(underPos);
                    } else {
                        mc.options.attackKey.setPressed(false);
                    }
                } else {
                    mc.options.attackKey.setPressed(false);
                }
            }

            case CLEARING_LAYER -> {
                if (!isInZone(mc.player.getBlockPos())) {
                    stopMovementKeys();
                    ChatUtility.send(Text.literal("§c[AutoMine] Вышли за углы шахты! Возвращаемся."));
                    currentState = State.WALKING_TO_MINE;
                    return;
                }

                if (breakTarget != null && !isValidBlockToMine(breakTarget)) {
                    breakTarget = null;
                }

                if (breakTarget == null) {
                    breakTarget = findNextBlockToClear();
                }

                if (breakTarget == null) {
                    stopMovementKeys();
                    mc.options.attackKey.setPressed(false);
                    return;
                }

                Vec3d targetVec = Vec3d.ofCenter(breakTarget);
                Angle angle = RotationUtility.calculate(targetVec);
                Client.ROTATION.rotate(SMOOTH_ROTATION, angle, 2, true, MovementCorrection.STRICT, Priorities.NORMAL);
                Angle currentAngle = Client.ROTATION.getRotate();
                float yawDiff = Math.abs(MathHelper.wrapDegrees(angle.getYaw() - currentAngle.getYaw()));
                float pitchDiff = Math.abs(angle.getPitch() - currentAngle.getPitch());
                double distToBlock = mc.player.getEyePos().distanceTo(targetVec);
                if (distToBlock > 2.0) {
                    mc.options.forwardKey.setPressed(true);
                } else {
                    mc.options.forwardKey.setPressed(false);
                }
                if (yawDiff < 10f && pitchDiff < 10f) {
                    mineBlock(breakTarget);
                } else {
                    mc.options.attackKey.setPressed(false);
                }
            }
        }
    }

    private void sendAnarchyJoinCommand(int targetNum) {
        String cmd = "an" + targetNum;
        NetworkUtility.sendCommand(cmd);
        ChatUtility.send(Text.literal("§a[AutoMine] Переход на анку: /" + cmd));

        currentState = State.WAITING_ANARCHY;
        stateTicks = 0;
    }

    private void executeWarpMine() {
        NetworkUtility.sendCommand("warp mine");
        ChatUtility.send(Text.literal("§a[AutoMine] Вызов /warp mine"));

        currentState = State.TELEPORTING_WARP;
        stateTicks = 0;
    }

    private void startPathToZone() {
        List<BlockPos> path = Pathfinder.findPath(mc.player.getBlockPos(), MINE_ENTRY);
        if (path != null && !path.isEmpty()) {
            pathExecutor.start(path, MINE_ENTRY);
            currentState = State.WALKING_TO_MINE;
        }
    }

    private void stopMovementKeys() {
        if (mc.options != null) {
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
        }
    }

    private void mineBlock(BlockPos pos) {
        int bestSlot = findBestTool(mc.world.getBlockState(pos));
        if (bestSlot != -1 && bestSlot != mc.player.getInventory().getSelectedSlot()) {
            mc.player.getInventory().setSelectedSlot(bestSlot);
            NetworkUtility.send(new UpdateSelectedSlotC2SPacket(bestSlot));
        }

        Direction side = getBreakFace(pos);

        if (!pos.equals(lastAttackTarget)) {
            NetworkUtility.send(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, side));
            mc.player.swingHand(Hand.MAIN_HAND);
            lastAttackTarget = pos;
        }

        mc.options.attackKey.setPressed(true);
    }

    private BlockPos findNextBlockToClear() {
        List<BlockPos> validBlocks = new ArrayList<>();
        BlockPos playerPos = mc.player.getBlockPos();

        for (int x = MIN_X; x <= MAX_X; x++) {
            for (int y = MIN_Y; y <= MAX_Y; y++) {
                for (int z = MIN_Z; z <= MAX_Z; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isValidBlockToMine(pos)) {
                        validBlocks.add(pos);
                    }
                }
            }
        }

        return validBlocks.stream()
                .min(Comparator.comparingDouble(p -> playerPos.getSquaredDistance(p)))
                .orElse(null);
    }
    private boolean isInZone(BlockPos pos) {
        return pos.getX() >= MIN_X && pos.getX() <= MAX_X &&
                pos.getZ() >= MIN_Z && pos.getZ() <= MAX_Z;
    }
    private boolean isInsideInnerZone(BlockPos pos) {
        return pos.getX() >= INNER_MIN_X && pos.getX() <= INNER_MAX_X &&
                pos.getZ() >= INNER_MIN_Z && pos.getZ() <= INNER_MAX_Z;
    }
    private boolean isValidBlockToMine(BlockPos pos) {
        if (mc.world == null) return false;
        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir() || state.getHardness(mc.world, pos) < 0) return false;

        return isInZone(pos) && pos.getY() >= MIN_Y && pos.getY() <= MAX_Y;
    }

    private Direction getBreakFace(BlockPos pos) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d center = Vec3d.ofCenter(pos);
        double dx = eye.x - center.x, dy = eye.y - center.y, dz = eye.z - center.z;
        double ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
        if (ax > ay && ax > az) return dx > 0 ? Direction.EAST : Direction.WEST;
        if (ay > ax && ay > az) return dy > 0 ? Direction.UP : Direction.DOWN;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private int findBestTool(BlockState state) {
        float bestSpeed = 1f;
        int bestSlot = mc.player.getInventory().getSelectedSlot();
        for (int i = 0; i < 9; i++) {
            float speed = mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private static class MineData {
        int anarchy;
        int timeLeft;
        MineData(int anarchy, int timeLeft) {
            this.anarchy = anarchy;
            this.timeLeft = timeLeft;
        }
    }
    private void startTgListener() {
        tgThread = new Thread(() -> {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            while (!Thread.currentThread().isInterrupted() && isEnabled()) {
                try {
                    long timeSinceLastSwitch = System.currentTimeMillis() - lastSwitchTimestamp;
                    if (timeSinceLastSwitch < 15000) {
                        Thread.sleep(1000);
                        continue;
                    }

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://127.0.0.1:8080"))
                            .timeout(Duration.ofSeconds(3))
                            .GET()
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        String json = response.body();
                        List<MineData> mines = new ArrayList<>();

                        JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();
                        for (JsonElement element : jsonArray) {
                            JsonObject obj = element.getAsJsonObject();

                            int anarchyNum = obj.get("anarchy").getAsInt();
                            int secondsLeft = obj.get("time_left").getAsInt();

                            if (anarchyNum < 100) continue;

                            boolean matchesVersion = switch (versionMode.get()) {
                                case V1_16_5 -> anarchyNum >= 1000;
                                case V1_21 -> anarchyNum < 1000;
                                case BOTH -> true;
                            };

                            if (matchesVersion) {
                                mines.add(new MineData(anarchyNum, secondsLeft));
                            }
                        }
                        MineData bestMine = mines.stream()
                                .min(Comparator.comparingInt(m -> m.timeLeft))
                                .orElse(null);

                        if (bestMine != null && bestMine.timeLeft <= 4) {
                            if (bestMine.anarchy != currentAnarchy) {
                                pendingAnarchy = bestMine.anarchy;
                            }
                        }
                    }
                } catch (Exception ignored) {}

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        tgThread.setDaemon(true);
        tgThread.start();
    }
}