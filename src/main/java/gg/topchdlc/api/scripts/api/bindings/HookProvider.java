package gg.topchdlc.api.scripts.api.bindings;

import gg.topchdlc.Client;
import gg.topchdlc.api.scripts.Script;
import gg.topchdlc.api.scripts.api.integrate.IHook;
import gg.topchdlc.api.scripts.api.integrate.ScriptHook;
import org.graalvm.polyglot.HostAccess;

public class HookProvider {
    Script script;

    public HookProvider(Script script) {
        this.script = script;
    }

    @HostAccess.Export
    public void hook(String enum_, IHook<?> solution) {
        Client.SCRIPTS.getScriptHook().createHook(ScriptHook.HookValue.valueOf(enum_), solution, this.script);
    }
}
