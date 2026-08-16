package gg.topchdlc.vse.shutki.module.modules.impl.farm;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;import net.minecraft.registry.Registries;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.Event;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventGameTick;
import gg.topchdlc.api.events.list.EventInput;
import gg.topchdlc.vse.shutki.module.modules.impl.player.AutoInvis;
import gg.topchdlc.vse.utils.player.MoveUtility;
import gg.topchdlc.vse.rotation.Angle;
import gg.topchdlc.vse.rotation.MovementCorrection;
import gg.topchdlc.vse.rotation.all.Rotation;
import gg.topchdlc.vse.shutki.module.modules.Category;
import gg.topchdlc.vse.shutki.module.modules.Module;
import gg.topchdlc.vse.shutki.module.settings.impl.button.ButtonSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.itemlist.ItemListSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.slider.SliderSetting;
import gg.topchdlc.vse.utils.client.text.ChatUtility;
import gg.topchdlc.vse.utils.math.Priorities;
import gg.topchdlc.vse.utils.math.RotationUtility;
import gg.topchdlc.vse.utils.math.TimeUtility;
import gg.topchdlc.vse.utils.network.NetworkUtility;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Random;

public class AutoVillager extends Module {
    public static final AutoVillager INSTANCE = new AutoVillager();

    private enum Phase { SCAN, BUY }
    private enum State {
        FIND_NEXT, WALK, LOOK, SMOOTH_LOOK, PRESS_USE, RELEASE_USE, WAIT_SCREEN, INSPECT, SELECT_TRADE, TAKE_RESULT, BUY, CLOSE
    }

    private static final double SCAN_RANGE = 50.0;
    private static final double DEST_DIST  = 1.8;
    private static final double STOP_DIST  = 0.5;

    private static class VillagerData {
        UUID uuid; double x, y, z; boolean valid;
        VillagerData(UUID u, double x, double y, double z, boolean v) {
            uuid=u; this.x=x; this.y=y; this.z=z; valid=v;
        }
    }

    private Phase phase = Phase.SCAN;
    private State state = State.FIND_NEXT;
    private final List<VillagerData> allVillagers = new ArrayList<>();
    private final Set<UUID> scanned = new HashSet<>();
    private int index = 0, waitTicks = 0, selectedTradeSlot = -1;
    private final TimeUtility stateTimer = new TimeUtility();
    private float currentYaw = 0f;
    private float moveSpeed = 1f;
    private float strafeNoise = 0f;
    private int noiseTimer = 0;
    private final Random rng = new Random();
    private float accel = 0f;
    private float moveTargetYaw = 0f;
    private boolean isWalking = false;
    private float targetLookYaw = 0f, targetLookPitch = 0f;
    private int lookTicks = 0;
    private static final int LOOK_TICKS_NEEDED = 12;
    private final ItemListSetting buyItems = itemListSetting("Buy Item");
    private final SliderSetting delay = sliderSetting("Delay", 300f, 50f, 2000f).increment(50f);
    private final CheckBox debug = checkbox("Debug", false);
    private static final Rotation SMOOTH = (cur, tgt) -> {
        float step = 6f;
        float dy = MathHelper.clamp(
               MathHelper.wrapDegrees(tgt.getYaw() - cur.getYaw()), -step, step);
        float dp = MathHelper.clamp(tgt.getPitch() - cur.getPitch(), -step, step);
        return new Angle(cur.getYaw() + dy,
                MathHelper.clamp(cur.getPitch() + dp, -90f, 90f));
    };

    private AutoVillager() {
        super("AutoVillager", Category.PLAYER, "авто покупка предметов у житаков");
    }

    @Override
    protected void onEnable() {
        loadVillagers();
        long validCount = allVillagers.stream().filter(v -> v.valid).count();
        if (validCount > 0) {
            phase = Phase.BUY;
            ChatUtility.send("§aAutoVillager: §f" + allVillagers.size() + "§a жителей (§f" + validCount + "§a валидных), покупаю");
        } else {
            phase = Phase.SCAN;
            ChatUtility.send("§aAutoVillager: §f" + allVillagers.size() + "§a жителей, сканирую");
        }
        index = 0; waitTicks = 0; state = State.FIND_NEXT;
        stopWalking(); stateTimer.reset();
    }

    @Override
    protected void onDisable() { stopWalking(); saveVillagers(); }

    EventBus<Event> inputBus = event -> {
        if (!(event instanceof EventInput e)) return;
        if (!isWalking || nullCheck()) return;
        e.setForward(1f);
        MoveUtility.silentCorrection(e, moveTargetYaw);
    };

    EventBus<Event> tickBus = event -> {
        if (!(event instanceof EventGameTick)) return;
        if (nullCheck()) return;
        if (AutoInvis.INSTANCE.isDrinking) return;
        if (state == State.WALK) { doWalk(); return; }

        if (state == State.LOOK || state == State.SMOOTH_LOOK || state == State.PRESS_USE
                || state == State.RELEASE_USE || state == State.WAIT_SCREEN) {
            switch (state) {
                case LOOK        -> doLook();
                case SMOOTH_LOOK -> doSmoothLook();
                case PRESS_USE   -> doPressUse();
                case RELEASE_USE -> doReleaseUse();
                case WAIT_SCREEN -> doWaitScreen();
            }
            return;
        }

        if (!stateTimer.reached((long) delay.get(), true)) return;
        switch (state) {
            case FIND_NEXT    -> doFindNext();
            case INSPECT      -> doInspect();
            case SELECT_TRADE -> doSelectTrade();
            case TAKE_RESULT  -> doTakeResult();
            case BUY          -> doBuy();
            case CLOSE        -> doClose();
        }
    };

    private void doFindNext() {
        if (phase == Phase.SCAN) {
            collectNearby();
            VillagerData next = null;
            for (VillagerData vd : allVillagers)
                if (!scanned.contains(vd.uuid)) { next = vd; break; }
            if (next == null) { finishScan(); return; }
        index = allVillagers.indexOf(next);
        walkInitialized = false;
        } else {
            List<VillagerData> valid = validList();
            if (valid.isEmpty()) { ChatUtility.send("§cAutoVillager: нет валидных"); setEnabled(false); return; }
            if (index >= valid.size()) index = 0;
            walkInitialized = false;
        }
        state = State.WALK;
    }

    private boolean walkInitialized = false;

    private void doWalk() {
        VillagerData vd = getTargetData();
        if (vd == null) { stopWalking(); state = State.CLOSE; return; }

        VillagerEntity villager = findEntity(vd);

        Vec3d dest;
        if (villager != null) {
            Vec3d vPos = villager.getEntityPos();
            Vec3d pPos = mc.player.getEntityPos();
            Vec3d dir  = pPos.subtract(vPos).normalize();
            dest = vPos.add(dir.multiply(DEST_DIST));
            Vec3d aim = vPos.add(0, villager.getHeight() * 0.65, 0);
            Angle angle = RotationUtility.calculate(aim);
            Client.ROTATION.rotate(SMOOTH, angle, 2, true,
                    MovementCorrection.LOOK, Priorities.NORMAL);
        } else {
            dest = new Vec3d(vd.x, vd.y, vd.z);
        }

        double dist = mc.player.getEntityPos().distanceTo(dest);
        if (dist <= STOP_DIST) { stopWalking(); state = State.LOOK; return; }
        walkTowards(dest, dist);
    }

    private void doLook() {
        stopWalking();
        VillagerEntity villager = findEntity(getTargetData());
        if (villager == null) { state = State.CLOSE; return; }
        if (mc.currentScreen != null) { mc.player.closeHandledScreen(); return; }
        Vec3d aim = villager.getEntityPos().add(0, villager.getHeight() * 0.65, 0);
        Vec3d eye  = mc.player.getEyePos();
        Vec3d diff = aim.subtract(eye);
        double h = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        targetLookYaw   = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90f;
        targetLookPitch = (float) -Math.toDegrees(Math.atan2(diff.y, h));
        lookTicks = 0;

        state = State.SMOOTH_LOOK;
    }

    private void doSmoothLook() {
        lookTicks++;

        VillagerEntity villager = findEntity(getTargetData());
        if (villager == null) { state = State.PRESS_USE; return; }

        Vec3d aim  = villager.getEntityPos().add(0, villager.getHeight() * 0.65, 0);
        Angle target = RotationUtility.calculate(aim);
        targetLookYaw   = target.getYaw();
        targetLookPitch = target.getPitch();

        Client.ROTATION.rotate(SMOOTH, target, 2, true, MovementCorrection.LOOK, Priorities.NORMAL);

        float curYaw   = mc.player.getYaw();
        float curPitch = mc.player.getPitch();
        float errYaw   = Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(targetLookYaw - curYaw));
        float errPitch = Math.abs(targetLookPitch - curPitch);

        if ((errYaw < 2.5f && errPitch < 2.5f) || lookTicks >= LOOK_TICKS_NEEDED) {
            state = State.PRESS_USE;
        }
    }

    private void doPressUse()   { mc.options.useKey.setPressed(true);  state = State.RELEASE_USE; }
    private void doReleaseUse() { mc.options.useKey.setPressed(false); waitTicks = 0; state = State.WAIT_SCREEN; }

    private void doWaitScreen() {
        waitTicks++;
        if (mc.player.currentScreenHandler instanceof MerchantScreenHandler) {
            waitTicks = 0;
            state = (phase == Phase.SCAN) ? State.INSPECT : State.BUY;
            return;
        }
        if (waitTicks >= 20) {
            waitTicks = 0;
            VillagerData vd = getTargetData();
            if (phase == Phase.SCAN && vd != null) scanned.add(vd.uuid);
            state = State.CLOSE;
        }
    }

    private void doInspect() {
        if (!(mc.player.currentScreenHandler instanceof MerchantScreenHandler handler)) { state = State.CLOSE; return; }
        VillagerData vd = getTargetData();
        if (vd != null) scanned.add(vd.uuid);

        Item want = getCurrencyItem();
        boolean valid = false;
        for (TradeOffer offer : handler.getRecipes()) {
            ItemStack r = offer.getSellItem();
            if (r.getItem() == want || (!buyItems.getItems().isEmpty() && isWanted(r.getItem()))) { valid = true; break; }
        }
        if (vd != null) vd.valid = valid;

        ChatUtility.send("§7AutoVillager: §f" + scanned.size() + "§7/§f" + allVillagers.size() + (valid ? " §a✓" : " §c✗"));
        saveVillagers();
        state = State.CLOSE;
    }

    private void doBuy() {
        if (!(mc.player.currentScreenHandler instanceof MerchantScreenHandler handler)) { state = State.CLOSE; return; }
        Item want = getCurrencyItem();

        for (int i = 0; i < handler.getRecipes().size(); i++) {
            TradeOffer offer = handler.getRecipes().get(i);
            if (offer.isDisabled()) continue;
            ItemStack result = offer.getSellItem();
            boolean matches = result.getItem() == want
                    || (!buyItems.getItems().isEmpty() && isWanted(result.getItem()));
            if (!matches) continue;
            ItemStack c1 = offer.getOriginalFirstBuyItem(), c2 = offer.getDisplayedSecondBuyItem();
            if (!hasEnough(c1.getItem(), c1.getCount())) continue;
            if (!c2.isEmpty() && !hasEnough(c2.getItem(), c2.getCount())) continue;
            selectedTradeSlot = i;
            state = State.SELECT_TRADE;
            return;
        }
        mc.player.closeHandledScreen();
        state = State.CLOSE;
    }

    private void doSelectTrade() {
        if (!(mc.player.currentScreenHandler instanceof MerchantScreenHandler handler)) { state = State.CLOSE; return; }
        if (selectedTradeSlot < 0) { state = State.CLOSE; return; }
        NetworkUtility.send(new SelectMerchantTradeC2SPacket(selectedTradeSlot));
        state = State.TAKE_RESULT;
    }

    private void doTakeResult() {
        if (!(mc.player.currentScreenHandler instanceof MerchantScreenHandler handler)) { state = State.CLOSE; return; }

        ItemStack output = handler.slots.get(2).getStack();
        if (output.isEmpty()) return;

        mc.interactionManager.clickSlot(handler.syncId, 2, 0, SlotActionType.QUICK_MOVE, mc.player);

        if (debug.get())
            ChatUtility.send("§aAutoVillager: куплено §f" + output.getName().getString() + " x" + output.getCount());
        mc.player.closeHandledScreen();
        selectedTradeSlot = -1;
        state = State.CLOSE;
    }

    private void doClose() {
        stopWalking();
        if (mc.currentScreen != null) { mc.player.closeHandledScreen(); return; }
        index++;
        state = State.FIND_NEXT;
    }
    private void walkTowards(Vec3d dest, double dist) {
        Vec3d diff = dest.subtract(mc.player.getEntityPos());
        moveTargetYaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90f;
        isWalking = true;
        mc.options.forwardKey.setPressed(true);
    }

    private void stopWalking() {
        isWalking = false;
        mc.options.forwardKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
        mc.player.setSprinting(false);
    }
    private void collectNearby() {
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof VillagerEntity v)) continue;
            if (mc.player.distanceTo(v) > SCAN_RANGE) continue;
            boolean known = allVillagers.stream().anyMatch(vd -> vd.uuid.equals(v.getUuid()));
            if (!known) allVillagers.add(new VillagerData(v.getUuid(), v.getX(), v.getY(), v.getZ(), false));
        }
    }

    private void finishScan() {
        long cnt = allVillagers.stream().filter(vd -> vd.valid).count();
        ChatUtility.send("§aScan done! Валидных: §f" + cnt + "§a из §f" + allVillagers.size());
        saveVillagers();
        if (cnt == 0) { ChatUtility.send("§cAutoVillager: нет жителей с нужным предметом"); setEnabled(false); return; }
        phase = Phase.BUY; index = 0; state = State.FIND_NEXT;
    }

    private List<VillagerData> validList() {
        List<VillagerData> list = new ArrayList<>();
        for (VillagerData vd : allVillagers) if (vd.valid) list.add(vd);
        return list;
    }

    private VillagerData getTargetData() {
        List<VillagerData> list = (phase == Phase.SCAN) ? allVillagers : validList();
        if (index < 0 || index >= list.size()) return null;
        return list.get(index);
    }

    private VillagerEntity findEntity(VillagerData vd) {
        if (vd == null) return null;
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof VillagerEntity v && v.getUuid().equals(vd.uuid)) {
                vd.x = v.getX(); vd.y = v.getY(); vd.z = v.getZ();
                return v;
            }
        }
        return null;
    }

    private boolean isWanted(Item item) { return buyItems.contains(Registries.ITEM.getId(item)); }

    private Item getCurrencyItem() {
        return Items.EMERALD;
    }

    private boolean hasEnough(Item item, int amount) {
        int total = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (!s.isEmpty() && s.getItem() == item) { total += s.getCount(); if (total >= amount) return true; }
        }
        return false;
    }

    private File getFile() { File d = Client.CLIENT_DIR.toFile(); d.mkdirs(); return new File(d, "villagers.json"); }

    private void saveVillagers() {
        try {
            JsonArray arr = new JsonArray();
            for (VillagerData vd : allVillagers) {
                JsonObject o = new JsonObject();
                o.addProperty("uuid",  vd.uuid.toString());
                o.addProperty("x",     vd.x);
                o.addProperty("y",     vd.y);
                o.addProperty("z",     vd.z);
                o.addProperty("valid", vd.valid);
                arr.add(o);
            }
            try (Writer w = new OutputStreamWriter(new FileOutputStream(getFile()), StandardCharsets.UTF_8)) {
                new Gson().toJson(arr, w);
            }
        } catch (Exception e) {
            if (debug.get()) ChatUtility.send("§cAutoVillager: ошибка сохранения: " + e.getMessage());
        }
    }

    private void loadVillagers() {
        allVillagers.clear(); scanned.clear();
        File f = getFile();
        if (!f.exists()) return;
        try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
            JsonArray arr = new Gson().fromJson(r, JsonArray.class);
            if (arr == null) return;
            for (var el : arr) {
                JsonObject o = el.getAsJsonObject();
                UUID uuid    = UUID.fromString(o.get("uuid").getAsString());
                double x     = o.get("x").getAsDouble();
                double y     = o.get("y").getAsDouble();
                double z     = o.get("z").getAsDouble();
                boolean valid = o.has("valid") && o.get("valid").getAsBoolean();
                allVillagers.add(new VillagerData(uuid, x, y, z, valid));
                if (valid) scanned.add(uuid);
            }
        } catch (Exception e) {
            if (debug.get()) ChatUtility.send("§cAutoVillager: ошибка загрузки: " + e.getMessage());
        }
    }
}
