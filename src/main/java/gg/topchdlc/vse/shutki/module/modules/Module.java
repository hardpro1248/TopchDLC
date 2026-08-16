package gg.topchdlc.vse.shutki.module.modules;

import com.google.gson.JsonObject;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.mixin.accessor.IClientWorld;
import gg.topchdlc.vse.shutki.module.modules.impl.misc.ClientSounds;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.utils.animations.impl.SmoothStepAnimation;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.client.get.DebugUtility;
import gg.topchdlc.vse.utils.client.sounds.SoundUtility;
import gg.topchdlc.vse.utils.client.text.TextUtility;
import gg.topchdlc.vse.utils.lang.LangUtility;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.network.packet.Packet;
import net.minecraft.text.Text;
import gg.topchdlc.Client;

import java.awt.*;
import java.util.*;

public abstract class Module extends Group {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();
    @Getter @Setter
    public String description;
    @Getter @Setter
    private Category category;
    private boolean enabled;
    @Getter @Setter
    private int key = -1;
    @Getter
    private final Set<Tag> tags;

    @Getter @Setter
    private boolean activatable = true;

    @Getter @Setter
    private boolean hidden = false;

    @Getter @Setter
    private BindType bindType = BindType.PRESS;

    @Override
    public String getDesc() {
        return LangUtility.get(getLangKey() + "._description", description);
    }

    public boolean isEnabled() {
        return enabled && !Client.IS_PANIC;
    }

    @Override
    public String getLangKey() {
        return "topchdlc.module." + name;
    }

    public static boolean nullCheck() {
        return mc.player == null || mc.world == null || mc.getNetworkHandler() == null;
    }

    public SmoothStepAnimation keybindAnimation = new SmoothStepAnimation(200, 1);

    public Module(String name, Category category, String description, Tag... tags) {
        super(name.trim());
        this.category = category;
        this.description = description;
        this._desc = description;
        this.tags = new HashSet<>(Set.of(tags));
    }

    public void addTag(Tag tag) {
        tags.add(tag);
    }

    public void removeTag(Tag tag) {
        tags.remove(tag);
    }

    protected void nonActivatable() {
        activatable = false;
        superEnable();
    }

    public String getKeyText() {
        return TextUtility.keyToString(this.getKey());
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(final boolean enabled) {
        if (this.enabled != enabled)
            setEnabled(enabled, getBindType() == BindType.PRESS);
    }
    protected void sendPacket(Packet<?> packet) {
        if (mc.getNetworkHandler() == null) return;

        mc.getNetworkHandler().sendPacket(packet);
    }
    protected void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        if (mc.getNetworkHandler() == null || mc.world == null) return;
        try (PendingUpdateManager pendingUpdateManager = ((IClientWorld)mc.world).client$pending().incrementSequence();) {
            int i = pendingUpdateManager.getSequence();
            mc.getNetworkHandler().sendPacket(packetCreator.predict(i));
        }
    }
    public void setEnabled(final boolean enabled, boolean notification) {
        if (!this.activatable || this.enabled == enabled) return;

        this.enabled = enabled;

        if (enabled) superEnable();
        else superDisable();

        if (notification) {
            if (ClientSounds.INSTANCE.isEnabled() && ClientSounds.INSTANCE.toggleFunction.get()) {
                SoundUtility.playSound(enabled ? ClientSounds.INSTANCE.soundPack.get().enable : ClientSounds.INSTANCE.soundPack.get().disable, ClientSounds.INSTANCE.volume.get());
            }

            Text module = Text.of(name).copy().withColor(ClientSettings.INSTANCE.getColor(0).getRGB());
            Text enableOrdisable = Text.of(enabled ? " включен!" : " выключен!").copy().withColor(enabled ? new Color(0, 200, 0, 255).getRGB() : new Color(200, 0, 0, 255).getRGB());
            Text mainText = Text.of("Модуль ").copy().append(module).copy().append(enableOrdisable).withColor(new Color(255 ,255,255,255).getRGB());

            Client.NOTIFIES.add(mainText, enabled ? IconUse.ONMODULE : IconUse.OFFMODULE, 2000);
        }
    }

    private void superEnable() {
      /**  if (mc.player != null)*/onEnable();
        Client.EVENTS.register(this);
    }

    private void superDisable() {
        if (mc.player != null) onDisable();
        Client.EVENTS.unregister(this);
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }
    public boolean isDisabled() {
        return !isEnabled();
    }
    public void save(JsonObject json) {
        JsonObject me = new JsonObject();
        me.addProperty("state", enabled);
        me.addProperty("bind", key);
        me.addProperty("bindType", bindType.ordinal());
        JsonObject s = new JsonObject();
        me.add("settings", s);

        super.save(s);

        json.add(name, me);
    }

    public void load(JsonObject json) {
        if (json.has(name)) {
            JsonObject me = json.get(name).getAsJsonObject();
            key = me.get("bind").getAsInt();
            if (me.has("bindType")) {
                bindType = BindType.values()[me.get("bindType").getAsInt()];
            }

            if (me.has("settings")) {
                super.load(me.get("settings").getAsJsonObject());
            }
            setEnabled(me.get("state").getAsBoolean(), false);
        }
    }

    public enum BindType {
        PRESS, HOLD;
    }

    protected void debug(String section, Object o) {
        DebugUtility.trace(name + " " + section, o);
    }
}