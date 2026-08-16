package gg.topchdlc.vse.utils.client.client;

import gg.topchdlc.Client;
import gg.topchdlc.vse.shutki.module.settings.impl.checkbox.CheckBox;
import gg.topchdlc.vse.shutki.module.settings.impl.enumsetting.EnumSetting;
import gg.topchdlc.vse.shutki.module.settings.impl.group.Group;
import gg.topchdlc.vse.shutki.module.settings.impl.multienum.MultiEnumSetting;
import gg.topchdlc.vse.utils.client.targets.ITargetSettings;

import gg.topchdlc.vse.utils.client.targets.RwAntiBotUtility;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;

import java.awt.*;

public class TargetSettings extends Group implements ITargetSettings {
    public TargetSettings() {
        super("Target Settings");
    }
    public static final TargetSettings INSTANCE = new TargetSettings();
    private final MultiEnumSetting<Target> target = multiEnumSetting("Targets", Target.Players,Target.Invisible);
  //  private final MultiEnumSetting<Target> visual = multiEnumSetting("Render", Target.Players, Target.Naked, Target.Friends, Target.Invisible);
    private final CheckBox rwAntiBot = checkbox("Rw AntiBot", false);

    public enum healthmode{Entity,Scoreboard}
    public final EnumSetting<healthmode> HealthMode  = enumSetting("Health Mode", healthmode.Entity);
   // private final Group teams = group("Teams").toggleable(false);
   // @AllArgsConstructor @SuppressWarnings("unused")
  //  private enum ArmorPart { Helmet(EquipmentSlot.HEAD), Chestplate(EquipmentSlot.CHEST), Leggings(EquipmentSlot.LEGS), Boots(EquipmentSlot.FEET); final EquipmentSlot slot; }
   // private final MultiEnumSetting<ArmorPart> teamsArmorParts = teams.multiEnumSetting("Check armor", ArmorPart.Helmet);
    public final CheckBox focus = checkbox("Focus", true);

    @Override
    public boolean isValid(LivingEntity entity) {
        return isValid(entity, target);
    }

  //  public boolean isValidVisual(LivingEntity entity) {
  //      return isValid(entity, visual);
  // }

    private boolean isValid(LivingEntity entity, MultiEnumSetting<Target> targets) {
        if (!entity.isAlive() || entity.getHealth() <= 0) return false;
        if (entity.equals(mc.player)) return false;
        if (entity instanceof ArmorStandEntity) return false;

        if (entity instanceof PlayerEntity playerEntity) {
            if (!targets.get(Target.Players)) return false;
            if (Client.FRIENDS.isFriend(playerEntity) && !targets.get(Target.Friends)) return false;
            if (rwAntiBot.get() && RwAntiBotUtility.isRwBot(playerEntity)) return false;

            if ((playerEntity.isInvisible() || playerEntity.hasStatusEffect(StatusEffects.INVISIBILITY)) && !targets.get(Target.Invisible)) return false;
            if (!hasAnyArmor(playerEntity) && !targets.get(Target.Naked)) return false;
          //  if (targets != visual && teams.isEnabled() && !teamsArmorParts.isEmpty()) {
             //   for (ArmorPart part : teamsArmorParts.get()) {
                //    ItemStack me = mc.player.getEquippedStack(part.slot);
               //     ItemStack target = playerEntity.getEquippedStack(part.slot);
                //Integer meColor = TargetsUtility.getArmorColor(me);
             //       Integer targetColor = TargetsUtility.getArmorColor(target);

                  //  if (meColor != null && targetColor != null && meColor.intValue() == targetColor.intValue()) return false;
            //    }
         //   }

            return true;
        } else if (entity instanceof LivingEntity) {
            return targets.get(Target.Mobs);
        }

        return targets.get(Target.Other);
    }

    public Color getColor(LivingEntity entity) {
        //if (teams.isEnabled() && !teamsArmorParts.isEmpty()) {
         //   for (ArmorPart part : teamsArmorParts.get()) {
         //       ItemStack target = entity.getEquippedStack(part.slot);
          //      Integer targetColor = TargetsUtility.getArmorColor(target);
         //       if (targetColor != null) return new Color(targetColor);
         //   }
     //   }
        if (entity instanceof PlayerEntity player && Client.FRIENDS.isFriend(player)) {
            return ClientColors.FRIEND_COLOR;
        }
        return null;
    }

    private static boolean hasAnyArmor(LivingEntity entity) {
        if (entity == null || mc.player == null) return false;

        return !entity.getEquippedStack(EquipmentSlot.HEAD).isEmpty() ||
                !entity.getEquippedStack(EquipmentSlot.CHEST).isEmpty() ||
                !entity.getEquippedStack(EquipmentSlot.LEGS).isEmpty() ||
                !entity.getEquippedStack(EquipmentSlot.FEET).isEmpty();
    }

    public enum Target {
        Players,
        Invisible,
        Friends,
        Naked,
        Mobs,
        Other
    }
}