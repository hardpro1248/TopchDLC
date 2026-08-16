package gg.topchdlc.vse.shutki.other.commands.impl;

import gg.topchdlc.Client;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.other.commands.Command;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import gg.topchdlc.vse.utils.client.text.TextUtility;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Create by daun kvass
 */
public class BindCommand extends Command {
    public static BindCommand INSTANCE = new BindCommand();

    public BindCommand() {
        super("bind", "управление биндами", "add", "remove", "list", "clear");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            error("usage: .bind <add | remove | list | clear> [module] [key]");
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "add", "set" -> {
                if (args.length < 3) {
                    error("usage: .bind add <module> [key]");
                    return;
                }
                
                String moduleName = args[2];
                Module module = Client.MODULES.get(moduleName);
                
                if (module == null) {
                    error("Модуль '" + moduleName + "' не найден");
                    return;
                }
                
                if (args.length >= 4) {
                    String keyName = args[3].toUpperCase();
                    int keyCode = getKeyCode(keyName);
                    if (keyCode == -1) {
                        error("Неизвестная клавиша: " + keyName);
                        return;
                    }
                    
                    module.setKey(keyCode);
                    ChatUtility.send(Text.literal("Бинд для " + module.getName() + " установлен на: " + TextUtility.keyToString(keyCode))
                            .withColor(new Color(100, 255, 100).getRGB()));
                } else {
                    ChatUtility.send(Text.literal("после нейма напиши букавку на которую хочешь забиндить" + module.getName() + " ")
                            .withColor(new Color(100, 200, 255).getRGB()));
                }
            }
            case "remove", "del", "unbind" -> {
                if (args.length < 3) {
                    error("usage: .bind remove <module>");
                    return;
                }
                
                String moduleName = args[2];
                Module module = Client.MODULES.get(moduleName);
                
                if (module == null) {
                    error("Модуль '" + moduleName + "' не найден");
                    return;
                }
                
                if (module.getKey() == -1) {
                    ChatUtility.send(Text.literal("У модуля " + module.getName() + " нет бинда")
                            .withColor(new Color(255, 200, 100).getRGB()));
                } else {
                    String oldKey = TextUtility.keyToString(module.getKey());
                    module.setKey(-1);
                    ChatUtility.send(Text.literal("Бинд " + oldKey + " удален с модуля " + module.getName())
                            .withColor(new Color(255, 100, 100).getRGB()));
                }
            }
            case "list" -> {
                List<Module> boundModules = new ArrayList<>();
                for (Module module : Client.MODULES.getModules()) {
                    if (module.getKey() != -1) {
                        boundModules.add(module);
                    }
                }
                
                if (boundModules.isEmpty()) {
                    ChatUtility.send(Text.literal("Нет модулей с биндами").withColor(new Color(200, 200, 200).getRGB()));
                } else {
                    ChatUtility.send(Text.literal("Модули с биндами:").withColor(new Color(100, 200, 255).getRGB()));
                    for (Module module : boundModules) {
                        String keyText = TextUtility.keyToString(module.getKey());
                        ChatUtility.send(Text.literal("  " + module.getName() + " -> " + keyText)
                                .withColor(new Color(200, 200, 200).getRGB()));
                    }
                }
            }
            case "clear" -> {
                int count = 0;
                for (Module module : Client.MODULES.getModules()) {
                    if (module.getKey() != -1) {
                        module.setKey(-1);
                        count++;
                    }
                }
                
                if (count == 0) {
                    ChatUtility.send(Text.literal("Нет модулей с биндами").withColor(new Color(200, 200, 200).getRGB()));
                } else {
                    ChatUtility.send(Text.literal("Удалено биндов: " + count).withColor(new Color(255, 100, 100).getRGB()));
                }
            }
            default -> error("usage: .bind <add | remove | list | clear> [module] [key]");
        }
    }
    
    private int getKeyCode(String keyName) {
        return switch (keyName) {
            case "LSHIFT", "LEFTSHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case "RSHIFT", "RIGHTSHIFT" -> GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "LCTRL", "LEFTCTRL" -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case "RCTRL", "RIGHTCTRL" -> GLFW.GLFW_KEY_RIGHT_CONTROL;
            case "LALT", "LEFTALT" -> GLFW.GLFW_KEY_LEFT_ALT;
            case "RALT", "RIGHTALT" -> GLFW.GLFW_KEY_RIGHT_ALT;
            case "SPACE" -> GLFW.GLFW_KEY_SPACE;
            case "ENTER" -> GLFW.GLFW_KEY_ENTER;
            case "TAB" -> GLFW.GLFW_KEY_TAB;
            case "BACKSPACE" -> GLFW.GLFW_KEY_BACKSPACE;
            case "CAPSLOCK" -> GLFW.GLFW_KEY_CAPS_LOCK;
            case "ESC", "ESCAPE" -> GLFW.GLFW_KEY_ESCAPE;
            case "LMB", "MOUSE1" -> -100;
            case "RMB", "MOUSE2" -> -101;
            case "MMB", "MOUSE3" -> -102;
            case "MOUSE4", "M4" -> -103;
            case "MOUSE5", "M5" -> -104;
            case "MOUSE6", "M6" -> -105;
            case "MOUSE7", "M7" -> -106;
            case "MOUSE8", "M8" -> -107;
            default -> {
                if (keyName.startsWith("F") && keyName.length() <= 3) {
                    try {
                        int fNum = Integer.parseInt(keyName.substring(1));
                        if (fNum >= 1 && fNum <= 25) {
                            yield GLFW.GLFW_KEY_F1 + (fNum - 1);
                        }
                    } catch (NumberFormatException ignored) {}
                }
                if (keyName.length() == 1) {
                    char c = keyName.charAt(0);
                    if (c >= 'A' && c <= 'Z') {
                        yield GLFW.GLFW_KEY_A + (c - 'A');
                    } else if (c >= '0' && c <= '9') {
                        yield GLFW.GLFW_KEY_0 + (c - '0');
                    }
                }
                
                yield -1;
            }
        };
    }
}
