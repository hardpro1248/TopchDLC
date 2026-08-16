package gg.topchdlc.vse.utils.client.get;

import gg.topchdlc.vse.shutki.module.modules.impl.render.Interface;
import gg.topchdlc.vse.utils.other.LogUtility;
import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class DebugUtility {
    public final Map<String, Trace> traces = new HashMap<>();

    public void trace(String name, Object content) {
        if (!Interface.INSTANCE.isEnabled() || !Interface.INSTANCE.MainElements.get(Interface.HudElements.Debug)) return;

        LogUtility.debug(String.format("DebugUtility trace %s %s", name, content));
        traces.put(name, new Trace(content.toString(), System.currentTimeMillis()));
    }

    public record Trace(String content, long timestamp) {}
}
