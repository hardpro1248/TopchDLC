package gg.topchdlc.vse.shutki.module.modules.impl.combat;

import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffects;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.utils.client.targets.TargetsUtility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Hand;

import java.util.Random;

/**
 * Create by daun kvass
 */
public class TriggerBot extends Module {
    public static final TriggerBot INSTANCE = new TriggerBot();
    private TriggerBot() {
        super("TriggerBot", Category.COMBAT, "Бьет, когда наводишься на цель");
    }
    CheckBox onlycrit = checkbox("Только криты", false);
    CheckBox shieldcheck = checkbox("Проверка на щит", true);
    CheckBox selfshieldcheck =checkbox("Бить с щитом", false);
    CheckBox randomDelay = checkbox("Рандомная задержка", false);
    CheckBox throughFriends = checkbox("Сквозь друзей", false);
    CheckBox spaceOnly = checkbox("Только с пробелом",false).visible(onlycrit::get);
    CheckBox pauseEating =  checkbox("Остановка при еде",true);

    private int ticksToWait = 0;
    private int currentTick = 0;
    private final Random random = new Random();

    EventBus<Event> events = event -> {
        if (event instanceof EventGameTick) {
            if (mc.player == null || mc.targetedEntity == null) return;
            if (!mc.targetedEntity.isLiving()) return;
            LivingEntity livingEntity = (LivingEntity) mc.targetedEntity;
            if (livingEntity instanceof PlayerEntity player && Client.FRIENDS.isFriend(player)) {
                if (!throughFriends.get()) return;
                LivingEntity target = TargetsUtility.find(6f, TargetsUtility.Sort.Angle);
                if (target == null) return;
                livingEntity = target;
            }
            if (mc.player.isUsingItem() && pauseEating.get()) {
                return;
            }
            if (mc.player.getAttackCooldownProgress(0.5f) > 0.9) {
                if (mc.player.fallDistance > 1 && !mc.player.isGliding()) return;
                if (!CustomCrit()) return;
                if (livingEntity.getHealth() == 0) return;
                if (mc.currentScreen != null) return;
                if (shieldcheck.get() && livingEntity.getActiveItem().getItem() instanceof ShieldItem) return;
                if (!selfshieldcheck.get() && mc.player.getActiveItem().getItem() instanceof ShieldItem) return;
                if (randomDelay.get()) {
                    if (currentTick < ticksToWait) {
                        currentTick++;
                        return;
                    }
                }
                mc.interactionManager.attackEntity(mc.player, livingEntity);
                mc.player.swingHand(Hand.MAIN_HAND);
                if (randomDelay.get()) {
                    ticksToWait = random.nextInt(4);
                    currentTick = 0;
                }
            }
        }
    };
    private boolean CustomCrit() {
        boolean reasonForSkipCrit = !onlycrit.get()
                || mc.player.getAbilities().flying
                || mc.player.hasStatusEffect(StatusEffects.LEVITATION)
                || mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                || mc.world.getBlockState(mc.player.getBlockPos()).getBlock() == Blocks.LADDER
                || mc.player.isGliding()
                || mc.player.isTouchingWater() || mc.player.isSubmergedInWater();

        if (mc.player.getAttackCooldownProgress(0.5f) < (mc.player.isOnGround() ? 1f : 0.9f))
            return false;

        boolean mergeWithSpeed = mc.player.isOnGround();

        if (!mc.options.jumpKey.isPressed() && mergeWithSpeed && spaceOnly.get())
            return true;

        if (mc.player.isInLava())
            return true;

        if (!reasonForSkipCrit)
            return !mc.player.isOnGround() && mc.player.fallDistance > 0.0f;
        return true;
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        currentTick = 0;
        ticksToWait = randomDelay.get() ? random.nextInt(4) : 0;
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        TargetsUtility.reset();
    }
}
