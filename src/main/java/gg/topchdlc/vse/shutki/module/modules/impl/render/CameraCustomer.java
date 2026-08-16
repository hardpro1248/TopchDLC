package gg.topchdlc.vse.shutki.module.modules.impl.render;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.EventPriority;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventGetFov;
import gg.topchdlc.api.events.list.EventKey;
import gg.topchdlc.api.events.list.EventScroll;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.keybind.KeybindSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import org.lwjgl.glfw.GLFW;

/**
 * Create by daun kvass
 */
public class CameraCustomer extends Module {
    public static final CameraCustomer INSTANCE = new CameraCustomer();

    public enum ZoomMode { TOGGLE, HOLD }

    public final Group sixseven;

    public final SliderSetting zoomAmount;
    public final SliderSetting zoomScroll;
    public final CheckBox      zoomSmooth;
    public final SliderSetting zoomSmoothSpeed;
    public final CheckBox      zoomCinematic;
    public final CheckBox      zoomHands;
    public final KeybindSetting zoomBind;
    public final EnumSetting<ZoomMode> zoomMode;
    public final SliderSetting cameraDistance;


    public boolean zoomActive = false;
    private boolean prevCinematic;
    private double  zoomValue, prevSensitivity;
    private double  zoomAnimCurrent = 1.0, zoomAnimTarget = 1.0;
    private long    zoomAnimLastMs  = 0;

    @EventPriority(EventPriority.LAST)
    EventBus<Event> events;

    public final CheckBox aspectratio = checkbox("AspectRatio",false);
    public final SliderSetting  widthSlider = sliderSetting("Strengh", 1, 0.2f, 2f).increment(0.01f).visible(aspectratio::get);
    private CameraCustomer() {
        super("Zoom", Category.Misc, "Зум: кнопка и сила зума");

        sixseven = group("Zoom");
        sixseven.toggleable(true);
        zoomAmount      = sixseven.sliderSetting("Zoom Amount", 6, 1, 100);
        zoomScroll      = sixseven.sliderSetting("Zoom Scroll", 1, 0, 100).increment(0.1f);
        zoomSmooth      = sixseven.checkbox("Zoom Smooth", true);
        zoomSmoothSpeed = sixseven.sliderSetting("Zoom Smooth Speed", 150, 50, 1000).increment(10f);
        zoomCinematic   = sixseven.checkbox("Zoom Cinematic", false);
        zoomHands       = sixseven.checkbox("Zoom Hands", false);
        zoomBind        = sixseven.keybindSetting("Zoom Bind", -1);
        zoomMode        = sixseven.enumSetting("Zoom Mode", ZoomMode.HOLD);

        cameraDistance = sliderSetting("Camera Distance", 4, 1, 20).increment(0.5f);

        zoomBind.bind(GLFW.GLFW_KEY_C);
        setEnabled(true, false);


        events = event -> {
            if (!isEnabled()) return;
            if (event instanceof EventKey e && zoomBind.getBind() != -1 && e.key == zoomBind.getBind()) {
                if (!sixseven.isEnabled()) return;
                if (zoomMode.get() == ZoomMode.TOGGLE) {
                    if (e.action == 1) {
                        if (zoomActive) deactivateZoom(); else activateZoom();
                    }
                } else {
                    if (e.action == 1) activateZoom();
                    else if (e.action == 0) deactivateZoom();
                }
            }

            if (!zoomActive || !sixseven.isEnabled()) return;
            if (event instanceof EventGameTick) {
                mc.options.smoothCameraEnabled = zoomCinematic.get();
                if (!zoomCinematic.get()) {
                    mc.options.getMouseSensitivity().setValue(prevSensitivity / Math.max(getZoomScaling() * 0.5, 1));
                }
            }
            if (event instanceof EventGetFov e) {
                e.fov /= (float) getZoomScaling();
            }
            if (event instanceof EventScroll e) {
                if (zoomScroll.get() > 0) {
                    zoomValue += e.vertical * 0.25 * (zoomScroll.get() * zoomValue);
                    if (zoomValue < 1) zoomValue = 1;
                    zoomAnimTarget = zoomValue;
                    e.cancel();
                }
            }
        };
    }

    @Override
    protected void onEnable() {
        zoomActive = false;
    }

    @Override
    protected void onDisable() {
        if (zoomActive) deactivateZoom();
    }

    private void activateZoom() {
        if (zoomActive) return;
        zoomActive = true;
        zoomValue = zoomAmount.get();
        zoomAnimCurrent = 1.0;
        zoomAnimTarget  = zoomValue;
        zoomAnimLastMs  = System.currentTimeMillis();
        prevCinematic   = mc.options.smoothCameraEnabled;
        prevSensitivity = mc.options.getMouseSensitivity().getValue();
    }

    private void deactivateZoom() {
        if (!zoomActive) return;
        zoomActive = false;
        zoomAnimTarget = 1.0;
        zoomAnimLastMs = System.currentTimeMillis();
        mc.options.smoothCameraEnabled = prevCinematic;
        mc.options.getMouseSensitivity().setValue(prevSensitivity);
    }

    private double getZoomScaling() {
        if (!zoomSmooth.get()) {
            zoomAnimCurrent = zoomActive ? zoomValue : 1.0;
            return zoomAnimCurrent;
        }
        long now = System.currentTimeMillis();
        double elapsed = (now - zoomAnimLastMs) / (double) zoomSmoothSpeed.get();
        zoomAnimLastMs = now;
        zoomAnimTarget = zoomActive ? zoomValue : 1.0;
        double speed = Math.min(elapsed * 8.0, 1.0);
        zoomAnimCurrent += (zoomAnimTarget - zoomAnimCurrent) * speed;
        return zoomAnimCurrent;
    }

    public float getCameraDistance(float original) {
        if (!isEnabled()) return original;
        return cameraDistance.get();
    }

    public float getAspectRatioWidth(float original) {
        if (!isEnabled() || !aspectratio.get()) return original;
        return original * widthSlider.get();
    }
}
