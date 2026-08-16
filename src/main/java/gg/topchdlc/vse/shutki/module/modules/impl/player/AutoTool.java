package gg.topchdlc.vse.shutki.module.modules.impl.player;

import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.List;

public class AutoTool extends Module {
    public static final AutoTool INSTANCE = new AutoTool();

    public CheckBox swapBack = checkbox("SwapBack", true);
    public CheckBox saveItem = checkbox("SaveItem", true);
    public CheckBox silent = checkbox("Silent", false);
    public CheckBox echestSilk = checkbox("EchestSilk", true);

    public static int itemIndex;
    private boolean swap;
    private long swapDelay;
    private final List<Integer> lastItem = new ArrayList<>();

    private AutoTool() {
        super("AutoTool", Category.PLAYER, "x");
    }

    EventBus<Event> events = event -> {
        if (event instanceof EventGameTick) onUpdate();
    };

    public void onUpdate() {
        if (!(mc.crosshairTarget instanceof BlockHitResult)) return;
        BlockHitResult result = (BlockHitResult) mc.crosshairTarget;
        BlockPos pos = result.getBlockPos();
        if (mc.world.getBlockState(pos).isAir())
            return;

        if (getTool(pos) != -1 && mc.options.attackKey.isPressed()) {
            lastItem.add(mc.player.getInventory().getSelectedSlot());

            if (silent.get()) mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(getTool(pos)));
            else mc.player.getInventory().setSelectedSlot(getTool(pos));

            itemIndex = getTool(pos);
            swap = true;

            swapDelay = System.currentTimeMillis();
        } else if (swap && !lastItem.isEmpty() && System.currentTimeMillis() >= swapDelay + 300 && swapBack.get()) {
            if (silent.get())
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(lastItem.get(0)));
            else mc.player.getInventory().setSelectedSlot(lastItem.get(0));

            itemIndex = lastItem.get(0);
            lastItem.clear();
            swap = false;
        }
    }

    public int getTool(final BlockPos pos) {
        if (mc.world == null || mc.player == null) return -1;

        int index = -1;
        float currentFastest = 1.0f;
        var blockState = mc.world.getBlockState(pos);

        if (blockState.isAir()) return -1;

        var enchantmentRegistry = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

        var efficiencyEntry = enchantmentRegistry.getOptional(Enchantments.EFFICIENCY).orElse(null);
        var silkTouchEntry = enchantmentRegistry.getOptional(Enchantments.SILK_TOUCH).orElse(null);

        for (int i = 0; i < 9; ++i) {
            final ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            if (saveItem.get() && stack.isDamageable() && (stack.getMaxDamage() - stack.getDamage() <= 10)) {
                continue;
            }

            float digSpeed = stack.getMiningSpeedMultiplier(blockState);

            if (digSpeed > 1.0f) {
                if (efficiencyEntry != null) {
                    int level = EnchantmentHelper.getLevel(efficiencyEntry, stack);
                    if (level > 0) {
                        digSpeed += (float) (level * level + 1);
                    }
                }
            }

            if (blockState.getBlock() instanceof EnderChestBlock && echestSilk.get()) {
                if (silkTouchEntry != null && EnchantmentHelper.getLevel(silkTouchEntry, stack) > 0) {
                    if (digSpeed > currentFastest) {
                        currentFastest = digSpeed;
                        index = i;
                    }
                }
            } else {
                if (digSpeed > currentFastest) {
                    currentFastest = digSpeed;
                    index = i;
                }
            }
        }
        return index;
    }
}