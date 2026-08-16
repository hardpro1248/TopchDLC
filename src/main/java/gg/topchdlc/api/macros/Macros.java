package gg.topchdlc.api.macros;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventKey;
import gg.topchdlc.api.macros.constructor.MacroBlocks;
import gg.topchdlc.api.ui.widgets.impl.MacrosWidget;
import lombok.Getter;

import java.util.ArrayList;

public class Macros {
    private ArrayList<Macro> macros = new ArrayList<>();

    @Getter
    private MacroBlocks blocks = new MacroBlocks();

    public Macros() {
        Client.EVENTS.register(this);
    }

    EventBus<EventKey> eventKey = key -> {
        if (key.action == 1) {
            for (Macro macro : macros) {
                if (macro.getKey() == key.getKey()) macro.execute();
            }
        }
    };

    public Macro addFromBuilder(ArrayList<MacrosWidget.BlockSample> samples) {
        Macro macro = new Macro(samples);
        macros.add(macro);
        return macro;
    }
}
