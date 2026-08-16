package gg.topchdlc.vse.shutki.module.modules.impl.movement;

import gg.topchdlc.api.autobuy.item.other.PotionEffectItemBuy;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventMove;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.utils.player.MoveUtility;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

public class Jesus extends Module {
    public Jesus() {
        super("Jesus", Category.MOVEMENT, "Хесус дефолт на воде просто");
    }
    public static final Jesus INSTANCE = new Jesus();

    public enum Mode { Velocity, MetaHvH }

    private final float melonBallSpeed = 0.44F;
    public EnumSetting<Mode> mode = enumSetting("Режим", Mode.Velocity);
    int ticks;
    EventBus<Event> events = event -> {
        if (event instanceof EventMove e) {

            switch (mode.get()) {
                case Velocity -> {
                    BlockPos playerPos = new BlockPos((int) mc.player.getX(), (int) (mc.player.getY() + 0.008D), (int) mc.player.getZ());
                    Block playerBlock = mc.world.getBlockState(playerPos).getBlock();
                    if (playerBlock == Blocks.WATER && !mc.player.isOnGround()) {
                        boolean isUp = mc.world.getBlockState(new BlockPos((int) mc.player.getX(), (int) (mc.player.getY() + 0.03D), (int) mc.player.getZ())).getBlock() == Blocks.WATER;
                        float yPort = MoveUtility.getSpeed() > 0.1D ? 0.02F : 0.032F;
                        mc.player.setVelocity(mc.player.getVelocity().x, (double) mc.player.fallDistance < 3.5D ? (double) (isUp ? yPort : -yPort) : -0.1D, mc.player.getVelocity().z);
                    }

                    double posY = mc.player.getY();
                    if (posY > (double) ((int) posY) + 0.89D && posY <= (double) ((int) posY + 1) || (double) mc.player.fallDistance > 3.5D) {
                        mc.player.setPosition(mc.player.getX(), (double) ((int) posY + 1) + 1.0E-45D, mc.player.getZ());
                        if (!mc.player.isTouchingWater()) {
                            BlockPos waterBlockPos = new BlockPos((int) mc.player.getX(), (int) (mc.player.getY() - 0.1D), (int) mc.player.getZ());
                            Block waterBlock = mc.world.getBlockState(waterBlockPos).getBlock();
                            if (waterBlock == Blocks.WATER) {
                                e.ground = false;
                                if (ticks == 1) {
                                    MoveUtility.setSpeed(1.1f);
                                    ticks = 0;
                                } else {
                                    ticks = 1;
                                }
                            }
                        }
                    }
                }
                case MetaHvH -> {
                    if (mc.player.isTouchingWater() || mc.player.isInLava()) {
                        StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
                        StatusEffectInstance DeEffect = mc.player.getStatusEffect(StatusEffects.SLOWNESS);
                        ItemStack offHandItem = mc.player.getOffHandStack();
                        String itemName = offHandItem.getName().getString();
                        float appliedSpeed = 0F;

                        if (itemName.contains("Ломтик Дыни") && speedEffect != null && speedEffect.getAmplifier() == 2) {
                            appliedSpeed = 0.4283F * 1.15F;
                        }
                        else {
                            if (speedEffect != null) {
                                if (speedEffect.getAmplifier() == 2) {
                                    appliedSpeed = melonBallSpeed * 1.15F;
                                }
                                else if (speedEffect.getAmplifier() == 1) {
                                    appliedSpeed = melonBallSpeed;
                                }
                            }
                            else {
                                appliedSpeed = melonBallSpeed * 0.68F;
                            }
                        }

                        if (DeEffect != null) {
                            appliedSpeed *= 0.85f;
                        }

                        MoveUtility.setSpeed(appliedSpeed);

                        boolean isMoving = MoveUtility.isKeyPressed();

                        double motionX = mc.player.getVelocity().x;
                        double motionZ = mc.player.getVelocity().z;

                        if (!isMoving) {
                            motionX = 0.0;
                            motionZ = 0.0;
                        }

                        double motionY = mc.options.jumpKey.isPressed() ? 0.019 : 0.003;

                        mc.player.setVelocity(motionX, motionY, motionZ);
                    }
                }
            }
        }
    };
}
