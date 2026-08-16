package gg.topchdlc.vse.shutki.module.modules.impl.movement.antiweb;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Create by daun kvass
 */
public class AntiWebVelocity extends Choice {
    public AntiWebVelocity() {
        super("Velocity");
    }

    final SliderSetting speedXZ = sliderSetting("Speed XZ", 0.2f, 0.01f, 1.0f).increment(0.01f);
    final SliderSetting speedY  = sliderSetting("Speed Y",  0.1f, 0.0f,  1.0f).increment(0.01f);

    private boolean isInCobweb() {
        var box = mc.player.getBoundingBox();
        int x1 = (int) Math.floor(box.minX), x2 = (int) Math.floor(box.maxX);
        int y1 = (int) Math.floor(box.minY), y2 = (int) Math.floor(box.maxY);
        int z1 = (int) Math.floor(box.minZ), z2 = (int) Math.floor(box.maxZ);
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

        Vec3d vel = mc.player.getVelocity();
        double hLen = Math.sqrt(vel.x * vel.x + vel.z * vel.z);

        double xz = speedXZ.get();
        double y  = speedY.get();

        double nx = hLen > 0 ? (vel.x / hLen) * xz : 0;
        double nz = hLen > 0 ? (vel.z / hLen) * xz : 0;
        double ny = vel.y < 0 ? -y : vel.y > 0 ? y : 0;

        mc.player.setVelocity(nx, ny, nz);
    }
}
