package gg.topchdlc.api.ui.parts.impl;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.ui.UICallback;
import gg.topchdlc.api.ui.UIStyle;
import gg.topchdlc.api.ui.parts.UIPart;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.math.ColorUtility;
import gg.topchdlc.vse.utils.math.MathUtility;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.NonFinal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

@Getter @Setter
public class UITextField extends UIPart {
    public boolean writing = false;
    private boolean hoveredDesc = false;
    private boolean sas = false;
    int limit = 100;

    private int cursor = -1;
    private int selectionEnd = -1;
    private boolean dragging = false;
    private int cursorPosition = 0;
    @NonFinal
    private float scrollOffset = 0;
    @NonFinal
    private long lastClickTime = 0;
    @NonFinal
    private long lastInputTime = System.currentTimeMillis();

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private static UITextField active;

    @Getter
    @Setter
    private String hideMask = null;

    private IconUse icon = null;
    private String text, desc;
    private UICallback callback = str -> {
    };
    private UICallback finishCallback = str -> {
    };

    private int lastText = -1;

    private float lastMouseX = -999, lastMouseY = -999;

    public UITextField() {
        text = "";
        desc = "";
    }

    @Override
    public void render(int mouseX, int mouseY) {
        boolean wasHovered = writing;

        if (!wasHovered && writing) {
            lastInputTime = System.currentTimeMillis();
        }
        if (style != UIStyle.TRANSPARENT)
            Client.RENDERER.drawModuleRect(x, y, width, height);
        if (style == UIStyle.OUTLINED)
            Client.RENDERER.outline(x, y, width, height, 0, new Vector4f(7), new Vector2f(1), ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE);
        Client.RENDERER.getCrenderSystem().push(x + 6, y, width - 12, 24);
        updateScrollOffset();
        float iconX = icon != null ? 18 : 6;
        if (icon != null) {
            Client.RENDERER.text(icon, x + 6, y + height / 2F - 4.5F, TextureUse.ICONS, 7, ClientColors.DARK_GRAY_COLOR);
        }
        if (!text.isEmpty()) {
            Client.RENDERER.text(hideMask != null ? hideMask.repeat(text.length()) : text, x + iconX + scrollOffset, y + height / 2f - 4.5f, TextureUse.SFMEDIUM, 7,
                    style == UIStyle.TRANSPARENT && hover(mouseX, mouseY) ? ColorUtility.injectAlpha(ClientColors.FORE_COLOR, 155) : ClientColors.FORE_COLOR);
        } else if (!writing && desc != null && !desc.isEmpty()) {
            Client.RENDERER.text(desc, x + iconX + scrollOffset, y + height / 2f - 4.5f, TextureUse.SFMEDIUM, 7, ClientColors.DARK_GRAY_COLOR);
        }

        if (writing && hasSelection()) {
            int start = Math.min(cursor, selectionEnd);
            int end = Math.max(cursor, selectionEnd);
            String textBefore = safeSubstring(hideMask != null ? hideMask.repeat(text.length()) : text, 0, start);
            String selectedText = safeSubstring(hideMask != null ? hideMask.repeat(text.length()) : text, start, end);
            float startX = x + iconX + scrollOffset + Client.RENDERER.textWidth(textBefore, TextureUse.SFMEDIUM, 7);
            float widthX = Client.RENDERER.textWidth(selectedText, TextureUse.SFMEDIUM, 7) + 2;
            Client.RENDERER.rect(startX, y + height / 2f - 5.5f, widthX, 11,
                    new Vector4f(0), 0,
                    new Color(85, 133, 232, 155),
                    new Color(85, 133, 232, 155),
                    new Color(85, 133, 232, 155),
                    new Color(85, 133, 232, 155));
        }

        if (writing && active == this) {
            boolean shouldBlink = ((System.currentTimeMillis() / 500) % 2 == 0);
            String beforeCursor = safeSubstring(hideMask != null ? hideMask.repeat(text.length()) : text, 0, cursorPosition);
            float cursorX = x + iconX + scrollOffset + Client.RENDERER.textWidth(beforeCursor, TextureUse.SFMEDIUM, 7);
            if (cursorX >= x + iconX && cursorX <= x + width - iconX && shouldBlink) {
                Client.RENDERER.text("|", cursorX, y + height / 2f - 4.5f, TextureUse.SFMEDIUM, 7, ClientColors.FORE_COLOR);
            }
        }

        Client.RENDERER.getCrenderSystem().pop();

        if (dragging) {
            int newCursorPos = getCursorIndexAt(mouseX);
            selectionEnd = newCursorPos;
            cursorPosition = newCursorPos;
            updateScrollOffset();
            lastInputTime = System.currentTimeMillis();
        }
    }

    private void updateScrollOffset() {
        if (text.isEmpty()) {
            scrollOffset = 0;
            return;
        }

        float textWidth = Client.RENDERER.textWidth(text, TextureUse.SFMEDIUM, 7);
        float visibleWidth = width - 12;

        String beforeCursor = safeSubstring(text, 0, cursorPosition);
        float cursorX = Client.RENDERER.textWidth(beforeCursor, TextureUse.SFMEDIUM, 7);

        if (textWidth <= visibleWidth) {
            scrollOffset = 0;
            return;
        }

        float leftBound = -scrollOffset;
        float rightBound = leftBound + visibleWidth;

        if (cursorX < leftBound) {
            scrollOffset = -cursorX;
        } else if (cursorX > rightBound - 5) {
            scrollOffset = -(cursorX - (visibleWidth - 10));
        }

        float maxOffset = -(textWidth - visibleWidth + 5);
        scrollOffset = Math.max(maxOffset, Math.min(0, scrollOffset));
    }

    private boolean hasSelection() {
        return cursor != -1 && selectionEnd != -1 && cursor != selectionEnd;
    }

    private String safeSubstring(String str, int start, int end) {
        if (str == null || str.isEmpty()) return "";
        int len = str.length();
        start = Math.max(0, Math.min(start, len));
        end = Math.max(start, Math.min(end, len));
        return str.substring(start, end);
    }

    private int getCursorIndexAt(int mouseX) {
        float textStartX = x + (icon != null ? 18 : 6) + scrollOffset;
        if (mouseX <= textStartX) return 0;
        if (text.isEmpty()) return 0;

        float relativeX = mouseX - textStartX;

        for (int i = 0; i <= text.length(); i++) {
            String substr = safeSubstring(text, 0, i);
            float w = Client.RENDERER.textWidth(substr, TextureUse.SFMEDIUM, 7);
            if (w >= relativeX) {
                return Math.min(i, text.length());
            }
        }
        return text.length();
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (lastMouseX == -999 || lastMouseY == -999) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }


        if (hover(mouseX, mouseY)) {
            active = this;
            long currentTime = System.currentTimeMillis();
            writing = true;
            Client.GLASS_GUI.WRITING = true;
            resetCursor();

            int clickPos = getCursorIndexAt(mouseX);
            selectionEnd = clickPos;
            cursorPosition = clickPos;

            if (currentTime - lastClickTime < 250) {
                cursor = 0;
                selectionEnd = text.length();
                cursorPosition = selectionEnd;
            } else {
                cursor = clickPos;
            }

            dragging = true;
            lastClickTime = currentTime;
            lastInputTime = currentTime;
            return true;
        } else if (MathUtility.delta(lastMouseX, mouseX) + MathUtility.delta(lastMouseY, mouseY) < 10 || !hover(mouseX, mouseY)) {
            writing = false;
            Client.GLASS_GUI.WRITING = false;
            active = null;
            resetCursor();
            dragging = false;
            if (finishCallback != null)
                finishCallback.onChanged(text);
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;

        return super.click(mouseX, mouseY, button);
    }

    @Override
    public void chartyped(char ch, int keyCode) {
        if (writing && active == this && text.length() < limit) {
            deleteSelectedText();
            String left = safeSubstring(text, 0, cursorPosition);
            String right = safeSubstring(text, cursorPosition);
            text = left + ch + right;
            callback.onChanged(text);
            cursorPosition++;
            resetCursor();
            lastInputTime = System.currentTimeMillis();
        }
        super.chartyped(ch, keyCode);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!writing || active != this) return;
        if (hasControlDown()) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_A:
                    cursor = 0;
                    selectionEnd = text.length();
                    cursorPosition = selectionEnd;
                    break;
                case GLFW.GLFW_KEY_C:
                    copy();
                    break;
                case GLFW.GLFW_KEY_V:
                    paste();
                    break;
                case GLFW.GLFW_KEY_X:
                    copy();
                    deleteSelectedText();
                    break;
                case GLFW.GLFW_KEY_BACKSPACE:
                case GLFW.GLFW_KEY_DELETE:
                    deleteAll();
                    break;
            }
        } else {
            switch (keyCode) {
                case GLFW.GLFW_KEY_BACKSPACE:
                    if (hasSelection()) {
                        deleteSelectedText();
                    } else if (cursorPosition > 0) {
                        text = safeSubstring(text, 0, cursorPosition - 1) +
                                safeSubstring(text, cursorPosition);
                        callback.onChanged(text);
                        cursorPosition--;
                        resetCursor();
                    }
                    break;
                case GLFW.GLFW_KEY_DELETE:
                    if (hasSelection()) {
                        deleteSelectedText();
                    } else if (cursorPosition < text.length()) {
                        text = safeSubstring(text, 0, cursorPosition) +
                                safeSubstring(text, cursorPosition + 1);
                        callback.onChanged(text);
                        resetCursor();
                    }
                    break;
                case GLFW.GLFW_KEY_LEFT:
                case GLFW.GLFW_KEY_RIGHT:
                    moveCursor(keyCode);
                    break;
                case GLFW.GLFW_KEY_ESCAPE:
                case GLFW.GLFW_KEY_ENTER:
                    writing = false;
                    Client.GLASS_GUI.WRITING = false;
                    resetCursor();
                    if (finishCallback != null)
                        finishCallback.onChanged(text);
                    break;
            }
        }
        super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void release(int button) {
        dragging = false;
        super.release(button);
    }

    private void resetCursor() {
        cursor = -1;
        selectionEnd = -1;
    }

    private void moveCursor(int keyCode) {
        boolean shift = hasShiftDown();

        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (cursorPosition > 0) {
                cursorPosition--;
                if (shift) {
                    if (cursor == -1) cursor = cursorPosition + 1;
                    selectionEnd = cursorPosition;
                } else {
                    resetCursor();
                }
            } else if (!shift) {
                resetCursor();
            }
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (cursorPosition < text.length()) {
                cursorPosition++;
                if (shift) {
                    if (cursor == -1) cursor = cursorPosition - 1;
                    selectionEnd = cursorPosition;
                } else {
                    resetCursor();
                }
            } else if (!shift) {
                resetCursor();
            }
        }
        updateScrollOffset();
        lastInputTime = System.currentTimeMillis();
    }

    private void deleteSelectedText() {
        if (hasSelection()) {
            int start = Math.min(cursor, selectionEnd);
            int end = Math.max(cursor, selectionEnd);
            replaceText(start, end, "");
        }
    }

    private void replaceText(int start, int end, String replacement) {
        start = Math.max(0, Math.min(start, text.length()));
        end = Math.max(start, Math.min(end, text.length()));
        text = safeSubstring(text, 0, start) + replacement + safeSubstring(text, end);
        callback.onChanged(text);
        cursorPosition = start + replacement.length();
        resetCursor();
        lastInputTime = System.currentTimeMillis();
    }

    private String safeSubstring(String str, int start) {
        return safeSubstring(str, start, str.length());
    }

    private void copy() {
        if (hasSelection()) {
            int start = Math.min(cursor, selectionEnd);
            int end = Math.max(cursor, selectionEnd);
            String selected = safeSubstring(text, start, end);
            mc.keyboard.setClipboard(selected);
        } else {
            mc.keyboard.setClipboard(text);
        }
    }

    private void paste() {
        String clip = mc.keyboard.getClipboard();
        if (clip == null || clip.isEmpty()) return;

        clip = clip.replaceAll("[\\r\\n\\t]", "");

        deleteSelectedText();

        String left = safeSubstring(text, 0, cursorPosition);
        String right = safeSubstring(text, cursorPosition);
        text = left + clip + right;
        callback.onChanged(text);

        cursorPosition += clip.length();
        resetCursor();
        lastInputTime = System.currentTimeMillis();
    }

    private void deleteAll() {
        text = "";
        callback.onChanged(text);
        cursorPosition = 0;
        resetCursor();
    }

    public static boolean hasControlDown() {
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL) ||
                InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    public static boolean hasShiftDown() {
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) ||
                InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public static boolean hasAltDown() {
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_ALT) ||
                InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);
    }
}