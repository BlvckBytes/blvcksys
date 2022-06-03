package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/03/2022

  A fake armor stand that is spawned for clients through packets only.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ArmorStandModel extends APersistentModel {

  @ModelProperty
  private OfflinePlayer creator;

  @ModelProperty(isUnique = true)
  private String name;

  @ModelProperty
  private Location loc;

  @ModelProperty(isNullable = true)
  private String displayName;

  @ModelProperty
  private boolean visible;

  @ModelProperty
  private boolean nameVisible;

  @ModelProperty
  private boolean small;

  @ModelProperty
  private boolean arms;

  @ModelProperty
  private boolean basePlate;

  @ModelProperty(isNullable = true)
  private ItemStack helmet, chestplate, leggings, boots;

  @ModelProperty(isNullable = true)
  private ItemStack hand, offHand;

  @ModelProperty(isNullable = true)
  private EulerAngle headPose, bodyPose, leftArmPose, rightArmPose, leftLegPose, rightLegPose;

  public static ArmorStandModel createDefault(OfflinePlayer creator, String name, Location loc) {
    return new ArmorStandModel(
      creator, name, loc,
      null, true, false, false, false, false,
      null, null, null, null, null, null,
      null, null, null, null, null, null
    );
  }
}
