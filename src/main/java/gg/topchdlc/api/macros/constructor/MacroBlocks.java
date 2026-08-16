package gg.topchdlc.api.macros.constructor;

import gg.topchdlc.api.macros.constructor.impl.JumpBlock;
import gg.topchdlc.api.macros.constructor.impl.SwingBlock;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class MacroBlocks {
    @Getter
    private ArrayList<MacroBlock> handled = new ArrayList<>();
    public MacroBlocks() {
        handled.addAll(List.of(
                new JumpBlock(),
                new SwingBlock()
        ));
    }
}
