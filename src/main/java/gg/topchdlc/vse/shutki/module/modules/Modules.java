package gg.topchdlc.vse.shutki.module.modules;

import gg.topchdlc.vse.shutki.module.modules.impl.combat.*;
import gg.topchdlc.vse.shutki.module.modules.impl.farm.*;
import gg.topchdlc.vse.shutki.module.modules.impl.helper.*;
import gg.topchdlc.vse.shutki.module.modules.impl.misc.*;
import gg.topchdlc.vse.shutki.module.modules.impl.movement.*;
import gg.topchdlc.vse.shutki.module.modules.impl.movement.Timer;
import gg.topchdlc.vse.shutki.module.modules.impl.player.*;
import gg.topchdlc.vse.shutki.module.modules.impl.render.*;
import gg.topchdlc.vse.shutki.module.modules.impl.render.FriendMarker;
import gg.topchdlc.vse.shutki.module.modules.impl.render.TargetESP;
import gg.topchdlc.vse.shutki.module.modules.impl.render.worldp.FireFly;
import gg.topchdlc.vse.shutki.module.modules.impl.render.worldp.Particles;
import gg.topchdlc.vse.shutki.module.modules.impl.render.worldp.Shatter;
import gg.topchdlc.vse.shutki.module.settings.Setting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.utils.other.LogUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import lombok.Getter;
import gg.topchdlc.Client;
import gg.topchdlc.api.events.EventBus;
import gg.topchdlc.api.events.list.EventKey;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

import static gg.topchdlc.vse.shutki.module.modules.Module.mc;


public final class Modules {
    private final Map<Class<? extends Module>, Module> byClass = new Object2ObjectLinkedOpenHashMap<>();
    @Getter
    private final List<Module> modules = new ArrayList<>();

    public Modules() {
        add(
                // COMBAT
                ProjectileHelper.INSTANCE,
                AimAssist.INSTANCE,
                TriggerBot.INSTANCE,
                Criticals.INSTANCE,
                AuraModule.INSTANCE,
                ElytraAura.INSTANCE,
                CrystalAura.INSTANCE,
                BowSpam.INSTANCE,
                VelocityModule.INSTANCE,
                AutoTotem.INSTANCE,
                ElytraTarget.INSTANCE,
                AutoPotion.INSTANCE,
                Hitbox.INSTANCE,
                CrystalOptimizer.INSTANCE,
                CrystalAuto.INSTANCE,
                AutoMace.INSTANCE,
                NoDelay.INSTANCE,

                // PLAYER
                AutoInvis.INSTANCE,
                Sprint.INSTANCE,
                AutoSwap.INSTANCE,
                AirStuck.INSTANCE,
                ElytraHelper.INSTANCE,
                ClientSounds.INSTANCE,
                DeathCoords.INSTANCE,
                GuiWalk.INSTANCE,
                Blink.INSTANCE,
                AutoAccept.INSTANCE,
                NoInteract.INSTANCE,
                AutoLeave.INSTANCE,
                NoEntityTrace.INSTANCE,
                ChestParser.INSTANCE,
                Nuker.INSTANCE,
                AutoEat.INSTANCE,
                AHHelper.INSTANCE,
                CameraCustomer.INSTANCE,
                HitSound.INSTANCE,

                // RENDER
                Interface.INSTANCE,
                Arrows.INSTANCE,
                Prediction.INSTANCE,
                HandShaderModule.INSTANCE,
                ViewModel.INSTANCE,
                FriendMarker.INSTANCE,
                HitAnimation.INSTANCE,
                Removals.INSTANCE,
                CustomWorld.INSTANCE,
                ShaderFog.INSTANCE,
                SwingAnimations.INSTANCE,
                Shatter.INSTANCE,
                HitMarker.INSTANCE,
                //Pet.INSTANCE,
                FreeLook.INSTANCE,
                ItemTrail.INSTANCE,
                FireFly.INSTANCE,
                Particles.INSTANCE,
                ESP.INSTANCE,
                SeeInvisible.INSTANCE,
                WardenHelper.INSTANCE,
                DynamicIsland.INSTANCE,
                ItemReplacer.INSTANCE,
                Box3D.INSTANCE,
                BlockHighlightModule.INSTANCE,
                BlockESP.INSTANCE,
                FullBright.INSTANCE,
                FreeCam.INSTANCE,
                BetterMinecraft.INSTANCE,
                ItemEsp.INSTANCE,
                ProjectilesNameTag.INSTANCE,
                PopEffect.INSTANCE,
                Cosmetic.INSTANCE,
                Optimization.INSTANCE,
                TargetESP.INSTANCE,


                //FARM
                ChestStealer.INSTANCE,
                AutoBuy.INSTANCE,
                AutoApple.INSTANCE,
                PotionCombo.INSTANCE,
                AutoVillager.INSTANCE,
                AutoSosiski.INSTANCE,
                AutoSwords.INSTANCE,
                AutoSardelki.INSTANCE,
                AutoMine.INSTANCE,
                AutoWarden.INSTANCE,

                // MISC
                AutoJoin.INSTANCE,
                ItemScroller.INSTANCE,
                ClickPearl.INSTANCE,
                AssistModule.INSTANCE,
                Nottification.INSTANCE,
                AutoTool.INSTANCE,
                WebTrap.INSTANCE,
                FakePlayer.INSTANCE,
                NameProtect.INSTANCE,
                RadialMenu.INSTANCE,
                MiddleClick.INSTANCE,


                // MOVEMENT
                AntiWeb.INSTANCE,
                ElytraGlide.INSTANCE,
                LevitationControl.INSTANCE,
                Speed.INSTANCE,
                ElytraBhop.INSTANCE,
                Jesus.INSTANCE,
                NoSlowDown.INSTANCE,
                Strafe.INSTANCE,
                Spider.INSTANCE,
                SuperFirework.INSTANCE,
                Timer.INSTANCE,
                AntiPush.INSTANCE,
                WaterSpeed.INSTANCE,
                Flight.INSTANCE,
                NoFall.INSTANCE

        );

        Client.EVENTS.register(this);
        printLang();
    }

    private void printLang() {
        try (var fos = new FileOutputStream("lang.conf")) {
            PrintStream ps = new PrintStream(fos);
            for (var m : modules) {
                printLangSetting(m, ps);
            }
            ps.flush();
        } catch (Exception e) {
            LogUtility.error(e, "printLang");
        }
    }

    private void printLangSetting(Setting<?> setting, PrintStream ps) {
        String name = setting.name.contains("(") ? "\"" + setting.name + "\"" : setting.name;
        if (setting instanceof Group g && !g.isEmpty()) {
            ps.println(name + "{");
            ps.println("_description = \"" + g._desc + '"');
            for (var child : g.getSettings()) {
                if (g.enabled != null && child == g.enabled) continue;
                printLangSetting(child, ps);
            }
            ps.println("}");
        } else {
            ps.println(name + "._description = \"" + setting._desc + '"');
        }
    }

    public void add(Module... modules) {
        for (Module m : modules) {
            addModule(m);
        }
        sync();
    }

    private void addModule(Module module) {
        byClass.put(module.getClass(), module);
        modules.add(module);
    }

    public void unregister(Module... modules) {
        for (Module m : modules) {
            unregisterModule(m);
        }
        sync();
    }

    private void unregisterModule(Module module) {
        byClass.remove(module.getClass());
        modules.remove(module);
    }

    private void sync() {
        modules.sort(Comparator.comparing(Module::getName));
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(final String name) {
        for (Module m : modules) {
            if (m.getName().equalsIgnoreCase(name)) {
                return (T) m;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(final Class<T> clazz) {
        return (T) byClass.get(clazz);
    }

    public List<Module> get(final Category category) {
        return this.modules.stream()
                .filter(module -> module.getCategory() == category)
                .collect(Collectors.toList());
    }

    EventBus<EventKey> onKey = event -> {
        if (mc.currentScreen != null || Client.GLASS_GUI.WRITING) return;
        this.getModules().forEach(module -> {
            if (module.getKey() != -1 && module.getKey() == event.getKey()) {
                if (module.getBindType() == Module.BindType.PRESS && event.getAction() == 1) {
                    module.toggle();
                } else if (module.getBindType() == Module.BindType.HOLD) {
                    if (event.getAction() == 1) {
                        module.setEnabled(true);
                    } else if (event.getAction() == 0) {
                        module.setEnabled(false);
                    }
                }
            }
        });
    };
}
