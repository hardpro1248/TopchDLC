package gg.topchdlc.vse.shutki.module.modules.impl.movement.timer;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.list.*;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.utils.network.NetworkUtility;

import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.PlayerInput;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TimerSpooky extends Choice {

    private static final float CHARGE_TIMER = 0.05F;
    private static final float BOOST_TIMER = 1.7F;
    private static final long CHARGE_DURATION_NANOS = 1_250_000_000L;
    private static final long MAX_BOOST_DURATION_NANOS = 2_400_000_000L;
    private static final int FULL_BOOST_JUMPS = 4;

    private Phase phase = Phase.CHARGING;
    private boolean cycleActive;
    private boolean airborne;
    private int completedJumps;
    private int groundTicks;
    private long phaseStartedAt;

    private final Queue<CommonPongC2SPacket> delayedTransactions = new ConcurrentLinkedQueue<>();

    public TimerSpooky() {
        super("TimerGrimSP");
    }

    @Override
    public void onEvent(Event event) {
        if (mc.player == null) return;

        if (NetworkUtility.sendingSilent) return;

        if (cycleActive) {
            if (event instanceof EventSendPacket sendEvent && sendEvent.getPacket() instanceof CommonPongC2SPacket packet) {
                delayedTransactions.add(packet);
                sendEvent.cancel();
                return;
            }

            if (event instanceof EventReceivePacket receiveEvent && receiveEvent.getPacket() instanceof PlayerPositionLookS2CPacket) {
                beginCharging();
            }
        }

        if (event instanceof EventInput eventInput) {
            if (!isMoving()) {
                groundTicks = 0;
                stopCycle();
                return;
            }

            if (phase == Phase.BOOSTING) {
                groundTicks = mc.player.verticalCollision ? groundTicks + 1 : 0;

                eventInput.setSprint(true);
                if (groundTicks > 0) {
                    eventInput.setJump(true);
                }
            }
        }

        if (event instanceof EventGameTick) {
            if (!isMoving()) {
                stopCycle();
                return;
            }

            long now = System.nanoTime();

            if (!cycleActive) {
                cycleActive = true;
                beginCharging();
                return;
            }

            if (phase == Phase.BOOSTING) {
                if (mc.player.verticalCollision) {
                    if (airborne) {
                        airborne = false;
                        completedJumps++;
                    }
                } else {
                    airborne = true;
                    if (completedJumps >= FULL_BOOST_JUMPS && mc.player.getVelocity().y <= 0.0D) {
                        beginCharging();
                        return;
                    }
                }
            }

            if (phase == Phase.CHARGING && now - phaseStartedAt >= CHARGE_DURATION_NANOS) {
                beginBoosting();
            } else if (phase == Phase.BOOSTING && now - phaseStartedAt >= MAX_BOOST_DURATION_NANOS) {
                beginCharging();
            }
        }
    }

    private boolean isMoving() {
        if (mc.player == null || mc.player.input == null) return false;

        PlayerInput input = mc.player.input.playerInput;
        return input.forward() || input.backward() || input.left() || input.right();
    }

    private void beginCharging() {
        flushTransactions();
        phase = Phase.CHARGING;
        airborne = false;
        completedJumps = 0;
        groundTicks = 0;
        phaseStartedAt = System.nanoTime();
        Client.TIMER = CHARGE_TIMER;
    }

    private void beginBoosting() {
        phase = Phase.BOOSTING;
        airborne = false;
        completedJumps = 0;
        groundTicks = 0;
        phaseStartedAt = System.nanoTime();
        Client.TIMER = BOOST_TIMER;
    }

    private void stopCycle() {
        flushTransactions();
        cycleActive = false;
        phase = Phase.CHARGING;
        airborne = false;
        completedJumps = 0;
        groundTicks = 0;
        phaseStartedAt = 0L;
        Client.TIMER = 1.0f;
    }

    private void flushTransactions() {
        CommonPongC2SPacket packet;
        while ((packet = delayedTransactions.poll()) != null) {
            NetworkUtility.sendSilentPackets(packet);
        }
    }

    @Override
    public void onEnabled() {
        stopCycle();
    }

    @Override
    public void onDisabled() {
        stopCycle();
    }

    private enum Phase {
        CHARGING,
        BOOSTING
    }
}