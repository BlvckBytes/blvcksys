package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ForeignKeyAction;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/10/2022

  Represents the bid of a user on an auction.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AHBidModel extends APersistentModel {

  @ModelProperty
  private OfflinePlayer creator;

  @ModelProperty(
    foreignKey = AHAuctionModel.class,
    foreignChanges = ForeignKeyAction.DELETE_CASCADE
  )
  private UUID auctionId;

  @ModelProperty
  private int amount;

  // Whether the player has retrieved this bid
  // again after loosing on an auction
  @ModelProperty
  private boolean retrieved;

  public static AHBidModel makeDefault(OfflinePlayer creator, AHAuctionModel auction, int amount) {
    return new AHBidModel(creator, auction.getId(), amount, false);
  }
}
