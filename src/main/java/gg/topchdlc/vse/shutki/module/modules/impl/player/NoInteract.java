package gg.topchdlc.vse.shutki.module.modules.impl.player;

import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.impl.combat.AuraModule;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;

public class NoInteract extends Module {
    public static final NoInteract INSTANCE = new NoInteract();
    private NoInteract() {
        super("NoInteract", Category.PLAYER, "Не дает контакт с блоками ");
    }

    public CheckBox onlyAura = checkbox("Only while Aura", false);

    public boolean shouldBlock() {
        if (!isEnabled()) return false;
        if (onlyAura.get()) return AuraModule.INSTANCE.isEnabled();
        return true;
    }
}


