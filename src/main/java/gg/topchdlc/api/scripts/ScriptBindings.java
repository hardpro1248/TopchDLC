package gg.topchdlc.api.scripts;

import gg.topchdlc.api.events.list.*;
import gg.topchdlc.api.scripts.api.bindings.*;
import gg.topchdlc.api.scripts.api.bindings.event.EventProvider;
import gg.topchdlc.api.scripts.api.bindings.support.AngleScripted;
import gg.topchdlc.api.scripts.api.integrate.ScriptHook;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ScriptBindings {
    public static void bind(Context ctx, Script script) {
        Value bindings = ctx.getBindings("js");

        // реги
        bindings.putMember("register", (Consumer<Map<String, Object>>) script::register);

        // апу
        bindings.putMember("Client", new ClientProvider(script));
        bindings.putMember("Events", new EventProvider(script));
        bindings.putMember("Reflect", new ReflectProvider());
        bindings.putMember("Render", new RenderProvider());
        bindings.putMember("Timer", new TimeProvider());
        bindings.putMember("Hook", new HookProvider(script));
        bindings.putMember("Game", new GameProvider());
        bindings.putMember("Drag", new DragProvider());

        Map<String, Object> fonts = new HashMap<>();
        for (FontProvider provider: FontProvider.values()) {
            fonts.put(provider.name(), provider.name());
        }
        bindings.putMember("Font", fonts);

        // сеи
        {
            Map<String, Object> categories = new HashMap<>();
            for (Category provider : Category.values()) {
                categories.put(provider.name(), provider.name());
            }
            bindings.putMember("Category", categories);
        }
        {
            Map<String, Object> hooks = new HashMap<>();
            for (ScriptHook.HookValue provider : ScriptHook.HookValue.values()) {
                hooks.put(provider.name(), provider.name());
            }
            bindings.putMember("Hooks", hooks);
        }

        // евенту
        bindings.putMember("Render2D", Event2D.class);
        bindings.putMember("GameTick", EventGameTick.class);
        bindings.putMember("ServerMove", EventMove.class);
        bindings.putMember("FireworkBoost", EventBoost.class);
        bindings.putMember("Attack", EventAttack.class);
        bindings.putMember("Render3D", Event3D.class);

        // сасутениг
        bindings.putMember("Slider", SliderSetting.class);
        bindings.putMember("CheckBox", CheckBox.class);
        bindings.putMember("Enums", EnumSetting.class);

        // сперма
        bindings.putMember("Vec3", Vector3f.class);
        bindings.putMember("Vec2f", Vector2f.class);

        bindings.putMember("Angle", AngleScripted.class);
    }
}
