package gg.topchdlc.vse.shutki.module.modules.impl.movement.antiweb;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.ChoiceSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;

/**
 * Create by daun kvass
 */
public class AntiWebMotion extends Choice {
    public AntiWebMotion() {
        super("Motion");
    }

    private static final float LONGRIF_XZ = 0.20f;
    private static final float LONGRIF_Y  = 1f;

    private static final float HW_XZ = 0.20f;
    private static final float HW_Y  = 0.6f;

    private final Choice longrif = new Choice("RW") {
        @Override public void onEvent(Event event) {}
    };
    private final Choice hw = new Choice("HW") {
        @Override public void onEvent(Event event){}};

    private final CustomChoice custom = new CustomChoice();
    final ChoiceSetting<Choice> preset = choiceSetting("Preset", 0, longrif, hw, custom);

    private static class CustomChoice extends Choice {
        final SliderSetting speedXZ = sliderSetting("Speed XZ", 0.15f, 0.01f, 7.0f).increment(0.01f);
        final SliderSetting speedY  = sliderSetting("Speed Y",  0.1f,  0.0f,  3.0f).increment(0.01f);
        CustomChoice() { super("Custom"); }
        @Override public void onEvent(Event event) {}
    }

    private boolean isInCobweb() {
        var box = mc.player.getBoundingBox();
        int x1 = (int) Math.floor(box.minX), y1 = (int) Math.floor(box.minY), z1 = (int) Math.floor(box.minZ);
        int x2 = (int) Math.floor(box.maxX), y2 = (int) Math.floor(box.maxY), z2 = (int) Math.floor(box.maxZ);
        for (int x = x1; x <= x2; x++)
            for (int y = y1; y <= y2; y++)
                for (int z = z1; z <= z2; z++)
                    if (mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.COBWEB)
                        return true;
        return false;
    }

    @Override
    public void onEvent(Event event) {
        if (!(event instanceof EventGameTick)) return;
        if (mc.player == null || mc.world == null || !isInCobweb()) return;

        float xz, y;
        Choice current = preset.get();

        if (current == longrif) {
            xz = LONGRIF_XZ;
            y = LONGRIF_Y;
        } else if (current == hw) {
            xz = HW_XZ;
            y = HW_Y;
        } else {
            xz = custom.speedXZ.get();
            y = custom.speedY.get();
        }

        PlayerInput input = mc.player.input.playerInput;
        float yaw = Client.ROTATION.getRotate().getYaw();

        float forward = input.forward() ? 1f : input.backward() ? -1f : 0f;
        float strafe  = input.left()    ? 1f : input.right()    ? -1f : 0f;

        if (forward != 0 && strafe != 0) {
            forward *= 0.7071f;
            strafe  *= 0.7071f;
        }

        double rad = Math.toRadians(yaw);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);

        double vx = (-sin * forward + cos * strafe) * xz;
        double vz = ( cos * forward + sin * strafe) * xz;

        double vy;
        if (input.jump())       vy =  y;
        else if (input.sneak()) vy = -y;
        else                    vy =  0.0;

        mc.player.setVelocity(vx, vy, vz);
    }
}