package gg.topchdlc.vse.shutki.screen.screens.ingame.objects;

import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.ui.widgets.impl.ModuleBindWidget;
import gg.topchdlc.vse.shutki.module.modules.Tag;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import gg.topchdlc.vse.utils.animations.impl.SmoothStepAnimation;
import gg.topchdlc.vse.utils.math.Rectangle;
import gg.topchdlc.vse.utils.math.TimeUtility;
import net.fabricmc.loader.impl.util.StringUtil;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import gg.topchdlc.Client;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.api.render.RendererObject;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.ui.widgets.impl.ModuleSettingWidget;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.math.ColorUtility;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.animations.impl.EaseInOutQuad;

import java.awt.*;
import java.util.List;

public class ModuleRenderer extends RendererObject {
    public final Module module;
    public boolean expanded = false, binding = false, bindingScrollingDirection;
    public final List<SettingRenderer<?>> settingRenderers;

    public final EaseInOutQuad expandAnim = new EaseInOutQuad(300, 1);
    public final EaseInOutQuad enableAnim = new EaseInOutQuad(300, 1);
    public final SmoothStepAnimation bindingScrolling = new SmoothStepAnimation(1000, 1);
    public final SmoothStepAnimation nameScrolling = new SmoothStepAnimation(1000, 1);
    private TimeUtility nameScroll = new TimeUtility();

    public final ModuleSettingWidget settingWidget;

    public ModuleRenderer(Module module) {
        this.module = module;
        settingRenderers = module.getSettingRenderers();
        settingWidget = new ModuleSettingWidget(this);
        this.bindWidget = new ModuleBindWidget(this);
    }

    Rectangle bindButton = new Rectangle();

    ModuleBindWidget bindWidget;

    @Override
    public void render(int mouseX, int mouseY) {
        String[] descWrapped = StringUtil.wrapLines(module.getDesc(), 25).split("\n");
        height = 20 + (module.getDesc().isEmpty() ? 0 : descWrapped.length * 10);

        float pEnableAnim = (float) (enableAnim.getEndPoint() - enableAnim.getOutput());

        Color contrast = Color.WHITE;
        Color thumbColor = ColorUtility.linear(ClientColors.DARK_GRAY_COLOR, ClientColors.GREEN, pEnableAnim);
        Color thumbBackColor = ClientColors.GUI_STROKE;

        Client.RENDERER.outlined(x, y, width, height);

        Client.RENDERER.rect(x + width - 25, y + height / 2f - 5.5f, 20, 11, new Vector4f(9), 1, thumbBackColor, thumbBackColor, thumbBackColor, thumbBackColor);
        Client.RENDERER.rect(x + width - 23 + (8 * pEnableAnim), y + height / 2f - 4, 8, 8, new Vector4f(6), 2, thumbColor, thumbColor, thumbColor, thumbColor);

        {
            String bind = binding ? "..." : module.getKey() == -1 ? "None" : this.module.getKeyText();

            float tWidth = Client.RENDERER.textWidth(bind, TextureUse.SFMEDIUM, 6) + 4;

            boolean shouldScroll = tWidth > 25;
            float offset = tWidth - 25;

            bindButton.bound(x + width - 40, y + height / 2f - 6, 12, 12);
            Client.RENDERER.drawModuleRect(bindButton);
            Client.RENDERER.textCenteredStrict(IconUse.LINK.glyph, bindButton.getX() + bindButton.getWidth() / 2F - 0.25F, bindButton.getY() + bindButton.getHeight() / 2F - 0.25F, TextureUse.ICONS, 8, ClientColors.DARK_GRAY_COLOR);
        }

        if (!settingRenderers.isEmpty()) {
            Client.RENDERER.drawModuleRect(x + width - 56, y + height / 2f - 6, 12, 12);
            Client.RENDERER.textCenteredStrict(IconUse.GEAR.glyph, x + width - 50, y + height / 2f, TextureUse.ICONS, 8, ClientColors.DARK_GRAY_COLOR);
        }

        float cursorX = x + 8;
        float nameY = y + 5.5f;

        float nameWidth = Client.RENDERER.textWidth(module.getName(), TextureUse.SFMEDIUM, 8);
        Client.RENDERER.text(module.getName(), cursorX, nameY, TextureUse.SFMEDIUM, 8, contrast);
        cursorX += nameWidth + 6;

        if (!module.getTags().isEmpty()) {
            for (Tag tag : module.getTags()) {
                String tagText = tag.getName();
                float textWidth = Client.RENDERER.textWidth(tagText, TextureUse.SFMEDIUM, 6);
                float tagWidth = textWidth + 6;
                float tagHeight = 9;
                float tagX = cursorX;
                float tagY = nameY + 0.5f;

                Client.RENDERER.rect(tagX - 3, tagY, tagWidth, tagHeight, new Vector4f(4.5f), 2,
                        tag.getColor(), tag.getColor(), tag.getColor(), tag.getColor());

                Client.RENDERER.text(tagText, tagX - 1F, tagY + tagHeight / 2F - Client.RENDERER.textHeight(TextureUse.SFMEDIUM, 6) / 2F, TextureUse.SFMEDIUM, 6, Color.WHITE);

                cursorX += tagWidth + 2.5F;
            }
        }

        for (int i = 0; i < descWrapped.length; i++) {
            Client.RENDERER.text(descWrapped[i], x + 8, y + 16 + i * 10, TextureUse.SFMEDIUM, 7, ClientColors.DARK_GRAY_COLOR);
        }

        expandAnim.setDirection(expanded ? Direction.BACKWARDS : Direction.FORWARDS);
        expandAnim.setEndPoint(1);

        enableAnim.setDirection(module.isEnabled() ? Direction.BACKWARDS : Direction.FORWARDS);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        boolean hoverBind = bindButton.hovered(mouseX, mouseY);
        if (hoverBind && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            this.module.setKey(-1);
            return true;
        }
        if ((hoverBind || (hover(mouseX, mouseY) && button == 2)) && !Client.GLASS_GUI.BINDING) {
            Client.GLASS_GUI.WIDGETS.register(this.bindWidget);
            return true;
        }

        if (hover(mouseX, mouseY)) {
            if (button == 0)
                module.setEnabled(!module.isEnabled());
            else if (button == 1 && !module.getSettings().isEmpty()) {
                Client.GLASS_GUI.setCurrentSetting(this);
                Client.GLASS_GUI.getSettingAnim().setDirection(Direction.BACKWARDS); // dabbackwood
            }
            return true;
        }
        return false;
    }

    @Override
    public void chartyped(char ch, int keyCode) {
        super.chartyped(ch, keyCode);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE)
                keyCode = -1;
            module.setKey(keyCode);
            binding = false;
            Client.GLASS_GUI.BINDING = false;
        }
    }

    public boolean isExpanded() {
        return expanded;
    }
}
