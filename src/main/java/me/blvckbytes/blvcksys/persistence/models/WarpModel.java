package me.blvckbytes.blvcksys.persistence.models;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  A warping-point that corresponds a unique name to a
  specific point within a specific world.
*/
@RequiredArgsConstructor
public class WarpModel extends APersistentModel {

  @Getter
  @Setter
  @NonNull
  @ModelProperty(isUnique = true)
  private String name;

  @Getter
  @Setter
  @NonNull
  @ModelProperty
  private Location loc;

  @Getter
  @Setter
  @NonNull
  @ModelProperty
  private OfflinePlayer creator;
}
