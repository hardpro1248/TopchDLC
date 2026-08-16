package gg.topchdlc.api.scripts;

import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScriptedModule extends Module {
    private String[] authors;
    public ScriptedModule(String name, Category category, int key, String[] authors, String desc) {
        super(name.concat(".js"), category, desc);
        this.setKey(key);
        this.authors = authors;
    }
}
