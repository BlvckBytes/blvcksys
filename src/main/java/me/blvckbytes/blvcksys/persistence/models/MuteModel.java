package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.OfflinePlayer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/17/2022

  A mute which has been casted on a player and has a temporary duration
  will prevent the player from writing any messages to the chat within
  that time period.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MuteModel extends ARevokeableModel {

  @ModelProperty
  private OfflinePlayer creator;

  @ModelProperty
  private OfflinePlayer target;

  // Duration of this mute in seconds
  @ModelProperty
  private Integer durationSeconds;

  // Null means that the creator didn't provide a reason
  @ModelProperty(isNullable = true)
  private String reason;

  /**
   * Get whether this mute is currently active
   */
  public boolean isActive() {
    return revokedAt == null &&
      ((createdAt.getTime() / 1000) + durationSeconds) - (System.currentTimeMillis() / 1000) > 0
    ;
  }
}
