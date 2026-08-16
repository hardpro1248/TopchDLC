package gg.topchdlc.api.scripts.api.integrate;

import gg.topchdlc.api.scripts.Script;

import java.util.HashMap;
import java.util.Map;

public class ScriptHook {
    public void createHook(HookValue hook, IHook<?> solution, Script script) {
        hook.processors.put(script, solution);
    }

    @SuppressWarnings("unchecked")
    public Object solute(HookValue hook, Object val) {
        for (Script s : hook.processors.keySet()) {
            if (s.getScriptedModule().isEnabled()) {
                return hook.processors.get(s).process(val);
            }
        }
        return null;
    }

    public void cleanup() {
        for (HookValue value : HookValue.values()) {
            value.processors.clear();
        }
    }

    public enum HookValue {
        ELYTRA_RESOLVER, AURA_ATTACK, AURA_POINT;
        public final Map<Script, IHook> processors = new HashMap<>();
    }
}
