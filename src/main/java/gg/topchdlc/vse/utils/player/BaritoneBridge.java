package gg.topchdlc.vse.utils.player;

import gg.topchdlc.MinecraftHolder;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Reflection bridge to Baritone (baritone.api.*). Lets modules drive Baritone
 * pathfinding when it is installed and installed in the game, while compiling
 * and running cleanly without it (like the original try/catch approach).
 */
public class BaritoneBridge implements MinecraftHolder {

    private static boolean resolved = false;
    private static boolean available = false;

    private static Method apiGetProvider;
    private static Method providerGetPrimaryBaritone;
    private static Method baritoneGetPathingBehavior;
    private static Method pbSetGoal;
    private static Method pbCancel;
    private static Method pbRepath;
    private static Method pbIsPathing;
    private static Method pbGetGoal;

    private static Constructor<?> goalNearCtor;
    private static Constructor<?> goalNearCtorLegacy;
    private static Method goalNearGetGoalPos;

    private static Object primaryBaritone;

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            Class<?> api = Class.forName("baritone.api.BaritoneAPI");
            apiGetProvider = api.getMethod("getProvider");
            Class<?> provider = apiGetProvider.getReturnType();
            providerGetPrimaryBaritone = provider.getMethod("getPrimaryBaritone");
            Object providerInstance = apiGetProvider.invoke(null);
            primaryBaritone = providerGetPrimaryBaritone.invoke(providerInstance);

            Class<?> baritone = providerGetPrimaryBaritone.getReturnType();
            baritoneGetPathingBehavior = baritone.getMethod("getPathingBehavior");

            Class<?> pb = baritoneGetPathingBehavior.getReturnType();
            Class<?> goal = Class.forName("baritone.api.pathing.goals.Goal");
            pbSetGoal = pb.getMethod("setGoal", goal);
            pbIsPathing = pb.getMethod("isPathing");
            pbGetGoal = pb.getMethod("getGoal");
            pbCancel = findMethod(pb, "cancel", "cancelEverything");
            pbRepath = findMethod(pb, "repath", "path", "pathAroundBlock");

            Class<?> goalNear = Class.forName("baritone.api.pathing.goals.GoalNear");
            goalNearCtor = findConstructor(goalNear, new Class[]{BlockPos.class, int.class});
            goalNearCtorLegacy = findConstructor(goalNear, new Class[]{int.class, int.class, int.class, int.class});
            goalNearGetGoalPos = findMethod(goalNear, "getGoalPos");

            available = primaryBaritone != null;
        } catch (Throwable t) {
            available = false;
        }
    }

    public static boolean isAvailable() {
        resolve();
        return available;
    }

    public static void goTo(BlockPos pos, int range) {
        resolve();
        if (!available) return;
        try {
            Object goal = newGoal(pos, range);
            if (goal == null) return;
            Object pb = baritoneGetPathingBehavior.invoke(primaryBaritone);
            pbSetGoal.invoke(pb, goal);
            if (pbRepath != null) {
                try {
                    pbRepath.invoke(pb);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void cancel() {
        resolve();
        if (!available) return;
        try {
            Object pb = baritoneGetPathingBehavior.invoke(primaryBaritone);
            if (pbCancel != null) pbCancel.invoke(pb);
        } catch (Throwable ignored) {
        }
    }

    public static boolean isPathing() {
        resolve();
        if (!available) return false;
        try {
            Object pb = baritoneGetPathingBehavior.invoke(primaryBaritone);
            return (Boolean) pbIsPathing.invoke(pb);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean hasGoal(BlockPos pos) {
        resolve();
        if (!available) return false;
        try {
            Object pb = baritoneGetPathingBehavior.invoke(primaryBaritone);
            Object goal = pbGetGoal.invoke(pb);
            return goal != null && matchesGoalPos(goal, pos);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object newGoal(BlockPos pos, int range) {
        try {
            if (goalNearCtor != null) {
                return goalNearCtor.newInstance(pos, range);
            }
            if (goalNearCtorLegacy != null) {
                return goalNearCtorLegacy.newInstance(pos.getX(), pos.getY(), pos.getZ(), range);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean matchesGoalPos(Object goal, BlockPos pos) {
        if (goalNearGetGoalPos != null) {
            try {
                Object goalPos = goalNearGetGoalPos.invoke(goal);
                return goalPos instanceof BlockPos bp && bp.equals(pos);
            } catch (Throwable ignored) {
            }
        }
        if (goal instanceof BlockPos bp) {
            return bp.equals(pos);
        }
        return false;
    }

    private static Method findMethod(Class<?> clazz, String... names) {
        for (String name : names) {
            try {
                return clazz.getMethod(name);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Constructor<?> findConstructor(Class<?> clazz, Class<?>[] params) {
        try {
            return clazz.getConstructor(params);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
