package gg.topchdlc.vse.shutki.module.modules.impl.render;

import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
/**
 * Create by daun kvass
 */
public class FreeLook extends Module {
    public static final FreeLook INSTANCE = new FreeLook();
    private boolean activeLook;
    private float cameraYaw;
    private float cameraPitch;
    private Perspective previousPerspective = Perspective.FIRST_PERSON;
    public FreeLook(){super("FreeLook", Category.Misc,"x");}

    EventBus<EventGameTick> tick = event -> {
        if (mc.player == null || mc.world == null) {
            stopFreeLook();
            return;
        }

        boolean shouldBeActive = FreeLook.INSTANCE.isEnabled() && mc.currentScreen == null;
        if (shouldBeActive && !activeLook) {
            startFreeLook();
        } else if (!shouldBeActive && activeLook) {
            stopFreeLook();
        }
    };


    private void startFreeLook() {
        if (mc.player == null) return;

        activeLook = true;
        cameraYaw = mc.player.getYaw();
        cameraPitch = mc.player.getPitch();
        previousPerspective = mc.options.getPerspective();
        mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
    }

    private void stopFreeLook() {
        if (!activeLook) return;

        activeLook = false;
        mc.options.setPerspective(previousPerspective);
    }

    public boolean isActiveLook() {
        return isEnabled() && activeLook;
    }

    public float getCameraYaw() {
        return cameraYaw;
    }

    public float getCameraPitch() {
        return cameraPitch;
    }

    public void handleMouseLook(double deltaX, double deltaY) {
        if (!isActiveLook()) return;

        cameraYaw += (float) deltaX * 0.15f;
        cameraPitch += (float) deltaY * 0.15f;
        cameraPitch = MathHelper.clamp(cameraPitch, -90.0f, 90.0f);
    }

    @Override
    protected void onDisable() {
        stopFreeLook();
    }
}
