package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ForeignKeyAction;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  Saves a single item of a crate and corresponds that to
  it's name as well as a probability.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CrateItemModel extends ASequencedModel {

  @ModelProperty(
    foreignKey = CrateModel.class,
    foreignChanges = ForeignKeyAction.DELETE_CASCADE
  )
  private UUID crateId;

  @ModelProperty
  private OfflinePlayer creator;

  @ModelProperty
  private ItemStack item;

  @ModelProperty
  private double probability;

}
