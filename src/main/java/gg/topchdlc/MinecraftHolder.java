package gg.topchdlc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

public interface MinecraftHolder {
    public static MinecraftClient mc = MinecraftClient.getInstance();
    public static Window window = MinecraftClient.getInstance().getWindow();
}
