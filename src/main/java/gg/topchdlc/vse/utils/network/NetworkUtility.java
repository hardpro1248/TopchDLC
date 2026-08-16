package gg.topchdlc.vse.utils.network;

import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventPacket;
import gg.topchdlc.api.events.list.sisi.exis;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.mixin.accessor.ClientConnectionAccessor;
import gg.topchdlc.mixin.accessor.IClientWorld;
import gg.topchdlc.vse.utils.math.TimeUtility;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@UtilityClass
public class NetworkUtility implements MinecraftHolder {
    private boolean shouldTriggerEvent = true;
    private boolean serverSprinting = false;
    public boolean blockInventoryPackets = false;
    public boolean interceptInventory = false;
    @Getter
    private float tpsFactor = 0;

    private int received = 0;
    private long lastReceive = 0;
    private TimeUtility tpsTimer = new TimeUtility();
    private final Queue<Packet<?>> queuedPackets = new ConcurrentLinkedQueue<>();
    private boolean isQueuingPackets = false;
    public void pauseEvents() {
        shouldTriggerEvent = false;
    }
    public void resumeEvents() {
        shouldTriggerEvent = true;
    }
    private boolean serverOnGround, serverSneaking, serverHorizontalCollision;
    public boolean shouldTriggerEvent() {
        return shouldTriggerEvent;
    }
    private float sprintingChangeTicks;
    public static boolean sendingSilent = false;
    public void updateServerSprint(boolean sprint) {
        serverSprinting = sprint;
    }
    public boolean serverSprinting() {
        return serverSprinting;
    }
    /// CODING NENE DLA SLABIH
    public void startQueuingPackets() {
        isQueuingPackets = true;
        queuedPackets.clear();
    }
    public void stopQueuingAndFlush() {
        isQueuingPackets = false;
        while (!queuedPackets.isEmpty()) {
            Packet<?> packet = queuedPackets.poll();
            if (packet != null && mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendPacket(packet);
            }
        }
    }
    
    public void stopQueuingAndFlushSequential() {
        isQueuingPackets = false;
        if (queuedPackets.isEmpty()) return;
        
        List<Packet<?>> packets = new ArrayList<>(queuedPackets);
        queuedPackets.clear();
        
        sendPacketsSequentially(packets, 0);
    }

    private void sendPacketsSequentially(List<Packet<?>> packets, int index) {
        if (index >= packets.size() || mc.getNetworkHandler() == null) return;
        
        Packet<?> packet = packets.get(index);
        if (packet != null) {
            mc.getNetworkHandler().sendPacket(packet);
        }
        
        if (index + 1 < packets.size()) {
            mc.execute(() -> sendPacketsSequentially(packets, index + 1));
        }
    }
    public void sendCommand(String cmd) {
        if (mc.getNetworkHandler() == null) return;
        String command = cmd.startsWith("/") ? cmd.substring(1) : cmd;
        mc.getNetworkHandler().sendChatCommand(command);
    }
    public boolean isQueuingPackets() {
        return isQueuingPackets;
    }
    public void queuePacket(Packet<?> packet) {
        if (isQueuingPackets) {
            queuedPackets.offer(packet);
        }
    }
    public int getQueuedPacketCount() {
        return queuedPackets.size();
    }
    private boolean shouldQueuePacket(Packet<?> packet) {
        if (!isInventoryPacket(packet)) return false;
        return isQueuingPackets;
    }
    private boolean isInventoryPacket(Packet<?> packet) {
        return packet instanceof ClickSlotC2SPacket ||
               packet instanceof UpdateSelectedSlotC2SPacket ||
               packet instanceof CreativeInventoryActionC2SPacket ||
               packet instanceof ButtonClickC2SPacket ||
               packet instanceof SelectMerchantTradeC2SPacket ||
               packet instanceof RenameItemC2SPacket;
    }
    public void sendAngle(Angle angle) {
        send(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), angle.getYaw(), angle.getPitch(), mc.player.isOnGround(), mc.player.horizontalCollision));
    }
    public void sendLook(Angle angle) {
        send(new PlayerMoveC2SPacket.LookAndOnGround(angle.getYaw(), angle.getPitch(), mc.player.isOnGround(), mc.player.horizontalCollision));
    }
    public void sendWithoutEvent(Runnable runnable) {
        pauseEvents();
        runnable.run();
        resumeEvents();
    }
    public void sendWithoutEvent(Packet<?> packet) {
        pauseEvents();
        send(packet);
        resumeEvents();
    }
    public void sendSilentPackets(Packet<?> packet) {
        if (mc.getNetworkHandler() == null) return;
        try {
            sendingSilent = true;
            mc.getNetworkHandler().sendPacket(packet);
        } finally {
            sendingSilent = false;
        }
    }

    public void send(Packet<?> packet) {
        if (mc.getNetworkHandler() == null) return;
        if (shouldQueuePacket(packet)) {
            queuedPackets.offer(packet);
            return;
        }
        if (packet instanceof ClickSlotC2SPacket click) {
            mc.interactionManager.clickSlot(click.syncId(), click.slot(), click.button(), click.actionType(), mc.player);
        } else {
            mc.getNetworkHandler().sendPacket(packet);
        }
    }
    public void sendInputPacket(boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean sneak, boolean sprint) {
        PlayerInput input = new PlayerInput(forward, backward, left, right, jump, sneak, sprint);
        mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(input));
    }
    public void sendOnlySneak(boolean sneak) {
        PlayerInput playerInput = mc.player.input.playerInput;
        sendInputPacket(playerInput.forward(), playerInput.backward(), playerInput.left(), playerInput.right(), playerInput.jump(), sneak, playerInput.sprint());
    }
    public void sendUse(Hand hand) {
        sendUse(hand, Client.ROTATION.getRotate());
    }
    public void sendUse(Hand hand, Angle angle) {
        try (PendingUpdateManager pendingUpdateManager = ((IClientWorld)mc.world).client$pending().incrementSequence()) {
            int i = pendingUpdateManager.getSequence();
            PlayerInteractItemC2SPacket packet = new PlayerInteractItemC2SPacket(hand, i, angle.getYaw(), angle.getPitch());
            NetworkUtility.send(packet);
        }
    }
    public void sendUse(Hand hand, BlockHitResult hitResult) {
        try (PendingUpdateManager pendingUpdateManager = ((IClientWorld)mc.world).client$pending().incrementSequence()) {
            int i = pendingUpdateManager.getSequence();
            PlayerInteractBlockC2SPacket packet = new PlayerInteractBlockC2SPacket(hand, hitResult, i);
            NetworkUtility.send(packet);
        }
    }
    public boolean is(String server) {
        return mc.getNetworkHandler() != null && mc.getNetworkHandler().getServerInfo() != null && mc.getNetworkHandler().getServerInfo().address.contains(server);
    }

    public void handleCPacket(Packet<?> packet) {
        if (packet instanceof PlayerMoveC2SPacket e) {
            PlayerState.lastGround = e.isOnGround();
            PlayerState.lastVertical = mc.player.verticalCollision;
        }
    }
    public void handleSPacket(Packet<?> packet) {
        if (packet instanceof WorldTimeUpdateS2CPacket e) {
            lastReceive = System.currentTimeMillis();
        }
    }

    public void handlePacket(Packet<?> packet) {
        if (!(mc.getNetworkHandler() instanceof ClientPlayNetworkHandler net)) return;
        if (mc.isOnThread()) {
            ClientConnectionAccessor.handlePacket(packet, net);
        } else {
            mc.execute(() -> ClientConnectionAccessor.handlePacket(packet, net));
        }
    }
    public void onPacketSend(final EventPacket x, exis e) {
        if (x.getPacket() instanceof ClientCommandC2SPacket command) {
            if (command.getMode().equals(ClientCommandC2SPacket.Mode.START_SPRINTING)) {
                e.cancelEvent(serverSprinting);
                if (!e.cancelEvent()) sprintingChangeTicks = 0;
                serverSprinting = true;
            } else if (command.getMode().equals(ClientCommandC2SPacket.Mode.STOP_SPRINTING)) {
                e.cancelEvent(!serverSprinting);
                if (!e.cancelEvent()) sprintingChangeTicks = 0;
                serverSprinting = false;
            }
        }
    }
    public void sendSilentPacket(Packet<?> packet) {
        List<Packet<?>> silentPackets = new ArrayList<>();
        silentPackets.add(packet);
        mc.getNetworkHandler().sendPacket(packet);
    }

    @UtilityClass
    public class PlayerState {
        public boolean lastGround = false, lastVertical = false;
        public int lastTp = 0;
    }
    public UUID offlineUUID(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }
    static { Client.EVENTS.register(new HandlerHelper()); }
    private class HandlerHelper {
        EventBus<Event> events = event -> {
            if (event instanceof EventGameTick) {
                if (tpsTimer.reached(1000, true)) {
                    tpsFactor = 20 - (1000.0f / ((System.currentTimeMillis() - lastReceive) * 50.0f));
                }
            }
        };
    }
}
