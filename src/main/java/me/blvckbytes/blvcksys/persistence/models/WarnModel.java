package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import me.blvckbytes.blvcksys.persistence.RowNumber;
import org.bukkit.OfflinePlayer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/17/2022

  A warn which has been casted on a player and has either a temporary
  or a permanent duration. Warns can be revoked to make them expire prematurely.
  If a player collects enough active warns, they're banned automatically.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WarnModel extends ARevokeableModel {

  @ModelProperty
  private OfflinePlayer creator;

  @ModelProperty
  private OfflinePlayer target;

  // Duration of this warn in seconds, null means permanent
  @ModelProperty(isNullable = true)
  private Integer durationSeconds;

  // Null means that the creator didn't provide a reason
  @ModelProperty(isNullable = true)
  private String reason;

  @RowNumber(partitionedBy = "target__uuid")
  private int number;

  /**
   * Get whether this warn is currently active
   */
  public boolean isActive() {
    return revokedAt == null && (
      durationSeconds == null ||
      ((createdAt.getTime() / 1000) + durationSeconds) - (System.currentTimeMillis() / 1000) > 0
    );
  }
}
