package gg.topchdlc.api.ui.widgets.impl;

import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.ui.UIWidget;
import gg.topchdlc.api.ui.parts.impl.UITextField;
import gg.topchdlc.vse.shutki.other.macros.Macro;
import gg.topchdlc.vse.utils.animations.Animation;
import gg.topchdlc.vse.utils.animations.Easings;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.client.text.TextUtility;
import gg.topchdlc.vse.utils.math.ColorUtility;
import gg.topchdlc.vse.utils.math.MathUtility;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.List;

/**
 * Create by daun kvass
 */
public class HomeMenuWidget extends UIWidget {
    private final Animation animation = new Animation();
    private boolean closing = false;

    public enum Tab { FRIENDS, MACROS }
    private Tab currentTab = Tab.FRIENDS;

    private final UITextField friendInput = new UITextField();
    private final UITextField macroCommandInput = new UITextField();
    private int newMacroKey = -1;
    private boolean bindingNewMacro = false;
    private Macro editingMacro = null;

    private float friendScroll = 0f;
    private float friendScrollAnim = 0f;
    private float macroScroll = 0f;
    private float macroScrollAnim = 0f;

    public HomeMenuWidget(float targetX, float targetY) {
        this.x = targetX;
        this.y = targetY;
        this.width = 175f;
        this.height = 195f;

        this.animation.set(0.0);
        this.animation.run(1.0, 0.18, Easings.LINEAR);
    }

    private void close() {
        if (!closing) {
            closing = true;
            this.animation.run(0.0, 0.15, Easings.LINEAR);
        }
    }

    @Override
    public void render(int mouseX, int mouseY) {
        animation.update();
        float progress = animation.get();

        if (closing && animation.isFinished() && progress <= 0.01f) {
            this.shouldRemove = true;
            return;
        }

        int bgAlpha = (int) (215 * progress);
        int borderAlpha = (int) (30 * progress);
        Color glassBg = new Color(10, 11, 16, bgAlpha);
        Color outlineColor = new Color(255, 255, 255, borderAlpha);

        Client.RENDERER.blur(x, y, width, height, new Vector4f(6), 10f, progress);
        Client.RENDERER.rect(x, y, width, height, new Vector4f(6), 1, glassBg, glassBg, glassBg, glassBg);
        Client.RENDERER.outline(x, y, width, height, 0.3f, new Vector4f(6), new Vector2f(1), outlineColor, outlineColor, outlineColor, outlineColor);

        if (progress > 0.15f) {
            float tabW = (width - 12f) / 2f;
            float tabH = 14f;

            boolean hoveredF = MathUtility.mouseIn(x + 4f, y + 4f, tabW, tabH, mouseX, mouseY);
            Color bgF = (currentTab == Tab.FRIENDS) ? ColorUtility.injectAlpha(ClientSettings.INSTANCE.getColor(0),150) : (hoveredF ? new Color(255, 255, 255, 15) : new Color(255, 255, 255, 6));
            Client.RENDERER.rect(x + 4f, y + 4f, tabW, tabH, new Vector4f(3f), 1, bgF, bgF, bgF, bgF);
            Client.RENDERER.text("Друзья", x + 4f + (tabW - Client.RENDERER.textWidth("Друзья", TextureUse.SFMEDIUM, 5.8f)) / 2f, y + 8f, TextureUse.SFMEDIUM, 5.8f, Color.WHITE);

            boolean hoveredM = MathUtility.mouseIn(x + 8f + tabW, y + 4f, tabW, tabH, mouseX, mouseY);
            Color bgM = (currentTab == Tab.MACROS) ? ColorUtility.injectAlpha(ClientSettings.INSTANCE.getColor(0),150) : (hoveredM ? new Color(255, 255, 255, 15) : new Color(255, 255, 255, 6));
            Client.RENDERER.rect(x + 8f + tabW, y + 4f, tabW, tabH, new Vector4f(3f), 1, bgM, bgM, bgM, bgM);
            Client.RENDERER.text("Макросы", x + 8f + tabW + (tabW - Client.RENDERER.textWidth("Макросы", TextureUse.SFMEDIUM, 5.8f)) / 2f, y + 8f, TextureUse.SFMEDIUM, 5.8f, Color.WHITE);

            if (currentTab == Tab.FRIENDS) {
                renderFriendsTab(mouseX, mouseY);
            } else {
                renderMacrosTab(mouseX, mouseY);
            }
        }
    }

    private void renderFriendsTab(int mouseX, int mouseY) {
        float topY = y + 22f;

        float fieldW = width - 26f;
        friendInput.bound(x + 4f, topY, fieldW, 14f).render(mouseX, mouseY);
        float addX = x + width - 18f;
        boolean addHovered = MathUtility.mouseIn(addX, topY, 14f, 14f, mouseX, mouseY);
        Color addBg = addHovered ? ClientSettings.INSTANCE.getColor(0) : new Color(255, 255, 255, 12);
        Client.RENDERER.rect(addX, topY, 14f, 14f, new Vector4f(3f), 1, addBg, addBg, addBg, addBg);
        Client.RENDERER.textCenteredStrict(IconUse.ADD.glyph, addX + 7f, topY + 9.7f, TextureUse.ICONS, 5.5f, Color.WHITE);

        List<String> friends = Client.FRIENDS.getFriendList();
        float listY = topY + 18f;
        float listH = height - 44f;
        float itemH = 18f;
        float totalContentH = friends.size() * itemH;

        friendScroll = MathHelper.clamp(friendScroll, -Math.max(0, totalContentH - listH), 0);
        friendScrollAnim = MathUtility.linearFps(friendScrollAnim, friendScroll, 15f);

        Client.RENDERER.getCrenderSystem().push(x, listY, width, listH);
        float curY = listY + friendScrollAnim;

        for (int i = 0; i < friends.size(); i++) {
            String nick = friends.get(i);
            boolean hovered = MathUtility.mouseIn(x + 4f, curY, width - 8f, itemH - 2f, mouseX, mouseY);
            Color itemBg = hovered ? new Color(255, 255, 255, 12) : new Color(255, 255, 255, 5);
            Client.RENDERER.rect(x + 4f, curY, width - 8f, itemH - 2f, new Vector4f(3f), 1, itemBg, itemBg, itemBg, itemBg);
            Client.RENDERER.textCenteredStrict(IconUse.PERSONS.glyph, x + 12f, curY + (itemH / 2f) +3f, TextureUse.ICONS, 6f, ClientSettings.INSTANCE.getColor(0));
            Client.RENDERER.text(nick, x + 22f, curY + (itemH / 2f) - 3f, TextureUse.SFMEDIUM, 5.8f, Color.WHITE);
            float delX = x + width - 18f;
            boolean delHovered = MathUtility.mouseIn(delX, curY + 2f, 12f, 12f, mouseX, mouseY);
            Color delColor = delHovered ? Color.RED : new Color(160, 160, 165);
            Client.RENDERER.textCenteredStrict(IconUse.CROSS.glyph, delX + 6f, curY + (itemH / 2f) + 2f, TextureUse.ICONS, 5.5f, delColor);

            curY += itemH;
        }

        if (friends.isEmpty()) {
            Client.RENDERER.text("Список друзей пуст", x + (width - Client.RENDERER.textWidth("Список друзей пуст", TextureUse.SFMEDIUM, 5.5f)) / 2f, listY + 20f, TextureUse.SFMEDIUM, 5.5f, new Color(120, 120, 125));
        }

        Client.RENDERER.getCrenderSystem().pop();
    }

    private void renderMacrosTab(int mouseX, int mouseY) {
        float topY = y + 22f;

        float inputW = width - 52f;
        macroCommandInput.bound(x + 4f, topY, inputW, 14f).render(mouseX, mouseY);

        float bindX = x + inputW + 6f;
        float bindW = 22f;
        boolean bindHovered = MathUtility.mouseIn(bindX, topY, bindW, 14f, mouseX, mouseY);
        Color bindBg = bindingNewMacro ? new Color(255, 255, 255, 25) : (bindHovered ? new Color(255, 255, 255, 15) : new Color(255, 255, 255, 8));
        Client.RENDERER.rect(bindX, topY, bindW, 14f, new Vector4f(3f), 1, bindBg, bindBg, bindBg, bindBg);

        String bindText = bindingNewMacro ? "..." : (newMacroKey == -1 ? "NONE" : TextUtility.keyToString(newMacroKey));
        Client.RENDERER.textCenteredStrict(bindText, bindX + bindW / 2f, topY + 8.7f, TextureUse.SFMEDIUM, 5.2f, Color.WHITE);

        float addX = x + width - 18f;
        boolean addHovered = MathUtility.mouseIn(addX, topY, 14f, 14f, mouseX, mouseY);
        Color addBg = addHovered ? ClientSettings.INSTANCE.getColor(0) : new Color(255, 255, 255, 12);
        Client.RENDERER.rect(addX, topY, 14f, 14f, new Vector4f(3f), 1, addBg, addBg, addBg, addBg);
        Client.RENDERER.textCenteredStrict(IconUse.ADD.glyph, addX + 7f, topY + 9.7f, TextureUse.ICONS, 5.5f, Color.WHITE);

        List<Macro> macros = Client.MACRO_MANAGER.getMacros();
        float listY = topY + 18f;
        float listH = height - 44f;
        float itemH = 18f;
        float totalContentH = macros.size() * itemH;

        macroScroll = MathHelper.clamp(macroScroll, -Math.max(0, totalContentH - listH), 0);
        macroScrollAnim = MathUtility.linearFps(macroScrollAnim, macroScroll, 15f);

        Client.RENDERER.getCrenderSystem().push(x, listY, width, listH);
        float curY = listY + macroScrollAnim;

        for (int i = 0; i < macros.size(); i++) {
            Macro macro = macros.get(i);
            boolean hovered = MathUtility.mouseIn(x + 4f, curY, width - 8f, itemH - 2f, mouseX, mouseY);

            Color itemBg = hovered ? new Color(255, 255, 255, 12) : new Color(255, 255, 255, 5);
            Client.RENDERER.rect(x + 4f, curY, width - 8f, itemH - 2f, new Vector4f(3f), 1, itemBg, itemBg, itemBg, itemBg);

            String cmdText = macro.getMessage();
            Client.RENDERER.text(cmdText, x + 8f, curY + (itemH / 2f) - 3f, TextureUse.SFMEDIUM, 5.5f, Color.WHITE);

            float mBindX = x + width - 42f;
            float mBindW = 20f;
            boolean mBindHovered = MathUtility.mouseIn(mBindX, curY + 2f, mBindW, 12f, mouseX, mouseY);
            boolean isEditingThis = editingMacro == macro;

            Color mBindBg = isEditingThis ? new Color(255, 255, 255, 30) : (mBindHovered ? new Color(255, 255, 255, 18) : new Color(255, 255, 255, 10));
            Client.RENDERER.rect(mBindX, curY + 2f, mBindW, 12f, new Vector4f(2.5f), 1, mBindBg, mBindBg, mBindBg, mBindBg);

            String mKeyStr = isEditingThis ? "..." : (macro.getKey() == -1 ? "NONE" : TextUtility.keyToString(macro.getKey()));
            Client.RENDERER.textCenteredStrict(mKeyStr, mBindX + mBindW / 2f, curY + 10f, TextureUse.SFMEDIUM, 5.0f, Color.WHITE);
            float delX = x + width - 18f;
            boolean delHovered = MathUtility.mouseIn(delX, curY + 2f, 12f, 12f, mouseX, mouseY);
            Color delColor = delHovered ? Color.RED : new Color(160, 160, 165);
            Client.RENDERER.textCenteredStrict(IconUse.CROSS.glyph, delX + 6f, curY + (itemH / 2f) + 2f, TextureUse.ICONS, 5.5f, delColor);

            curY += itemH;
        }

        if (macros.isEmpty()) {
            Client.RENDERER.text("Список макросов пуст", x + (width - Client.RENDERER.textWidth("Список макросов пуст", TextureUse.SFMEDIUM, 5.5f)) / 2f, listY + 20f, TextureUse.SFMEDIUM, 5.5f, new Color(120, 120, 125));
        }

        Client.RENDERER.getCrenderSystem().pop();
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (!hover(mouseX, mouseY)) {
            close();
            return true;
        }

        float tabW = (width - 12f) / 2f;
        float tabH = 14f;

        if (MathUtility.mouseIn(x + 4f, y + 4f, tabW, tabH, mouseX, mouseY) && button == 0) {
            currentTab = Tab.FRIENDS;
            return true;
        }
        if (MathUtility.mouseIn(x + 8f + tabW, y + 4f, tabW, tabH, mouseX, mouseY) && button == 0) {
            currentTab = Tab.MACROS;
            return true;
        }

        float topY = y + 22f;

        if (currentTab == Tab.FRIENDS) {
            if (friendInput.click(mouseX, mouseY, button)) return true;

            float addX = x + width - 18f;
            if (MathUtility.mouseIn(addX, topY, 14f, 14f, mouseX, mouseY) && button == 0) {
                addFriendFromInput();
                return true;
            }

            List<String> friends = Client.FRIENDS.getFriendList();
            float listY = topY + 18f;
            float itemH = 18f;
            float curY = listY + friendScrollAnim;

            for (int i = 0; i < friends.size(); i++) {
                float delX = x + width - 18f;
                if (MathUtility.mouseIn(delX, curY + 2f, 12f, 12f, mouseX, mouseY) && button == 0) {
                    Client.FRIENDS.removeFriend(friends.get(i));
                    Client.FRIENDS.save();
                    return true;
                }
                curY += itemH;
            }
        } else {
            if (macroCommandInput.click(mouseX, mouseY, button)) return true;

            float inputW = width - 52f;
            float bindX = x + inputW + 6f;
            float bindW = 22f;
            if (MathUtility.mouseIn(bindX, topY, bindW, 14f, mouseX, mouseY) && button == 0) {
                bindingNewMacro = true;
                editingMacro = null;
                return true;
            }

            float addX = x + width - 18f;
            if (MathUtility.mouseIn(addX, topY, 14f, 14f, mouseX, mouseY) && button == 0) {
                addMacroFromInput();
                return true;
            }

            List<Macro> macros = Client.MACRO_MANAGER.getMacros();
            float listY = topY + 18f;
            float itemH = 18f;
            float curY = listY + macroScrollAnim;

            for (int i = 0; i < macros.size(); i++) {
                Macro macro = macros.get(i);

                float mBindX = x + width - 42f;
                float mBindW = 20f;
                if (MathUtility.mouseIn(mBindX, curY + 2f, mBindW, 12f, mouseX, mouseY) && button == 0) {
                    editingMacro = macro;
                    bindingNewMacro = false;
                    return true;
                }

                float delX = x + width - 18f;
                if (MathUtility.mouseIn(delX, curY + 2f, 12f, 12f, mouseX, mouseY) && button == 0) {
                    Client.MACRO_MANAGER.removeMacro(macro.getKey());
                    return true;
                }

                curY += itemH;
            }
        }

        return true;
    }

    private void addFriendFromInput() {
        String name = friendInput.getText().trim();
        if (!name.isEmpty() && !Client.FRIENDS.isFriend(name)) {
            Client.FRIENDS.addFriend(name);
            Client.FRIENDS.save();
            friendInput.setText("");
        }
    }

    private void addMacroFromInput() {
        String cmd = macroCommandInput.getText().trim();
        if (!cmd.isEmpty() && newMacroKey != -1) {
            Client.MACRO_MANAGER.addMacro(newMacroKey, cmd);
            macroCommandInput.setText("");
            newMacroKey = -1;
            bindingNewMacro = false;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (hover((int) mouseX, (int) mouseY)) {
            if (currentTab == Tab.FRIENDS) {
                friendScroll += (float) (verticalAmount * 14);
            } else {
                macroScroll += (float) (verticalAmount * 14);
            }
            return true;
        } else {
            close();
            return false;
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (currentTab == Tab.FRIENDS) {
            friendInput.keyPressed(keyCode, scanCode, modifiers);
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                addFriendFromInput();
            }
        } else {
            macroCommandInput.keyPressed(keyCode, scanCode, modifiers);

            if (bindingNewMacro) {
                newMacroKey = (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) ? -1 : keyCode;
                bindingNewMacro = false;
                return;
            }

            if (editingMacro != null) {
                int newKey = (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) ? -1 : keyCode;
                String msg = editingMacro.getMessage();
                Client.MACRO_MANAGER.removeMacro(editingMacro.getKey());
                if (newKey != -1) {
                    Client.MACRO_MANAGER.addMacro(newKey, msg);
                }
                editingMacro = null;
                return;
            }

            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                addMacroFromInput();
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
        }
    }

    @Override
    public void chartyped(char ch, int keyCode) {
        if (currentTab == Tab.FRIENDS) {
            friendInput.chartyped(ch, keyCode);
        } else {
            if (!bindingNewMacro && editingMacro == null) {
                macroCommandInput.chartyped(ch, keyCode);
            }
        }
    }
}