package gg.topchdlc.vse.shutki.module.modules.impl.combat;

import gg.topchdlc.api.events.list.EventPacket;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.event.GameEvent;

import static gg.topchdlc.MinecraftHolder.mc;
import static gg.topchdlc.vse.shutki.module.modules.impl.combat.Criticals.getEntity;
import static gg.topchdlc.vse.shutki.module.modules.impl.combat.Criticals.getInteractType;

public class CrystalOptimizer extends Module {
    public static final CrystalOptimizer INSTANCE = new CrystalOptimizer();

    private CrystalOptimizer() {
        super("CrystalOptimizer", Category.Misc, "Оптимизирует кристаллы");
    }

    @EventHandler
    public void onPacketSend(EventPacket event) {
        if (mc.player == null || mc.world == null) return;

        if (event.getPacket() instanceof PlayerInteractEntityC2SPacket packet) {
            if (getInteractType(packet) == Criticals.InteractType.ATTACK) {
                Entity entity = getEntity(packet);

                if (entity instanceof EndCrystalEntity crystal) {
                    if (canDestroyCrystal(mc.player)) {
                        crystal.setRemoved(Entity.RemovalReason.KILLED);
                        crystal.emitGameEvent(GameEvent.ENTITY_DIE);
                        retargetCrosshair(mc, crystal);
                    }
                }
            }
        }
    }

    private boolean canDestroyCrystal(ClientPlayerEntity player) {
        double damage = player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
        damage += getWeaponDamage(player.getMainHandStack());

        StatusEffectInstance strength = player.getStatusEffect(StatusEffects.STRENGTH);
        if (strength != null) {
            damage += 3.0 * (strength.getAmplifier() + 1);
        }

        StatusEffectInstance weakness = player.getStatusEffect(StatusEffects.WEAKNESS);
        if (weakness != null) {
            damage -= 4.0 * (weakness.getAmplifier() + 1);
        }

        return damage > 0.0;
    }

    private double getWeaponDamage(ItemStack item) {
        if (item.isEmpty()) return 0.0;

        double[] sum = new double[]{0.0};

        item.applyAttributeModifiers(EquipmentSlot.MAINHAND, (RegistryEntry<EntityAttribute> attribute, EntityAttributeModifier modifier) -> {
            if (EntityAttributes.ATTACK_DAMAGE.equals(attribute)) {
                sum[0] += modifier.value();
            }
        });

        return sum[0];
    }

    private void retargetCrosshair(MinecraftClient client, EndCrystalEntity crystal) {
        ClientPlayerEntity player = client.player;
        if (player != null && client.crosshairTarget != null && client.targetedEntity == crystal) {
            double reach = player.getAbilities().creativeMode ? 5.0F : 4.5F;
            HitResult retraced = player.raycast(reach, 1.0F, false);
            client.targetedEntity = null;
            client.crosshairTarget = retraced;
        }
    }
}