package gg.topchdlc.vse.shutki.screen.screens.main;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.joml.Vector2f;
import org.joml.Vector4f;
import gg.topchdlc.Client;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.screen.screens.account.Account;
import gg.topchdlc.vse.shutki.screen.screens.account.AccountManager;
import gg.topchdlc.vse.shutki.screen.screens.main.widgets.AccountButton;
import gg.topchdlc.vse.shutki.screen.screens.main.widgets.CustomButton;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.math.ColorUtility;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AccountScreen extends Screen {

    private final Screen parent;
    private final AccountManager accountManager;
    private List<AccountButton> accountButtons = new ArrayList<>();
    private CustomButton backButton;
    private CustomButton addButton;
    private String searchText = "";
    private boolean searchFocused = false;
    private String addText = "";
    private boolean addFocused = false;
    private float inputCursorBlink = 0;
    private float fadeInProgress = 0f;
    private boolean fadingIn = true;
    private static final float FADE_IN_SPEED = 0.1f;
    private float scrollOffset = 0f;
    private float targetScrollOffset = 0f;
    private float panelX, panelY, panelW, panelH;
    private static final float ACC_BTN_W = 85;
    private static final float ACC_BTN_H = 22;
    private static final float ACC_BTN_GAP = 4;
    private int columns = 4;
    private boolean needsRefresh = false;

    public AccountScreen(Screen parent) {
        super(Text.literal("Accounts"));
        this.parent = parent;
        this.accountManager = AccountManager.getInstance();
    }

    @Override
    protected void init() {
        super.init();
        fadeInProgress = 0f;
        fadingIn = true;
        scrollOffset = 0f;
        targetScrollOffset = 0f;
        columns = Math.max(3, Math.min(6, (int)((width * 0.48f) / (ACC_BTN_W + ACC_BTN_GAP))));
        panelW = columns * (ACC_BTN_W + ACC_BTN_GAP) + 16;
        panelH = Math.min(height * 0.7f, 300);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        float btnW = 70;
        float btnH = 18;
        float btnY = panelY + panelH - btnH - 8;
        addButton = CustomButton.CustomButtonBuilder.build(
                panelX + panelW / 2 - btnW - 4, btnY,
                btnW, btnH,
                "Add",
                CustomButton.CustomButtonBuilder.ButtonType.MAIN,
                this::addOfflineAccount
        );
        backButton = CustomButton.CustomButtonBuilder.build(
                panelX + panelW / 2 + 4, btnY,
                btnW, btnH,
                "Back",
                CustomButton.CustomButtonBuilder.ButtonType.RED,
                () -> client.setScreen(parent)
        );
        refreshAccountList();
    }

    private void refreshAccountList() {
        accountButtons.clear();
        List<Account> accounts = accountManager.getAccounts();
        if (!searchText.trim().isEmpty()) {
            String query = searchText.trim().toLowerCase();
            List<Account> filtered = new ArrayList<>();
            for (Account account : accounts) {
                if (account.getUsername().toLowerCase().contains(query)) {
                    filtered.add(account);
                }
            }
            accounts = filtered;
        }
        accounts.sort((a, b) -> {
            boolean af = accountManager.isFavorite(a.getUsername());
            boolean bf = accountManager.isFavorite(b.getUsername());
            return java.lang.Boolean.compare(bf, af);
        });
        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            AccountButton btn = AccountButton.AccountButtonBuilder.build(
                    0, 0,
                    ACC_BTN_W, ACC_BTN_H,
                    account,
                    () -> loginAccount(account),
                    () -> deleteAccount(account),
                    () -> copyAccount(account),
                    () -> favoriteAccount(account)
            );
            String currentName = MinecraftClient.getInstance().getSession().getUsername();
            btn.setSelected(account.getUsername().equals(currentName));
            btn.setFavorite(accountManager.isFavorite(account.getUsername()));
            accountButtons.add(btn);
        }
    }

    private void loginAccount(Account account) {
        if (accountManager.login(account)) {
            needsRefresh = true;
        }
    }

    private void deleteAccount(Account account) {
        accountManager.removeAccount(account);
        needsRefresh = true;
    }

    private void copyAccount(Account account) {
        client.keyboard.setClipboard(account.getUsername());
    }

    private void favoriteAccount(Account account) {
        accountManager.toggleFavorite(account.getUsername());
        needsRefresh = true;
    }

    private void addOfflineAccount() {
        if (addText.trim().isEmpty()) return;
        if (accountManager.loginOffline(addText.trim())) {
            addText = "";
            needsRefresh = true;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (needsRefresh) {
            needsRefresh = false;
            refreshAccountList();
        }
        if (fadingIn) {
            fadeInProgress += FADE_IN_SPEED * delta;
            if (fadeInProgress >= 1f) {
                fadeInProgress = 1f;
                fadingIn = false;
            }
        }
        scrollOffset += (targetScrollOffset - scrollOffset) * 0.25f;
        float animAlpha = easeOutCubic(fadeInProgress);
        float animSlide = (1f - easeOutCubic(fadeInProgress)) * 20f;
        if (!VideoWallpaper.render(context, width, height)) {
            AnimatedBackground.render(delta, width, height);
        }
        Client.RENDERER.setDrawContext(context);
        float realPanelY = panelY + animSlide;
        Vector4f round = new Vector4f(8, 8, 8, 8);
        Vector2f smooth = new Vector2f(1, 1);
        Color panelBg = ColorUtility.injectAlpha(ClientColors.BACK_COLOR, (int)(235 * animAlpha));
        Client.RENDERER.blur(panelX, realPanelY, panelW, panelH, round, 14, animAlpha);
        Client.RENDERER.outline(panelX, realPanelY, panelW, panelH, 0, round, smooth, panelBg, panelBg, panelBg, panelBg);
        String currentName = MinecraftClient.getInstance().getSession().getUsername();
        Client.RENDERER.textCentered("Accounts", panelX + panelW / 2, realPanelY + 8, TextureUse.SFMEDIUM, 10, ColorUtility.injectAlpha(ClientColors.FORE_COLOR, (int)(255 * animAlpha)));
        Client.RENDERER.textCentered(currentName, panelX + panelW / 2, realPanelY + 20, TextureUse.SFMEDIUM, 8, ColorUtility.injectAlpha(ClientColors.BRAIN_COLOR, (int)(200 * animAlpha)));
        float inputW = 100;
        float inputH = 16;
        float inputX = panelX + panelW - inputW - 10;
        float inputY = realPanelY + 10;
        Vector4f inputRound = new Vector4f(6, 6, 6, 6);
        Color inputBg = searchFocused
                ? new Color(255, 255, 255, (int)(50 * animAlpha))
                : new Color(255, 255, 255, (int)(25 * animAlpha));
        Color inputBorder = new Color(255, 255, 255, (int)((searchFocused ? 100 : 55) * animAlpha));
        Client.RENDERER.blur(inputX, inputY, inputW, inputH, inputRound, 28, animAlpha);
        Client.RENDERER.rect(inputX, inputY, inputW, inputH, inputRound, 1, inputBg, inputBg, inputBg, inputBg);
        Client.RENDERER.outline(inputX, inputY, inputW, inputH, 0.5f, inputRound, new Vector2f(1, 1), inputBorder, inputBorder, inputBorder, inputBorder);
        Client.RENDERER.text(IconUse.SEARCH.glyph, inputX + 3, inputY + inputH / 2 - 3.5f, TextureUse.ICONS, 7, new Color(255, 255, 255, (int)(255 * animAlpha)));
        String displayText = searchText.isEmpty() && !searchFocused ? "Search..." : searchText;
        Color textColor = searchText.isEmpty() && !searchFocused
                ? new Color(255, 255, 255, (int)(200 * animAlpha))
                : new Color(255, 255, 255, (int)(255 * animAlpha));
        Client.RENDERER.text(displayText, inputX + 16, inputY + inputH / 2 - 3, TextureUse.SFMEDIUM, 7, textColor);
        if (searchFocused) {
            inputCursorBlink += delta * 0.15f;
            if (((int)inputCursorBlink) % 2 == 0) {
                float cursorX = inputX + 16 + Client.RENDERER.textWidth(searchText, TextureUse.SFMEDIUM, 7);
                context.fill((int)cursorX, (int)(inputY + 3), (int)(cursorX + 1), (int)(inputY + inputH - 3), new Color(255, 255, 255, (int)(180 * animAlpha)).getRGB());
            }
        }
        float gridX = panelX + 8;
        float gridY = realPanelY + 35;
        float gridH = panelH - 70;
        Vector4f gridRound = new Vector4f(6, 6, 6, 6);
        Color gridBg = new Color(0, 0, 0, (int)(60 * animAlpha));
        Color gridBorder = new Color(255, 255, 255, (int)(10 * animAlpha));
        Client.RENDERER.blur(panelX + 6, gridY, panelW - 12, gridH, gridRound, 10, animAlpha);
        Client.RENDERER.rect(panelX + 6, gridY, panelW - 12, gridH, gridRound, 1, gridBg, gridBg, gridBg, gridBg);
        Client.RENDERER.outline(panelX + 6, gridY, panelW - 12, gridH, 0.5f, gridRound, new Vector2f(1, 1), gridBorder, gridBorder, gridBorder, gridBorder);
        context.enableScissor((int)panelX, (int)gridY, (int)(panelX + panelW), (int)(gridY + gridH));
        if (accountButtons.isEmpty()) {
            Client.RENDERER.textCentered("No accounts", panelX + panelW / 2, gridY + gridH / 2 - 8, TextureUse.SFMEDIUM, 8, ColorUtility.injectAlpha(ClientColors.FORE_COLOR, (int)(70 * animAlpha)));
            Client.RENDERER.textCentered("Enter nick and press Enter", panelX + panelW / 2, gridY + gridH / 2 + 4, TextureUse.SFMEDIUM, 6, ColorUtility.injectAlpha(ClientColors.FORE_COLOR, (int)(40 * animAlpha)));
        } else {
            for (int i = 0; i < accountButtons.size(); i++) {
                AccountButton btn = accountButtons.get(i);
                int col = i % columns;
                int row = i / columns;
                float btnX = gridX + col * (ACC_BTN_W + ACC_BTN_GAP);
                float btnY = gridY + row * (ACC_BTN_H + ACC_BTN_GAP) - scrollOffset;
                btn.setX(btnX);
                btn.setY(btnY);
                btn.renderWithContext(context, mouseX, mouseY);
            }
        }
        context.disableScissor();
        float btnY = realPanelY + panelH - 18 - 8;
        float addW = 150;
        float addH = 15;
        float addX = panelX + panelW / 2 - addW / 2;
        float addY = btnY - addH - 7;
        Vector4f addRound = new Vector4f(6, 6, 6, 6);
        Color addBg = addFocused
                ? new Color(255, 255, 255, (int)(50 * animAlpha))
                : new Color(255, 255, 255, (int)(25 * animAlpha));
        Color addBorder = new Color(255, 255, 255, (int)((addFocused ? 100 : 55) * animAlpha));
        Client.RENDERER.blur(addX, addY, addW, addH, addRound, 28, animAlpha);
        Client.RENDERER.rect(addX, addY, addW, addH, addRound, 1, addBg, addBg, addBg, addBg);
        Client.RENDERER.outline(addX, addY, addW, addH, 0.5f, addRound, new Vector2f(1, 1), addBorder, addBorder, addBorder, addBorder);
        String addDisplay = addText.isEmpty() && !addFocused ? "Enter nick..." : addText;
        Color addTextColor = addText.isEmpty() && !addFocused
                ? new Color(255, 255, 255, (int)(200 * animAlpha))
                : new Color(255, 255, 255, (int)(255 * animAlpha));
        Client.RENDERER.text(addDisplay, addX + 6, addY + addH / 2 - 3, TextureUse.SFMEDIUM, 7, addTextColor);
        if (addFocused) {
            inputCursorBlink += delta * 0.15f;
            if (((int)inputCursorBlink) % 2 == 0) {
                float cursorX = addX + 6 + Client.RENDERER.textWidth(addText, TextureUse.SFMEDIUM, 7);
                context.fill((int)cursorX, (int)(addY + 3), (int)(cursorX + 1), (int)(addY + addH - 3), new Color(255, 255, 255, (int)(180 * animAlpha)).getRGB());
            }
        }
        addButton.setY(btnY);
        backButton.setY(btnY);
        addButton.render(mouseX, mouseY);
        backButton.render(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        float animSlide = (1f - easeOutCubic(fadeInProgress)) * 20f;
        float realPanelY = panelY + animSlide;
        float inputW = 100;
        float inputH = 16;
        float inputX = panelX + panelW - inputW - 10;
        float inputY = realPanelY + 10;
        searchFocused = mouseX >= inputX && mouseX <= inputX + inputW
                && mouseY >= inputY && mouseY <= inputY + inputH;
        float addW = 150;
        float addH = 15;
        float addX = panelX + panelW / 2 - addW / 2;
        float addY = realPanelY + panelH - 26 - addH - 7;
        addFocused = !searchFocused
                && mouseX >= addX && mouseX <= addX + addW
                && mouseY >= addY && mouseY <= addY + addH;
        List<AccountButton> buttonsCopy = new ArrayList<>(accountButtons);
        for (AccountButton btn : buttonsCopy) {
            if (btn.click((int)mouseX, (int)mouseY, button)) {
                break;
            }
        }
        if (button == 0) {
            addButton.click((int)mouseX, (int)mouseY, button);
            backButton.click((int)mouseX, (int)mouseY, button);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        int scanCode = input.scancode();
        int modifiers = input.modifiers();

        if (searchFocused || addFocused) {
            boolean ctrl = (modifiers & 2) != 0;
            boolean inSearch = searchFocused;
            if (keyCode == 257) {
                if (inSearch) {
                    searchFocused = false;
                } else {
                    addOfflineAccount();
                }
                return true;
            }
            if (keyCode == 259) {
                if (ctrl) {
                    if (inSearch) searchText = "";
                    else addText = "";
                } else if (inSearch) {
                    if (!searchText.isEmpty()) searchText = searchText.substring(0, searchText.length() - 1);
                } else {
                    if (!addText.isEmpty()) addText = addText.substring(0, addText.length() - 1);
                }
                if (inSearch) needsRefresh = true;
                return true;
            }
            if (keyCode == 256) {
                searchFocused = false;
                addFocused = false;
                return true;
            }
            if (ctrl && keyCode == 86) {
                String clipboard = client.keyboard.getClipboard();
                if (clipboard != null) {
                    StringBuilder sb = new StringBuilder(inSearch ? searchText : addText);
                    for (char c : clipboard.toCharArray()) {
                        if (sb.length() >= 16) break;
                        if (Character.isLetterOrDigit(c) || c == '_') {
                            sb.append(c);
                        }
                    }
                    if (inSearch) searchText = sb.toString();
                    else addText = sb.toString();
                    if (inSearch) needsRefresh = true;
                }
                return true;
            }
            if (ctrl && keyCode == 65) {
                if (inSearch) searchText = "";
                else addText = "";
                if (inSearch) needsRefresh = true;
                return true;
            }
            if (ctrl && keyCode == 67) {
                String current = inSearch ? searchText : addText;
                if (!current.isEmpty()) {
                    client.keyboard.setClipboard(current);
                }
                return true;
            }
        } else {
            if (keyCode == 256) {
                client.setScreen(parent);
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        char chr = (char) input.codepoint();
        if (searchFocused || addFocused) {
            String current = searchFocused ? searchText : addText;
            if (current.length() < 16) {
                if (Character.isLetterOrDigit(chr) || chr == '_') {
                    if (searchFocused) searchText += chr;
                    else addText += chr;
                    if (searchFocused) needsRefresh = true;
                    return true;
                }
            }
        }
        return super.charTyped(input);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int rows = (int) Math.ceil((double) accountButtons.size() / columns);
        float contentHeight = rows * (ACC_BTN_H + ACC_BTN_GAP);
        float gridH = panelH - 70;
        float maxScroll = Math.max(0, contentHeight - gridH);
        targetScrollOffset -= verticalAmount * 20;
        targetScrollOffset = Math.max(0, Math.min(maxScroll, targetScrollOffset));
        return true;
    }

    private float easeOutCubic(float x) {
        return 1f - (float) Math.pow(1f - x, 3);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !searchFocused && !addFocused;
    }
}