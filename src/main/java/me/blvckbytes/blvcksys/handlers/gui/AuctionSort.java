package me.blvckbytes.blvcksys.handlers.gui;

import lombok.AllArgsConstructor;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/09/2022

  Lists all available auction sorting types as well as their displaynames.
*/
@AllArgsConstructor
public enum AuctionSort {
  NEWEST(ConfigKey.GUI_AH_SORT_NEWEST),
  OLDEST(ConfigKey.GUI_AH_SORT_OLDEST),
  HIGHEST_BID(ConfigKey.GUI_AH_SORT_HIGHEST_BID),
  LOWEST_BID(ConfigKey.GUI_AH_SORT_LOWEST_BID),
  MOST_BIDS(ConfigKey.GUI_AH_SORT_MOST_BIDS),
  LEAST_BIDS(ConfigKey.GUI_AH_SORT_LEAST_BIDS),
  INSTANT_BUY(ConfigKey.GUI_AH_SORT_INSTANT_BUY)
  ;

  private final ConfigKey name;

  /**
   * Get the display name of this sort type
   * @param cfg Config ref
   * @return Config value containing the name
   */
  public ConfigValue getName(IConfig cfg) {
    return cfg.get(name);
  }
}
