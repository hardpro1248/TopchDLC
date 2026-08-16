package gg.topchdlc.api.macros.constructor;

import gg.topchdlc.vse.shutki.module.settings.Setting;

import java.util.ArrayList;

public interface MacroBlock {
    default ArrayList<Setting<?>> getSettings() { return new ArrayList<>(); }
    void execute();
    String renderName();
}
