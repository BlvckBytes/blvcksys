package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.MigrationDefault;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/24/2022

  Saves the enderchest of a player which consists of multiple
  pages, each represented by an inventory.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EnderchestModel extends APersistentModel {

  // How many rows to have per page
  public static final int PAGE_ROWS = 5;
  public static final int NUM_PAGES = 3;
  public static final int DEFAULT_MAX_SLOTS = 18;

  @ModelProperty
  private OfflinePlayer owner;

  @ModelProperty
  private Inventory page1, page2, page3;

  @ModelProperty(migrationDefault = MigrationDefault.ZERO)
  private int lastMaxSlots;

  /**
   * Create a new empty enderchest with empty inventories
   * @param owner Enderchest owner
   */
  public static EnderchestModel createEmpty(OfflinePlayer owner) {
    return new EnderchestModel(owner, createEmptyPage(), createEmptyPage(), createEmptyPage(), DEFAULT_MAX_SLOTS);
  }

  /**
   * Create a new empty enderchest page inventory
   */
  private static Inventory createEmptyPage() {
    return Bukkit.createInventory(null, 9 * PAGE_ROWS);
  }
}
