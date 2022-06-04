package me.blvckbytes.blvcksys.packets.communicators.armorstand;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/03/2022

  Represents all customizable properties of an armor stand created by packets.
*/
@Getter
@Setter
@AllArgsConstructor
public class ArmorStandProperties implements Cloneable {
  // Whether the name is visible
  private boolean nameVisible;

  // Whether the armor stand is visible
  private boolean visible;

  // Whether the location should be shifted up to the name internally
  private boolean shifted;

  // Whether the armor stand has arms
  private boolean arms;

  // Whether the armor stand is small
  private boolean small;

  // Whether the armor stand has a base plate
  private boolean baseplate;

  // Displayname of the armor stand
  private @Nullable String name;

  // Armor of the armor stand
  private @Nullable ItemStack helmet, chestplate, leggings, boots;

  // Item in the armor stand's hands
  private @Nullable ItemStack hand, offHand;

  // Poses of all body parts
  private @Nullable EulerAngle headPose, bodyPose, leftArmPose, rightArmPose, leftLegPose, rightLegPose;

  /**
   * Create a new hologram armor stand by it's displayed text
   * @param text Text to display
   */
  public ArmorStandProperties(String text) {
    this.name = text;
    this.nameVisible = true;
    this.shifted = true;
  }

  @NotNull
  public ArmorStandProperties clone() {
    try {
      return (ArmorStandProperties) super.clone();
    } catch (CloneNotSupportedException var2) {
      throw new Error(var2);
    }
  }
}
