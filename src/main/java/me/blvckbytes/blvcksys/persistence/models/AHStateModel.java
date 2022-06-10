package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.handlers.gui.AuctionCategory;
import me.blvckbytes.blvcksys.handlers.gui.AuctionSort;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.OfflinePlayer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/10/2022

  Stores the state of the main auction house screen for every player.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AHStateModel extends APersistentModel {

  @ModelProperty
  private OfflinePlayer owner;

  @ModelProperty
  private AuctionCategory category;

  @ModelProperty
  private AuctionSort sort;

  @ModelProperty(isNullable = true)
  private String search;

  public static AHStateModel makeDefault(OfflinePlayer owner) {
    return new AHStateModel(owner, AuctionCategory.ALL, AuctionSort.NEWEST, null);
  }
}
