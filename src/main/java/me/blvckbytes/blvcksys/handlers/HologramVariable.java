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
  PLAYER_NAME("{player_name}"),
  PLAYER_EXPERIENCE("{player_experience}"),
  WORLD_NAME("{world_name}"),

  // Date and time
  CURRENT_TIME("{current_time}"),
  CURRENT_DATE("{current_date}"),
  CURRENT_DAY("{current_day}"),
  ;

  private final String placeholder;

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
