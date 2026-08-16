package gg.topchdlc.vse.shutki.module.modules.impl.render;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event2D;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.animations.Animation;
import gg.topchdlc.vse.utils.animations.Easings;

/**
 * Create by daun kvass
 */
public class BetterMinecraft extends Module {
    public static final BetterMinecraft INSTANCE = new BetterMinecraft();

    private BetterMinecraft() {
        super("BetterMinecraft", Category.RENDER, "xxx");
    }

    public final Group anim = group("Animation");
    public final CheckBox inventoryAnim = anim.checkbox("Inventory Anim", true);
    public final CheckBox tabAnim       = anim.checkbox("Tab Anim", true);
    public final CheckBox chatAnim      = anim.checkbox("Chat Anim", true);
    public final CheckBox chatCharAnim  = anim.checkbox("Chat Letter Anim", true);
    public final CheckBox hotbarAnim    = anim.checkbox("Hotbar Anim", true);

    public final CheckBox stackMessages = add(new CheckBox("Stack Messages", false));
    public final SliderSetting animSpeed = anim.sliderSetting("Anim Speed", 180f, 150f, 300f).increment(1);

    public final Animation inventoryAnimation = new Animation();
    public final Animation tabAnimation       = new Animation();
    public final Animation chatAnimation      = new Animation();
    public final Animation hotbarAnimation    = new Animation();

    private boolean lastInventoryOpen = false;
    private boolean lastTabOpen       = false;
    private boolean lastChatOpen      = false;
    private boolean initialized       = false;

    EventBus<Event> events = event -> {
        if (!(event instanceof Event2D)) return;
        if (mc.player == null) return;

        float dur = animSpeed.get() / 1000.0f;
        if (!initialized) {
            initialized = true;
            hotbarAnimation.set(1.0);
        }

        boolean inventoryOpen = mc.currentScreen instanceof HandledScreen<?>;
        boolean tabOpen = mc.options.playerListKey.isPressed();
        boolean chatOpen = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;

        if (inventoryOpen != lastInventoryOpen) {
            if (inventoryOpen) inventoryAnimation.set(0.0);
            inventoryAnimation.run(inventoryOpen ? 1.0 : 0.0, dur, Easings.CUBIC_OUT);
            lastInventoryOpen = inventoryOpen;
        }
        inventoryAnimation.update();

        if (tabOpen != lastTabOpen) {
            tabAnimation.run(tabOpen ? 1.0 : 0.0, dur, Easings.CUBIC_OUT);
            lastTabOpen = tabOpen;
        }
        tabAnimation.update();

        if (chatOpen != lastChatOpen) {
            if (chatOpen) chatAnimation.set(0.0);
            chatAnimation.run(chatOpen ? 1.0 : 0.0, dur, Easings.CUBIC_OUT);
            lastChatOpen = chatOpen;
        }
        chatAnimation.update();

        boolean hotbarVisible = !inventoryOpen;
        float hotbarTarget = hotbarVisible ? 1.0f : 0.0f;
        hotbarAnimation.run(hotbarTarget, dur, Easings.CUBIC_OUT, true);
        hotbarAnimation.update();
    };

    @Override
    protected void onEnable() {
        initialized = false;
        if (mc.worldRenderer != null) mc.worldRenderer.reload();
    }

    @Override
    protected void onDisable() {
        inventoryAnimation.set(1.0);
        tabAnimation.set(0.0);
        chatAnimation.set(1.0);
        hotbarAnimation.set(1.0);
        lastInventoryOpen = false;
        lastTabOpen = false;
        lastChatOpen = false;
        initialized = false;
        if (mc.worldRenderer != null) mc.worldRenderer.reload();
    }

    public float getInventoryAnim() { return inventoryAnimation.get(); }
    public float getTabAnim() { return tabAnimation.get(); }
    public float getChatAnim() { return chatAnimation.get(); }
    public float getHotbarAnim() { return hotbarAnimation.get(); }
}