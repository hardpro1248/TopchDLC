package gg.topchdlc.mixin.screen;

import gg.topchdlc.vse.shutki.other.commands.Commands;
import gg.topchdlc.vse.utils.client.mixin.IChatHud;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import gg.topchdlc.Client;
import gg.topchdlc.vse.shutki.module.modules.impl.helper.AHHelper;
import gg.topchdlc.vse.shutki.other.commands.Command;
import gg.topchdlc.vse.shutki.other.commands.impl.GpsCommand;
import gg.topchdlc.vse.utils.client.text.ChatUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gg.topchdlc.MinecraftHolder.mc;

@Mixin(ChatScreen.class)
public class ChatScreenMixin extends Screen {
    protected ChatScreenMixin(Text title) {
        super(title);
    }

    @Shadow
    protected TextFieldWidget chatField;

    @Unique
    private int selectedSuggestion = 0;

    @Unique
    private static final Pattern COORDS_PATTERN = Pattern.compile(
            "\\[\\s*(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)\\s*\\]" +
                    "|\\[\\s*x=(-?\\d+)\\s+y=(-?\\d+)\\s+z=(-?\\d+)\\s*\\]"
    );

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (Client.HUD.click((int) mouseX, (int) mouseY, button)) {
            cir.setReturnValue(true);
            return;
        }
        if (button == 0 && mc.player != null) {
            int scaledHeight = mc.getWindow().getScaledHeight();
            DrawnTextConsumer.ClickHandler clickHandler = new DrawnTextConsumer.ClickHandler(this.textRenderer, (int) mouseX, (int) mouseY);
            mc.inGameHud.getChatHud().render(clickHandler, scaledHeight, mc.inGameHud.getTicks(), true);
            Style style = clickHandler.getStyle();

            IChatHud chatHud = (IChatHud) mc.inGameHud.getChatHud();
            if (style != null) {
                net.minecraft.text.Text hoveredText = chatHud.getMessageAt(mouseX, mouseY);
                Matcher m = hoveredText != null ? COORDS_PATTERN.matcher(hoveredText.getString()) : null;
                if (m == null || !m.find()) {
                    m = findCoordsInMessages(chatHud.getMessages());
                }
                if (m != null) {
                    double x = m.group(1) != null ? Double.parseDouble(m.group(1)) : Double.parseDouble(m.group(4));
                    double z = m.group(3) != null ? Double.parseDouble(m.group(3)) : Double.parseDouble(m.group(6));
                    GpsCommand.INSTANCE.setTarget(x, z);
                    ChatUtility.send(String.format("GPS -> %.0f %.0f", x, z));
                    cir.setReturnValue(true);
                    return;
                }
                ClickEvent clickEvent = style.getClickEvent();
                if (clickEvent instanceof ClickEvent.RunCommand runCmd) {
                    String command = runCmd.command();
                    String cmd = command.startsWith("/") ? command.substring(1) : command;
                    mc.player.networkHandler.sendChatCommand(cmd);
                    cir.setReturnValue(true);
                    return;
                } else if (clickEvent instanceof ClickEvent.SuggestCommand suggestCmd) {
                    String command = suggestCmd.command();
                    String cmd = command.startsWith("/") ? command.substring(1) : command;
                    mc.player.networkHandler.sendChatCommand(cmd);
                    cir.setReturnValue(true);
                    return;
                }
            }
            List<String> suggestions = getSuggestions();
            if (!suggestions.isEmpty()) {
                int itemH = 12;
                int padX = 4, padY = 3;
                int maxW = suggestions.stream().mapToInt(s -> mc.textRenderer.getWidth(s)).max().orElse(0) + padX * 2;
                int totalH = suggestions.size() * itemH + padY * 2;
                int x = 2;
                int y = this.height - 14 - totalH - 2;
                for (int i = 0; i < suggestions.size(); i++) {
                    int iy = y + padY + i * itemH;
                    if (mouseX >= x && mouseX <= x + maxW && mouseY >= iy && mouseY <= iy + itemH) {
                        applySuggestion(suggestions.get(i));
                        cir.setReturnValue(true);
                        return;
                    }
                }
            }
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        int keyCode = input.key();
        List<String> suggestions = getSuggestions();
        if (!suggestions.isEmpty()) {
            if (keyCode == 258) {
                applySuggestion(suggestions.get(selectedSuggestion));
                cir.setReturnValue(true);
            } else if (keyCode == 265) {
                selectedSuggestion = (selectedSuggestion - 1 + suggestions.size()) % suggestions.size();
                cir.setReturnValue(true);
            } else if (keyCode == 264) {
                selectedSuggestion = (selectedSuggestion + 1) % suggestions.size();
                cir.setReturnValue(true);
            }
        }
    }

    @Unique
    private void applySuggestion(String suggestion) {
        if (chatField == null) return;
        String text = chatField.getText();
        String[] parts = text.split(" ", -1);
        if (parts.length == 1) {
            chatField.setText(Commands.PREFIX + suggestion + " ");
        } else {
            parts[parts.length - 1] = suggestion;
            chatField.setText(String.join(" ", parts) + " ");
        }
        chatField.setCursorToEnd(false);
        selectedSuggestion = 0;
    }

    @Unique
    private List<String> getSuggestions() {
        List<String> result = new ArrayList<>();
        if (chatField == null || mc == null) return result;
        String text = chatField.getText();

        if (!text.startsWith(Commands.PREFIX) || text.length() < Commands.PREFIX.length()) return result;

        String raw = text.substring(Commands.PREFIX.length());
        String[] parts = raw.split(" ", -1);

        List<Command> commands = Client.COMMANDS.getCommands();

        if (parts.length == 1) {
            String prefix = parts[0].toLowerCase();
            for (Command cmd : commands) {
                if (cmd.getName().toLowerCase().startsWith(prefix) && !cmd.getName().equalsIgnoreCase(prefix)) {
                    result.add(cmd.getName());
                }
            }
        } else if (parts.length == 2) {
            String cmdName = parts[0].toLowerCase();
            String subPrefix = parts[1].toLowerCase();
            for (Command cmd : commands) {
                if (cmd.getName().equalsIgnoreCase(cmdName)) {
                    for (String sub : cmd.getSubcommands()) {
                        if (sub.toLowerCase().startsWith(subPrefix)) {
                            result.add(sub);
                        }
                    }
                    break;
                }
            }
        }
        return result;
    }

    @Unique
    private void renderSuggestions(DrawContext context, int mouseX, int mouseY) {
        List<String> suggestions = getSuggestions();
        if (suggestions.isEmpty()) { selectedSuggestion = 0; return; }
        if (selectedSuggestion >= suggestions.size()) selectedSuggestion = 0;

        boolean isSubcmd = isSubcommandMode();
        String cmdPrefix = isSubcmd ? getCommandPrefix() : "";

        int offsetX = isSubcmd ? (mc.textRenderer.getWidth(Commands.PREFIX + cmdPrefix + " ") + 4) : 2;

        int itemH = 12;
        int padX = 4, padY = 3;
        int maxW = suggestions.stream().mapToInt(s -> mc.textRenderer.getWidth(s)).max().orElse(0) + padX * 2;
        int totalH = suggestions.size() * itemH + padY * 2;
        int x = offsetX;
        int hintBarH = 12;
        int hintGap = 2;
        int y = this.height - 14 - hintBarH - hintGap - totalH - 2;

        context.fill(x, y, x + maxW, y + totalH, 0xD0000000);

        for (int i = 0; i < suggestions.size(); i++) {
            String s = suggestions.get(i);
            int iy = y + padY + i * itemH;
            boolean hovered = mouseX >= x && mouseX <= x + maxW && mouseY >= iy && mouseY <= iy + itemH;
            boolean selected = i == selectedSuggestion;
            int color = selected ? 0xFFFFFF00 : (hovered ? 0xFFCCCCCC : 0xFFFFFFFF);
            context.drawTextWithShadow(mc.textRenderer, s, x + padX, iy + 2, color);
        }

        String selected = suggestions.get(selectedSuggestion);
        String hintCmd = getHintText(selected);

        int dotW = mc.textRenderer.getWidth(Commands.PREFIX);
        int cmdW = mc.textRenderer.getWidth(hintCmd);
        int hintY = this.height - 14 - hintBarH - hintGap + 1;
        int hintW = dotW + cmdW + padX * 2;

        context.fill(2, hintY - 1, 2 + hintW, hintY + hintBarH - 1, 0xD0000000);

        context.drawTextWithShadow(mc.textRenderer, Commands.PREFIX, 2 + padX, hintY + 1, 0xFFFFFFFF);
        context.drawTextWithShadow(mc.textRenderer, hintCmd, 2 + padX + dotW, hintY + 1, 0x99AAAAAA);
    }

    @Unique
    private boolean isSubcommandMode() {
        if (chatField == null) return false;
        String text = chatField.getText();
        if (!text.startsWith(Commands.PREFIX)) return false;
        return text.substring(Commands.PREFIX.length()).split(" ", -1).length >= 2;
    }

    @Unique
    private String getCommandPrefix() {
        if (chatField == null) return "";
        String text = chatField.getText();
        if (!text.startsWith(Commands.PREFIX)) return "";
        String[] parts = text.substring(Commands.PREFIX.length()).split(" ", -1);
        return parts.length >= 1 ? parts[0] : "";
    }

    @Unique
    private String getHintText(String suggestion) {
        if (chatField == null) return suggestion;
        String text = chatField.getText();
        if (!text.startsWith(Commands.PREFIX)) return suggestion;
        String raw = text.substring(Commands.PREFIX.length());
        String[] parts = raw.split(" ", -1);
        if (parts.length >= 2) {
            return parts[0] + " " + suggestion;
        }
        return suggestion;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderSuggestionsAndPrice(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        renderAHPrice(context);
        renderSuggestions(context, mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        Client.HUD.release(click.button());
        return super.mouseReleased(click);
    }

    @Unique
    private Matcher findCoordsInMessages(List<ChatHudLine> msgs) {
        if (msgs == null) return null;
        int limit = Math.min(msgs.size(), 15);
        for (int i = 0; i < limit; i++) {
            ChatHudLine line = msgs.get(i);
            try {
                for (java.lang.reflect.RecordComponent rc : ChatHudLine.class.getRecordComponents()) {
                    if (Text.class.isAssignableFrom(rc.getType())) {
                        Object val = rc.getAccessor().invoke(line);
                        if (val instanceof Text t) {
                            Matcher m = COORDS_PATTERN.matcher(t.getString());
                            if (m.find()) return m;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    @Unique
    private void renderAHPrice(DrawContext context) {
        if (!AHHelper.INSTANCE.isEnabled()) return;
        if (mc == null || mc.player == null || chatField == null) return;

        String chatText = chatField.getText();
        if (chatText == null || !chatText.toLowerCase().startsWith("/ah sell ")) return;

        String priceStr = chatText.substring("/ah sell ".length()).trim();
        boolean hasMarker = priceStr.endsWith(".");
        if (hasMarker) priceStr = priceStr.substring(0, priceStr.length() - 1).trim();

        long price;
        try {
            price = Long.parseLong(priceStr);
        } catch (NumberFormatException e) {
            return;
        }
        ItemStack held = mc.player.getMainHandStack();
        if (held.isEmpty()) return;
        int count = held.getCount();
        if (count <= 1) return;
        long totalPrice = price * count;
        String display = totalPrice + "$  (x" + count + ")";
        int textWidth = mc.textRenderer.getWidth(display);
        int x = 25;
        int y = this.height - 32;
        context.fill(x - 2, y - 2, x + textWidth + 4, y + 10, 0xCC0A0A0C);
        context.drawTextWithShadow(mc.textRenderer, display, x, y, 0xFFFFFF55);
    }
}