package gg.topchdlc.vse.shutki.screen.screens.ingame;

import com.google.gson.JsonObject;
import gg.topchdlc.Client;
import gg.topchdlc.MinecraftHolder;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventKey;
import gg.topchdlc.api.render.RendererObject;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.modules.impl.render.Interface;
import gg.topchdlc.vse.shutki.module.settings.SettingRenderer;
import gg.topchdlc.vse.shutki.module.settings.impl.button.ButtonSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.text.TextSetting;
import gg.topchdlc.vse.utils.animations.Animation;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.animations.Easings;
import gg.topchdlc.vse.utils.animations.impl.SmoothStepAnimation;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.client.client.SkinManager;
import gg.topchdlc.vse.utils.math.ColorUtility;
import gg.topchdlc.vse.utils.math.MathUtility;
import gg.topchdlc.vse.utils.math.easings.EasingEnum;
import gg.topchdlc.vse.utils.other.LogUtility;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GlassClickGui extends RendererObject implements MinecraftHolder {

    private boolean isOpened = false;
    public Drag drag;
    private SmoothStepAnimation animation;
    private Category currentCategory = Category.COMBAT;
    private float scroll = 0;
    private float scrollAnim = 0;
    private String searchText = "";
private boolean searchFocused = false;
    private float clientSettingsScroll = 0;
    private float clientSettingsScrollAnim = 0f;
    private float categorySwitchProgress = 1f;
    private static final Identifier LOGO = Identifier.of("topchdlc", "images/ui/logo.png");
    private final float WIDTH = 480f;
    private final float HEIGHT = 290f;
    public boolean WRITING = false;
    public boolean BINDING = false;
    public final Widgets WIDGETS = new Widgets();
    public Module bindingModule = null;
    private final SmoothStepAnimation settingAnim = new SmoothStepAnimation(300, 1);
    private String hoveredSettingDesc = null;
    private String lastHoveredDesc = "";
    private final Animation tooltipAnim = new Animation();

    public enum SettingsTab { CONFIGS, CLIENT, THEMES, PROXY, SKINS }
    private SettingsTab settingsTab = null;
    private final List<SettingRenderer<?>> clientTabRenderers;
    private final List<SettingRenderer<?>> themeTabRenderers;
    private final List<SettingRenderer<?>> proxyTabRenderers;
    private final List<SettingRenderer<?>> skinTabRenderers;
    private final String[][] recommendedSkins = {
            {"Danhber", "Главный разработчик чита"},
            {"CupOfCola", "Друг разработчика"},
            {"Sp1dyk", "Друг разработчика 2"},
            {"marko228777", "Друг разработчика 3"},
            {"WURER2", "Тестирующий чита"},
            {"Notch", "Создатель Minecraft"},
            {"jeb_", "Разработчик Minecraft"},
            {"Dream", "Известный ютубер"},
            {"Technoblade", "Легенда Minecraft"},
            {"TommyInnit", "Известный стример"},
    };
    private final TextSetting skinSearch = new TextSetting("Поиск скина по нику", "");
    private final SettingRenderer<?> skinSearchRenderer = skinSearch.wrap();
    private float skinScroll = 0f;
    private float skinScrollAnim = 0f;
    private final List<float[]> skinRowBounds = new ArrayList<>();
    private String skinLastTyped = "";
    private long skinSearchChangedAt = 0;
    private String skinLastRequested = "";
    private final TextSetting configName = new TextSetting("Название", "config");
    private final ButtonSetting configSaveButton = new ButtonSetting("Сохранить конфиг", this::saveConfig);
    private final SettingRenderer<?> configNameRenderer = configName.wrap();
    private final SettingRenderer<?> configSaveRenderer = configSaveButton.wrap();
    private final List<float[]> configRowBounds = new ArrayList<>();

    // Friends tab
    private final TextSetting friendInput = new TextSetting("Ник", "");
    private final SettingRenderer<?> friendInputRenderer = friendInput.wrap();
    private final List<float[]> friendRowBounds = new ArrayList<>();
    private float friendScroll = 0f;
    private float friendScrollAnim = 0f;
    private boolean friendInputFocused = false;

    public SmoothStepAnimation getSettingAnim() {
        return settingAnim;
    }

    public Category getCurrent() {
        return currentCategory;
    }

    public void setCurrentSetting(Object obj) {
    }

    private final Set<Module> expandedModules = new HashSet<>();
    private final Map<Module, Animation> moduleExpandAnims = new HashMap<>();
    private final Map<Module, Double> moduleExpandTargets = new HashMap<>();
    private final Map<Category, Float> categoryWidths = new HashMap<>();
    private final Map<Module, Float> moduleToggleAnims = new HashMap<>();

    public GlassClickGui() {
        Client.EVENTS.register(this);
        this.drag = new Drag("GlassClickGui", () -> this.isOpened);
        this.animation = new SmoothStepAnimation(200, 1);
        drag.x = window.getScaledWidth() / 2f - WIDTH / 2f;
        drag.y = window.getScaledHeight() / 2f - HEIGHT / 2f;
        drag.width = WIDTH;
        drag.height = HEIGHT;

        this.clientTabRenderers = List.of(
                ClientSettings.INSTANCE.clickguiBind.wrap(),
                ClientSettings.INSTANCE.radialItemKey.wrap(),
                ClientSettings.INSTANCE.radialPotionKey.wrap(),
                ClientSettings.INSTANCE.customizeSetting.wrap()
        );
        this.themeTabRenderers = new ArrayList<>(List.of(ClientSettings.INSTANCE.getTheme().wrap()));

        Group interfaceTheme = new Group("Тема интерфейса");
        interfaceTheme.add(Interface.INSTANCE.hudStyle);
        interfaceTheme.add(Interface.INSTANCE.style);
        this.themeTabRenderers.add(interfaceTheme.wrap());
        this.proxyTabRenderers = List.of(ClientSettings.INSTANCE.proxy.wrap());
        this.skinTabRenderers = List.of(
                ClientSettings.INSTANCE.customizeSetting.skinName.wrap(),
                ClientSettings.INSTANCE.customizeSetting.applySkin.wrap()
        );

        for (Category cat : Category.values()) {
            categoryWidths.put(cat, 20f);
        }
    }

    public void save(JsonObject json) {
        JsonObject guiState = new JsonObject();
        guiState.addProperty("category", currentCategory.name());
        guiState.addProperty("scroll", scroll);
        json.add("clickgui", guiState);
    }

    public void load(JsonObject json) {
        if (!json.has("clickgui")) return;
        JsonObject guiState = json.getAsJsonObject("clickgui");
        if (guiState.has("category")) {
            try {
                currentCategory = Category.valueOf(guiState.get("category").getAsString());
            } catch (Exception ignored) {
            }
        }
        if (guiState.has("scroll")) {
            scroll = guiState.get("scroll").getAsFloat();
        }
    }

    public void setOpened(boolean opened) {
        this.isOpened = opened;
        if (!opened) {
            searchFocused = false;
            synchronized (WIDGETS) {
                WIDGETS.get().clear();
            }
            if (mc.mouse != null) {
                mc.mouse.lockCursor();
            }
        } else {
            if (mc.mouse != null) {
                mc.mouse.unlockCursor();
            }
        }
    }

    public void resetGui() {
        drag.width = WIDTH;
        drag.height = HEIGHT;
        drag.x = window.getScaledWidth() / 2f - WIDTH / 2f;
        drag.y = window.getScaledHeight() / 2f - HEIGHT / 2f;
        scroll = 0;
        scrollAnim = 0;
        expandedModules.clear();
        searchText = "";
        categorySwitchProgress = 1f;
    }

    public boolean isOpened() {
        return isOpened;
    }

    EventBus<EventKey> eventKey = event -> {
        int key = event.getKey();

        if (key == ClientSettings.INSTANCE.clickguiBind.getBind()) {
            if (event.action == 0) {
                if (Client.IS_PANIC) return;
                if (!isOpened && mc.currentScreen == null) {
                    setOpened(true);
                }
            }
            event.cancel();
        } else if (key == GLFW.GLFW_KEY_ESCAPE && isOpened) {
            if (event.action == 0) {
                boolean closedWidget = false;
                synchronized (WIDGETS) {
                    if (!WIDGETS.get().isEmpty()) {
                        for (var widget : new ArrayList<>(WIDGETS.get())) {
                            widget.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
                            WIDGETS.unregister(widget);
                        }
                        closedWidget = true;
                    }
                }

                if (closedWidget) {
                    event.cancel();
                    return;
                }
                if (searchFocused) {
                    searchFocused = false;
                    event.cancel();
                    return;
                }
                if (!expandedModules.isEmpty()) {
                    expandedModules.clear();
                    event.cancel();
                    return;
                }
                setOpened(false);
            }
            event.cancel();
        }
    };

    @Override
    public void render(int mouseX, int mouseY) {
        if (!Client.INITIALIZED) return;

        Client.RENDERER.getCrenderSystem().layer(CRenderSystem.RenderLayer.OVERLAY);

        animation.setDirection(isOpened ? Direction.BACKWARDS : Direction.FORWARDS);
        float anim = 1f - animation.getOutput();
        if (anim < 0.05f) return;

        hoveredSettingDesc = null;

        if (drag.dragging) {
            drag.x = MathUtility.linear(drag.x, mouseX - drag.dX, 0.5f);
            drag.y = MathUtility.linear(drag.y, mouseY - drag.dY, 0.5f);
        }

        scrollAnim = MathUtility.linearFps(scrollAnim, scroll, 10f);
        clientSettingsScrollAnim = MathUtility.linearFps(clientSettingsScrollAnim, clientSettingsScroll, 10f);
        categorySwitchProgress = MathUtility.linearFps(categorySwitchProgress, 1f, 10f);
        MatrixStack stack = Client.RENDERER.getStack();
        Client.RENDERER.getCrenderSystem().alpha(anim);

        stack.push();

        try {
            MathUtility.scale(stack, drag.x + WIDTH / 2f, drag.y + HEIGHT / 2f, anim * 0.05f + 0.95f);

            Client.RENDERER.blur(drag.x, drag.y, WIDTH, HEIGHT, new Vector4f(10), 12f, 1f);
            Color mainBg = new Color(8, 9, 14, ClientSettings.INSTANCE.menuOpacity.getInt());
            Client.RENDERER.rect(drag.x, drag.y, WIDTH, HEIGHT, new Vector4f(10), 1, mainBg, mainBg, mainBg, mainBg);
            Color border = new Color(255, 255, 255, 8);
            Client.RENDERER.outline(drag.x, drag.y, WIDTH, HEIGHT, 0.2f, new Vector4f(10), new Vector2f(1), border, border, border, border);

            renderBrand();

            renderTopSearch(mouseX, mouseY);

            if (currentCategory == Category.Settings) {
                renderClientSettingsTab(mouseX, mouseY);
            } else if (currentCategory == Category.Friends) {
                renderFriendsTab(mouseX, mouseY);
            } else {
                renderModulesGrid(mouseX, mouseY);
            }
            renderBottomCategories(mouseX, mouseY);

            synchronized (WIDGETS) {
                WIDGETS.render(mouseX, mouseY);
            }

        } finally {
            stack.pop();
            Client.RENDERER.getCrenderSystem().alpha(1f);
        }

        if (hoveredSettingDesc != null) {
            lastHoveredDesc = hoveredSettingDesc;
            tooltipAnim.run(1.0, 0.15, Easings.LINEAR);
        } else {
            tooltipAnim.run(0.0, 0.12, Easings.LINEAR);
        }
        tooltipAnim.update();

        if (tooltipAnim.get() > 0.01f && !lastHoveredDesc.isEmpty()) {
            renderHoveredTooltip(mouseX, mouseY, tooltipAnim.get(), lastHoveredDesc);
        }
    }

    private void renderHoveredTooltip(int mouseX, int mouseY, float progress, String text) {
        float fontSize = 5.5f;
        float textW = Client.RENDERER.textWidth(text, TextureUse.SFMEDIUM, fontSize);
        float pad = 5f;
        float tooltipW = textW + pad * 2f;
        float tooltipH = 13f;

        float tooltipX = mouseX + 10f;
        float tooltipY = mouseY - 5f;

        if (tooltipX + tooltipW > window.getScaledWidth()) {
            tooltipX = mouseX - tooltipW - 5f;
        }
        if (tooltipY + tooltipH > window.getScaledHeight()) {
            tooltipY = mouseY - tooltipH - 5f;
        }

        int bgAlpha = (int) (220 * progress);
        int borderAlpha = (int) (30 * progress);

        Color bg = new Color(10, 12, 18, bgAlpha);
        Color border = new Color(255, 255, 255, borderAlpha);

        Client.RENDERER.blur(tooltipX, tooltipY, tooltipW, tooltipH, new Vector4f(4), 8f, progress);
        Client.RENDERER.rect(tooltipX, tooltipY, tooltipW, tooltipH, new Vector4f(4), 1, bg, bg, bg, bg);
        Client.RENDERER.outline(tooltipX, tooltipY, tooltipW, tooltipH, 0.3f, new Vector4f(4), new Vector2f(1), border, border, border, border);

        Color textColor = new Color(255, 255, 255, (int) (255 * progress));
        Client.RENDERER.text(text, tooltipX + pad, tooltipY + (tooltipH - fontSize) / 2f - 0.5f, TextureUse.SFMEDIUM, fontSize, textColor);
    }

    private void renderBottomCategories(int mouseX, int mouseY) {
        Category[] categories = Category.values();

        float totalCatWidth = 0;
        float[] currentWidths = new float[categories.length];

        for (int i = 0; i < categories.length; i++) {
            Category cat = categories[i];
            boolean selected = (currentCategory == cat);
            float textW = Client.RENDERER.textWidth(cat.name(), TextureUse.SFMEDIUM, 5.5f);
            float targetW = selected ? (18f + textW + 8f) : 20f;

            float curW = MathUtility.linearFps(categoryWidths.getOrDefault(cat, 20f), targetW, 10f);
            categoryWidths.put(cat, curW);
            currentWidths[i] = curW;
            totalCatWidth += curW + 3f;
        }
        totalCatWidth -= 3f;

        float startX = drag.x + (WIDTH - totalCatWidth) / 2f;
        float startY = drag.y + HEIGHT - 21f;
        float barH = 18f;

        Color barBg = new Color(6, 6, 10, 25);
        Client.RENDERER.rect(startX - 4, startY - 2, totalCatWidth + 8, barH, new Vector4f(6), 1, barBg, barBg, barBg, barBg);

        float curX = startX;
        for (int i = 0; i < categories.length; i++) {
            Category cat = categories[i];
            float w = currentWidths[i];
            boolean selected = (currentCategory == cat);
            boolean hovered = MathUtility.mouseIn(curX, startY, w, 15, mouseX, mouseY);

            if (selected) {
                Color selBg = new Color(255, 255, 255, 12);
                Color selBorder = new Color(255, 255, 255, 6);
                Client.RENDERER.rect(curX, startY, w, 15, new Vector4f(4), 1, selBg, selBg, selBg, selBg);
                Client.RENDERER.outline(curX, startY, w, 15, 0.2f, new Vector4f(4), new Vector2f(1), selBorder, selBorder, selBorder, selBorder);
            } else if (hovered) {
                Color hovBg = new Color(255, 255, 255, 5);
                Client.RENDERER.rect(curX, startY, w, 15, new Vector4f(4), 1, hovBg, hovBg, hovBg, hovBg);
            }

            Color iconColor = selected ? Color.WHITE : (hovered ? new Color(180, 180, 185) : new Color(100, 100, 105));
            Client.RENDERER.text(getIconForCategory(cat), curX + 5, startY + 3.5f, TextureUse.ICONS, 5.2f, iconColor);

            if (w > 24f) {
                Client.RENDERER.getCrenderSystem().push(curX + 14, startY, w - 14, 15);
                Color textColor = selected ? Color.WHITE : new Color(160, 160, 165);
                Client.RENDERER.text(cat.name(), curX + 15, startY + 4f, TextureUse.SFMEDIUM, 5.5f, textColor);
                Client.RENDERER.getCrenderSystem().pop();
            }

            curX += w + 3f;
        }
    }

    private void renderModulesGrid(int mouseX, int mouseY) {
        float contentX = drag.x + 10;
        float contentY = drag.y + 28;
        float contentW = WIDTH - 20;
        float contentH = HEIGHT - 52;

        List<Module> modules = getFilteredModules();
        float gap = 5f;
        int numCols = 2;
        float cardW = (contentW - gap) / numCols;
        float headerH = 28f;

        float catAlpha = (float) EasingEnum.CubicOut.ease(categorySwitchProgress);
        float catYOffset = (1f - catAlpha) * 6f;

        Client.RENDERER.getCrenderSystem().alpha(catAlpha);
        Client.RENDERER.getCrenderSystem().push(contentX, contentY, contentW, contentH);

        boolean hasAnyExpandedInGrid = !expandedModules.isEmpty();
        float[] colY = new float[]{scrollAnim + catYOffset, scrollAnim + catYOffset};
        MatrixStack stack = Client.RENDERER.getStack();

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            int col = i % numCols;

            boolean isExpanded = expandedModules.contains(module);
            boolean enabled = module.isEnabled();
            Animation anim = moduleExpandAnims.computeIfAbsent(module, m -> new Animation());
            double target = isExpanded ? 1.0 : 0.0;
            double lastTarget = moduleExpandTargets.getOrDefault(module, -1.0);
            if (target != lastTarget) {
                anim.run(target, 0.22, Easings.LINEAR);
                moduleExpandTargets.put(module, target);
            }
            anim.update();
            float easedProgress = anim.get();

            float fullSettingsH = 0f;
            for (SettingRenderer<?> setting : module.getSettingRenderers()) {
                boolean isVisible = setting.getSetting().getVisible().get();
                setting.visible.setDirection(isVisible ? Direction.FORWARDS : Direction.BACKWARDS);
                float visAnim = setting.visible.getOutput();

                if (visAnim > 0.001f) {
                    float h = Math.max(14f, setting.getHeight());
                    fullSettingsH += (h + 2.5f) * visAnim;
                }
            }
            if (fullSettingsH > 0) fullSettingsH += 4f;

            float currentSettingsH = fullSettingsH * easedProgress;
            float cardH = headerH + currentSettingsH;

            float cardX = contentX + col * (cardW + gap);
            float cardY = contentY + colY[col];

            boolean hovered = MathUtility.mouseIn(cardX, cardY, cardW, cardH, mouseX, mouseY);
            float alphaFactor = (hasAnyExpandedInGrid && !isExpanded) ? 0.35f : 1.0f;

            Color cardBg;
            if (isExpanded) {
                cardBg = applyAlpha(new Color(12, 14, 20, 95), alphaFactor);
            } else if (enabled) {
                cardBg = applyAlpha(new Color(16, 18, 26, 60), alphaFactor);
            } else {
                cardBg = applyAlpha(hovered ? new Color(255, 255, 255, 6) : new Color(255, 255, 255, 3), alphaFactor);
            }

            Client.RENDERER.rect(cardX, cardY, cardW, cardH, new Vector4f(6), 1, cardBg, cardBg, cardBg, cardBg);
            Color outlineColor = isExpanded ? new Color(255, 255, 255, 10) : (enabled ? new Color(255, 255, 255, 5) : new Color(255, 255, 255, 2));
            Client.RENDERER.outline(cardX, cardY, cardW, cardH, 0.1f, new Vector4f(6), new Vector2f(1), applyAlpha(outlineColor, alphaFactor), applyAlpha(outlineColor, alphaFactor), applyAlpha(outlineColor, alphaFactor), applyAlpha(outlineColor, alphaFactor));
            Color textColor = isExpanded ? Color.WHITE : (enabled ? new Color(230, 230, 235) : new Color(135, 135, 140));
            Client.RENDERER.text(trimToWidth(module.getName(), cardW - 32f, 5.5f), cardX + 6, cardY + 5, TextureUse.SFMEDIUM, 5.5f, applyAlpha(textColor, alphaFactor));
            String desc = module.getDescription() != null ? module.getDescription() : "";
            if (!desc.isEmpty()) {
                Client.RENDERER.text(trimToWidth(desc, cardW - 32f, 4.3f), cardX + 6, cardY + 16, TextureUse.SFMEDIUM, 4.3f, applyAlpha(new Color(95, 95, 100), alphaFactor));
            }
            float toggleX = cardX + cardW - 23;
            float toggleY = cardY + 9;
            renderToggleSwitch(module, toggleX, toggleY, alphaFactor);

            if (easedProgress > 0.01f && fullSettingsH > 0) {
                Client.RENDERER.getCrenderSystem().push(cardX, cardY + headerH, cardW, currentSettingsH);
                float curSettingY = cardY + headerH;

                for (SettingRenderer<?> setting : module.getSettingRenderers()) {
                    float visAnim = setting.visible.getOutput();
                    if (visAnim <= 0.001f && !setting.getSetting().getVisible().get()) continue;

                    float realH = Math.max(14f, setting.getHeight());
                    float animH = realH * visAnim;



                    stack.push();
                    MathUtility.scale(stack, cardX + cardW / 2f, curSettingY + animH / 2f, 1.0f, visAnim);

                    setting.bound(cardX + 5, curSettingY, cardW - 10, realH);
                    setting.render(mouseX, mouseY);

                    stack.pop();

                    curSettingY += (realH + 2.5f) * visAnim;
                }
                Client.RENDERER.getCrenderSystem().pop();
            }

            colY[col] += cardH + gap;
        }

        float maxColY = Math.max(colY[0], colY[1]) - (scrollAnim + catYOffset);
        float maxScroll = Math.max(0, maxColY - contentH + 10);
        scroll = Math.max(-maxScroll, Math.min(0, scroll));

        Client.RENDERER.getCrenderSystem().pop();
        Client.RENDERER.getCrenderSystem().alpha(1f);
    }

    private void renderToggleSwitch(Module module, float x, float y, float alphaFactor) {
        float currentAnim = moduleToggleAnims.getOrDefault(module, 0f);
        float targetAnim = module.isEnabled() ? 1f : 0f;
        currentAnim = MathUtility.linearFps(currentAnim, targetAnim, 10f);
        moduleToggleAnims.put(module, currentAnim);

        Color thumbBackColor = applyAlpha(new Color(35, 35, 35, 90), alphaFactor);
        Color thumbColor = applyAlpha(ColorUtility.linear(ClientColors.DARK_GRAY_COLOR, ClientSettings.INSTANCE.getColor(0), currentAnim), alphaFactor);

        Client.RENDERER.rect(x, y, 17, 8f, new Vector4f(4f), 1, thumbBackColor, thumbBackColor, thumbBackColor, thumbBackColor);
        float knobX = x + 1.2f + (8f * currentAnim);
        Client.RENDERER.rect(knobX, y + 1f, 6.5f, 6f, new Vector4f(3f), 1, thumbColor, thumbColor, thumbColor, thumbColor);
    }

    private void renderBrand() {
        float logoSize = 9f;
        float logoX = drag.x + 10f;
        float logoY = drag.y + 10f;
        Client.RENDERER.texture(LOGO, logoX, logoY, logoSize, logoSize, 1f, new Vector4f(0),
                Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);

        String name = "TopchDLC";
        float textSize = 6f;
        float textX = logoX + logoSize + 3f;
        float textY = logoY + logoSize / 2f - Client.RENDERER.textHeight(TextureUse.SFMEDIUM, textSize) / 2f;

        Color glow = new Color(40, 110, 255);
        for (float radius = 1.6f; radius >= 0.4f; radius -= 0.4f) {
            Color layer = new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 26);
            Client.RENDERER.text(name, textX - radius, textY, TextureUse.SFMEDIUM, textSize, layer);
            Client.RENDERER.text(name, textX + radius, textY, TextureUse.SFMEDIUM, textSize, layer);
            Client.RENDERER.text(name, textX, textY - radius, TextureUse.SFMEDIUM, textSize, layer);
            Client.RENDERER.text(name, textX, textY + radius, TextureUse.SFMEDIUM, textSize, layer);
        }
        Client.RENDERER.text(name, textX, textY, TextureUse.SFMEDIUM, textSize, new Color(120, 190, 255));
    }

    private void renderTopSearch(int mouseX, int mouseY) {
        float searchW = 110f;
        float searchH = 17f;
        float searchX = drag.x + (WIDTH - searchW) / 2f;
        float searchY = drag.y + 6f;

        Color searchBg = searchFocused ? new Color(255, 255, 255, 12) : new Color(12, 12, 16, 100);
        Client.RENDERER.rect(searchX, searchY, searchW, searchH, new Vector4f(8), 1, searchBg, searchBg, searchBg, searchBg);
        Color searchBorder = new Color(255, 255, 255, searchFocused ? 25 : 8);
        Client.RENDERER.outline(searchX, searchY, searchW, searchH, 0.5f, new Vector4f(8), new Vector2f(1), searchBorder, searchBorder, searchBorder, searchBorder);

        Client.RENDERER.text(IconUse.SEARCH, searchX + 6, searchY + 5, TextureUse.ICONS, 5f, new Color(120, 120, 125));
        String text = searchText.isEmpty() ? "Поиск..." : searchText;
        Color textColor = searchText.isEmpty() ? new Color(90, 90, 95) : Color.WHITE;
        Client.RENDERER.text(text, searchX + 16, searchY + 5.5f, TextureUse.SFMEDIUM, 5f, textColor);

        if (searchFocused && System.currentTimeMillis() % 1000 < 500) {
            float cursorX = searchX + 16 + Client.RENDERER.textWidth(searchText, TextureUse.SFMEDIUM, 5f);
            Client.RENDERER.rect(cursorX, searchY + 4, 1f, 8, new Vector4f(0), 1, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);
        }
    }

    private void renderFriendsTab(int mouseX, int mouseY) {
        float contentX = drag.x + 10;
        float contentY = drag.y + 28;
        float contentW = WIDTH - 20;
        float contentH = HEIGHT - 52;

        float catAlpha = (float) EasingEnum.CubicOut.ease(categorySwitchProgress);
        Client.RENDERER.getCrenderSystem().alpha(catAlpha);
        Client.RENDERER.getCrenderSystem().push(contentX, contentY, contentW, contentH);

        // Input field + add button
        float fieldW = contentW - 40f;
        float fieldH = 18f;
        friendInputRenderer.bound(contentX + 4, contentY + 2, fieldW, fieldH);
        friendInputRenderer.render(mouseX, mouseY);

        float addX = contentX + contentW - 32f;
        boolean addHovered = MathUtility.mouseIn(addX, contentY + 2, 28f, fieldH, mouseX, mouseY);
        Color addBg = addHovered ? ClientSettings.INSTANCE.getColor(0) : new Color(255, 255, 255, 12);
        Client.RENDERER.rect(addX, contentY + 2, 28f, fieldH, new Vector4f(4), 1, addBg, addBg, addBg, addBg);
        Client.RENDERER.textCenteredStrict(IconUse.ADD.glyph, addX + 14f, contentY + 11f, TextureUse.ICONS, 5.5f, Color.WHITE);

        // Friends list
        List<String> friends = Client.FRIENDS.getFriendList();
        float listY = contentY + 24f;
        float listH = contentH - 26f;
        float itemH = 20f;
        float totalContentH = friends.size() * itemH;

        friendScroll = Math.max(-Math.max(0, totalContentH - listH), Math.min(0, friendScroll));
        friendScrollAnim = MathUtility.linearFps(friendScrollAnim, friendScroll, 15f);

        friendRowBounds.clear();
        float curY = listY + friendScrollAnim;

        for (int i = 0; i < friends.size(); i++) {
            String nick = friends.get(i);
            boolean hovered = MathUtility.mouseIn(contentX + 4, curY, contentW - 8f, itemH - 2f, mouseX, mouseY);
            Color itemBg = hovered ? new Color(255, 255, 255, 12) : new Color(255, 255, 255, 5);
            Client.RENDERER.rect(contentX + 4, curY, contentW - 8f, itemH - 2f, new Vector4f(4), 1, itemBg, itemBg, itemBg, itemBg);
            Client.RENDERER.textCenteredStrict(IconUse.PERSONS.glyph, contentX + 12f, curY + (itemH / 2f) + 3f, TextureUse.ICONS, 6f, ClientSettings.INSTANCE.getColor(0));
            Client.RENDERER.text(nick, contentX + 22f, curY + (itemH / 2f) - 3f, TextureUse.SFMEDIUM, 5.8f, Color.WHITE);

            float delX = contentX + contentW - 18f;
            boolean delHovered = MathUtility.mouseIn(delX, curY + 2f, 12f, 12f, mouseX, mouseY);
            Color delColor = delHovered ? Color.RED : new Color(160, 160, 165);
            Client.RENDERER.textCenteredStrict(IconUse.CROSS.glyph, delX + 6f, curY + (itemH / 2f) + 2f, TextureUse.ICONS, 5.5f, delColor);

            friendRowBounds.add(new float[]{contentX + 4, curY, contentW - 8f, itemH - 2f, i});
            curY += itemH;
        }

        if (friends.isEmpty()) {
            Client.RENDERER.text("Список друзей пуст", contentX + (contentW - Client.RENDERER.textWidth("Список друзей пуст", TextureUse.SFMEDIUM, 5.5f)) / 2f, listY + 20f, TextureUse.SFMEDIUM, 5.5f, new Color(120, 120, 125));
        }

        Client.RENDERER.getCrenderSystem().pop();
        Client.RENDERER.getCrenderSystem().alpha(1f);
    }

    private void renderClientSettingsTab(int mouseX, int mouseY) {
        float contentX = drag.x + 10;
        float contentY = drag.y + 28;
        float contentW = WIDTH - 20;
        float contentH = HEIGHT - 52;

        Client.RENDERER.getCrenderSystem().push(contentX, contentY, contentW, contentH);

        if (settingsTab == null) {
            renderSettingsSquares(mouseX, mouseY, contentX, contentY, contentW, contentH);
        } else {
            renderSettingsTabPanel(mouseX, mouseY, contentX, contentY, contentW, contentH);
        }

        Client.RENDERER.getCrenderSystem().pop();
    }

    private void renderSettingsSquares(int mouseX, int mouseY, float contentX, float contentY, float contentW, float contentH) {
        float gap = 6f;
        int cols = 3;
        float w = (contentW - (cols - 1) * gap) / cols;
        float h = (contentH - gap) / 2f;

        SettingsTab[] tabs = { SettingsTab.CONFIGS, SettingsTab.CLIENT, SettingsTab.THEMES, SettingsTab.PROXY, SettingsTab.SKINS };
        float[] xs = new float[tabs.length];
        float[] ys = new float[tabs.length];
        for (int i = 0; i < tabs.length; i++) {
            int col = i % cols;
            int row = i / cols;
            xs[i] = contentX + col * (w + gap);
            ys[i] = contentY + row * (h + gap);
        }

        for (int i = 0; i < tabs.length; i++) {
            SettingsTab tab = tabs[i];
            float rx = xs[i], ry = ys[i];
            boolean hovered = MathUtility.mouseIn(rx, ry, w, h, mouseX, mouseY);

            Color cBg = hovered ? new Color(255, 255, 255, 8) : new Color(255, 255, 255, 3);
            Client.RENDERER.rect(rx, ry, w, h, new Vector4f(8), 1, cBg, cBg, cBg, cBg);
            Color cOut = hovered ? new Color(255, 255, 255, 25) : new Color(255, 255, 255, 6);
            Client.RENDERER.outline(rx, ry, w, h, 0.4f, new Vector4f(8), new Vector2f(1), cOut, cOut, cOut, cOut);

            IconUse icon = iconForTab(tab);
            Client.RENDERER.getCrenderSystem().push(rx, ry, w, h);
            Client.RENDERER.text(icon, rx + 12, ry + 14, TextureUse.ICONS, 9f, hovered ? Color.WHITE : new Color(150, 150, 155));
            Client.RENDERER.text(tabTitle(tab), rx + 12, ry + 36, TextureUse.SFMEDIUM, 6.5f, hovered ? Color.WHITE : new Color(200, 200, 205));
            Client.RENDERER.text(tabSubtitle(tab), rx + 12, ry + 48, TextureUse.SFMEDIUM, 4.3f, new Color(110, 110, 115));
            Client.RENDERER.getCrenderSystem().pop();
        }
    }

    private void renderSettingsTabPanel(int mouseX, int mouseY, float contentX, float contentY, float contentW, float contentH) {
        boolean backHover = MathUtility.mouseIn(contentX + 2, contentY + 2, 18, 18, mouseX, mouseY);
        Color backBg = backHover ? new Color(255, 255, 255, 10) : new Color(255, 255, 255, 3);
        Client.RENDERER.rect(contentX + 2, contentY + 2, 18, 18, new Vector4f(5), 1, backBg, backBg, backBg, backBg);
        Client.RENDERER.text(IconUse.BACK, contentX + 6.5f, contentY + 5.5f, TextureUse.ICONS, 6f, backHover ? Color.WHITE : new Color(150, 150, 155));
        Client.RENDERER.text(tabTitle(settingsTab), contentX + 26, contentY + 5f, TextureUse.SFMEDIUM, 6f, Color.WHITE);

        if (settingsTab == SettingsTab.CONFIGS) {
            renderConfigsPanel(mouseX, mouseY, contentX, contentY, contentW, contentH);
        } else if (settingsTab == SettingsTab.SKINS) {
            renderSkinsTab(mouseX, mouseY, contentX, contentY, contentW, contentH);
        } else {
            Client.RENDERER.getCrenderSystem().push(contentX, contentY + 26, contentW, contentH - 26);
            float offsetY = clientSettingsScrollAnim + 2;
            float totalH = 0f;
            for (SettingRenderer<?> setting : getSettingsTabRenderers()) {
                if (!setting.getSetting().getVisible().get()) continue;

                float sH = Math.max(18, setting.getHeight());
                setting.bound(contentX + 4, contentY + 26 + offsetY, contentW - 8, sH);
                setting.render(mouseX, mouseY);

                offsetY += sH + 4f;
                totalH += sH + 4f;
            }
            if (settingsTab == SettingsTab.CLIENT || settingsTab == SettingsTab.SKINS) {
                totalH += renderSkinPreview(mouseX, mouseY, contentX, contentY + 26 + offsetY, contentW - 8);
            }
            float maxScroll = Math.max(0, totalH - (contentH - 40));
            clientSettingsScroll = Math.max(-maxScroll, Math.min(0, clientSettingsScroll));
            Client.RENDERER.getCrenderSystem().pop();
        }
    }

    private void renderConfigsPanel(int mouseX, int mouseY, float contentX, float contentY, float contentW, float contentH) {
        configRowBounds.clear();
        float offsetY = clientSettingsScrollAnim + 2f;

        configNameRenderer.bound(contentX + 4, contentY + 26 + offsetY, contentW - 8, 20);
        configNameRenderer.render(mouseX, mouseY);
        offsetY += 24f;

        configSaveRenderer.bound(contentX + 4, contentY + 26 + offsetY, contentW - 8, 20);
        configSaveRenderer.render(mouseX, mouseY);
        offsetY += 26f;

        Client.RENDERER.text("Сохраненные конфиги:", contentX + 6, contentY + 26 + offsetY, TextureUse.SFMEDIUM, 5f, new Color(130, 130, 135));
        offsetY += 11f;

        String[] configs = Client.CONFIG.list();
        float rowH = 20f;
        for (int i = 0; i < configs.length; i++) {
            String c = configs[i];
            float rx = contentX + 4;
            float ry = contentY + 26 + offsetY;
            float rw = contentW - 8;
            boolean loadH = MathUtility.mouseIn(rx + rw - 90, ry, 42, rowH, mouseX, mouseY);
            boolean delH = MathUtility.mouseIn(rx + rw - 44, ry, 40, rowH, mouseX, mouseY);

            Color rBg = (loadH || delH) ? new Color(255, 255, 255, 6) : new Color(255, 255, 255, 2);
            Client.RENDERER.rect(rx, ry, rw, rowH, new Vector4f(4), 1, rBg, rBg, rBg, rBg);
            Client.RENDERER.text(trimToWidth(c, rw - 104, 5f), rx + 8, ry + 6, TextureUse.SFMEDIUM, 5f, new Color(220, 220, 225));
            Client.RENDERER.text("Загрузить", rx + rw - 90, ry + 6, TextureUse.SFMEDIUM, 5f, loadH ? Color.WHITE : new Color(120, 200, 120));
            Client.RENDERER.text("Удалить", rx + rw - 44, ry + 6, TextureUse.SFMEDIUM, 5f, delH ? Color.WHITE : new Color(210, 110, 110));

            configRowBounds.add(new float[]{rx, ry, rw, rowH, i});
            offsetY += rowH + 3f;
        }

        float maxScroll = Math.max(0, (24f + 26f + 11f + configs.length * (rowH + 3f)) - (contentH - 40));
        clientSettingsScroll = Math.max(-maxScroll, Math.min(0, clientSettingsScroll));
    }

    private void renderSkinsTab(int mouseX, int mouseY, float contentX, float contentY, float contentW, float contentH) {
        // Search field
        float fieldH = 18f;
        skinSearchRenderer.bound(contentX + 4, contentY + 26, contentW - 8, fieldH);
        skinSearchRenderer.render(mouseX, mouseY);

        // Live поиск по любому нику через Mojang API (не только по рекомендациям)
        String rawSearch = skinSearch.getText().trim();
        if (!rawSearch.isEmpty()) {
            boolean requested = skinLastRequested.equalsIgnoreCase(rawSearch);
            boolean loaded = SkinManager.INSTANCE.getLoadedName() != null && SkinManager.INSTANCE.getLoadedName().equalsIgnoreCase(rawSearch);
            if (!requested && !loaded) {
                if (!skinLastTyped.equals(rawSearch)) {
                    skinLastTyped = rawSearch;
                    skinSearchChangedAt = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - skinSearchChangedAt > 600) {
                    skinLastRequested = rawSearch;
                    ClientSettings.INSTANCE.customizeSetting.customModel.set(true);
                    SkinManager.INSTANCE.applySkin(rawSearch);
                }
            }
        } else {
            skinLastTyped = "";
            skinLastRequested = "";
            skinSearchChangedAt = 0;
        }

        // Preview of currently loaded skin
        float previewY = contentY + 26 + fieldH + 4f;
        float previewH = renderSkinPreview(mouseX, mouseY, contentX, previewY, contentW - 8);

        // Recommended skins grid
        float gridY = previewY + previewH + 4f;
        float gridH = contentY + contentH - gridY - 4f;
        if (gridH < 10f) gridH = 10f;

        String search = skinSearch.getText().trim().toLowerCase();
        List<String[]> filtered = new ArrayList<>();
        for (String[] s : recommendedSkins) {
            if (search.isEmpty() || s[0].toLowerCase().contains(search)) {
                filtered.add(s);
            }
        }

        int cols = 2;
        float gap = 5f;
        float cardW = (contentW - (cols - 1) * gap) / cols;
        float cardH = 34f;
        float totalContentH = filtered.size() * (cardH + gap);

        skinScroll = Math.max(-Math.max(0, totalContentH - gridH), Math.min(0, skinScroll));
        skinScrollAnim = MathUtility.linearFps(skinScrollAnim, skinScroll, 15f);

        skinRowBounds.clear();
        float curY = gridY + skinScrollAnim;

        for (int i = 0; i < filtered.size(); i++) {
            String[] skin = filtered.get(i);
            int col = i % cols;
            int row = i / cols;
            float rx = contentX + 4 + col * (cardW + gap);
            float ry = curY + row * (cardH + gap);

            boolean hovered = MathUtility.mouseIn(rx, ry, cardW, cardH, mouseX, mouseY);
            Color cBg = hovered ? new Color(255, 255, 255, 10) : new Color(255, 255, 255, 4);
            Client.RENDERER.rect(rx, ry, cardW, cardH, new Vector4f(5), 1, cBg, cBg, cBg, cBg);
            Color cOut = hovered ? new Color(255, 255, 255, 20) : new Color(255, 255, 255, 5);
            Client.RENDERER.outline(rx, ry, cardW, cardH, 0.3f, new Vector4f(5), new Vector2f(1), cOut, cOut, cOut, cOut);

            Client.RENDERER.text(IconUse.PLAYER, rx + 8, ry + 8, TextureUse.ICONS, 6f, ClientSettings.INSTANCE.getColor(0));
            Client.RENDERER.text(skin[0], rx + 22, ry + 5, TextureUse.SFMEDIUM, 5.5f, hovered ? Color.WHITE : new Color(210, 210, 215));
            Client.RENDERER.text(trimToWidth(skin[1], cardW - 30f, 4.3f), rx + 22, ry + 17, TextureUse.SFMEDIUM, 4.3f, new Color(110, 110, 115));

            int recIdx = -1;
            for (int r = 0; r < recommendedSkins.length; r++) {
                if (recommendedSkins[r][0].equals(skin[0])) { recIdx = r; break; }
            }
            skinRowBounds.add(new float[]{rx, ry, cardW, cardH, recIdx});
        }

        if (filtered.isEmpty()) {
            String hint = "Введите ник — поиск по всем скинам Minecraft";
            Client.RENDERER.text(hint, contentX + (contentW - Client.RENDERER.textWidth(hint, TextureUse.SFMEDIUM, 5.5f)) / 2f, gridY + 20f, TextureUse.SFMEDIUM, 5.5f, new Color(120, 120, 125));
        }
    }

    private boolean clickSkinsTab(int mouseX, int mouseY, int button) {
        if (button != 0) return false;

        float contentX = drag.x + 10;
        float contentY = drag.y + 28;
        float contentW = WIDTH - 20;

        float fieldH = 18f;
        skinSearchRenderer.bound(contentX + 4, contentY + 26, contentW - 8, fieldH);
        if (skinSearchRenderer.click(mouseX, mouseY, button)) return true;

        for (float[] b : skinRowBounds) {
            if (!MathUtility.mouseIn(b[0], b[1], b[2], b[3], mouseX, mouseY)) continue;
            int idx = (int) b[4];
            if (idx >= 0 && idx < recommendedSkins.length) {
                String nick = recommendedSkins[idx][0];
                ClientSettings.INSTANCE.customizeSetting.customModel.set(true);
                SkinManager.INSTANCE.applySkin(nick);
            }
            return true;
        }
        return false;
    }

    private boolean clickConfigsPanel(int mouseX, int mouseY, int button) {
        if (button != 0) return false;

        float contentX = drag.x + 10;
        float contentY = drag.y + 28;
        float contentW = WIDTH - 20;
        float offsetY = clientSettingsScrollAnim + 2f;

        configNameRenderer.bound(contentX + 4, contentY + 26 + offsetY, contentW - 8, 20);
        if (configNameRenderer.click(mouseX, mouseY, button)) return true;

        configSaveRenderer.bound(contentX + 4, contentY + 26 + offsetY + 24f, contentW - 8, 20);
        if (configSaveRenderer.click(mouseX, mouseY, button)) return true;

        for (float[] b : configRowBounds) {
            if (!MathUtility.mouseIn(b[0], b[1], b[2], b[3], mouseX, mouseY)) continue;
            String name = Client.CONFIG.list()[(int) b[4]];
            if (MathUtility.mouseIn(b[0] + b[2] - 90, b[1], 42, b[3], mouseX, mouseY)) {
                Client.CONFIG.load(name);
            } else if (MathUtility.mouseIn(b[0] + b[2] - 44, b[1], 40, b[3], mouseX, mouseY)) {
                Client.CONFIG.delete(name);
            }
            return true;
        }
        return false;
    }

    private void saveConfig() {
        String n = configName.getText().trim();
        if (n.isEmpty()) return;
        Client.CONFIG.save(n);
    }

    private void addFriendFromInput() {
        String name = friendInput.getText().trim();
        if (!name.isEmpty() && !Client.FRIENDS.isFriend(name)) {
            Client.FRIENDS.addFriend(name);
            Client.FRIENDS.save();
            friendInput.setText("");
        }
    }

    private SettingsTab settingsSquareAt(int mouseX, int mouseY, float contentX, float contentY, float contentW, float contentH) {
        float gap = 6f;
        int cols = 3;
        float w = (contentW - (cols - 1) * gap) / cols;
        float h = (contentH - gap) / 2f;
        SettingsTab[] tabs = { SettingsTab.CONFIGS, SettingsTab.CLIENT, SettingsTab.THEMES, SettingsTab.PROXY, SettingsTab.SKINS };
        for (int i = 0; i < tabs.length; i++) {
            int col = i % cols;
            int row = i / cols;
            float rx = contentX + col * (w + gap);
            float ry = contentY + row * (h + gap);
            if (MathUtility.mouseIn(rx, ry, w, h, mouseX, mouseY)) return tabs[i];
        }
        return null;
    }

    private IconUse iconForTab(SettingsTab tab) {
        return switch (tab) {
            case CONFIGS -> IconUse.REFRESH;
            case CLIENT -> IconUse.GEAR;
            case THEMES -> IconUse.STAR;
            case PROXY -> IconUse.GLOBE;
            case SKINS -> IconUse.PLAYER;
        };
    }

    private String tabTitle(SettingsTab tab) {
        return switch (tab) {
            case CONFIGS -> "Конфиги";
            case CLIENT -> "Клиент";
            case THEMES -> "Темы";
            case PROXY -> "Прокси";
            case SKINS -> "Скины";
        };
    }

    private String tabSubtitle(SettingsTab tab) {
        return switch (tab) {
            case CONFIGS -> "Создание и загрузка";
            case CLIENT -> "Клавиши, плащ";
            case THEMES -> "Цвета клиента";
            case PROXY -> "SOCKS4 / SOCKS5";
            case SKINS -> "Кастомные скины";
        };
    }

    private List<SettingRenderer<?>> getSettingsTabRenderers() {
        if (settingsTab == null) return List.of();
        return switch (settingsTab) {
            case CLIENT -> clientTabRenderers;
            case THEMES -> themeTabRenderers;
            case PROXY -> proxyTabRenderers;
            case SKINS -> skinTabRenderers;
            default -> List.of();
        };
    }

    private List<SettingRenderer<?>> settingsActiveRenderers() {
        if (settingsTab == null) return List.of();
        if (settingsTab == SettingsTab.CONFIGS) {
            return List.of(configNameRenderer, configSaveRenderer);
        }
        if (settingsTab == SettingsTab.SKINS) {
            List<SettingRenderer<?>> list = new ArrayList<>();
            list.add(skinSearchRenderer);
            list.addAll(skinTabRenderers);
            return list;
        }
        return getSettingsTabRenderers();
    }

    @Override
    public boolean click(int mouseX, int mouseY, int button) {
        if (!isOpened) return false;

        for (var widget : WIDGETS.get().reversed()) {
            if (widget.click(mouseX, mouseY, button)) return true;
        }
        if (currentCategory == Category.Settings) {
            float contentX = drag.x + 10;
            float contentY = drag.y + 28;
            float contentW = WIDTH - 20;
            float contentH = HEIGHT - 52;
            if (MathUtility.mouseIn(contentX, contentY, contentW, contentH, mouseX, mouseY)) {
                if (settingsTab == null) {
                    SettingsTab tab = settingsSquareAt(mouseX, mouseY, contentX, contentY, contentW, contentH);
                    if (tab != null) {
                        settingsTab = tab;
                        clientSettingsScroll = 0;
                    }
                    return true;
                }
                if (MathUtility.mouseIn(contentX + 2, contentY + 2, 18, 18, mouseX, mouseY)) {
                    settingsTab = null;
                    clientSettingsScroll = 0;
                    return true;
                }
                if (settingsTab == SettingsTab.CONFIGS) {
                    clickConfigsPanel(mouseX, mouseY, button);
                    return true;
                }
                if (settingsTab == SettingsTab.SKINS) {
                    clickSkinsTab(mouseX, mouseY, button);
                    return true;
                }
                float offsetY = clientSettingsScrollAnim + 2f;
                for (SettingRenderer<?> setting : getSettingsTabRenderers()) {
                    if (!setting.getSetting().getVisible().get()) continue;

                    float sH = Math.max(18, setting.getHeight());
                    if (MathUtility.mouseIn(contentX + 4, contentY + 26 + offsetY, contentW - 8, sH, mouseX, mouseY)) {
                        setting.click(mouseX, mouseY, button);
                        return true;
                    }
                    offsetY += sH + 4f;
                }
                return true;
            }
        }
        if (currentCategory == Category.Friends) {
            float contentX = drag.x + 10;
            float contentY = drag.y + 28;
            float contentW = WIDTH - 20;
            float contentH = HEIGHT - 52;

            if (MathUtility.mouseIn(contentX, contentY, contentW, contentH, mouseX, mouseY)) {
                float fieldW = contentW - 40f;
                float fieldH = 18f;
                friendInputRenderer.bound(contentX + 4, contentY + 2, fieldW, fieldH);
                if (friendInputRenderer.click(mouseX, mouseY, button)) {
                    friendInputFocused = true;
                    return true;
                }

                float addX = contentX + contentW - 32f;
                if (MathUtility.mouseIn(addX, contentY + 2, 28f, fieldH, mouseX, mouseY) && button == 0) {
                    addFriendFromInput();
                    return true;
                }

                for (float[] b : friendRowBounds) {
                    if (!MathUtility.mouseIn(b[0], b[1], b[2], b[3], mouseX, mouseY)) continue;
                    float delX = b[0] + b[2] - 18f;
                    if (MathUtility.mouseIn(delX, b[1] + 2f, 12f, 12f, mouseX, mouseY) && button == 0) {
                        List<String> friends = Client.FRIENDS.getFriendList();
                        int idx = (int) b[4];
                        if (idx >= 0 && idx < friends.size()) {
                            Client.FRIENDS.removeFriend(friends.get(idx));
                            Client.FRIENDS.save();
                        }
                        return true;
                    }
                }
                return true;
            }
        }

        float searchW = 110f;
        float searchH = 17f;
        float searchX = drag.x + (WIDTH - searchW) / 2f;
        float searchY = drag.y + 6f;
        if (MathUtility.mouseIn(searchX, searchY, searchW, searchH, mouseX, mouseY)) {
            searchFocused = true;
            return true;
        }
        Category[] categories = Category.values();

        float totalCatWidth = 0;
        for (Category cat : categories) totalCatWidth += categoryWidths.getOrDefault(cat, 20f) + 3f;
        float startX = drag.x + (WIDTH - totalCatWidth + 3f) / 2f;
        float startY = drag.y + HEIGHT - 21f;

        float curX = startX;
        for (Category cat : categories) {
            float w = categoryWidths.getOrDefault(cat, 20f);
            if (MathUtility.mouseIn(curX, startY, w, 15, mouseX, mouseY)) {
                if (currentCategory != cat) {
                    currentCategory = cat;
                    categorySwitchProgress = 0f;
                    expandedModules.clear();
                    scroll = 0;
                    searchFocused = false;
                }
                return true;
            }
            curX += w + 3f;
        }
        float contentX = drag.x + 10;
        float contentY = drag.y + 28;
        float contentW = WIDTH - 20;
        float gap = 5f;
        int numCols = 2;
        float cardW = (contentW - gap) / numCols;
        float headerH = 28f;

        List<Module> modules = getFilteredModules();
        float[] colY = new float[]{scrollAnim, scrollAnim};

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            int col = i % numCols;

            boolean isExpanded = expandedModules.contains(module);

            Animation anim = moduleExpandAnims.computeIfAbsent(module, m -> new Animation());
            float easedProgress = anim.get();

            float fullSettingsH = 0f;
            if (isExpanded || easedProgress > 0.01f) {
                for (SettingRenderer<?> setting : module.getSettingRenderers()) {
                    boolean isVisible = setting.getSetting().getVisible().get();
                    float visAnim = setting.visible.getOutput();
                    if (visAnim > 0.001f) {
                        float h = Math.max(14f, setting.getHeight());
                        fullSettingsH += (h + 2.5f) * visAnim;
                    }
                }
                if (fullSettingsH > 0) fullSettingsH += 4f;
            }

            float cardH = headerH + fullSettingsH * easedProgress;
            float cardX = contentX + col * (cardW + gap);
            float cardY = contentY + colY[col];

            if (MathUtility.mouseIn(cardX, cardY, cardW, cardH, mouseX, mouseY)) {
                searchFocused = false;
                float toggleX = cardX + cardW - 23;
                float toggleY = cardY + 9;
                if (MathUtility.mouseIn(toggleX - 2, toggleY - 2, 21, 12, mouseX, mouseY)) {
                    module.toggle();
                    return true;
                }
                if (isExpanded && mouseY > cardY + headerH) {
                    float curSettingY = cardY + headerH;
                    for (SettingRenderer<?> setting : module.getSettingRenderers()) {
                        float visAnim = setting.visible.getOutput();
                        if (!setting.getSetting().getVisible().get() || visAnim < 0.5f) continue;

                        float sH = Math.max(14, setting.getHeight());
                        if (MathUtility.mouseIn(cardX + 5, curSettingY, cardW - 10, sH, mouseX, mouseY)) {
                            setting.click(mouseX, mouseY, button);
                            return true;
                        }
                        curSettingY += (sH + 2.5f) * visAnim;
                    }
                    return true;
                }
                if (button == 2) {
                    bindingModule = module;
                    BINDING = true;
                    float popupX = cardX + cardW / 2f - 80f;
                    float popupY = cardY - 86f;
                    if (popupY < drag.y + 4) popupY = cardY + headerH + 4;
                    BindPopupWidget popup = new BindPopupWidget(module, this, popupX, popupY);
                    WIDGETS.register(popup);
                    return true;
                }

                if (isExpanded) {
                    expandedModules.remove(module);
                } else {
                    expandedModules.add(module);
                }
                return true;
            }

            colY[col] += cardH + gap;
        }

        if (MathUtility.mouseIn(drag.x, drag.y, WIDTH, HEIGHT, mouseX, mouseY)) {
            drag.dX = mouseX - drag.x;
            drag.dY = mouseY - drag.y;
            drag.dragging = true;
            return true;
        }

        return false;
    }

    @Override
    public void release(int button) {
        drag.dragging = false;
        WIDGETS.release(button);

        if (currentCategory == Category.Settings) {
            for (var setting : settingsActiveRenderers()) {
                setting.release(button);
            }
        }

        for (Module module : expandedModules) {
            for (var setting : module.getSettingRenderers()) {
                setting.release(button);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!isOpened) return false;

        synchronized (WIDGETS) {
            for (var widget : WIDGETS.get().reversed()) {
                if (widget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
                    return true;
                }
            }
        }

        if (currentCategory == Category.Settings) {
            float contentX = drag.x + 10;
            float contentY = drag.y + 28;
            float contentW = WIDTH - 20;
            float contentH = HEIGHT - 52;
            if (MathUtility.mouseIn(contentX, contentY, contentW, contentH, (int) mouseX, (int) mouseY)) {
                clientSettingsScroll += (float) verticalAmount * 16f;
                return true;
            }
        }

        if (currentCategory == Category.Friends) {
            float contentX = drag.x + 10;
            float contentY = drag.y + 28;
            float contentW = WIDTH - 20;
            float contentH = HEIGHT - 52;
            if (MathUtility.mouseIn(contentX, contentY, contentW, contentH, (int) mouseX, (int) mouseY)) {
                friendScroll += (float) verticalAmount * 16f;
                return true;
            }
        }

        scroll += (float) verticalAmount * 16f;
        return true;
    }

    @Override
    public void mouseDragged(int mouseX, int mouseY) {
        if (currentCategory == Category.Settings) {
            for (var setting : settingsActiveRenderers()) {
                setting.mouseDragged(mouseX, mouseY);
            }
        }

        for (Module module : expandedModules) {
            for (var setting : module.getSettingRenderers()) {
                setting.mouseDragged(mouseX, mouseY);
            }
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isOpened) return;

        synchronized (WIDGETS) {
            for (var widget : WIDGETS.get().reversed()) {
                widget.keyPressed(keyCode, scanCode, modifiers);
            }
        }

        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
                scroll = 0;
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
            }
            return;
        }

        if (currentCategory == Category.Friends) {
            friendInputRenderer.keyPressed(keyCode, scanCode, modifiers);
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                addFriendFromInput();
            }
        }

        if (currentCategory == Category.Settings) {
            for (var setting : settingsActiveRenderers()) {
                setting.keyPressed(keyCode, scanCode, modifiers);
            }
        }

        for (Module module : expandedModules) {
            for (var setting : module.getSettingRenderers()) {
                setting.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    @Override
    public void chartyped(char ch, int keyCode) {
        if (!isOpened) return;

        synchronized (WIDGETS) {
            for (var widget : WIDGETS.get().reversed()) {
                widget.chartyped(ch, keyCode);
            }
        }

        if (searchFocused) {
            if (Character.isLetterOrDigit(ch) || ch == ' ' || ch == '-' || ch == '_') {
                searchText += ch;
                scroll = 0;
            }
            return;
        }

        if (currentCategory == Category.Friends) {
            friendInputRenderer.chartyped(ch, keyCode);
        }

        if (currentCategory == Category.Settings) {
            for (var setting : settingsActiveRenderers()) {
                setting.chartyped(ch, keyCode);
            }
        }

        for (Module module : expandedModules) {
            for (var setting : module.getSettingRenderers()) {
                setting.chartyped(ch, keyCode);
            }
        }
    }

    private float renderSkinPreview(int mouseX, int mouseY, float contentX, float y, float contentW) {
        SkinManager sm = SkinManager.INSTANCE;
        if (!sm.isLoading() && sm.getError().isEmpty() && sm.getLoadedSkin() == null) return 0f;
        float previewH = 0f;

        if (sm.isLoading()) {
            previewH = 20f;
            Client.RENDERER.rect(contentX + 4, y, contentW - 8, previewH, new Vector4f(4), 1, new Color(255, 255, 255, 4), new Color(255, 255, 255, 4), new Color(255, 255, 255, 4), new Color(255, 255, 255, 4));
            Client.RENDERER.text("Загрузка скина...", contentX + 10, y + 6, TextureUse.SFMEDIUM, 5f, new Color(150, 150, 155));
            return previewH + 4f;
        }

        if (!sm.getError().isEmpty()) {
            previewH = 20f;
            Client.RENDERER.rect(contentX + 4, y, contentW - 8, previewH, new Vector4f(4), 1, new Color(255, 255, 255, 4), new Color(255, 255, 255, 4), new Color(255, 255, 255, 4), new Color(255, 255, 255, 4));
            Client.RENDERER.text(sm.getError(), contentX + 10, y + 6, TextureUse.SFMEDIUM, 5f, new Color(210, 110, 110));
            return previewH + 4f;
        }

        if (sm.getLoadedSkin() != null) {
            previewH = 76f;
            Client.RENDERER.rect(contentX + 4, y, contentW - 8, previewH, new Vector4f(4), 1, new Color(255, 255, 255, 4), new Color(255, 255, 255, 4), new Color(255, 255, 255, 4), new Color(255, 255, 255, 4));
            Client.RENDERER.text("Скин: " + sm.getLoadedName(), contentX + 10, y + 4, TextureUse.SFMEDIUM, 5f, new Color(200, 200, 205));

            if (mc.player != null) {
                DrawContext context = Client.RENDERER.getDrawContext();
                if (context != null) {
                    float boxW = 52f;
                    float boxH = 60f;
                    float boxX1 = contentX + contentW / 2f - boxW / 2f;
                    float boxY1 = y + previewH - boxH - 10f;
                    try {
                        InventoryScreen.drawEntity(context, (int) boxX1, (int) boxY1, (int) (boxX1 + boxW), (int) (boxY1 + boxH), (int) (boxH * 0.45f), 0.0625f, mouseX, mouseY, mc.player);
                    } catch (Exception e) {
                        LogUtility.error(e, "skin preview");
                    }
                }
            }
            return previewH + 4f;
        }

        return 0f;
    }

    private Color applyAlpha(Color color, float alphaFactor) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * alphaFactor));
    }

    private String trimToWidth(String text, float width, float size) {
        if (text == null) return "";
        if (Client.RENDERER.textWidth(text, TextureUse.SFMEDIUM, size) <= width) return text;
        String result = text;
        while (result.length() > 1 && Client.RENDERER.textWidth(result + "...", TextureUse.SFMEDIUM, size) > width) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }

    private List<Module> getFilteredModules() {
        if (Client.MODULES == null) return new ArrayList<>();

        if (!searchText.isEmpty()) {
            List<Module> result = new ArrayList<>();
            String search = searchText.toLowerCase().replace(" ", "");
            for (Module m : Client.MODULES.getModules()) {
                if (m.isHidden()) continue;
                if (m.getName().toLowerCase().replace(" ", "").contains(search)) {
                    result.add(m);
                }
            }
            return result;
        }

        return Client.MODULES.get(currentCategory).stream().filter(m -> !m.isHidden()).collect(java.util.stream.Collectors.toList());
    }

    private IconUse getIconForCategory(Category cat) {
        return switch (cat) {
            case COMBAT -> IconUse.FIGHT;
            case MOVEMENT -> IconUse.MOVEMENT;
            case PLAYER -> IconUse.PLAYER;
            case RENDER -> IconUse.RENDER;
            case Misc -> IconUse.MISC;
            case Friends -> IconUse.PERSONS;
            case Settings -> IconUse.GEAR;
        };
    }
}