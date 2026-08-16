package gg.topchdlc.vse.utils.client.get;

import gg.topchdlc.MinecraftHolder;
import lombok.experimental.UtilityClass;
import net.minecraft.client.util.InputUtil;

@UtilityClass
public class InputUtility implements MinecraftHolder {
    public boolean isKeyPressed(int key) {
        return InputUtil.isKeyPressed(window, key);
    }
}
