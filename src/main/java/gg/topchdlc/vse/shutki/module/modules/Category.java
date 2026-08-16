package gg.topchdlc.vse.shutki.module.modules;

import gg.topchdlc.api.render.system.IconUse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import gg.topchdlc.vse.utils.math.Rectangle;
/**
 * Create by daun kvass
 */
@Getter
@RequiredArgsConstructor
public enum Category {
    COMBAT("Combat", IconUse.FIGHT.glyph),
    MOVEMENT("Movement", IconUse.MOVEMENT.glyph),
    PLAYER("Player", IconUse.PLAYER.glyph),
    Misc("Misc",IconUse.MISC.glyph),
    RENDER("Render", IconUse.RENDER.glyph),
    Friends("Friends", IconUse.PERSONS.glyph),
    Settings("Settings", IconUse.GEAR.glyph);

    private final String name;
    private final String icon;
    public float animation;
    public final Rectangle rect = new Rectangle(0, 0, 0, 0);
}
