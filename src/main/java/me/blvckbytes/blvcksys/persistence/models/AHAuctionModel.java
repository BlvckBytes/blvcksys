package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.handlers.gui.AuctionCategory;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/10/2022

  Stores a single auction, which is made up of the item to sell as well
  as all the auction's parameters. Auctions are deleted as soon as the buyer
  has taken the item out and the seller has retrieved the money.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AHAuctionModel extends APersistentModel {

  @ModelProperty
  private OfflinePlayer creator;

  @ModelProperty
  private ItemStack item;

  // This field is null for instant buy auctions
  @ModelProperty(isNullable = true)
  private Integer durationSeconds;

  @ModelProperty
  private int startBid;

  @ModelProperty
  private AuctionCategory category;

  // The player (acting as an AH moderator) which
  // cancelled this auction prematurely
  @ModelProperty(isNullable = true)
  private OfflinePlayer canceller;

  // Whether the item has been sold, items which expire before any
  // bids are not sold, even though they're inactive
  @ModelProperty
  private boolean sold;

  /**
   * Checks whether the auction is still active (can be bidden on)
   */
  public boolean isActive() {
    return !sold && System.currentTimeMillis() < createdAt.getTime() + durationSeconds * 1000 && canceller == null;
  }
}
