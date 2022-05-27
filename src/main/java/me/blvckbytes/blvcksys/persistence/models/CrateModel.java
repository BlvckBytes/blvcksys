package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  Saves a crate and it's properties.
*/
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CrateModel extends APersistentModel {

  @ModelProperty
  private OfflinePlayer creator;

  @ModelProperty(isUnique = true)
  private String name;

  @ModelProperty(isNullable = true)
  private Location loc;

}
