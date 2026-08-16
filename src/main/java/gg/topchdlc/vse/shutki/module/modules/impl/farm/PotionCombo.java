package gg.topchdlc.vse.shutki.module.modules.impl.farm;

import gg.topchdlc.vse.utils.network.NetworkUtility;
import net.minecraft.block.AnvilBlock;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import gg.topchdlc.api.autobuy.AutoBuyUtil;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventReceivePacket;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import gg.topchdlc.vse.utils.math.TimeUtility;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class PotionCombo extends Module {
    public static final PotionCombo INSTANCE = new PotionCombo();

    private final SliderSetting actionDelay = sliderSetting("Задержка", 150f, 50f, 1000f).increment(10f);
    private final SliderSetting maxXpPrice = sliderSetting("Цена опыта при покупке", 500f, 10f, 10000f).increment(50f);

    private enum State {
        IDLE,BUY_XP_SEARCH,BUY_XP_WAIT_AH,BUY_XP_DELAY,BUY_XP_BUY_CLICK,BUY_XP_WAIT_BUY,DROP_XP,FIND_ANVIL,OPEN_ANVIL,WAIT_ANVIL_OPEN,ANVIL_PICK_1,ANVIL_DROP_1, ANVIL_RETURN_1,ANVIL_PICK_2, ANVIL_DROP_2, ANVIL_RETURN_2,ANVIL_WAIT_RESULT,ANVIL_TAKE_RESULT,ANVIL_CLOSE,AFK_MOVE_BACK,AFK_WAIT,AFK_MOVE_FORWARD
    }
    private State state = State.IDLE;
    private final TimeUtility timer = new TimeUtility();
    private int slot1 = -1, slot2 = -1;
    private Vec3d startPos = null;
    private int moveTicks = 0;
    private PotionCombo() {
        super("PotionCombo", Category.Misc, "Соединение зелий с задержкой на AH");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        state = State.IDLE;
        timer.reset();
        startPos = null;
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        releaseAllKeys();
    }

    private record PotionData(RegistryEntry<StatusEffect> type, int level, int count) {}

    private PotionData getPotion(ItemStack stack) {
        if (stack.isEmpty()) return null;
        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) return null;

        List<StatusEffectInstance> effects = new ArrayList<>();
        contents.getEffects().forEach(effects::add);
        if (effects.size() != 1) return null;

        StatusEffectInstance effect = effects.get(0);
        RegistryEntry<StatusEffect> type = effect.getEffectType();
        if (type.equals(StatusEffects.STRENGTH) || type.equals(StatusEffects.SPEED)) {
            return new PotionData(type, effect.getAmplifier() + 1, stack.getCount());
        }
        return null;
    }

    private int[] findMergePair() {
        List<Integer> s2 = new ArrayList<>(), v2 = new ArrayList<>(), s3 = new ArrayList<>(), v3 = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            PotionData d = getPotion(mc.player.getInventory().getStack(i));
            if (d == null) continue;
            if (d.type.equals(StatusEffects.STRENGTH)) {
                if (d.level == 2) { if (d.count >= 2) return new int[]{i, i}; s2.add(i); }
                else if (d.level == 3) s3.add(i);
            } else if (d.type.equals(StatusEffects.SPEED)) {
                if (d.level == 2) { if (d.count >= 2) return new int[]{i, i}; v2.add(i); }
                else if (d.level == 3) v3.add(i);
            }
        }
        if (s2.size() >= 2) return new int[]{s2.get(0), s2.get(1)};
        if (v2.size() >= 2) return new int[]{v2.get(0), v2.get(1)};
        if (!s3.isEmpty() && !v3.isEmpty()) return new int[]{s3.get(0), v3.get(0)};
        return null;
    }

    EventBus<Event> events = event -> {
        if (mc.player == null) return;

        if (event instanceof EventReceivePacket e) {
            if (e.getPacket() instanceof GameMessageS2CPacket packet) {
                String msg = packet.content().getString().toLowerCase(Locale.ROOT);
                if (msg.contains("не доступна в режиме afk") || msg.contains("недоступно нажимать в режиме afk")) {
                    if (state == State.BUY_XP_WAIT_AH || state == State.BUY_XP_SEARCH) {
                        startPos = mc.player.getEntityPos();
                        moveTicks = 0;
                        state = State.AFK_MOVE_BACK;
                        timer.reset();
                    }
                }
            }
        }

        if (!(event instanceof EventGameTick)) return;
        long delay = (long) actionDelay.get();

        if (state.name().startsWith("ANVIL_") && !(mc.currentScreen instanceof AnvilScreen)) {
            state = State.IDLE; return;
        }

        switch (state) {
            case IDLE:
                if (timer.reached(500)) {
                    int[] pair = findMergePair();
                    if (pair != null) {
                        slot1 = pair[0]; slot2 = pair[1];
                        state = (mc.player.experienceLevel < 5 && !hasXpBottles()) ? State.BUY_XP_SEARCH : State.DROP_XP;
                    }
                    timer.reset();
                }
                break;
            case AFK_MOVE_BACK:
                tickMove(true);
                break;

            case AFK_WAIT:
                releaseAllKeys();
                if (timer.reached(500)) {
                    startPos = mc.player.getEntityPos();
                    moveTicks = 0;
                    state = State.AFK_MOVE_FORWARD;
                    timer.reset();
                }
                break;

            case AFK_MOVE_FORWARD:
                tickMove(false);
                break;

            case BUY_XP_SEARCH:
                if (timer.reached(delay)) {
                    NetworkUtility.sendCommand("ah search опыт");
                    state = State.BUY_XP_WAIT_AH;
                    timer.reset();
                }
                break;

            case BUY_XP_WAIT_AH:
                if (mc.currentScreen instanceof GenericContainerScreen) {
                    state = State.BUY_XP_DELAY;
                    timer.reset();
                } else if (timer.reached(4000)) state = State.IDLE;
                break;

            case BUY_XP_DELAY:
                if (timer.reached(3000)) {
                    state = State.BUY_XP_BUY_CLICK;
                    timer.reset();
                }
                break;

            case BUY_XP_BUY_CLICK:
                if (mc.currentScreen instanceof GenericContainerScreen screen) {
                    Slot target = screen.getScreenHandler().slots.stream()
                            .filter(s -> s.getStack().getItem() == Items.EXPERIENCE_BOTTLE)
                            .filter(s -> (AutoBuyUtil.getPrice(s.getStack()) / Math.max(1, s.getStack().getCount())) <= maxXpPrice.get())
                            .min(Comparator.comparingLong(s -> AutoBuyUtil.getPrice(s.getStack()))).orElse(null);

                    if (target != null && target.id < screen.getScreenHandler().slots.size()) {
                        click(target.id, 0, SlotActionType.QUICK_MOVE);
                        state = State.BUY_XP_WAIT_BUY;
                    } else {
                        mc.player.closeHandledScreen();
                        state = State.IDLE;
                    }
                    timer.reset();
                }
                break;

            case BUY_XP_WAIT_BUY:
                if (timer.reached(1000)) {
                    if (mc.currentScreen instanceof GenericContainerScreen) mc.player.closeHandledScreen();
                    state = State.DROP_XP;
                    timer.reset();
                }
                break;

            case DROP_XP:
                if (mc.player.experienceLevel >= 5) state = State.FIND_ANVIL;
                else if (timer.reached(delay / 2)) {
                    int bottleSlot = findItem(Items.EXPERIENCE_BOTTLE);
                    if (bottleSlot != -1) {
                        if (bottleSlot < 9) mc.player.getInventory().setSelectedSlot(bottleSlot);
                        else click(bottleSlot, mc.player.getInventory().getSelectedSlot(), SlotActionType.SWAP);
                        mc.player.setPitch(90);
                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                        timer.reset();
                    } else state = State.BUY_XP_SEARCH;
                }
                break;

            case FIND_ANVIL:
                if (findAnvilPos() != null) state = State.OPEN_ANVIL;
                else { ChatUtility.send(Text.of("Наковальня не найдена")); setEnabled(false); }
                break;

            case OPEN_ANVIL:
                BlockPos pos = findAnvilPos();
                if (pos != null) {
                    float[] rots = getRotations(Vec3d.ofCenter(pos));
                    mc.player.setYaw(rots[0]); mc.player.setPitch(rots[1]);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
                    state = State.WAIT_ANVIL_OPEN;
                    timer.reset();
                }
                break;

            case WAIT_ANVIL_OPEN:
                if (mc.currentScreen instanceof AnvilScreen) { state = State.ANVIL_PICK_1; timer.reset(); }
                break;

            case ANVIL_PICK_1:
                if (timer.reached(delay)) {
                    if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                        click(invToGui(slot1), 0, SlotActionType.PICKUP);
                    } else {
                        click(invToGui(slot1), 0, SlotActionType.PICKUP);
                        state = State.ANVIL_DROP_1;
                    }
                    timer.reset();
                }
                break;

            case ANVIL_DROP_1:
                if (timer.reached(delay)) {
                    if (mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                        state = State.ANVIL_PICK_1;
                    } else {
                        click(0, 1, SlotActionType.PICKUP);
                        state = State.ANVIL_RETURN_1;
                    }
                    timer.reset();
                }
                break;

            case ANVIL_RETURN_1:
                if (timer.reached(delay)) {
                    if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                        click(invToGui(slot1), 0, SlotActionType.PICKUP);
                    } else {
                        state = State.ANVIL_PICK_2;
                    }
                    timer.reset();
                }
                break;

            case ANVIL_PICK_2:
                if (timer.reached(delay)) {
                    if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                        click(invToGui(slot2), 0, SlotActionType.PICKUP);
                    } else {
                        click(invToGui(slot2), 0, SlotActionType.PICKUP);
                        state = State.ANVIL_DROP_2;
                    }
                    timer.reset();
                }
                break;

            case ANVIL_DROP_2:
                if (timer.reached(delay)) {
                    if (mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                        state = State.ANVIL_PICK_2;
                    } else {
                        click(1, 1, SlotActionType.PICKUP);
                        state = State.ANVIL_RETURN_2;
                    }
                    timer.reset();
                }
                break;

            case ANVIL_RETURN_2:
                if (timer.reached(delay)) {
                    if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                        click(invToGui(slot2), 0, SlotActionType.PICKUP);
                    } else {
                        state = State.ANVIL_WAIT_RESULT;
                    }
                    timer.reset();
                }
                break;

            case ANVIL_WAIT_RESULT:
                if (mc.currentScreen instanceof AnvilScreen screen) {
                    if (!screen.getScreenHandler().getSlot(2).getStack().isEmpty()) {
                        state = State.ANVIL_TAKE_RESULT;
                        timer.reset();
                    }
                    else if (timer.reached(1500)) {
                        click(0, 0, SlotActionType.QUICK_MOVE);
                        click(1, 0, SlotActionType.QUICK_MOVE);
                        state = State.IDLE;
                        timer.reset();
                    }
                }
                break;
            case ANVIL_TAKE_RESULT:
                if (timer.reached(delay)) {
                    click(2, 0, SlotActionType.QUICK_MOVE);
                    state = State.ANVIL_CLOSE;
                    timer.reset();
                }
                break;

            case ANVIL_CLOSE:
                if (mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                    mc.player.closeHandledScreen();
                    state = State.IDLE;
                } else {
                    click(-999, 0, SlotActionType.PICKUP);
                }
                timer.reset();
                break;
        }
    };
    private void tickMove(boolean backward) {
        if (startPos == null) { startPos = mc.player.getEntityPos(); moveTicks = 0; }
        double walked = Math.sqrt(mc.player.squaredDistanceTo(startPos.x, mc.player.getY(), startPos.z));

        if (walked >= 1.5 || moveTicks > 60) {
            releaseAllKeys();
            state = backward ? State.AFK_WAIT : State.BUY_XP_SEARCH;
            timer.reset();
            return;
        }
        moveTicks++;
        setKeyState(mc.options.backKey, backward);
        setKeyState(mc.options.forwardKey, !backward);
    }

    private void setKeyState(net.minecraft.client.option.KeyBinding key, boolean pressed) {
        KeyBinding.setKeyPressed(key.getDefaultKey(), pressed);
        key.setPressed(pressed);
    }

    private void releaseAllKeys() {
        setKeyState(mc.options.forwardKey, false);
        setKeyState(mc.options.backKey, false);
    }

    private void click(int slot, int button, SlotActionType type) {
        if (mc.player.currentScreenHandler == null) return;
        if (slot < 0 || slot >= mc.player.currentScreenHandler.slots.size()) return;
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, button, type, mc.player);
    }

    private int invToGui(int slot) { return (slot < 9) ? slot + 30 : slot - 9 + 3; }

    private int findItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 36; i++) if (mc.player.getInventory().getStack(i).getItem() == item) return i;
        return -1;
    }

    private boolean hasXpBottles() { return findItem(Items.EXPERIENCE_BOTTLE) != -1; }

    private BlockPos findAnvilPos() {
        BlockPos p = mc.player.getBlockPos();
        BlockPos best = null; double dist = Double.MAX_VALUE;
        for (int x = -5; x <= 5; x++)
            for (int y = -3; y <= 3; y++)
                for (int z = -5; z <= 5; z++) {
                    BlockPos pos = p.add(x, y, z);
                    if (mc.world.getBlockState(pos).getBlock() instanceof AnvilBlock) {
                        double d = mc.player.squaredDistanceTo(pos.toCenterPos());
                        if (d < dist) { dist = d; best = pos; }
                    }
                }
        return best;
    }

    private float[] getRotations(Vec3d target) {
        Vec3d diff = target.subtract(mc.player.getEyePos());
        float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90;
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z)));
        return new float[]{yaw, pitch};
    }
}