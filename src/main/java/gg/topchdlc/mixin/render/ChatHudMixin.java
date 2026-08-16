package gg.topchdlc.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.list.EventAddMessage;
import gg.topchdlc.mixin.screen.InsertChatMixin;
import gg.topchdlc.vse.shutki.module.modules.impl.render.BetterMinecraft;
import gg.topchdlc.vse.utils.animations.Easings;
import gg.topchdlc.vse.utils.client.mixin.IChatHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gg.topchdlc.MinecraftHolder.mc;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin implements IChatHud {
    @Shadow private List<ChatHudLine> messages;
    @Shadow private List<ChatHudLine.Visible> visibleMessages;
    @Shadow public abstract void addMessage(Text message, MessageSignatureData signatureData, MessageIndicator indicator);

    @Unique private static Method getTextStyleAtMethod = null;

    @Unique private String pendingStack = null;
    @Unique private final HashMap<String, Integer> countMap = new HashMap<>();
    @Unique private static final Pattern STACK_PATTERN = Pattern.compile("^(.*?)\\s*\u00a77\\(x(\\d+)\\)$", Pattern.DOTALL);

    @Unique private static final Map<OrderedText, Long> TEXT_TIMESTAMPS = new IdentityHashMap<>();

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"), cancellable = true)
    private void onAddMessage(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci) {
        if (!RenderSystem.isOnRenderThread()) {
            MinecraftClient.getInstance().execute(() -> this.addMessage(message, signatureData, indicator));
            ci.cancel();
            return;
        }

        EventAddMessage event = EventAddMessage.build(message);
        Client.EVENTS.post(event);
        if (event.isCancelled()) { ci.cancel(); return; }
        if (event.replacement != null) {
            ci.cancel();
            this.addMessage(event.replacement, signatureData, indicator);
            return;
        }
        if (BetterMinecraft.INSTANCE.isEnabled() && BetterMinecraft.INSTANCE.stackMessages.get()) {
            if (tryStackMessage(message)) ci.cancel();
        }
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("TAIL"))
    private void onAddMessageTail(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci) {
        if (pendingStack != null && messages != null && !messages.isEmpty()) {
            countMap.put(pendingStack, 1);
            pendingStack = null;
        }
    }


    @Inject(method = "clear", at = @At("HEAD"))
    private void onClear(boolean clearHistory, CallbackInfo ci) {
        countMap.clear();
        pendingStack = null;
        TEXT_TIMESTAMPS.clear();
    }

    @Unique
    @Override
    public boolean tryStackMessage(Text message) {
        if (messages == null) return false;
        String incoming = stripStack(message.getString());
        if (incoming.isBlank()) return false;

        if (mc == null || mc.inGameHud == null) return false;

        Integer currentCount = countMap.get(incoming);
        if (currentCount == null) {
            pendingStack = incoming;
            return false;
        }

        for (int i = messages.size() - 1; i >= 0; i--) {
            String s = client$getTextFromLine(messages.get(i));
            if (s != null && stripStack(s).equals(incoming)) {
                messages.remove(i);
                break;
            }
        }
        for (int i = visibleMessages.size() - 1; i >= 0; i--) {
            String vs = client$getTextFromVisibleLine(visibleMessages.get(i));
            if (vs != null && vs.trim().startsWith(incoming)) {
                visibleMessages.remove(i);
            }
        }

        int count = currentCount + 1;
        MutableText newText = message.copy().append(Text.literal(" \u00a77(x" + count + ")"));
        int ticks = mc.inGameHud.getTicks();
        ChatHudLine updated = new ChatHudLine(ticks, newText, null, MessageIndicator.system());
        messages.add(0, updated);
        ((InsertChatMixin) mc.inGameHud.getChatHud()).invokeAddVisibleMessage(updated);
        countMap.put(incoming, count);
        return true;
    }

    @Unique
    private String stripStack(String s) {
        Matcher m = STACK_PATTERN.matcher(s);
        return m.matches() ? m.group(1).trim() : s.trim();
    }

    @Unique
    private String client$getTextFromLine(ChatHudLine line) {
        try {
           RecordComponent[] comps = ChatHudLine.class.getRecordComponents();
            if (comps != null) {
                for (RecordComponent rc : comps) {
                    if (Text.class.isAssignableFrom(rc.getType())) {
                        Object val = rc.getAccessor().invoke(line);
                        if (val instanceof Text t) return t.getString();
                    }
                }
            }
        } catch (Exception ignored) {}
        try {
            for (java.lang.reflect.Method m : ChatHudLine.class.getDeclaredMethods()) {
                m.setAccessible(true);
                if (m.getParameterCount() == 0 && Text.class.isAssignableFrom(m.getReturnType())) {
                    Object val = m.invoke(line);
                    if (val instanceof Text t) return t.getString();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Unique
    private String client$getTextFromVisibleLine(ChatHudLine.Visible line) {
        try {
            for (java.lang.reflect.Field f : ChatHudLine.Visible.class.getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(line);
                if (val instanceof OrderedText ot) {
                    StringBuilder sb = new StringBuilder();
                    ot.accept((i, style, cp) -> { sb.append(Character.toChars(cp)); return true; });
                    return sb.toString();
                }
                if (val instanceof Text t) return t.getString();
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Unique
    @Override
    public List<ChatHudLine> getMessages() {
        return messages;
    }

    @Unique
    @Override
    public Text getMessageAt(double mouseX, double mouseY) {
        if (messages == null || messages.isEmpty()) return null;

        Style style = this.client$getTextStyleAtDynamic(mouseX, mouseY);

        if (style == null) return null;
        if (mc == null) return null;
        double chatScale = mc.options.getChatScale().getValue();
        if (chatScale <= 0) chatScale = 1.0;
        double lineSpacing = mc.options.getChatLineSpacing().getValue();
        int lineHeight = (int) Math.ceil(9.0 * (lineSpacing + 1.0));
        int windowHeight = mc.getWindow().getScaledHeight();
        int chatBottom = windowHeight - 40;
        double scaledMouseY = mouseY / chatScale;
        double scaledChatBottom = chatBottom / chatScale;
        int lineIndex = (int) Math.floor((scaledChatBottom - scaledMouseY) / lineHeight);
        for (int delta = -1; delta <= 1; delta++) {
            int idx = lineIndex + delta;
            if (idx >= 0 && idx < messages.size()) {
                Text t = client$getTextFromLineAsText(messages.get(idx));
                if (t != null) return t;
            }
        }
        return client$getTextFromLineAsText(messages.get(0));
    }

    @Unique
    private Style client$getTextStyleAtDynamic(double x, double y) {
        try {
            if (getTextStyleAtMethod == null) {
                for (java.lang.reflect.Method m : ChatHud.class.getDeclaredMethods()) {
                    if (m.getParameterCount() == 2
                            && m.getParameterTypes()[0] == double.class
                            && m.getParameterTypes()[1] == double.class
                            && Style.class.isAssignableFrom(m.getReturnType())) {
                        m.setAccessible(true);
                        getTextStyleAtMethod = m;
                        break;
                    }
                }
            }
            if (getTextStyleAtMethod != null) {
                return (Style) getTextStyleAtMethod.invoke(this, x, y);
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Unique
    private Text client$getTextFromLineAsText(ChatHudLine line) {
        try {
            RecordComponent[] comps = ChatHudLine.class.getRecordComponents();
            if (comps != null) {
                for (RecordComponent rc : comps) {
                    if (Text.class.isAssignableFrom(rc.getType())) {
                        Object val = rc.getAccessor().invoke(line);
                        if (val instanceof Text t) return t;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Unique
    @Override
    public void removeClientMessages() {
        try {
            if (messages != null) messages.removeIf(this::isClientMessage);
            if (visibleMessages != null) visibleMessages.removeIf(this::isClientMessageVisible);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Unique
    private boolean isClientMessage(ChatHudLine line) {
        String text = client$getTextFromLine(line);
        if (text == null || text.isEmpty()) return false;
        String lower = text.toLowerCase();
        return lower.contains("topchdlc") || lower.contains("panic") || lower.contains("code:")
                || lower.contains("restore") || lower.contains("attached") || lower.contains("detached");
    }

    @Unique
    private boolean isClientMessageVisible(ChatHudLine.Visible line) {
        String text = client$getTextFromVisibleLine(line);
        if (text == null || text.isEmpty()) return false;
        String lower = text.toLowerCase();
        return lower.contains("vitalya") || lower.contains("panic") || lower.contains("code:")
                || lower.contains("restore") || lower.contains("attached") || lower.contains("detached");
    }

    @Unique
    private String getTextFromLine(ChatHudLine line) { return client$getTextFromLine(line); }
    @Unique
    private String getTextFromVisibleLine(ChatHudLine.Visible line) { return client$getTextFromVisibleLine(line); }
}