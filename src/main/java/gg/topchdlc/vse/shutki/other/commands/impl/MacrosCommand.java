package gg.topchdlc.vse.shutki.other.commands.impl;

import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import gg.topchdlc.Client;
import gg.topchdlc.vse.shutki.other.commands.Command;
import gg.topchdlc.vse.shutki.other.macros.Macro;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import gg.topchdlc.vse.utils.client.text.TextUtility;

import java.awt.*;
import java.util.List;

public class MacrosCommand extends Command {
    public static MacrosCommand INSTANCE = new MacrosCommand();

    public MacrosCommand() {
        super("macros", "макросы там чтоб команду по бинду отправлять", "add", "remove", "list", "clear");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            error("usage: .macros <add | remove | list | clear> [key] [message]");
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "add" -> {
                if (args.length < 4) {
                    error("usage: .macros add <key> <message>");
                    ChatUtility.send(Text.literal("Пример: .macros add x !Я пидор").withColor(new Color(200, 200, 200).getRGB()));
                    return;
                }

                String keyName = args[2].toUpperCase();
                int keyCode = getKeyCode(keyName);

                if (keyCode == -1) {
                    error("Неизвестная клавиша: " + keyName);
                    return;
                }
                StringBuilder messageBuilder = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    if (i > 3) messageBuilder.append(" ");
                    messageBuilder.append(args[i]);
                }
                String message = messageBuilder.toString();

                Client.MACRO_MANAGER.addMacro(keyCode, message);
                ChatUtility.send(Text.literal("Макрос добавлен: " + TextUtility.keyToString(keyCode) + " -> \"" + message + "\"")
                        .withColor(new Color(100, 255, 100).getRGB()));
            }
            case "remove", "del" -> {
                if (args.length < 3) {
                    error("usage: .macros remove <key>");
                    return;
                }

                String keyName = args[2].toUpperCase();
                int keyCode = getKeyCode(keyName);

                if (keyCode == -1) {
                    error("Неизвестная клавиша: " + keyName);
                    return;
                }

                if (Client.MACRO_MANAGER.removeMacro(keyCode)) {
                    ChatUtility.send(Text.literal("Макрос удален: " + TextUtility.keyToString(keyCode))
                            .withColor(new Color(255, 100, 100).getRGB()));
                } else {
                    ChatUtility.send(Text.literal("Макрос не найден для клавиши: " + TextUtility.keyToString(keyCode))
                            .withColor(new Color(255, 200, 100).getRGB()));
                }
            }
            case "list" -> {
                List<Macro> macros = Client.MACRO_MANAGER.getMacros();

                if (macros.isEmpty()) {
                    ChatUtility.send(Text.literal("Нет сохраненных макросов").withColor(new Color(200, 200, 200).getRGB()));
                } else {
                    ChatUtility.send(Text.literal("Список макросов:").withColor(new Color(100, 200, 255).getRGB()));
                    for (Macro macro : macros) {
                        String keyText = TextUtility.keyToString(macro.getKey());
                        ChatUtility.send(Text.literal("  " + keyText + " -> \"" + macro.getMessage() + "\"")
                                .withColor(new Color(200, 200, 200).getRGB()));
                    }
                }
            }
            case "clear" -> {
                List<Macro> macros = Client.MACRO_MANAGER.getMacros();
                int count = macros.size();

                if (count == 0) {
                    ChatUtility.send(Text.literal("Нет макросов для удаления").withColor(new Color(200, 200, 200).getRGB()));
                } else {
                    Client.MACRO_MANAGER.clearMacros();
                    ChatUtility.send(Text.literal("Удалено макросов: " + count).withColor(new Color(255, 100, 100).getRGB()));
                }
            }
            default -> error("usage: .macros <add | remove | list | clear> [key] [message]");
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
