package gg.topchdlc.vse.shutki.module.modules.impl.combat;

import gg.topchdlc.api.events.list.*;
import gg.topchdlc.mixin.accessor.IClientPlayerEntity;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;

public class Criticals extends Module {
    public static final Criticals INSTANCE = new Criticals();

    public Criticals() {
        super("Criticals", Category.COMBAT, "^&");
    }

    public final EnumSetting<Mode> mode = enumSetting("Mode", Mode.UpdatedNCP);
    public static boolean cancelCrit;

    private boolean shouldCrit;

    @EventHandler
    public void onPacketSend(EventSendPacket event) {
        if (event.getPacket() instanceof PlayerInteractEntityC2SPacket packet && getInteractType(packet) == InteractType.ATTACK) {
            if (mode.is(Mode.MiniJump)) return;

            Entity ent = getEntity(packet);
            if (ent == null || ent instanceof EndCrystalEntity || cancelCrit)
                return;

            if (mode.is(Mode.Rw)) {
                shouldCrit = true;
            } else {
                doCrit();
            }
        }
    }

    @EventHandler
    public void onPostMotion(EventPostMotion event) {
        if (shouldCrit && mode.is(Mode.Rw)) {
            doCrit();
            shouldCrit = false;
        }
    }

    public void doCrit() {
        if (isDisabled() || mc.player == null || mc.world == null)
            return;

        boolean canCrit = mc.player.isOnGround() || mc.player.getAbilities().flying || mode.is(Mode.Grim) || (mode.is(Mode.Rw) && isInCobweb());

        if (canCrit && !mc.player.isInLava() && !mc.player.isSubmergedInWater()) {
            switch (mode.get()) {
                case Rw -> {
                    if (isInCobweb()) {
                        critPacket(0.0625, false);
                        critPacket(0.0015, false);
                        critPacket(0.0, false);
                    } else {
                        critPacket(0.000000271875, false);
                        critPacket(0., false);
                    }
                }
                case MiniJump -> {
                    if (mc.player.isOnGround()) {
                        Vec3d currentVel = mc.player.getVelocity();
                        mc.player.setVelocity(currentVel.x, 0.16, currentVel.z);
                    }
                }
                case OldNCP -> {
                    critPacket(0.00001058293536, false);
                    critPacket(0.00000916580235, false);
                    critPacket(0.00000010371854, false);
                }
                case Ncp -> {
                    critPacket(0.0625D, false);
                    critPacket(0., false);
                }
                case UpdatedNCP -> {
                    critPacket(0.000000271875, false);
                    critPacket(0., false);
                }
                case Strict -> {
                    critPacket(0.062600301692775, false);
                    critPacket(0.07260029960661, false);
                    critPacket(0., false);
                    critPacket(0., false);
                }
                case Grim -> {
                    if (!mc.player.isOnGround())
                        critPacket(-0.000001, true);
                }
            }
        }
    }

    public boolean isInCobweb() {
        return mc.world.getBlockState(mc.player.getBlockPos()).getBlock() == Blocks.COBWEB ||
                mc.world.getBlockState(new BlockPos((int) mc.player.getX(), (int) (mc.player.getY() + 0.5), (int) mc.player.getZ())).getBlock() == Blocks.COBWEB;
    }

    private void critPacket(double yDelta, boolean full) {
        if (mc.player == null) return;
        if (!full)
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + yDelta, mc.player.getZ(), false, false));
        else
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY() + yDelta, mc.player.getZ(), ((IClientPlayerEntity) mc.player).getLastYaw(), ((IClientPlayerEntity) mc.player).getLastPitch(), false, false));
    }

    public static Entity getEntity(PlayerInteractEntityC2SPacket packet) {
        try {
            Field field = PlayerInteractEntityC2SPacket.class.getDeclaredField("entityId");
            field.setAccessible(true);
            int id = field.getInt(packet);
            return mc.world.getEntityById(id);
        } catch (Exception e) {
            return null;
        }
    }

    public static InteractType getInteractType(PlayerInteractEntityC2SPacket packet) {
        final InteractType[] result = new InteractType[1];
        packet.handle(new PlayerInteractEntityC2SPacket.Handler() {
            @Override public void interact(Hand hand) { result[0] = InteractType.INTERACT; }
            @Override public void interactAt(Hand hand, Vec3d pos) { result[0] = InteractType.INTERACT_AT; }
            @Override public void attack() { result[0] = InteractType.ATTACK; }
        });
        return result[0];
    }

    public enum InteractType { INTERACT, ATTACK, INTERACT_AT }
    public enum Mode { Ncp, Strict, OldNCP, UpdatedNCP, Grim, MiniJump, Rw }
}