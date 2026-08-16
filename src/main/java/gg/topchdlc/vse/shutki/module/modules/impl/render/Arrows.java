package gg.topchdlc.vse.shutki.module.modules.impl.render;

import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.Event2D;
import gg.topchdlc.api.render.system.IconUse;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.math.MathUtility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4f;

import java.awt.*;
import java.util.List;

public class Arrows extends Module {
    public static final Arrows INSTANCE = new Arrows();

    public final CheckBox arrows3d     = checkbox("Arrows 3D", false);
    public final CheckBox showDistance = checkbox("Show Distance", true);
    public final CheckBox showVertical = checkbox("Show Vertical", true);
    public final EnumSetting<ArrowStyle> style = enumSetting("Style", ArrowStyle.Default);
    public final SliderSetting radius = sliderSetting("Radius", 90, 20, 300).increment(1);
    public final SliderSetting size   = sliderSetting("Size", 42, 10, 100).increment(1);

    @AllArgsConstructor @Getter
    public enum ArrowStyle {
        Default("Default"),
        Arrow("arrow"),
        Line("Line");
        final String renderName;
    }

    private float angle = 0;

    EventBus<Event> bus = event -> {
        if (!(event instanceof Event2D)) return;
        if (mc.world == null || mc.player == null) return;

        float tick = mc.getRenderTickCounter().getTickProgress(true);
        angle = MathHelper.lerpAngleDegrees(tick, angle,
                angle + MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getYaw() - angle) * 0.1f);
        Vec3d lLerp = mc.player.getLerpedPos(tick);

        MatrixStack stack = Client.RENDERER.getStack();
        List<AbstractClientPlayerEntity> players = mc.world.getPlayers();
        boolean showDist = players.size() < 20;
        float F = mc.gameRenderer.getCamera().getPitch();

        for (PlayerEntity player : players) {
            if (player instanceof AbstractClientPlayerEntity p && !(player instanceof ClientPlayerEntity)) {
                stack.push();
                Vec3d pLerp = p.getLerpedPos(tick);

                Color color = ClientSettings.INSTANCE.targetSettings.getColor(player);
                if (color == null) color = ClientSettings.INSTANCE.getColor(0);

                float fY = arrows3d.get() ? F / 90F : 1;

                double d = lLerp.x - pLerp.x;
                double e = lLerp.z - pLerp.z;
                double ang = Math.atan2(d, e) + this.angle * MathUtility.TO_RADIANS;

                float r = radius.get();
                float s = size.get();
                float half = s / 2f;

                float x = (float) (mc.getWindow().getScaledWidth() / 2f + Math.sin(ang) * r);
                float y = (float) (mc.getWindow().getScaledHeight() / 2f + Math.cos(ang) * r * fY);

                stack.translate(MathUtility.scaledX(x), MathUtility.scaledY(y), 0);
                stack.multiply(RotationAxis.NEGATIVE_Z.rotation((float) (ang - Math.PI / 2)));
                stack.translate(-MathUtility.scaledX(x), -MathUtility.scaledY(y), 0);

                Client.RENDERER.texture(Identifier.of("topchdlc",
                                style.is(ArrowStyle.Line) ? "images/ui/triangle2.png" :
                                        style.is(ArrowStyle.Arrow) ? "images/ui/triangle3.png" : "images/ui/triangle.png"),
                        x - half, y - half + fY, s, s, 0, new Vector4f(0),
                        color, color, color, color);
                stack.pop();

                if (showDist) {
                    String text = String.format("%s", Math.ceil(pLerp.distanceTo(lLerp) * 10F) / 10F);
                    if (showDistance.get()) {
                        Client.RENDERER.textCentered(text, x, y + 4, TextureUse.SFMEDIUM, 6, ClientColors.FORE_COLOR);
                    }
                    if (showVertical.get() && MathUtility.delta((float) lLerp.y, (float) pLerp.y) > 10) {
                        Client.RENDERER.text(lLerp.y > pLerp.y ? IconUse.DOWN : IconUse.UP,
                                x - Client.RENDERER.textWidth(text, TextureUse.SFMEDIUM, 6),
                                y + 3, TextureUse.ICONS, 8, ClientColors.FORE_COLOR);
                    }
                }
            }
        }
    };

    private Arrows() {
        super("Arrows", Category.RENDER, "Стрелки к игрокам");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        Client.EVENTS.register(bus);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Client.EVENTS.unregister(bus);
    }
}