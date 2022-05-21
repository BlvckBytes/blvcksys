package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.MigrationDefault;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/06/2022

  A kit that corresponds a unique name to a set of items a
  player can request after the cooldown has expired.
*/
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KitModel extends ACooldownModel {

  public KitModel(String name, Inventory items, int cooldownSeconds, OfflinePlayer creator) {
    super(cooldownSeconds);

    this.name = name;
    this.items = items;
    this.creator = creator;
  }

  @ModelProperty(isUnique = true)
  private String name;

  @ModelProperty
  private Inventory items;

  @ModelProperty
  private OfflinePlayer creator;

  @ModelProperty(isNullable = true, migrationDefault = MigrationDefault.NULL)
  private ItemStack representitiveItem;

  /**
   * Get the number of items this kit contains
   */
  public int getNumItems() {
    int numItems = 0;

    for (ItemStack is : items.getContents()) {
      if (is != null && is.getType() != Material.AIR)
        numItems++;
    }

    return numItems;
  }
}
