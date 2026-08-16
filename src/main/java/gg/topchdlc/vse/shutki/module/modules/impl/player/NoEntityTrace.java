package gg.topchdlc.vse.shutki.module.modules.impl.player;

import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;

/**
 * Create by daun kvass
 */
public class NoEntityTrace extends Module {
    public static final NoEntityTrace INSTANCE = new NoEntityTrace();
    private NoEntityTrace(){super("NoEntityTrace", Category.PLAYER,"x");}
    public final CheckBox ponly = checkbox("Pickaxe Only", true);
    public final CheckBox noSword =checkbox("No Sword", true);
}
