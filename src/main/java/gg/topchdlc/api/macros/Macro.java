package gg.topchdlc.api.macros;

import gg.topchdlc.api.macros.constructor.MacroBlock;
import gg.topchdlc.api.ui.widgets.impl.MacrosWidget;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

public class Macro {
    @Getter @Setter
    int key = -1;

    ArrayList<MacroBlock> insn = new ArrayList<>();

    public Macro(ArrayList<MacrosWidget.BlockSample> samples) {
        for (MacrosWidget.BlockSample sample : samples) {
            insn.add(sample.getOriginal());
        }
    }

    public void execute() {
        for (MacroBlock block : insn) {
            block.execute();
        }
    }
}
