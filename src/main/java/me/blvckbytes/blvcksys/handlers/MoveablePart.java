package me.blvckbytes.blvcksys.handlers;

import lombok.AllArgsConstructor;
import me.blvckbytes.blvcksys.packets.communicators.armorstand.ArmorStandProperties;
import org.bukkit.util.EulerAngle;

import java.util.function.BiConsumer;
import java.util.function.Function;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/03/2022

  Specifies all available movable parts an armor stand has to offer.
*/
@AllArgsConstructor
public enum MoveablePart {
  HEAD(ArmorStandProperties::getHeadPose, ArmorStandProperties::setHeadPose),
  BODY(ArmorStandProperties::getBodyPose, ArmorStandProperties::setBodyPose),
  LEFT_ARM(ArmorStandProperties::getLeftArmPose, ArmorStandProperties::setLeftArmPose),
  RIGHT_ARM(ArmorStandProperties::getRightArmPose, ArmorStandProperties::setRightArmPose),
  LEFT_LEG(ArmorStandProperties::getLeftLegPose, ArmorStandProperties::setLeftLegPose),
  RIGHT_LEG(ArmorStandProperties::getRightLegPose, ArmorStandProperties::setRightLegPose),
  ;

  private final Function<ArmorStandProperties, EulerAngle> getter;
  private final BiConsumer<ArmorStandProperties, EulerAngle> setter;

  /**
   * Sets the property within a property wrapper
   * @param props Property wrapper
   * @param angle Value to set
   */
  public void set(ArmorStandProperties props, EulerAngle angle) {
    setter.accept(props, angle);
  }

  /**
   * Gets the property from a property wrapper
   * @param props Property wrapper
   * @return Current value
   */
  public EulerAngle get(ArmorStandProperties props) {
    EulerAngle angle = getter.apply(props);
    return angle == null ? new EulerAngle(0, 0, 0) : angle;
  }
}
