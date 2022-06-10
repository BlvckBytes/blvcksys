package me.blvckbytes.blvcksys.handlers.gui;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import org.bukkit.Material;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/09/2022

  Lists all available auction categories as well as their representitive
  icons and displaynames.
*/
@AllArgsConstructor
public enum AuctionCategory {
  COMBAT(Material.DIAMOND_SWORD, ConfigKey.GUI_AH_CAT_COMBAT_NAME),
  ARMOR(Material.IRON_CHESTPLATE, ConfigKey.GUI_AH_CAT_ARMOR_NAME),
  TOOLS(Material.GOLDEN_AXE, ConfigKey.GUI_AH_CAT_TOOLS_NAME),
  BUILDING(Material.OAK_LOG, ConfigKey.GUI_AH_CAT_BUILDING_NAME),
  MISC(Material.DEAD_BUSH, ConfigKey.GUI_AH_CAT_MISC_NAME),
  ALL(Material.CHEST_MINECART, ConfigKey.GUI_AH_CAT_ALL_NAME)
  ;

  @Getter
  private final Material mat;

  private final ConfigKey name;

  /**
   * Get the display name of this catecory
   * @param cfg Config ref
   * @return Config value containing the name
   */
  public ConfigValue getName(IConfig cfg) {
    return cfg.get(name);
  }
}

