package gg.topchdlc.api.scripts.api.bindings;

import gg.topchdlc.api.scripts.api.bindings.support.TimerScripted;
import org.graalvm.polyglot.HostAccess;

public class TimeProvider {
    @HostAccess.Export
    public TimerScripted create() {
        return new TimerScripted();
    }
}
