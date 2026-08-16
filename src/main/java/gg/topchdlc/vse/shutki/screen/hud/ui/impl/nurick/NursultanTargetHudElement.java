package gg.topchdlc.vse.shutki.screen.hud.ui.impl.nurick;

import gg.topchdlc.Client;
import gg.topchdlc.api.drags.Drag;
import gg.topchdlc.api.render.system.TextureUse;
import gg.topchdlc.api.render.system.sys2d.CRenderSystem;
import gg.topchdlc.vse.shutki.module.modules.impl.misc.NameProtect;
import gg.topchdlc.vse.shutki.screen.hud.ui.HudElement;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.utils.animations.Direction;
import gg.topchdlc.vse.utils.animations.impl.SmoothStepAnimation;
import gg.topchdlc.vse.utils.client.client.ClientColors;
import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.client.get.HealthUtility;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import gg.topchdlc.vse.utils.math.MathUtility;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Create by daun kvass
 */
public class NursultanTargetHudElement extends HudElement {
    CheckBox showOnHover = settings.checkbox("Show on hover", true);

    private LivingEntity lastHoveredEntity = null;
    private long lastHoverTime = 0;
    private float healthAnim   = 0;
    private float absAnim      = 0;
    private LivingEntity prevTarget = null;

    private final SmoothStepAnimation scrollAnim = new SmoothStepAnimation(1000, 1);
    private long scrollDelay = 0;

    public NursultanTargetHudElement(Drag drag) {
        super("NursultanTargetHud", drag);
        Client.EVENTS.register(this);
        animation.setDuration(450);
    }

    @Override
    public void render(int mouseX, int mouseY) {
        LivingEntity target = TargetsUtility.getTarget();
        LivingEntity hoveredEntity = null;
        if (mc.targetedEntity instanceof LivingEntity living) {
            hoveredEntity = living;
        }
        if (hoveredEntity != null) {
            lastHoverTime = System.currentTimeMillis();
            lastHoveredEntity = hoveredEntity;
        }
         boolean inChat = mc.currentScreen instanceof ChatScreen;
        boolean isHovering =showOnHover.get() &&
                lastHoveredEntity != null &&
                (System.currentTimeMillis() - lastHoverTime < 2000);
        boolean shown  = inChat || TargetsUtility.getTarget() != null || isHovering;
        if (TargetsUtility.getTarget() != null) {
            prevTarget = TargetsUtility.getTarget();
        } else if (isHovering) {
            prevTarget = lastHoveredEntity;
        } else if (inChat || prevTarget == null) {
            prevTarget = mc.player;
        }
        if (!isHovering && target == null && !inChat) {
            lastHoveredEntity = null;
        }

        animation.setDirection(shown ? Direction.BACKWARDS : Direction.FORWARDS);

        healthAnim = MathUtility.linearFps(healthAnim, HealthUtility.get(prevTarget), 10f);
        absAnim    = MathUtility.linearFps(absAnim, prevTarget.getAbsorptionAmount(), 10f);

        float anim = 1f - animation.getOutput();
        if (anim < 0.001f) return;
        float invAnim = (float) Math.pow(1f - anim, 2);

        float W = 100f, H = 36f;
        drag.width  = W;
        drag.height = H;

        CRenderSystem sys = Client.RENDERER.getCrenderSystem();
        float prevA = sys.alpha();
        sys.alpha(anim * prevA);

        Client.RENDERER.blur(x, y, W, H, new Vector4f(7), 15f, 1f);
        Color bg = new Color(10, 10, 12, 180);
        Client.RENDERER.rect(x, y, W, H, new Vector4f(7), 1f, bg, bg, bg, bg);
        float headSize = 28f;
        float headX = x + 3f - (invAnim * 15f);
        float headY = y + (H - headSize) / 2f;

        if (prevTarget instanceof PlayerEntity player) {
            SkinTextures textures = mc.getSkinProvider().supplySkinTextures(player.getGameProfile(), true).get();
            Client.RENDERER.renderHead(textures, headX, headY, headSize);
        } else {
            Identifier currentPhoto = ClientSettings.INSTANCE.customizeSetting.photomode.get().getIdentifier();
            Client.RENDERER.texture(currentPhoto, headX, headY, headSize, headSize, 1.5F, new Vector4f(0.04F, 0.038F, 0.345F, 0.5F), new Vector4f(2), Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);
        }


        float textX = x + headSize + 7f;
        float textAreaW = W - headSize - 14f;
        String name = NameProtect.INSTANCE.getEntityName(prevTarget);
        float nameY = y + 5f - (invAnim * 8f);
        sys.push(textX, y, textAreaW, H);
        {
            float nameW = Client.RENDERER.textWidth(name, TextureUse.SFMEDIUM, 7f);
            if (nameW > textAreaW) {
                if (scrollAnim.isDone() && System.currentTimeMillis() - scrollDelay > 1000) {
                    scrollDelay = System.currentTimeMillis();
                    scrollAnim.changeDirection();
                }
                float delta = nameW - textAreaW;
                Client.RENDERER.text(name, textX - delta * (float) scrollAnim.getOutput(), nameY, TextureUse.SFMEDIUM, 7f, ClientColors.FORE_COLOR);
            } else {
                Client.RENDERER.text(name, textX, nameY, TextureUse.SFMEDIUM, 7f, ClientColors.FORE_COLOR);
            }
        }
        sys.pop();

        float hpY = y + 16f;
        float hpX = textX + (invAnim * 10f);
        String hpStr = (healthAnim > 100f) ? "MANY" : String.valueOf(Math.round(healthAnim * 10f) / 10f);

        Client.RENDERER.text("HP: " + hpStr, hpX, hpY, TextureUse.SFMEDIUM, 5f, ClientColors.FORE_COLOR);

        if (absAnim > 0.1f) {
            float hpTextW = Client.RENDERER.textWidth("HP: " + hpStr, TextureUse.SFMEDIUM, 5f);
            String absStr = " + " + Math.round(absAnim * 10f) / 10f;
            Client.RENDERER.text(absStr, hpX + hpTextW, hpY, TextureUse.SFMEDIUM, 5f, new Color(255, 200, 0));
        }

        float barX = textX - 1f;
        float barY = y + H - 12f;
        float barW = textAreaW + 2f;
        float barH = 7.5f;

        Client.RENDERER.rect(barX, barY, barW, barH, new Vector4f(4), 1f, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE, ClientColors.GUI_STROKE);

        float maxHp = Math.max(1f, prevTarget.getMaxHealth());

        float hpFactor = MathUtility.clamp(healthAnim / maxHp, 0f, 1f);
        Color c1 = ClientSettings.INSTANCE.getColor(0);
        Color c2 = ClientSettings.INSTANCE.getColor(90);
        if (hpFactor > 0.01f) {
            Client.RENDERER.rect(barX, barY, barW * hpFactor, barH, new Vector4f(4), 1f, c1, c1, c2, c2);
        }

        if (absAnim > 0.1f) {
            float absFactor = MathUtility.clamp(absAnim / maxHp, 0f, 1f);
            float absWidth = barW * absFactor;
            Color g1 = new Color(255, 215, 0);
            Color g2 = new Color(255, 140, 0);
            Client.RENDERER.rect(barX + barW - absWidth, barY, absWidth, barH, new Vector4f(4), 1f, g1, g1, g2, g2);
        }

        List<ItemStack> items = new ArrayList<>();
        items.add(prevTarget.getEquippedStack(EquipmentSlot.MAINHAND));
        items.add(prevTarget.getEquippedStack(EquipmentSlot.HEAD));
        items.add(prevTarget.getEquippedStack(EquipmentSlot.CHEST));
        items.add(prevTarget.getEquippedStack(EquipmentSlot.LEGS));
        items.add(prevTarget.getEquippedStack(EquipmentSlot.FEET));
        items.add(prevTarget.getEquippedStack(EquipmentSlot.OFFHAND));
        items.removeIf(ItemStack::isEmpty);

        if (!items.isEmpty()) {
            float boxSize = 9f, spacing = 2f;
            float totalW = items.size() * boxSize + (items.size() - 1) * spacing;
            float startX = x + (W - totalW) / 2f;
            float itemY = y + H + 2f + (invAnim * 12f);

            DrawContext ctx = Client.RENDERER.getDrawContext();
            if (ctx != null) {
                Client.RENDERER.queueTask(() -> {
                    var matrices = ctx.getMatrices();
                    for (int i = 0; i < items.size(); i++) {
                        ItemStack item = items.get(i);
                        float ix = startX + i * (boxSize + spacing);

                        matrices.pushMatrix();
                        matrices.translate(ix, itemY);
                        float scale = boxSize / 16f;
                        matrices.scale(scale, scale);
                        ctx.drawItem(item, 0, 0);
                        matrices.popMatrix();

                        if (item.isDamageable()) {
                            float durability = 1.0f - ((float) item.getDamage() / item.getMaxDamage());

                            if (durability < 1.0f) {
                                float itemDurH = 1.2f;
                                float itemDurY = itemY + boxSize - 0.5f;

                                Color durCol = Color.getHSBColor(durability / 3f, 1f, 1f);

                                int alphaMain = (int) (255 * anim);
                                int alphaBack = (int) (160 * anim);

                                Color barBg = new Color(0, 0, 0, alphaBack);
                                Color barColor = new Color(durCol.getRed(), durCol.getGreen(), durCol.getBlue(), alphaMain);

                                Client.RENDERER.rect(ix, itemDurY, boxSize, itemDurH, new Vector4f(0), 1, barBg, barBg, barBg, barBg);

                                Client.RENDERER.rect(ix, itemDurY, boxSize * durability, itemDurH, new Vector4f(0), 1, barColor, barColor, barColor, barColor);
                            }
                        }
                    }
                });
            }
        }

        sys.alpha(prevA);
    }

}