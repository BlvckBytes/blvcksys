package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/18/2022

  Saves a sign which is rendered on a per-player basis, to
  allow for custom variables.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerSignModel extends APersistentModel {

  @ModelProperty
  private OfflinePlayer creator;

  @ModelProperty(isNullable = true)
  private OfflinePlayer lastEditor;

  @ModelProperty
  private Location loc;

  @ModelProperty
  private String line1, line2, line3, line4;

}
