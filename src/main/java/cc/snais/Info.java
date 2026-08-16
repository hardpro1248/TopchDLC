package cc.snais;

import net.minecraft.util.Nullables;

import static gg.topchdlc.MinecraftHolder.mc;

/**
 * Create by daun kvass
 */
public class Info {
    public static String NAME = "TopchDLC";
    public static String getServ() {
        if (mc.isInSingleplayer()) return "LocalHost";
        return Nullables.map(mc.getCurrentServerEntry(), info -> info.address);
    }
    public static String Client = "TopchDLC v0.0.1";

}
