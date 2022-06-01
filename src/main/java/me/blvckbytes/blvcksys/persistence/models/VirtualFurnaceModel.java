package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/01/2022

  Stores a snapshot of a player's virtual-furnace's current state.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VirtualFurnaceModel extends APersistentModel {

  @ModelProperty(isUnique = true)
  private OfflinePlayer owner;

  @ModelProperty(isUnique = true)
  private int index;

  @ModelProperty(isNullable = true)
  private ItemStack smelting, smelted, powerSource;

  @ModelProperty
  private int remainingBurningTime;

  @ModelProperty
  private int elapsedSmeltingTime;
}
