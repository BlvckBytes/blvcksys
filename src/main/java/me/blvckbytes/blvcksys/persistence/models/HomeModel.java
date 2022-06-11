package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/16/2022

  A home point which can be set at any location and will represent
  that point in a world by a custom name the player gets to choose.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HomeModel extends APersistentModel {

  @ModelProperty(isUnique = true)
  private OfflinePlayer creator;

  @ModelProperty(isUnique = true)
  private String name;

  @ModelProperty
  private Location loc;

  @ModelProperty
  private Material icon;

  @ModelProperty
  private ChatColor color;

  public static HomeModel createDefault(OfflinePlayer creator, String name, Location loc) {
    return new HomeModel(creator, name, loc, Material.GRASS_BLOCK, ChatColor.GOLD);
  }
}
