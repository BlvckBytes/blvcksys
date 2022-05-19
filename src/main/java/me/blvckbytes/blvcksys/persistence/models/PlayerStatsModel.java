package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.MigrationDefault;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.OfflinePlayer;

import java.util.Date;

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

  @ModelProperty(isNullable = true, migrationDefault = MigrationDefault.NULL)
  private Date lastLogin;

  /**
   * Get the last login timestamp as a human readable string
   */
  public String getLastLoginStr() {
    return getLastLoginStr(false);
  }

  /**
   * Get the last login timestamp as a human readable string
   * @param shortFormat Whether to display only the date without the time
   */
  public String getLastLoginStr(boolean shortFormat) {
    return lastLogin == null ? "/" : (shortFormat ? dfShort : df).format(lastLogin);
  }

  public static PlayerStatsModel createDefault(OfflinePlayer owner) {
    return new PlayerStatsModel(owner, 0, 0, 0, null);
  }
}
