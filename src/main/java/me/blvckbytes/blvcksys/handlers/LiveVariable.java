package me.blvckbytes.blvcksys.handlers;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/09/2022

  Specifies all available variables with their template representation.
*/
@Getter
@AllArgsConstructor
public enum LiveVariable {

  // Player specific
  PLAYER_NAME("{player_name}", Long.MAX_VALUE),
  PLAYER_EXPERIENCE("{player_experience}", 20 * 3),
  WORLD_NAME("{world_name}", Long.MAX_VALUE),
  PLAYER_KILLS("{player_kills}", 20 * 3),
  PLAYER_DEATHS("{player_deaths}", 20 * 3),
  PLAYER_KD("{player_kd}", 20 * 3),
  PLAYER_MONEY("{player_money}", 20 * 3),
  PLAYER_PLAYTIME("{player_playtime}", 20 * 10),

  // Date and time
  CURRENT_TIME("{current_time}", 20),
  CURRENT_DATE("{current_date}", 20 * 10),
  CURRENT_DAY("{current_day}", 20 * 10),

  // Globals
  TOP_KILLS_VALUE_1("{top_kills_value_1}", 20 * 3),
  TOP_KILLS_VALUE_2("{top_kills_value_2}", 20 * 3),
  TOP_KILLS_VALUE_3("{top_kills_value_3}", 20 * 3),
  TOP_KILLS_VALUE_4("{top_kills_value_4}", 20 * 3),
  TOP_KILLS_VALUE_5("{top_kills_value_5}", 20 * 3),
  TOP_KILLS_PLAYER_1("{top_kills_player_1}", 20 * 3),
  TOP_KILLS_PLAYER_2("{top_kills_player_2}", 20 * 3),
  TOP_KILLS_PLAYER_3("{top_kills_player_3}", 20 * 3),
  TOP_KILLS_PLAYER_4("{top_kills_player_4}", 20 * 3),
  TOP_KILLS_PLAYER_5("{top_kills_player_5}", 20 * 3),
  ;

  private final String placeholder;
  private final long updatePeriodTicks;

  /**
   * Find a live variable by it's placeholder
   * @param placeholder Placeholder to look up
   * @return LiveVariable on success, null if the placeholder is unknown
   */
  public static LiveVariable fromPlaceholder(String placeholder) {
    for (LiveVariable var : LiveVariable.values()) {
      if (var.placeholder.equalsIgnoreCase(placeholder))
        return var;
    }
    return null;
  }
}
