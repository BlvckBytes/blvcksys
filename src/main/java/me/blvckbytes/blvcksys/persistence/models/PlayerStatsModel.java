package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.OfflinePlayer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/12/2022

  Stores all statistics a player has on this server.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerStatsModel extends APersistentModel {

  @ModelProperty
  private OfflinePlayer owner;

  @ModelProperty
  private int kills;

  @ModelProperty
  private int deaths;

  @ModelProperty
  private int money;

  public static PlayerStatsModel createDefault(OfflinePlayer owner) {
    return new PlayerStatsModel(owner, 0, 0, 0);
  }
}
