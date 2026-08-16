package gg.topchdlc.vse.shutki.module.modules.impl.movement.flight;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventReceivePacket;
import gg.topchdlc.api.events.list.EventSendPacket;
import gg.topchdlc.vse.shutki.module.modules.impl.movement.Flight;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.utils.player.MoveUtility;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class NCPFly extends Choice {
    public NCPFly() {
        super("NCP");
    }

    private final CheckBox noVanillaKick = checkbox("Не кикать на ваниле", false);

    private final Set<PlayerMoveC2SPacket> allowedPackets = new HashSet<>();
    private final HashMap<Integer, Vec3d> allowedPositionsAndIDs = new HashMap<>();
    private int tpID = -1;

    @Override
    public void onEnabled() {
        tpID = -1;
        allowedPackets.clear();
        allowedPositionsAndIDs.clear();
        if (mc.player != null) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(mc.player.getYaw(), 90.0f, false, false));
        }
    }

    @Override
    public void onDisabled() {
        allowedPackets.clear();
        allowedPositionsAndIDs.clear();
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventGameTick) {
            if (mc.player == null || mc.world == null) return;

            if (allowedPackets.size() >= 400) {
                PlayerMoveC2SPacket firstKey = allowedPackets.iterator().next();
                if (firstKey != null) {
                    allowedPackets.remove(firstKey);
                }
            }

            double motionX = 0.0;
            double motionY = 0.0;
            double motionZ = 0.0;
            double ySpeed = 0.0624;
            double moveSpeed = 0.2543;

            int ticksFlying = mc.player.age;
            boolean hasMotion = MoveUtility.hasMovement(mc.player.input.playerInput) && !mc.options.jumpKey.isPressed();
            boolean tickBoost = hasMotion ? (ticksFlying % 4 == 3) : (ticksFlying % 5 == 2);
            boolean tickFlyTickDown = noVanillaKick.get() && ticksFlying % 60 >= 58;
            boolean tickFlyTickUp = noVanillaKick.get() && !tickFlyTickDown && ticksFlying % 60 >= 56;

            int boostFactor = tickBoost ? 2 : 1;

            if (mc.options.jumpKey.isPressed() && ticksFlying > 1) {
                motionY = ySpeed;
                mc.player.setVelocity(mc.player.getVelocity().x, 0, mc.player.getVelocity().z);
            } else {
                if (mc.player.input.playerInput.sneak()) {
                    motionY = -ySpeed;
                } else {
                    motionY = 0.0;
                }
            }

            boolean antiKicking = false;
            if ((tickFlyTickUp || tickFlyTickDown) && motionY == 0.0 || motionY > 0.0 && tickFlyTickDown || motionY < 0.0 && tickFlyTickUp) {
                motionY = tickFlyTickUp ? 0.05 : -0.05;
                antiKicking = true;
            }

            if (hasMotion) {
                double motionYaw = Math.toRadians(MoveUtility.getdir());
                motionX = -Math.sin(motionYaw) * moveSpeed;
                motionZ = Math.cos(motionYaw) * moveSpeed;
            } else {
                boostFactor += boostFactor > 1 && !antiKicking ? 1 : 0;
            }

            mc.player.setVelocity(motionX * boostFactor, motionY * boostFactor, motionZ * boostFactor);

            if (ticksFlying == 0 || motionX != 0.0 || motionY != 0.0 || motionZ != 0.0) {
                sendMovePackets(motionX, motionY, motionZ, boostFactor);
            }
        }

        if (event instanceof EventSendPacket e) {
            if (e.getPacket() instanceof PlayerMoveC2SPacket packet) {
                if (!allowedPackets.contains(packet)) {
                    e.cancel();
                }
            }
        }

        if (event instanceof EventReceivePacket e) {
            if (e.getPacket() instanceof PlayerPositionLookS2CPacket packet) {
                if (mc.player == null) return;

                Vec3d changePos = packet.change().position();
                Vec3d rubberBandPos = new Vec3d(changePos.x, changePos.y, changePos.z);

                if (rubberBandPos.distanceTo(mc.player.getEntityPos()) > 8.0) {
                    Flight.INSTANCE.toggle();
                    return;
                }

                if (allowedPositionsAndIDs.containsKey(packet.teleportId()) &&
                        allowedPositionsAndIDs.get(packet.teleportId()).equals(rubberBandPos)) {
                    allowedPositionsAndIDs.remove(packet.teleportId());
                    mc.player.networkHandler.sendPacket(new TeleportConfirmC2SPacket(packet.teleportId()));
                    e.cancel();
                    return;
                }

                tpID = packet.teleportId();
                mc.player.networkHandler.sendPacket(new TeleportConfirmC2SPacket(packet.teleportId()));
            }
        }
    }

    private void sendMovePackets(double motionX, double motionY, double motionZ, int factor) {
        if (mc.player == null) return;

        for (int i = 1; i < factor + 1; i++) {
            Vec3d pos = mc.player.getEntityPos().add(motionX * i, motionY * i, motionZ * i);

            PlayerMoveC2SPacket.PositionAndOnGround packet = new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true, false);
            PlayerMoveC2SPacket.PositionAndOnGround bounds = new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y + 512.0, pos.z, true, false);

            allowedPackets.add(packet);
            allowedPackets.add(bounds);

            mc.player.networkHandler.sendPacket(packet);
            mc.player.networkHandler.sendPacket(bounds);

            if (tpID < 0) break;

            tpID++;
            mc.player.networkHandler.sendPacket(new TeleportConfirmC2SPacket(tpID));
            allowedPositionsAndIDs.put(tpID, pos);
        }
    }
}
