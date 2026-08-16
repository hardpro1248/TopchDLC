package gg.topchdlc.vse.shutki.screen.hud.ui.impl.nurick.icon;

import gg.topchdlc.vse.shutki.screen.hud.notification.Notify;

import java.awt.*;

// Иконки из шрифта нуриковского
// y глазик
// o закрытый глазик
// t звёздочка
// P нурик
//B прикольная луна
// Z мусорка
// A утка
// E потионс
// S метеор
// D шестеренка
// F компас
// I крестик
// G инфо типо
// R пипетка
// H галочка
//K переключать влево
// L щиток с крестиком
// M опопвещение ромб
// N стрела в месте
// $ спутник
// U партиклы
public class NurickIcons {
    public static final String NURICK      = "P";
    public static final String USER      = "W";
    public static final String FPS       = "X";
    public static final String PING      = "Q";
    public static final String BPS       = "@";
    public static final String PARTICLES = "U";
    public static final String SATELLITE = "$";
    public static final String NOTIFY    = "M";
    public static final String INFO      = "G";
    public static final String CHECK     = "H";
    public static final String CROSS     = "I";
    public static final String Energy = "#";
    public static final String ARROW_RIGHT = "J";
    public static final String CHASI = "V";
    public static final String TIMER = "T";
    public static final String WARN      = "M";
    public static final String EYE_OPEN   = "Y";
    public static final String EYE_CLOSED = "O";
    public static final String KEYBOARD = "C";
    public static final String STAR         = "T";
    public static final String MOON         = "B";
    public static final String TRASH        = "Z";
    public static final String DUCK         = "A";
    public static final String METEOR       = "S";
    public static final String GEAR         = "D";
    public static final String COMPASS      = "F";
    public static final String PIPETTE      = "R";
    public static final String ARROW_LEFT   = "K";
    public static final String SHIELD_CROSS = "L";
    public static final String ARROW_PLACE  = "N";
    public static final String POTION = "E";


    public static boolean isItemAction(Notify notify) {
        String text = notify.getText().getString().toLowerCase();
        return text.contains("подобран") || text.contains("pickup") ||
                text.contains("свап") || text.contains("swap") ||
                text.contains("использован") || text.contains("использовал");
    }

    public static String getForNotify(Notify notify) {
        String text = notify.getText().getString().toLowerCase();

        if (text.contains("enabled") || text.contains("включен")) return ARROW_LEFT;
        if (text.contains("disabled") || text.contains("выключен")) return ARROW_RIGHT;

        return NOTIFY;
    }
    public static Color getColor(String glyph) {
        if (glyph.equals(ARROW_LEFT)) return new Color(100, 255, 100);
        if (glyph.equals(ARROW_RIGHT)) return new Color(255, 100, 100);
        return Color.WHITE;
    }
}
