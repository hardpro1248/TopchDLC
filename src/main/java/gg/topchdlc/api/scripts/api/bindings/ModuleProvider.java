package gg.topchdlc.api.scripts.api.bindings;

import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;

public class ModuleProvider {
    private Module module;
    public ModuleProvider(Module module) {
        this.module = module;
    }

    public String getName() {
        return module.getName();
    }
    public int getKey() {
        return module.getKey();
    }
    public Category getCategory() {
        return module.getCategory();
    }
    public boolean isEnabled() {
        return module.isEnabled();
    }
}
