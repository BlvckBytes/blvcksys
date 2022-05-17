package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/17/2022

  Represents a model that can be revoked, separately to being deleted,
  in order to still remain in history but not have any further effect.
*/
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
  @Setter
  @ModelProperty(isNullable = true)
  protected String revocationReason;

  /**
   * Set this model to be revoked by an executing player
   * @param revoker Player that executes the revocation
   * @param revocationReason Reason of the revocation, optional
   */
  public void setRevoked(OfflinePlayer revoker, @Nullable String revocationReason) {
    this.revoker = revoker;
    this.revocationReason = revocationReason;
    this.revokedAt = new Date();
  }

  /**
   * Check whether or not this model has been revoked already
   */
  public boolean isRevoked() {
    return revoker != null;
  }

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
