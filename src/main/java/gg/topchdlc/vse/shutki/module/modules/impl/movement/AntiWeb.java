package gg.topchdlc.vse.shutki.module.modules.impl.movement;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventKey;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.impl.movement.antiweb.AntiWebMotion;
import gg.topchdlc.vse.shutki.module.modules.impl.movement.antiweb.AntiWebVelocity;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.Choice;
import gg.topchdlc.vse.shutki.module.settings.impl.choice.ChoiceSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.keybind.KeybindSetting;
import org.lwjgl.glfw.GLFW;

/**
 * Create by daun kvass
 */
public class AntiWeb extends Module {
    public static final AntiWeb INSTANCE = new AntiWeb();

    private AntiWeb() {
        super("AntiWeb", Category.MOVEMENT, "Позволяет нормально двигаться в паутине");
    }

    public ChoiceSetting<Choice> mode = choiceSetting("Mode", 0,
            new AntiWebMotion(),
            new AntiWebVelocity()
    );

    public enum BindMode { Toggle, Hold }

    public KeybindSetting noCollisionBind = keybindSetting("No Collision Bind", GLFW.GLFW_KEY_UNKNOWN);
    public EnumSetting<BindMode> bindMode = enumSetting("Bind Mode", BindMode.Toggle);

    public boolean noCollisionActive = false;

    @Override
    protected void onEnable() {
        mode.onEnabled();
        noCollisionActive = false;
    }

    @Override
    protected void onDisable() {
        mode.onDisabled();
        noCollisionActive = false;
    }

    EventBus<Event> events = event -> {
        mode.onEvent(event);

        if (!(event instanceof EventKey key)) return;
        if (noCollisionBind.getBind() == GLFW.GLFW_KEY_UNKNOWN) return;
        if (!noCollisionBind.matches(key)) return;

        if (bindMode.is(BindMode.Toggle)) {
            if (key.getAction() == GLFW.GLFW_PRESS) {
                noCollisionActive = !noCollisionActive;
            }
        } else {
            noCollisionActive = key.getAction() != GLFW.GLFW_RELEASE;
        }
    };
}
