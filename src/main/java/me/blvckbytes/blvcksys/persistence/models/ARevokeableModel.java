package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.OfflinePlayer;

import java.util.Date;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/17/2022

  Represents a model that can be revoked, separately to being deleted,
  in order to still remain in history but not have any further effect.
*/
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ARevokeableModel extends APersistentModel {

  // The player that revoked this model, null means that the ban
  // hasn't yet been revoked
  @ModelProperty(isNullable = true)
  protected OfflinePlayer revoker;

  // The date of revocation, null means that the model hasn't yet been revoked
  @ModelProperty(isNullable = true)
  protected Date revokedAt;

  // The reason of revocation, null means that the model hasn't yet been revoked
  @ModelProperty(isNullable = true)
  protected String revocationReason;

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
}
