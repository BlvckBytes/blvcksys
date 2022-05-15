package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.OfflinePlayer;

import java.util.Date;

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
public class BanModel extends APersistentModel {

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

  // The player that revoked this ban prematurely, null means that the ban
  // hasn't yet been revoked
  @ModelProperty(isNullable = true)
  private OfflinePlayer revoker;

  // The date of revocation, null means that the ban hasn't yet been revoked
  @ModelProperty(isNullable = true)
  private Date revokedAt;

  // The reason of revocation, null means that the ban hasn't yet been revoked
  @ModelProperty(isNullable = true)
  private String revocationReason;

  /**
   * Get the revokedAt timestamp as a human readable string
   */
  public String getRevokedAtStr() {
    return getRevokedAtStr(false);
  }

  /**
   * Get the revokedAt timestamp as a human readable string
   * @param shortFormat Whether to display only the date without the time
   */
  public String getRevokedAtStr(boolean shortFormat) {
    return revokedAt == null ? "/" : (shortFormat ? dfShort : df).format(revokedAt);
  }

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
