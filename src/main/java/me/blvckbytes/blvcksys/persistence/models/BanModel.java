package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.OfflinePlayer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/13/2022

  A ban which has been casted on a player and has either a temporary
  or a permanent duration and may include an IP address. Bans can be
  revoked to make them expire prematurely.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BanModel extends ARevokeableModel {

  @ModelProperty
  private OfflinePlayer creator;

  @ModelProperty
  private OfflinePlayer target;

  // Duration of this ban in seconds, null means permanent
  @ModelProperty(isNullable = true)
  private Integer durationSeconds;

  // IP of the target at the time of being banned, null means that just the
  // player has been banned, but not their address
  @ModelProperty(isNullable = true)
  private String ipAddress;

  // Null means that the creator didn't provide a reason
  @ModelProperty(isNullable = true)
  private String reason;

  /**
   * Get whether this ban is currently active
   */
  public boolean isActive() {
    return revokedAt == null && (
      durationSeconds == null ||
      ((createdAt.getTime() / 1000) + durationSeconds) - (System.currentTimeMillis() / 1000) > 0
    );
  }
}
