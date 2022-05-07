package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  A warping-point that corresponds a unique name to a
  specific point within a specific world.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WarpModel extends APersistentModel {

  @ModelProperty(isUnique = true)
  private String name;

  @ModelProperty
  private Location loc;

  @ModelProperty
  private OfflinePlayer creator;
}
