package me.blvckbytes.blvcksys.handlers;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/09/2022

  Specifies all available variables with their template representation
  that can be used within hologram lines.
*/
@Getter
@AllArgsConstructor
public enum HologramVariable {

  // Player specific
  PLAYER_NAME("{player_name}", Long.MAX_VALUE),
  PLAYER_EXPERIENCE("{player_experience}", 20 * 5),
  WORLD_NAME("{world_name}", Long.MAX_VALUE),

  // Date and time
  CURRENT_TIME("{current_time}", 20),
  CURRENT_DATE("{current_date}", 20 * 10),
  CURRENT_DAY("{current_day}", 20 * 10),
  ;

  private final String placeholder;
  private final long updatePeriodTicks;

  /**
   * Find a hologram variable by it's placeholder
   * @param placeholder Placeholder to look up
   * @return HologramVariable on success, null if the placeholder is unknown
   */
  public static HologramVariable fromPlaceholder(String placeholder) {
    for (HologramVariable var : HologramVariable.values()) {
      if (var.placeholder.equalsIgnoreCase(placeholder))
        return var;
    }
    return null;
  }
}
