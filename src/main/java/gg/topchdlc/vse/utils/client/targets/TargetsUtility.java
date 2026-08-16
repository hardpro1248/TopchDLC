package gg.topchdlc.vse.utils.client.targets;

import gg.topchdlc.vse.utils.client.client.ClientSettings;
import gg.topchdlc.vse.utils.client.get.HealthUtility;
import gg.topchdlc.vse.utils.math.RotationUtility;
import gg.topchdlc.vse.utils.player.PlayerUtility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Supplier;

import static gg.topchdlc.MinecraftHolder.mc;

public class TargetsUtility {
    @Getter
    private static LivingEntity target;
    @Setter
    @Getter
    private static LivingEntity lastTarget;
    @Getter
    private static boolean targetLocked = false;

    private static long lastSearchTime = -1;
    private static float lastSearchRange = -1;
    private static Sort lastSearchSort = null;

    public static boolean isValid() {
        return target != null && ClientSettings.INSTANCE.targetSettings.isValid(target);
    }

    public static LivingEntity find(float range, Sort sort) {
        find(range, sort, ClientSettings.INSTANCE.targetSettings);
        return isValid() ? target : null;
    }

    private static void find(float range, Sort sort, ITargetSettings validator) {
        if (mc.player == null || mc.world == null) {
            target = null;
            return;
        }

        lastTarget = target;
        boolean targetLockedEnabled = ClientSettings.INSTANCE.targetSettings.focus.get();

        if (targetLockedEnabled && target != null && target.isAlive() && !target.isRemoved() &&
                mc.player.squaredDistanceTo(target) <= (range * range) && validator.isValid(target)) {
            targetLocked = true;
            return;
        }

        if (targetLockedEnabled && (target == null || !target.isAlive() || target.isRemoved() ||
                mc.player.squaredDistanceTo(target) > (range * range) || !validator.isValid(target))) {
            unlockTarget();
        }

        long currentTick = mc.world.getTime();
        if (currentTick == lastSearchTime && range == lastSearchRange && sort == lastSearchSort) {
            return;
        }
        lastSearchTime = currentTick;
        lastSearchRange = range;
        lastSearchSort = sort;

        LivingEntity bestTarget = null;
        float rangeSq = range * range;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player || living instanceof ClientPlayerEntity) continue;
            if (!living.isAlive() || living.isRemoved()) continue;
            if (mc.player.squaredDistanceTo(living) > rangeSq) continue;
            if (!validator.isValid(living)) continue;

            if (bestTarget == null) {
                bestTarget = living;
            } else {
                if (sort.compare(living, bestTarget) < 0) {
                    bestTarget = living;
                }
            }
        }

        target = bestTarget;
    }

    public static void unlockTarget() {
        targetLocked = false;
    }

    private static ArrayList<LivingEntity> getTargets(float range, ITargetSettings validator) {
        ArrayList<LivingEntity> entities = new ArrayList<>();
        if (mc.player == null || mc.world == null) return entities;

        float rangeSq = range * range;
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity living &&
                    !(entity instanceof ClientPlayerEntity) &&
                    mc.player.squaredDistanceTo(living) <= rangeSq &&
                    living.isAlive() && !living.isRemoved() &&
                    validator.isValid(living)) {
                entities.add(living);
            }
        }
        return entities;
    }

    public static void reset() {
        target = null;
        targetLocked = false;
        lastSearchTime = -1;
    }

    public static void lockTarget() {
        if (target != null) targetLocked = true;
    }

    public static void toggleTargetLock() {
        targetLocked = target != null && !targetLocked;
    }

    public static Integer getArmorColor(ItemStack stack) {
        if (stack.isIn(ItemTags.DYEABLE)) {
            return DyedColorComponent.getColor(stack, DyedColorComponent.DEFAULT_COLOR);
        } else {
            return null;
        }
    }

    @AllArgsConstructor
    @Getter
    public enum Sort {
        Distance(() -> Comparator.comparingDouble(mc.player::squaredDistanceTo)),
        Health(() -> Comparator.comparingDouble(HealthUtility::get)),
        Angle(() -> Comparator.comparingDouble(RotationUtility::calculateFOVFromCamera)),
        Adaptive(
                () -> Comparator.comparingDouble(PlayerUtility::compareArmor)
                        .thenComparingDouble(RotationUtility::calculateFOVFromCamera)
                        .thenComparingDouble(mc.player::squaredDistanceTo)
                        .thenComparingDouble(HealthUtility::get)
        );

        final Supplier<Comparator<LivingEntity>> comparator;

        public int compare(LivingEntity e1, LivingEntity e2) {
            if (mc.player == null) return 0;

            switch (this) {
                case Distance:
                    return Double.compare(mc.player.squaredDistanceTo(e1), mc.player.squaredDistanceTo(e2));
                case Health:
                    return Double.compare(HealthUtility.get(e1), HealthUtility.get(e2));
                case Angle:
                    return Double.compare(RotationUtility.calculateFOVFromCamera(e1), RotationUtility.calculateFOVFromCamera(e2));
                case Adaptive:
                    int armorComp = Double.compare(PlayerUtility.compareArmor(e1), PlayerUtility.compareArmor(e2));
                    if (armorComp != 0) return armorComp;

                    int angleComp = Double.compare(RotationUtility.calculateFOVFromCamera(e1), RotationUtility.calculateFOVFromCamera(e2));
                    if (angleComp != 0) return angleComp;

                    int distComp = Double.compare(mc.player.squaredDistanceTo(e1), mc.player.squaredDistanceTo(e2));
                    if (distComp != 0) return distComp;

                    return Double.compare(HealthUtility.get(e1), HealthUtility.get(e2));
            }
            return 0;
        }
    }
}